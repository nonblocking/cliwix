package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model.{Organization, OrganizationMembers}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
class OrganizationHandlerIntegrationTest {

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

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1, user2)))
    org1.setCountryCode("AT")
    val subOrg1 = new Organization("org2")

    val insertedOrganization = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result
    val insertedSubOrganization = this.dispatchHandler
      .execute(OrganizationInsertCommand(insertedCompany.getCompanyId, subOrg1, insertedOrganization)).result

    assertNotNull(insertedOrganization)
    assertNotNull(insertedSubOrganization)
    assertTrue(this.liferayEntityComparator.equals(org1, insertedOrganization))
    assertTrue(this.liferayEntityComparator.equals(subOrg1, insertedSubOrganization))

    val organizations = this.dispatchHandler.execute(OrganizationListCommand(insertedCompany.getCompanyId)).result

    assertTrue(organizations.size == 1)
    assertTrue(this.liferayEntityComparator.equals(org1, organizations(0)))
    assertTrue(this.liferayEntityComparator.equals(subOrg1, organizations(0).getSubOrganizations()(0)))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    val user3 = createTestUser("user3")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user3))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1, user2, user3)))
    org1.setCountryCode("AT")
    val subOrg1 = new Organization("org2")

    val insertedOrganization = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result
    val insertedSubOrganization = this.dispatchHandler
      .execute(OrganizationInsertCommand(insertedCompany.getCompanyId, subOrg1, insertedOrganization)).result

    insertedOrganization.setCountryCode("DE")
    insertedOrganization.getOrganizationMembers.setMemberUsers(toMemberUsers(user1, user2))

    val updatedOrganization = this.dispatchHandler.execute(UpdateCommand(insertedOrganization)).result
    assertTrue(this.liferayEntityComparator.equals(insertedOrganization, updatedOrganization))

    val organizations = this.dispatchHandler.execute(OrganizationListCommand(insertedCompany.getCompanyId)).result

    assertTrue(organizations.size == 1)
    assertTrue(this.liferayEntityComparator.equals(insertedOrganization, organizations(0)))
    assertTrue(organizations(0).getSubOrganizations.size() == 1)
    assertTrue(this.liferayEntityComparator.equals(insertedSubOrganization, organizations(0).getSubOrganizations()(0)))
  }

  @Test
  @TransactionalRollback
  def getByIdTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val org1 = new Organization("org1")
    org1.setCountryCode("AT")
    val subOrg1 = new Organization("org2")

    val insertedOrganization = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result
    val insertedSubOrganization = this.dispatchHandler
      .execute(OrganizationInsertCommand(insertedCompany.getCompanyId, subOrg1, insertedOrganization)).result

    val org = this.dispatchHandler.execute(GetByDBIdCommand(insertedSubOrganization.getOrganizationId, classOf[Organization])).result

    assertNotNull(org)
    assertTrue(this.liferayEntityComparator.equals(insertedSubOrganization, org))
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val org1 = new Organization("org1")
    org1.setCountryCode("AT")
    val subOrg1 = new Organization("org2")

    val insertedOrganization = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result
    val insertedSubOrganization = this.dispatchHandler
      .execute(OrganizationInsertCommand(insertedCompany.getCompanyId, subOrg1, insertedOrganization)).result

    val org = this.dispatchHandler.execute(GetByIdentifierOrPathCommand(insertedSubOrganization.identifiedBy(), insertedCompany.getCompanyId, classOf[Organization])).result

    assertNotNull(org)
    assertTrue(this.liferayEntityComparator.equals(insertedSubOrganization, org))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user1 = createTestUser("user1")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))

    val org1 = new Organization("org1", new OrganizationMembers(toMemberUsers(user1)))
    org1.setCountryCode("AT")
    val subOrg1 = new Organization("org2")

    val insertedOrganization = this.dispatchHandler.execute(OrganizationInsertCommand(insertedCompany.getCompanyId, org1, null)).result
    val insertedSubOrganization = this.dispatchHandler
      .execute(OrganizationInsertCommand(insertedCompany.getCompanyId, subOrg1, insertedOrganization)).result

    this.dispatchHandler.execute(DeleteCommand(insertedSubOrganization))

    val organizations = this.dispatchHandler.execute(OrganizationListCommand(insertedCompany.getCompanyId)).result

    assertTrue(organizations.size == 1)
    assertTrue(organizations(0).getSubOrganizations == null)
  }

}
