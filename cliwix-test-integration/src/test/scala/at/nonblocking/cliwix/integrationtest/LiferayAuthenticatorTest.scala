package at.nonblocking.cliwix.integrationtest

import java.{util => jutil}

import at.nonblocking.cliwix.core.{LiferayAuthenticatorImpl, ExecutionContext, LiferayAuthenticator}
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model._
import com.liferay.portal.model.RoleConstants
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.{Ignore, Test}

import scala.beans.BeanProperty

@RunWith(classOf[CliwixIntegrationTestRunner])
class LiferayAuthenticatorTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayAuthenticator: LiferayAuthenticator = _

  @Ignore("Fails against 6.1.2")
  @Test
  @TransactionalRollback
  def insertTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val admin1 = new User("adminuser1", "adminuser1@nonblocking.at", new Password(false, "foobar"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, admin1)).result

    val admin2 = new User("adminuser2", "adminuser2@nonblocking.at", new Password(false, "foobar"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, admin2)).result

    val testuser1 = new User("testuser1", "testuser1@nonblocking.at", new Password(false, "foobar"),
      null, "Hans", null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi!")
    this.dispatchHandler.execute(UserInsertCommand(insertedCompany.getCompanyId, testuser1)).result

    val userGroup1 = new UserGroup("ADMIN_GROUP", "Admin Group", null)
    userGroup1.setMemberUsers(toMemberUsers(admin1))
    this.dispatchHandler.execute(UserGroupInsertCommand(insertedCompany.getCompanyId, userGroup1)).result

    val roleAssignmentsAdministrator = new RegularRoleAssignment(RoleConstants.ADMINISTRATOR)
    roleAssignmentsAdministrator.setMemberUsers(toMemberUsers(admin2))
    roleAssignmentsAdministrator.setMemberUserGroups(toMemberUserGroups(userGroup1))
    this.dispatchHandler.execute(RegularRoleAssignmentInsertCommand(insertedCompany.getCompanyId, roleAssignmentsAdministrator))

    val roleAssignmentsPowerUser = new RegularRoleAssignment(RoleConstants.POWER_USER)
    roleAssignmentsPowerUser.setMemberUsers(toMemberUsers(testuser1))
    this.dispatchHandler.execute(RegularRoleAssignmentInsertCommand(insertedCompany.getCompanyId, roleAssignmentsPowerUser))

    this.liferayAuthenticator.asInstanceOf[LiferayAuthenticatorImpl].setDefaultWebId(company.getWebId)

    assertTrue(this.liferayAuthenticator.login("adminuser1", "foobar"))
    assertFalse(this.liferayAuthenticator.login("adminuser1", "foobaromatic"))
    assertTrue(this.liferayAuthenticator.login("adminuser2", "foobar"))
    assertTrue(this.liferayAuthenticator.login("adminuser2@nonblocking.at", "foobar"))
    assertFalse(this.liferayAuthenticator.login("testuser1", "foobar"))
  }

}
