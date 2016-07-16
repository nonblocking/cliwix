package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.{ExecutionContext, CliwixCommandExecutionException}
import at.nonblocking.cliwix.core.command._
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
class SiteRoleAssignmentHandlerIntegrationTest {

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

    val role1 = new Role("TEST_ROLE1", ROLE_TYPE.SITE)
    val role2 = new Role("TEST_ROLE2", ROLE_TYPE.SITE)
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))

    val site = createTestSiteWithMembers(List(userGroup1.getName), List(user1.getScreenName, user2.getScreenName), Nil)
    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val siteRoleAssignment = new SiteRoleAssignment("TEST_ROLE1")
    siteRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup1))
    siteRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(SiteRoleAssignmentInsertCommand(insertedSite.getSiteId, siteRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)
    assertTrue(this.liferayEntityComparator.equals(siteRoleAssignment, insertedRoleAssignment))

    val roleAssignments = this.dispatchHandler.execute(SiteRoleAssignmentListCommand(insertedSite.getSiteId)).result

    assertNotNull(roleAssignments)
    assertTrue(roleAssignments.contains(siteRoleAssignment.identifiedBy()))

    assertTrue(this.liferayEntityComparator.equals(siteRoleAssignment, roleAssignments.get(siteRoleAssignment.identifiedBy())))
  }

  @Test(expected = classOf[CliwixCommandExecutionException])
  @TransactionalRollback
  def invalidRoleTypeTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1", ROLE_TYPE.ORGANIZATION)
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))

    val site = createTestSiteWithMembers(List(userGroup1.getName), List(user1.getScreenName, user2.getScreenName), Nil)
    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val siteRoleAssignment = new SiteRoleAssignment("TEST_ROLE1")
    siteRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup1))
    siteRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    this.dispatchHandler.execute(SiteRoleAssignmentInsertCommand(insertedSite.getSiteId, siteRoleAssignment))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1", ROLE_TYPE.SITE)
    val role2 = new Role("TEST_ROLE2", ROLE_TYPE.SITE)
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    val user3 = createTestUser("user3")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user3))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))

    val site = createTestSiteWithMembers(List(userGroup1.getName), List(user1.getScreenName, user2.getScreenName), Nil)
    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val siteRoleAssignment = new SiteRoleAssignment("TEST_ROLE1")
    siteRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup1))
    siteRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(SiteRoleAssignmentInsertCommand(insertedSite.getSiteId, siteRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)
    assertTrue(this.liferayEntityComparator.equals(siteRoleAssignment, insertedRoleAssignment))

    siteRoleAssignment.setMemberUsers(toMemberUsers(user1, user3))
    siteRoleAssignment.setMemberUserGroups(null)

    val updatedRoleAssignments = this.dispatchHandler.execute(UpdateCommand(insertedRoleAssignment)).result

    assertNotNull(updatedRoleAssignments)
    assertTrue(this.liferayEntityComparator.equals(insertedRoleAssignment, updatedRoleAssignments))

    val roleAssignments = this.dispatchHandler.execute(SiteRoleAssignmentListCommand(insertedSite.getSiteId)).result

    assertNotNull(roleAssignments)
    assertTrue(roleAssignments.contains(siteRoleAssignment.identifiedBy()))

    assertTrue(this.liferayEntityComparator.equals(insertedRoleAssignment, roleAssignments.get(siteRoleAssignment.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1", ROLE_TYPE.SITE)
    val role2 = new Role("TEST_ROLE2", ROLE_TYPE.SITE)
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))

    val site = createTestSiteWithMembers(List(userGroup1.getName), List(user1.getScreenName, user2.getScreenName), Nil)
    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result

    val siteRoleAssignment = new SiteRoleAssignment("TEST_ROLE1")
    siteRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup1))
    siteRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(SiteRoleAssignmentInsertCommand(insertedSite.getSiteId, siteRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)

    val roleAssignments = this.dispatchHandler.execute(SiteRoleAssignmentListCommand(insertedSite.getSiteId)).result

    assertNotNull(roleAssignments)
    assertTrue(roleAssignments.contains(siteRoleAssignment.identifiedBy()))

    this.dispatchHandler.execute(DeleteCommand(insertedRoleAssignment))

    val roleAssignments2 = this.dispatchHandler.execute(SiteRoleAssignmentListCommand(insertedSite.getSiteId)).result
    assertFalse(roleAssignments2.contains(siteRoleAssignment.identifiedBy()))
  }

}
