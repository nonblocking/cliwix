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
class OrganizationRoleAssignmentHandlerIntegrationTest {

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

    val role1 = new Role("TEST_ROLE1", ROLE_TYPE.ORGANIZATION)
    val role2 = new Role("TEST_ROLE2", ROLE_TYPE.ORGANIZATION)
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    val insertedOrg1 = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result

    val orgRoleAssignment = new OrganizationRoleAssignment("TEST_ROLE1")
    orgRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(OrganizationRoleAssignmentInsertCommand(insertedOrg1.getOrganizationId, orgRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)
    assertTrue(this.liferayEntityComparator.equals(orgRoleAssignment, insertedRoleAssignment))

    val roleAssignments = this.dispatchHandler.execute(OrganizationRoleAssignmentListCommand(insertedOrg1.getOrganizationId)).result

    assertNotNull(roleAssignments)
    assertTrue(roleAssignments.contains(orgRoleAssignment.identifiedBy()))

    assertTrue(this.liferayEntityComparator.equals(orgRoleAssignment, roleAssignments.get(orgRoleAssignment.identifiedBy())))
  }

  @Test(expected = classOf[CliwixCommandExecutionException])
  @TransactionalRollback
  def invalidRoleTypeTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1", ROLE_TYPE.SITE)
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    val insertedOrg1 = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result

    val orgRoleAssignment = new OrganizationRoleAssignment("TEST_ROLE1")
    orgRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    this.dispatchHandler.execute(OrganizationRoleAssignmentInsertCommand(insertedOrg1.getOrganizationId, orgRoleAssignment))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1", ROLE_TYPE.ORGANIZATION)
    val role2 = new Role("TEST_ROLE2", ROLE_TYPE.ORGANIZATION)
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    val user3 = createTestUser("user3")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user3))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    val insertedOrg1 = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result

    val orgRoleAssignment = new OrganizationRoleAssignment("TEST_ROLE1")
    orgRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(OrganizationRoleAssignmentInsertCommand(insertedOrg1.getOrganizationId, orgRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)
    assertTrue(this.liferayEntityComparator.equals(orgRoleAssignment, insertedRoleAssignment))

    insertedRoleAssignment.setMemberUsers(toMemberUsers(user1, user3))

    val updatedRoleAssignments = this.dispatchHandler.execute(UpdateCommand(insertedRoleAssignment)).result

    assertNotNull(updatedRoleAssignments)
    assertTrue(this.liferayEntityComparator.equals(insertedRoleAssignment, updatedRoleAssignments))

    val roleAssignments = this.dispatchHandler.execute(OrganizationRoleAssignmentListCommand(insertedOrg1.getOrganizationId)).result

    assertNotNull(roleAssignments)
    assertTrue(roleAssignments.contains(orgRoleAssignment.identifiedBy()))

    assertTrue(this.liferayEntityComparator.equals(insertedRoleAssignment, roleAssignments.get(orgRoleAssignment.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1", ROLE_TYPE.ORGANIZATION)
    val role2 = new Role("TEST_ROLE2", ROLE_TYPE.ORGANIZATION)
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1))
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2))

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    val insertedOrg1 = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result

    val orgRoleAssignment = new OrganizationRoleAssignment("TEST_ROLE1")
    orgRoleAssignment.setMemberUsers(toMemberUsers(user1, user2))

    val insertedRoleAssignment = this.dispatchHandler.execute(OrganizationRoleAssignmentInsertCommand(insertedOrg1.getOrganizationId, orgRoleAssignment)).result

    assertNotNull(insertedRoleAssignment)

    val roleAssignments1 = this.dispatchHandler.execute(OrganizationRoleAssignmentListCommand(insertedOrg1.getOrganizationId)).result
    assertTrue(roleAssignments1.contains(orgRoleAssignment.identifiedBy()))

    this.dispatchHandler.execute(DeleteCommand(insertedRoleAssignment))

    val roleAssignments2 = this.dispatchHandler.execute(OrganizationRoleAssignmentListCommand(insertedOrg1.getOrganizationId)).result
    assertFalse(roleAssignments2.contains(orgRoleAssignment.identifiedBy()))
  }

}
