import com.google.inject.Inject
import com.mongodb.BasicDBObject
import com.mongodb.DB
import com.mongodb.util.JSON
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
                new BasicDBObject([tag: [$exists: false]]),
                new BasicDBObject(),
                new BasicDBObject([date: -1])
        )
      } then { transaction ->
        if (transaction == null) {
          response.status 404, 'No untagged transaction available'
          response.send()
        } else {
          render JSON.serialize(transaction)
        }
      }

    }

  }
}
