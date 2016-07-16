package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model._
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
class PortletConfigurationHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  @Test
  @TransactionalRollback
  def insertTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setPageOrder(0)
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    val pc = new PortletConfiguration("23", List(new Preference("foo", "bar"), new Preference("foo2", List("a", "b", "c"))))

    val insertedPC = this.dispatchHandler.execute(PortletConfigurationInsertCommand(insertedPage.getPortletLayoutId, pc)).result

    assertNotNull(insertedPC)
    assertTrue(this.liferayEntityComparator.equals(pc, insertedPC))

    val configurationList = dispatchHandler.execute(PortletConfigurationListCommand(insertedPage.getPortletLayoutId)).result

    assertTrue(configurationList.contains(pc.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(pc, configurationList.get(pc.identifiedBy())))
    assertEquals("a", configurationList.get(pc.identifiedBy()).getPreferences.find(_.getName == "foo2").get.getValues()(0))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setPageOrder(0)
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val pc = new PortletConfiguration("23", List(new Preference("foo", "bar"), new Preference("foo2", List("a", "b", "c"))))

    val insertedPC = this.dispatchHandler.execute(PortletConfigurationInsertCommand(insertedPage.getPortletLayoutId, pc)).result

    insertedPC.setPreferences(List(new Preference("A", "A1"), new Preference("B", "B1")))

    val updatedPC = this.dispatchHandler.execute(UpdateCommand(insertedPC)).result

    assertTrue(this.liferayEntityComparator.equals(insertedPC, updatedPC))

    val configurationList = dispatchHandler.execute(PortletConfigurationListCommand(insertedPage.getPortletLayoutId)).result

    assertTrue(configurationList.contains(pc.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(updatedPC, configurationList.get(pc.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setPageOrder(0)
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val pc = new PortletConfiguration("23", List(new Preference("foo", "bar"), new Preference("foo2", List("a", "b", "c"))))

    val insertedPC = this.dispatchHandler.execute(PortletConfigurationInsertCommand(insertedPage.getPortletLayoutId, pc)).result

    this.dispatchHandler.execute(DeleteCommand(insertedPC))

    val configurationList = dispatchHandler.execute(PortletConfigurationListCommand(insertedPage.getPortletLayoutId)).result

    assertFalse(configurationList.contains(pc.identifiedBy()))
  }

}
