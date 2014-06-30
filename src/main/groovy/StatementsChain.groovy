import com.google.inject.Inject
import com.mongodb.BasicDBObject
import com.mongodb.DB
import groovy.util.slurpersupport.GPathResult
import javassist.NotFoundException
import javassist.tools.web.BadHttpRequest
import ratpack.groovy.handling.GroovyChainAction

import java.text.SimpleDateFormat

/**
 * This micro service is responsible for importing bank statements into the database
 */
@com.google.inject.Singleton
class StatementsChain extends GroovyChainAction {

  final DB db

  @Inject
  StatementsChain(final DB db) {
    this.db = db
  }

  @Override
  protected void execute() throws Exception {
    /**
     * Imports a new statement into the database
     */
    post {
      def xmlStream = request.body.inputStream
      def xml = new XmlSlurper().parse(xmlStream)
      def statement = xml.BkToCstmrStmt.Stmt
      def statementId = statement.Id.text()

      blocking {
        def found = db.statements.findOne(new BasicDBObject('_id', statementId)) != null
        if (found) {
          throw new BadHttpRequest()
        }
        def entries = statement.Ntry.collect { GPathResult ntry ->
          [
                  stmtId     : statementId,
                  date       : new SimpleDateFormat('yyyy-mm-dd').parse(ntry.BookgDt.Dt.toString()),
                  amount     : ntry.Amt.toDouble(),
                  description: ntry.AddtlNtryInf.text(),
                  dbitOrCrdt : ntry.CdtDbtInd.text()
          ]
        }
        db.transactions << entries
        db.statements << ["_id": statementId, "imported": Calendar.instance.time]
      } onError { e ->
        if (!(e instanceof BadHttpRequest)) {
          throw e
        }
        response.status 400
        response.send "Statement with ID = ${statementId} already exists."
      } then {
        response.status 201
        response.send "Statement with ID = ${statementId} imported."
      }
    }

    /**
     * Deletes a statement and its related transactions
     */
    delete(':id') {
      def stmtId = allPathTokens['id']

      blocking {
        def result = db.statements.remove(new BasicDBObject('_id': stmtId))
        if (result.n == 0) {
          throw new NotFoundException("Statement '${stmtId}' doesn't exist.")
        }
        db.transactions.remove(new BasicDBObject('stmtId': stmtId))
      } onError { e ->
        if (!(e instanceof NotFoundException)) {
          throw e
        }
        response.status 404
        response.send e.message
      } then {
        response.status 200
        response.send "Statement ${stmtId} and related transactions deleted."
      }
    }
  }

}
