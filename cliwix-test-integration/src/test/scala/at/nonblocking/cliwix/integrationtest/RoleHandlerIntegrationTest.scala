package at.nonblocking.cliwix.integrationtest

import java.{util => jutil}

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.ExecutionContext._
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model.{LocalizedTextContent, ROLE_TYPE, Role}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
class RoleHandlerIntegrationTest {

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
    role1.setDescriptions(List(new LocalizedTextContent("en_US", "Test Role 1"), new LocalizedTextContent("de_DE", "Test Rolle 1")))
    role1.setTitles(List(new LocalizedTextContent("en_US", "Role 1"), new LocalizedTextContent("de_DE", "Rolle 1")))

    val role2 = new Role("TEST_ROLE2")
    role2.setDescriptions(List(new LocalizedTextContent("en_US", "Test Role 2"), new LocalizedTextContent("de_DE", "Test Rolle 2")))
    role2.setTitles(List(new LocalizedTextContent("en_US", "Role 2"), new LocalizedTextContent("de_DE", "Rolle 2")))

    val siteRole = new Role("SITE_TEST_ROLE1")
    siteRole.setTitles(List(new LocalizedTextContent("en_US", "Site Role 1"), new LocalizedTextContent("de_DE", "Site Rolle 1")))

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    val insertedRole2 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result
    val insertedSiteRole = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, siteRole)).result

    assertTrue(this.liferayEntityComparator.equals(role1, insertedRole1))
    assertTrue(this.liferayEntityComparator.equals(role2, insertedRole2))
    assertTrue(this.liferayEntityComparator.equals(siteRole, insertedSiteRole))

    val roleList = dispatchHandler.execute(RoleListCommand(insertedCompany.getCompanyId)).result

    assertTrue(roleList.contains(insertedRole1.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(role1, roleList.get(insertedRole1.identifiedBy())))
    assertTrue(roleList.contains(insertedRole2.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(role2, roleList.get(insertedRole2.identifiedBy())))
    assertTrue(roleList.contains(siteRole.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(siteRole, roleList.get(siteRole.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def updateTitlesTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    role1.setDescriptions(List(new LocalizedTextContent("en_US", "Test Role 1"), new LocalizedTextContent("de_DE", "Test Rolle 1")))
    role1.setTitles(List(new LocalizedTextContent("en_US", "Role 1"), new LocalizedTextContent("de_DE", "Rolle 1")))

    val siteRole = new Role("SITE_TEST_ROLE1")
    siteRole.setTitles(List(new LocalizedTextContent("en_US", "Site Role 1"), new LocalizedTextContent("de_DE", "Site Rolle 1")))

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    val insertedSiteRole = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, siteRole)).result

    val newTitleList = new jutil.ArrayList(insertedRole1.getTitles)
    newTitleList.add(new LocalizedTextContent("fr_FR", "Foo bar"))
    insertedRole1.setTitles(newTitleList)
    siteRole.setTitles(newTitleList)

    val updatedRole1 = this.dispatchHandler.execute(UpdateCommand(insertedRole1)).result
    val updatedSiteRole = this.dispatchHandler.execute(UpdateCommand(insertedSiteRole)).result

    assertTrue(this.liferayEntityComparator.equals(insertedRole1, updatedRole1))
    assertTrue(this.liferayEntityComparator.equals(insertedSiteRole, updatedSiteRole))
  }

  @Test
  @TransactionalRollback
  def updateTypeTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    role1.setDescriptions(List(new LocalizedTextContent("en_US", "Test Role 1"), new LocalizedTextContent("de_DE", "Test Rolle 1")))
    role1.setTitles(List(new LocalizedTextContent("en_US", "Role 1"), new LocalizedTextContent("de_DE", "Rolle 1")))

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result

    insertedRole1.setType(ROLE_TYPE.ORGANIZATION)

    val updatedRole1 = this.dispatchHandler.execute(UpdateCommand(insertedRole1)).result

    assertTrue(this.liferayEntityComparator.equals(insertedRole1, updatedRole1))
  }


  @Test
  @TransactionalRollback
  def getByIdTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    role1.setDescriptions(List(new LocalizedTextContent("en_US", "Test Role 1"), new LocalizedTextContent("de_DE", "Test Rolle 1")))
    role1.setTitles(List(new LocalizedTextContent("en_US", "Role 1"), new LocalizedTextContent("de_DE", "Rolle 1")))

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result

    val r = this.dispatchHandler.execute(GetByDBIdCommand(insertedRole1.getRoleId, classOf[Role])).result

    assertNotNull(r)
    assertTrue(this.liferayEntityComparator.equals(insertedRole1, r))
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    role1.setDescriptions(List(new LocalizedTextContent("en_US", "Test Role 1"), new LocalizedTextContent("de_DE", "Test Rolle 1")))
    role1.setTitles(List(new LocalizedTextContent("en_US", "Role 1"), new LocalizedTextContent("de_DE", "Rolle 1")))

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result

    val r = this.dispatchHandler.execute(GetByIdentifierOrPathCommand(insertedRole1.getName, insertedCompany.getCompanyId, classOf[Role])).result

    assertNotNull(r)
    assertTrue(this.liferayEntityComparator.equals(insertedRole1, r))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    role1.setDescriptions(List(new LocalizedTextContent("en_US", "Test Role 1"), new LocalizedTextContent("de_DE", "Test Rolle 1")))
    role1.setTitles(List(new LocalizedTextContent("en_US", "Role 1"), new LocalizedTextContent("de_DE", "Rolle 1")))

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result

    this.dispatchHandler.execute(DeleteCommand(insertedRole1))

    val roleList = dispatchHandler.execute(RoleListCommand(insertedCompany.getCompanyId)).result

    assertFalse(roleList.contains(insertedRole1.identifiedBy()))
  }

}
