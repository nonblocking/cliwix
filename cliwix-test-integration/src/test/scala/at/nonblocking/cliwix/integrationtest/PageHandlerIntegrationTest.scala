package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command.{CompanyInsertCommand, PageListCommand, SiteInsertCommand, _}
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model._
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
class PageHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  @Test
  @TransactionalRollback
  def insertPageTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))
    page.setKeywordsList(List(new LocalizedTextContent("en_US", "first,test")))
    page.setRobotsList(List(new LocalizedTextContent("en_US", "*")))
    page.setThemeId("mytheme")
    page.setColorSchemeId("00")
    page.setHidden(false)
    page.setHtmlTitles(List(new LocalizedTextContent("de_DE", "Test Page 1")))
    page.setPageSettings(List(new PageSetting("foo1", "bar1"), new PageSetting("foo2", "bar2")))
    page.setPageOrder(1)

    val subPage = new Page(PAGE_TYPE.PORTLET, "/test1_1/bar", List(new LocalizedTextContent("de_DE", "Test1_1")))
    subPage.setDescriptions(List(new LocalizedTextContent("en_US", "First sub page")))
    subPage.setPageOrder(11)

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result
    val insertedSubPage = this.dispatchHandler.execute(PageInsertCommand(subPage, insertedPage, publicPageSet)).result

    assertNotNull(insertedPage)
    assertTrue(this.liferayEntityComparator.equals(page, insertedPage))
    assertEquals("publicPages:/test1", insertedPage.getPath)
    assertTrue(insertedPage.getOwnerGroupId > 0)
    assertNotNull(insertedSubPage)
    assertTrue(this.liferayEntityComparator.equals(subPage, insertedSubPage))
    assertEquals("publicPages:/test1_1/bar", insertedSubPage.getPath)
    assertTrue(insertedSubPage.getOwnerGroupId > 0)

    val pageList = dispatchHandler.execute(PageListCommand(insertedSite.getSiteId, privatePages = false)).result

    assertTrue(pageList.exists(_.identifiedBy() == page.identifiedBy()))
    val pageFromDb = pageList.find(_.identifiedBy() == page.identifiedBy()).get
    assertTrue(this.liferayEntityComparator.equals(page, pageFromDb))
    assertTrue(this.liferayEntityComparator.equals(subPage, pageFromDb.getSubPages()(0)))
  }

  @Test
  @TransactionalRollback
  def updatePageTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))
    page.setKeywordsList(List(new LocalizedTextContent("en_US", "first,test")))
    page.setRobotsList(List(new LocalizedTextContent("en_US", "*")))
    page.setThemeId("mytheme")
    page.setColorSchemeId("00")
    page.setHidden(false)
    page.setHtmlTitles(List(new LocalizedTextContent("de_DE", "Test Page 1")))
    page.setPageSettings(List(new PageSetting("foo1", "bar1"), new PageSetting("foo2", "bar2")))
    page.setPageOrder(2)

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    assertTrue(this.liferayEntityComparator.equals(page, insertedPage))
    page.setDescriptions(List(new LocalizedTextContent("en_US", "Updated page")))
    insertedPage.setNames(List(new LocalizedTextContent("de_DE", "Test1 aktualisiert"), new LocalizedTextContent("en_US", "Test1 updated")))

    val updatedPage = this.dispatchHandler.execute(UpdateCommand(insertedPage)).result

    assertTrue(this.liferayEntityComparator.equals(insertedPage, updatedPage))
    assertEquals("publicPages:/test1", updatedPage.getPath)
    assertTrue(updatedPage.getOwnerGroupId > 0)

    val pageList = dispatchHandler.execute(PageListCommand(insertedSite.getSiteId, privatePages = false)).result

    assertTrue(pageList.exists(_.identifiedBy() == updatedPage.identifiedBy()))
    val pageFromDb = pageList.find(_.identifiedBy() == updatedPage.identifiedBy()).get
    assertTrue(this.liferayEntityComparator.equals(updatedPage, pageFromDb))
  }

  @Test
  @TransactionalRollback
  def insertPageWithExistingFriendlyURLTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))
    page.setKeywordsList(List(new LocalizedTextContent("en_US", "first,test")))
    page.setRobotsList(List(new LocalizedTextContent("en_US", "*")))
    page.setThemeId("mytheme")
    page.setColorSchemeId("00")
    page.setHidden(false)
    page.setHtmlTitles(List(new LocalizedTextContent("de_DE", "Test Page 1")))
    page.setPageSettings(List(new PageSetting("foo1", "bar1"), new PageSetting("foo2", "bar2")))
    page.setPageOrder(2)

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    assertTrue(this.liferayEntityComparator.equals(page, insertedPage))
    page.setDescriptions(List(new LocalizedTextContent("en_US", "Updated page")))
    insertedPage.setNames(List(new LocalizedTextContent("de_DE", "Test1 aktualisiert"), new LocalizedTextContent("en_US", "Test1 updated")))

    val insertedPage2 = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    assertFalse(this.liferayEntityComparator.equals(insertedPage, insertedPage2))
    assertEquals("publicPages:/test11", insertedPage2.getPath)

    val pageList = dispatchHandler.execute(PageListCommand(insertedSite.getSiteId, privatePages = false)).result


    assertTrue(pageList.exists(_.identifiedBy() == insertedPage.identifiedBy()))
    assertTrue(pageList.exists(_.identifiedBy() == insertedPage2.identifiedBy()))
  }

  @Test
  @TransactionalRollback
  def getByIdTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setPageOrder(0)

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    val p = this.dispatchHandler.execute(GetByDBIdCommand(insertedPage.getPortletLayoutId, classOf[Page])).result

    assertNotNull(p)
    assertTrue(this.liferayEntityComparator.equals(p, insertedPage))
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))
    page.setPageOrder(0)

    val subPage = new Page(PAGE_TYPE.PORTLET, "/test1_1", List(new LocalizedTextContent("de_DE", "Test1_1")))
    subPage.setDescriptions(List(new LocalizedTextContent("en_US", "First sub page")))
    subPage.setPageOrder(0)

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result
    val insertedSubPage = this.dispatchHandler.execute(PageInsertCommand(subPage, insertedPage, publicPageSet)).result

    val p = this.dispatchHandler.execute(GetByIdentifierOrPathWithinGroupCommand(insertedSubPage.getPath, insertedCompany.getCompanyId, insertedSite.getSiteId, classOf[Page])).result

    assertNotNull(p)
    assertEquals("publicPages:/test1_1", p.getPath)
    assertTrue(this.liferayEntityComparator.equals(p, insertedSubPage))
  }

  @Test
  @TransactionalRollback
  def deletePageTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))
    page.setPageOrder(0)

    val subPage = new Page(PAGE_TYPE.PORTLET, "/test1_1", List(new LocalizedTextContent("de_DE", "Test1_1")))
    subPage.setDescriptions(List(new LocalizedTextContent("en_US", "First sub page")))
    subPage.setPageOrder(0)

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result
    val insertedSubPage = this.dispatchHandler.execute(PageInsertCommand(subPage, insertedPage, publicPageSet)).result

    this.dispatchHandler.execute(DeleteCommand(insertedSubPage))

    val pageList = dispatchHandler.execute(PageListCommand(insertedSite.getSiteId, privatePages = false)).result

    val pageFromDb = pageList.find(_.identifiedBy() == page.identifiedBy()).get
    assertTrue(pageFromDb.getSubPages == null)
  }

}
