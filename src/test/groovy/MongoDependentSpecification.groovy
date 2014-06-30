import com.gmongo.GMongo
import com.mongodb.DB
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory
import spock.lang.Specification

/**
 * This class exposes a fresh mongo instance for each test
 */
class MongoDependentSpecification extends Specification {

  private static final MongodForTestsFactory mongoFactory =
          MongodForTestsFactory.with(Version.Main.PRODUCTION)

  static {
    mongoFactory.addShutdownHook { mongoFactory.shutdown() }
  }

  DB db

  void setup() {
    def mongo = new GMongo(mongoFactory.newMongo())
    db = mongo.getDB(UUID.randomUUID().toString())
  }

}
