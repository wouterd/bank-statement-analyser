import com.gmongo.GMongo
import com.mongodb.DB

import static ratpack.groovy.Groovy.ratpack

ratpack {
  bindings {
    def mongo = new GMongo('localhost')
    def db = mongo.getDB('monies')
    bind(DB.class, db)
  }

  handlers {
    prefix 'statements', registry.get(StatementsChain.class)
    prefix 'transactions', registry.get(TransactionsChain.class)
  }
}
