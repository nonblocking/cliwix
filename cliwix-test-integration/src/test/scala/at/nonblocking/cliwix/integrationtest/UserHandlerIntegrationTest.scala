package at.nonblocking.cliwix.integrationtest

import java.{util => jutil}

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
class UserHandlerIntegrationTest {

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

    val user = new User("testuser1", "testuser1@nonblocking.at", new Password(false, "test"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")

    val insertedUser = this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user)).result

    assertNotNull(insertedUser)
    assertTrue(this.liferayEntityComparator.equals(user, insertedUser))
    val userList = dispatchHandler.execute(UserListCommand(insertedCompany.getCompanyId)).result

    assertTrue(userList.contains(user.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(user, userList.get(user.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def insertMinimalTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user = new User("testuser1", "testuser1@nonblocking.at", "Hans")

    val insertedUser = this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user)).result

    assertNotNull(insertedUser)

    val userList = dispatchHandler.execute(UserListCommand(insertedCompany.getCompanyId)).result
    assertTrue(userList.contains(user.identifiedBy()))
  }

  @Test
  @TransactionalRollback
  def insertDuplicateEmailAddressTest() {
    val company = createTestCompany()
    company.getCompanyConfiguration.setMailDomain("nb.at")

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user1 = new User("testuser1", "testuser@nonblocking.at", new Password(false, "test"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")

    val insertedUser1 = this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user1)).result

    val user2 = new User("testuser2", "testuser@nonblocking.at", new Password(false, "test2"),
      null, "Franz", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")

    val insertedUser2 = this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user2)).result

    val userList = dispatchHandler.execute(UserListCommand(insertedCompany.getCompanyId)).result

    assertTrue(userList.contains(user1.identifiedBy()))
    assertTrue(userList.contains(user2.identifiedBy()))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user = new User("testuser1", "testuser1@nonblocking.at", new Password(false, "test"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")

    val insertedUser = this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user)).result

    insertedUser.setMiddleName("Tiberious")
    insertedUser.setBirthDate(new jutil.Date())

    val updatedUser = this.dispatchHandler.execute(UpdateCommand(insertedUser)).result

    assertTrue(this.liferayEntityComparator.equals(insertedUser, updatedUser))

    val userList = dispatchHandler.execute(UserListCommand(insertedCompany.getCompanyId)).result

    assertTrue(userList.contains(user.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(updatedUser, userList.get(user.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def getByIdTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user = new User("testuser1", "testuser1@nonblocking.at", new Password(false, "test"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")

    val insertedUser = this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user)).result

    val u = this.dispatchHandler.execute(GetByDBIdCommand(insertedUser.getUserId, classOf[User])).result

    assertNotNull(u)
    assertTrue(this.liferayEntityComparator.equals(insertedUser, u))
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user = new User("testuser1", "testuser1@nonblocking.at", new Password(false, "test"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")

    val insertedUser = this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user)).result

    val u = this.dispatchHandler.execute(GetByIdentifierOrPathCommand(insertedUser.getScreenName, insertedCompany.getCompanyId, classOf[User])).result

    assertNotNull(u)
    assertTrue(this.liferayEntityComparator.equals(insertedUser, u))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val user = new User("testuser1", "testuser1@nonblocking.at", new Password(false, "test"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")

    val insertedUser = this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, user)).result

    this.dispatchHandler.execute(DeleteCommand(insertedUser))

    val userList = dispatchHandler.execute(UserListCommand(insertedCompany.getCompanyId)).result

    assertFalse(userList.contains(user.identifiedBy()))
  }

}
