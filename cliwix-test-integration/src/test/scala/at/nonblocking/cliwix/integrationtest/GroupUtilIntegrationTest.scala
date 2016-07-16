package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command.{CompanyInsertCommand, SiteInsertCommand}
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model._
import org.junit.Assert._
import org.junit._
import org.junit.runner.RunWith

import scala.beans.BeanProperty

@RunWith(classOf[CliwixIntegrationTestRunner])
class GroupUtilIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var groupUtil: GroupUtil = _

  @Test
  @TransactionalRollback
  def testSiteGroup() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()
    site.getSiteConfiguration.setDescription("My test site")

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val groupEntityAndId = this.groupUtil.getLiferayEntityForGroupId(insertedSite.getSiteId)

    assertEquals(classOf[Site], groupEntityAndId.get._1)
    assertEquals(insertedSite.getSiteId, groupEntityAndId.get._2)
  }


}
