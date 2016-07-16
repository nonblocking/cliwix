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
class RegularRoleAssignmentHandlerIntegrationTest {

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

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null))

    val regularRoleAssignment = new RegularRoleAssignment("TEST_ROLE1")
    regularRoleAssignment.setMemberOrganizations(toMemberOrganizations(org1))
    regularRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup1))
    regularRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(RegularRoleAssignmentInsertCommand(insertedCompany.getCompanyId, regularRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)
    assertTrue(this.liferayEntityComparator.equals(regularRoleAssignment, insertedRoleAssignment))

    val roleAssignments = this.dispatchHandler.execute(RegularRoleAssignmentListCommand(insertedCompany.getCompanyId)).result

    assertNotNull(roleAssignments)
    assertTrue(roleAssignments.contains(regularRoleAssignment.identifiedBy()))

    assertTrue(this.liferayEntityComparator.equals(regularRoleAssignment, roleAssignments.get(regularRoleAssignment.identifiedBy())))
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

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null))

    val regularRoleAssignment = new RegularRoleAssignment("TEST_ROLE1")
    regularRoleAssignment.setMemberOrganizations(toMemberOrganizations(org1))
    regularRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup1))
    regularRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    this.dispatchHandler.execute(RegularRoleAssignmentInsertCommand(insertedCompany.getCompanyId, regularRoleAssignment))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")
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

    val userGroup2 = new UserGroup("USER_GROUP2", "Test User Group 2", toMemberUsers(user2))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup2))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null))

    val regularRoleAssignment = new RegularRoleAssignment("TEST_ROLE1")
    regularRoleAssignment.setMemberOrganizations(toMemberOrganizations(org1))
    regularRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup1))
    regularRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(RegularRoleAssignmentInsertCommand(insertedCompany.getCompanyId, regularRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)
    assertTrue(this.liferayEntityComparator.equals(regularRoleAssignment, insertedRoleAssignment))

    insertedRoleAssignment.setMemberUsers(toMemberUsers(user1, user3))
    insertedRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup2))
    insertedRoleAssignment.setMemberOrganizations(null)

    val updatedRoleAssignments = this.dispatchHandler.execute(UpdateCommand(insertedRoleAssignment)).result

    assertNotNull(updatedRoleAssignments)
    assertTrue(this.liferayEntityComparator.equals(insertedRoleAssignment, updatedRoleAssignments))

    val roleAssignments = this.dispatchHandler.execute(RegularRoleAssignmentListCommand(insertedCompany.getCompanyId)).result

    assertNotNull(roleAssignments)
    assertTrue(roleAssignments.contains(regularRoleAssignment.identifiedBy()))

    assertTrue(this.liferayEntityComparator.equals(insertedRoleAssignment, roleAssignments.get(regularRoleAssignment.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null))

    val regularRoleAssignment = new RegularRoleAssignment("TEST_ROLE1")
    regularRoleAssignment.setMemberOrganizations(toMemberOrganizations(org1))
    regularRoleAssignment.setMemberUserGroups(toMemberUserGroups(userGroup1))
    regularRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(RegularRoleAssignmentInsertCommand(insertedCompany.getCompanyId, regularRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)

    val roleAssignments1 = this.dispatchHandler.execute(RegularRoleAssignmentListCommand(insertedCompany.getCompanyId)).result
    assertTrue(roleAssignments1.contains(regularRoleAssignment.identifiedBy()))

    this.dispatchHandler.execute(DeleteCommand(insertedRoleAssignment))

    val roleAssignments2 = this.dispatchHandler.execute(RegularRoleAssignmentListCommand(insertedCompany.getCompanyId)).result
    assertFalse(roleAssignments2.contains(regularRoleAssignment.identifiedBy()))
  }

}
