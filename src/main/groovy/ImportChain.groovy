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
class ImportChain extends GroovyChainAction {

  final DB db

  @Inject
  ImportChain(final DB db) {
    this.db = db
  }

  @Override
  protected void execute() throws Exception {

    post {
      def xmlStream = request.body.inputStream
      def xml = new XmlSlurper().parse(xmlStream)
      def statement = xml.BkToCstmrStmt.Stmt
      def statementId = statement.Id.text()

      def find = db.statements.findOne(new BasicDBObject('_id', statementId))
      def statementFound = find != null

      if (statementFound) {
        response.status(400)
        response.send('Statement has already been imported')
        return
      }

      def entries = statement.Ntry.collect { GPathResult ntry ->
        [
                stmtId     : statementId,
                date       : new SimpleDateFormat('yyyy-mm-dd').parse(ntry.BookgDt.Dt.toString()),
                amount     : ntry.Amt.toDouble(),
                description: ntry.AddtlNtryInf.text()
        ]
      }

      db.transactions << entries
      db.statements << ["_id": statementId, "imported": Calendar.instance.time]

      response.status 201
      response.send()
    }
  }

}
