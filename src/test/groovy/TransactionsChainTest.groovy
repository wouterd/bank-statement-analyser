import groovy.json.JsonSlurper
import ratpack.test.handling.HandlingResult

import static ratpack.groovy.test.GroovyUnitTest.handle

class TransactionsChainTest extends MongoDependentSpecification {

  def "Returns an untagged transaction"() {
    when:
    loadTestSetInDatabase()

    def result = requestUntaggedTransaction()

    then:
    assert result.status.code == 200
    def object = new JsonSlurper().parse(result.rendered(String).toCharArray())
    assert object['tag'] == null
  }

  def "Returns the most recent untagged transaction"() {
    when:
    loadTestSetInDatabase()
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

  private HandlingResult requestUntaggedTransaction() {
    handle new TransactionsChain(db), {
      uri 'untagged'
      method 'get'
    }
  }

  private void loadTestSetInDatabase() {
    db.transactions << [
            [tag: 'someTag', date: new Date(2014, 04, 12)],
            [dbitOrCrdt: 'DBIT', date: new Date(2013, 05, 06)],
            [stmtId: 'abc', date: new Date(2014, 06, 12)],
            [tag: 'tag!', date: new Date(2014, 03, 12)]
    ]
  }
}
