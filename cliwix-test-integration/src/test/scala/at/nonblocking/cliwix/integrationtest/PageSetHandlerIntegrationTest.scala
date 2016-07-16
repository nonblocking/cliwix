package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command.{CompanyInsertCommand, PageSetReadCommand, SiteInsertCommand, _}
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty

@RunWith(classOf[CliwixIntegrationTestRunner])
class PageSetHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  @Test
  @TransactionalRollback
  def updatePublicPageSetTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    //The page set is implicitly crated by SiteInsertCommand
    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result
    publicPageSet.setDefaultThemeId("classic")
    publicPageSet.setDefaultColorSchemeId("02")

    val updatedPublicPageSet = this.dispatchHandler.execute(UpdateCommand(publicPageSet)).result

    assertTrue(this.liferayEntityComparator.equals(publicPageSet, updatedPublicPageSet))

    val publicPageSetFromDb = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result
    assertTrue(this.liferayEntityComparator.equals(publicPageSet, publicPageSetFromDb))
  }

  @Test
  @TransactionalRollback
  def updatePrivatePageSetTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    //The page set is implicitly crated by SiteInsertCommand
    val privatePageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = true)).result
    privatePageSet.setDefaultThemeId("classic")
    privatePageSet.setDefaultColorSchemeId("02")

    val updatedPrivatePageSet = this.dispatchHandler.execute(UpdateCommand(privatePageSet)).result

    assertTrue(this.liferayEntityComparator.equals(privatePageSet, updatedPrivatePageSet))

    val privatePageSetFromDb = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = true)).result
    assertTrue(this.liferayEntityComparator.equals(privatePageSet, privatePageSetFromDb))
  }

  @Test
  @TransactionalRollback
  def deletePrivatePageSetTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    //The page set is implicitly crated by SiteInsertCommand
    val privatePageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = true)).result

    this.dispatchHandler.execute(DeleteCommand(privatePageSet))

    val ps = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = true)).result
    assertNull(ps)
  }

}
