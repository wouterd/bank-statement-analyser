import com.google.inject.Inject
import com.mongodb.BasicDBObject
import com.mongodb.DB
import com.mongodb.DBObject
import com.mongodb.util.JSON
import org.bson.types.ObjectId
import ratpack.groovy.handling.GroovyChainAction

/**
 * Created by wouter on 28-06-14.
 */
@com.google.inject.Singleton
class TransactionsChain extends GroovyChainAction {

  final DB db

  @Inject
  TransactionsChain(final DB db) {
    this.db = db
  }

  @Override
  protected void execute() throws Exception {

    get 'untagged', {
      blocking {
        db.transactions.findOne(
                new BasicDBObject(['tag': ['$exists': false]]),
                new BasicDBObject(),
                new BasicDBObject(['date': -1]))
      } then { transaction ->
        render JSON.serialize(transaction)
      }

    }

    handler ':id', {
      byMethod {
        get {
          def id = allPathTokens['id']

          def isValidId = ObjectId.isValid(id)

          if (!isValidId) {
            response.status 404
            response.send "Invalid Object Id (${id})"
            return
          }

          blocking {
            db.transactions.findOne(new BasicDBObject('_id', new ObjectId(id)))
          } then { transaction ->
            if (transaction == null) {
              response.status 404
              response.send "Transaction with ID = '${id}' doesn't exist"
              return
            }
            render JSON.serialize(transaction)
          }
        }

        put {
          def id = allPathTokens['id']

          def update = JSON.parse(request.body.text) as DBObject

          blocking {
            db.transactions.update(new BasicDBObject('_id', new ObjectId(id)), update)
          } then { result ->
            if (result.n == 0) {
              response.status 404
              response.send()
              return
            }

            response.status 201
            render 'Document updated'
          }
        }
      }
    }
  }
}
