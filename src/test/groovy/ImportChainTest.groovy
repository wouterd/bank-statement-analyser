import ratpack.test.handling.HandlingResult

import static ratpack.test.UnitTest.handle

class ImportChainTest extends MongoDependentSpecification {

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

  private HandlingResult importCamtFileWithSingleEntry() {
    def chain = new ImportChain(db)
    handle(chain, {
      def input = getClass().getResourceAsStream '/camt-file-with-single-entry.xml'
      it.body(input.bytes, 'application/xml')
      it.method('post')
    })
  }
}
