package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model.{PortalPreferences, Preference}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
class PortalPreferencesHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  @Test
  @TransactionalRollback
  def updateAndReadTest() {
    val company = createTestCompany()
    company.getCompanyConfiguration.setDefaultGreeting("Grüße dich!")
    company.getCompanyConfiguration.setHomeUrl("/web/test")

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val portalPreferences = dispatchHandler.execute(PortalPreferencesReadCommand(insertedCompany.getCompanyId)).result

    val newPortalPreferences = new PortalPreferences()
    newPortalPreferences.setPortalPreferencesId(portalPreferences.getPortalPreferencesId)
    newPortalPreferences.setPreferences(List(new Preference("locales", "de_DE,en_US"), new Preference("foo2", "bar2")))

    val updatedPortalPreferences = this.dispatchHandler.execute(UpdateCommand(newPortalPreferences)).result

    assertNotNull(updatedPortalPreferences)

    val portalPreferences2 = dispatchHandler.execute(PortalPreferencesReadCommand(insertedCompany.getCompanyId)).result

    assertTrue(this.liferayEntityComparator.equals(updatedPortalPreferences, portalPreferences2))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val portalPreferences = dispatchHandler.execute(PortalPreferencesReadCommand(insertedCompany.getCompanyId)).result

    dispatchHandler.execute(DeleteCommand(portalPreferences))

    val pp = dispatchHandler.execute(PortalPreferencesReadCommand(insertedCompany.getCompanyId)).result

    assertNull(pp)
  }


}
