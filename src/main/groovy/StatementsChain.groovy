import com.google.inject.Inject
import com.mongodb.BasicDBObject
import com.mongodb.DB
import groovy.util.slurpersupport.GPathResult
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

      blocking({
        def found = db.statements.findOne(new BasicDBObject('_id', statementId)) != null
        if (found) {
          throw new IllegalArgumentException("Statement with ID = ${statementId} already exists.")
        }
      }).onError({ e ->
        response.status 400
        response.send e.message
      }).then({
        blocking({
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
        }).then({
          response.status 201
          response.send()
        })
      })
    }

    /**
     * Deletes a statement and its related transactions
     */
    delete(':id') {
      def stmtId = allPathTokens['id']

      blocking({
        db.statements.remove(new BasicDBObject('_id': stmtId))
      }).then({ result ->
        if (result.n == 0) {
          println 'statement doesn\'t exist'
          response.status 404
          response.send("Statement '${stmtId}' doesn't exist.")
          return
        }

        blocking({
          db.transactions.remove(new BasicDBObject('stmtId': stmtId))
        }).then({
          response.status 200
          response.send()
        })
      })
    }
  }

}
