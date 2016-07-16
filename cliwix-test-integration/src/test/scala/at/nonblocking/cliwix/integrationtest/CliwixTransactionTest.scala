package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.CliwixCommandExecutionException
import at.nonblocking.cliwix.core.command.{CompanyInsertCommand, CompanyListCommand}
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.core.transaction.CliwixTransaction
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import com.liferay.portal.CompanyWebIdException
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty

@RunWith(classOf[CliwixIntegrationTestRunner])
class CliwixTransactionTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var transaction: CliwixTransaction = _

  @Test
  def rollbackTest() {
    val company = createTestCompany()

    transaction.executeWithinLiferayTransaction(readOnly = false, rollback = true) {
      this.dispatchHandler.execute(CompanyInsertCommand(company))
    }

    val companies = this.dispatchHandler.execute(new CompanyListCommand(true)).result

    assertFalse(companies.containsKey(company.identifiedBy()))
  }

  @Test
  def commitTest() {
    val company = createTestCompany()

    transaction.executeWithinLiferayTransaction(readOnly = false, rollback = false) {
      this.dispatchHandler.execute(CompanyInsertCommand(company))
    }

    val companies = this.dispatchHandler.execute(new CompanyListCommand(true)).result

    assertTrue(companies.containsKey(company.identifiedBy()))
  }

  @Test
  def errorTest() {
    val company = createTestCompany()

    try {
      transaction.executeWithinLiferayTransaction(readOnly = false, rollback = false) {
        this.dispatchHandler.execute(CompanyInsertCommand(company))
        this.dispatchHandler.execute(CompanyInsertCommand(company))
      }
    } catch {
      case e: CliwixCommandExecutionException =>
        assertTrue(e.getCause.isInstanceOf[CompanyWebIdException])
    }

    val companies = this.dispatchHandler.execute(new CompanyListCommand(true)).result

    assertFalse(companies.containsKey(company.identifiedBy()))
  }

}
