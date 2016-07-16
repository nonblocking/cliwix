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
class SiteHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  @Test
  @TransactionalRollback
  def insertSiteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user = createTestUser()
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user))
    val userGroup2 = new UserGroup("USER_GROUP2", "Test User Group 2", null)
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup2))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user)))
    this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result

    val site = createTestSiteWithMembers(List(userGroup1.getName), List(user.getScreenName), List(org1.getName))
    site.getSiteConfiguration.setVirtualHostPublicPages("public.nonblocking.at")
    site.getSiteConfiguration.setVirtualHostPrivatePages("private.nonblocking.at")

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    assertNotNull(insertedSite)
    assertTrue(this.liferayEntityComparator.equals(site, insertedSite))

    val siteList = dispatchHandler.execute(SiteListCommand(insertedCompany.getCompanyId, withConfiguration = true)).result

    assertTrue(siteList.contains(site.identifiedBy()))
    assertNotNull(siteList.get(site.identifiedBy()).getSiteConfiguration)
    assertTrue(this.liferayEntityComparator.equals(site, siteList.get(site.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def updateSiteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user = createTestUser()
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user))
    val userGroup2 = new UserGroup("USER_GROUP2", "Test User Group 2", null)
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup2))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user)))
    this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null))

    val site = createTestSiteWithMembers(List(userGroup1.getName), Nil, Nil)
    site.getSiteConfiguration.setVirtualHostPublicPages("public.nonblocking.at")
    site.getSiteConfiguration.setVirtualHostPrivatePages("private.nonblocking.at")

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    assertNotNull(insertedSite)
    assertTrue(this.liferayEntityComparator.equals(site, insertedSite))

    insertedSite.getSiteConfiguration.setDescription("Updated test site")
    insertedSite.getSiteMembers.setMemberUserGroups(List(new MemberUserGroup(userGroup2.getName)))
    insertedSite.getSiteConfiguration.setVirtualHostPrivatePages(null)

    val updatedSite = this.dispatchHandler.execute(UpdateCommand(insertedSite)).result

    assertNotNull(updatedSite)
    assertTrue(this.liferayEntityComparator.equals(insertedSite, updatedSite))

    val siteList = dispatchHandler.execute(SiteListCommand(insertedCompany.getCompanyId, withConfiguration = true)).result

    assertTrue(siteList.contains(site.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(insertedSite, siteList.get(site.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def updateSiteNoConfigurationTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user = createTestUser()
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user))
    val userGroup2 = new UserGroup("USER_GROUP2", "Test User Group 2", null)
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup2))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user)))
    this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null))

    val site = createTestSiteWithMembers(List(userGroup1.getName), Nil, Nil)

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    insertedSite.setSiteConfiguration(null)
    insertedSite.setSiteMembers(null)

    val updatedSite = this.dispatchHandler.execute(UpdateCommand(insertedSite)).result

    assertTrue(this.liferayEntityComparator.equals(site, updatedSite))
  }

  @Test
  @TransactionalRollback
  def getByIdTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val s = this.dispatchHandler.execute(GetByDBIdCommand(insertedSite.getDbId, classOf[Site])).result

    assertNotNull(s)
    assertTrue(this.liferayEntityComparator.equals(s, insertedSite))
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val s = this.dispatchHandler.execute(GetByIdentifierOrPathCommand(insertedSite.identifiedBy(), insertedCompany.getCompanyId, classOf[Site])).result

    assertNotNull(s)
    assertTrue(this.liferayEntityComparator.equals(s, insertedSite))
  }

  @Test
  @TransactionalRollback
  def deleteSiteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    this.dispatchHandler.execute(DeleteCommand(insertedSite))

    val siteList = dispatchHandler.execute(SiteListCommand(insertedCompany.getCompanyId, withConfiguration = true)).result

    assertFalse(siteList.contains(site.identifiedBy()))
  }
}
