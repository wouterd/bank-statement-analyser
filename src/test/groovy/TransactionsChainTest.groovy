import com.mongodb.util.JSON
import groovy.json.JsonSlurper
import ratpack.test.handling.HandlingResult

import static ratpack.groovy.test.GroovyUnitTest.handle

class TransactionsChainTest extends MongoDependentSpecification {
  private TransactionsChain chain

  @Override
  void setup() {
    chain = new TransactionsChain(db)
  }

  def "Returns an untagged transaction"() {
    given:
    loadTestSetInDatabase()

    when:
    def result = requestUntaggedTransaction()

    then:
    assert result.status.code == 200
    def object = new JsonSlurper().parse(result.rendered(String).toCharArray())
    println object
    assert object['tag'] == null
  }

  def "Returns the most recent untagged transaction"() {
    given:
    loadTestSetInDatabase()

    when:
    def result = requestUntaggedTransaction()

    then:
    assert result.status.code == 200
    def object = new JsonSlurper().parse(result.rendered(String).toCharArray())
    assert object['stmtId'] == 'abc'
  }

  def "Returns a 404 when no untagged transaction can be found"() {
    when:
    def result = requestUntaggedTransaction()

    then:
    assert result.status.code == 404
  }

  def "Tags a given transaction"() {
    given:
    loadTestSetInDatabase()

    when:
    def result = tagTransaction '2/tag', JSON.serialize([tag: 'leTag'])

    then:
    assert result.status.code == 200
    assert db.transactions.findOne([_id: '2'])['tag'] == 'leTag'
  }

  def "Returns a 404 when trying to tag an nonexistent transaction"() {
    given:
    loadTestSetInDatabase()

    when:
    def result = tagTransaction '5/tag', JSON.serialize([tag: 'a tag'])

    then:
    assert result.status.code == 404
  }

  def "Should return a Bad Request code when a non-json document passed"() {
    when:
    def result = tagTransaction '2/tag', 'something wrong'

    then:
    assert result.status.code == 400
  }

  private HandlingResult requestUntaggedTransaction() {
    handle chain, {
      uri 'untagged'
      method 'get'
    }
  }

  private HandlingResult tagTransaction(String path, String content) {
    handle chain, {
      uri path
      body content, 'application/json'
      method 'put'
    }
  }

  private void loadTestSetInDatabase() {
    db.transactions << [
            [_id: '1', tag: 'someTag', date: new Date(2014, 04, 12)],
            [_id: '2', dbitOrCrdt: 'DBIT', date: new Date(2013, 05, 06)],
            [_id: '3', stmtId: 'abc', date: new Date(2014, 06, 12)],
            [_id: '4', tag: 'tag!', date: new Date(2014, 03, 12)]
    ]
  }
}
