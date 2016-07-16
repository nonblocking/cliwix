package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model.UserGroup
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
class UserGroupHandlerIntegrationTest {

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
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user1))

    val insertedUserGroup = this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1)).result

    assertNotNull(insertedUserGroup)
    assertTrue(this.liferayEntityComparator.equals(userGroup1, insertedUserGroup))

    val userGroupList = dispatchHandler.execute(UserGroupListCommand(insertedCompany.getCompanyId)).result

    assertTrue(userGroupList.contains(userGroup1.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(userGroup1, userGroupList.get(userGroup1.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user1 = createTestUser("user1")
    val user2 = createTestUser("user2")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", toMemberUsers(user1))

    val insertedUserGroup = this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1)).result

    assertTrue(this.liferayEntityComparator.equals(userGroup1, insertedUserGroup))

    insertedUserGroup.setDescription("Updated user group")
    insertedUserGroup.setMemberUsers(toMemberUsers(user2))

    val updatedUserGroup = this.dispatchHandler.execute(UpdateCommand(insertedUserGroup)).result

    assertTrue(this.liferayEntityComparator.equals(insertedUserGroup, updatedUserGroup))

    val userGroupList = dispatchHandler.execute(UserGroupListCommand(insertedCompany.getCompanyId)).result

    assertTrue(userGroupList.contains(insertedUserGroup.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(insertedUserGroup, userGroupList.get(userGroup1.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def getByIdTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", null)

    val insertedUserGroup = this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1)).result

    val ug = this.dispatchHandler.execute(GetByDBIdCommand(insertedUserGroup.getUserGroupId, classOf[UserGroup])).result

    assertNotNull(ug)
    assertTrue(this.liferayEntityComparator.equals(insertedUserGroup, ug))
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", null)

    val insertedUserGroup = this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1)).result

    val ug = this.dispatchHandler.execute(GetByIdentifierOrPathCommand(insertedUserGroup.getName, insertedCompany.getCompanyId, classOf[UserGroup])).result

    assertNotNull(ug)
    assertTrue(this.liferayEntityComparator.equals(insertedUserGroup, ug))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user1 = createTestUser("user1")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1))

    val userGroup1 = new UserGroup("USER_GROUP1", "Test User Group 1", null)

    val insertedUserGroup = this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1)).result

    this.dispatchHandler.execute(DeleteCommand(insertedUserGroup))

    val userGroupList = dispatchHandler.execute(UserGroupListCommand(insertedCompany.getCompanyId)).result

    assertFalse(userGroupList.contains(insertedUserGroup.identifiedBy()))
  }

}
