import com.mongodb.BasicDBObject
import ratpack.test.handling.HandlingResult

import static ratpack.test.UnitTest.handle

class StatementsChainTest extends MongoDependentSpecification {

  StatementsChain chain

  @Override
  void setup() {
    chain = new StatementsChain(db)
  }

  def 'Imports a bank statement into Mongo as Transactions'() {
    when:
    def result = importCamtFileWithSingleEntry()

    then:
    assert result.exception == null
    assert result.status.code == 201

    def object = db.getCollection('transactions').findOne()
    assert object != null
    assert object.get('stmtId') == '0479741549.2014-04-01'
    assert object.get('amount') == 16.51
    assert (object.get('description') as String).contains('Florius')
  }

  def 'Does not import a bank statement if it was already imported previously'() {
    when:
    importCamtFileWithSingleEntry()
    def result = importCamtFileWithSingleEntry()

    then:
    assert result.status.code == 400
  }

  def "Deletes a statement if it exists"() {
    when:
    importCamtFileWithSingleEntry()
    deleteStatement('0479741549.2014-04-01')

    then:
    def statement = db.statements.findOne(_id: '0479741549.2014-04-01')
    assert statement == null
    def transactions = db.transactions.find(new BasicDBObject('stmtId', '0479741549.2014-04-01'))
    assert transactions.count() == 0
  }

  def "Returns 404 not found when trying to delete unknown statement id"() {
    when:
    def result = deleteStatement('non-existing-id')

    then:
    assert result.status.code == 404
  }

  def "Only removes transactions with the given statement Id"() {
    when:
    db.statements << [_id: 'a']
    db.transactions << [
            [stmtId: 'a'],
            [stmtId: 'b'],
            [stmtId: 'a'],
            [stmtId: 'c'],
            [stmtId: 'a'],
            [stmtId: 'd']
    ]
    deleteStatement('a')

    then:
    assert db.transactions.find().count() == 3
  }

  def "Only remove the statement with the specified ID"() {
    when:
    db.statements << [
            [_id: 'a'],
            [_id: 'b'],
            [_id: 'c'],
            [_id: 'd']
    ]
    deleteStatement('c')

    then:
    assert db.statements.find().count() == 3
  }

  def "Can re-import a deleted statement"() {
    when:
    importCamtFileWithSingleEntry()
    deleteStatement('0479741549.2014-04-01')
    def result = importCamtFileWithSingleEntry()

    then:
    assert result.status.code == 201
    assert db.transactions.find().count() == 1
    assert db.statements.find().count() == 1
  }

  private HandlingResult deleteStatement(String stmtId) {
    handle(chain, {
      it.method('delete')
      it.uri(stmtId)
    })
  }

  private HandlingResult importCamtFileWithSingleEntry() {
    handle(chain, {
      def input = getClass().getResourceAsStream '/camt-file-with-single-entry.xml'
      it.body(input.bytes, 'application/xml')
      it.method('post')
    })
  }
}
