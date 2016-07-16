package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.{ExecutionContextFlags, ExecutionContext}
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model._
import com.liferay.portlet.documentlibrary.model.DLFileEntry
import org.junit.Assert._
import org.junit.{Before, After, Test}
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
@ExplicitExecutionContext
class RolePermissionHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  // Necessary because the resource actions are not initialized in the test setup
  @Before
  def before() = ExecutionContext.init(flags = ExecutionContextFlags(ignoreNonExistingResourceActions = true))

  @After
  def after() = ExecutionContext.destroy()

  @Test
  @TransactionalRollback
  def insertRolePermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    val insertedRole2 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val rolePermission = new RolePermission(classOf[DLFileEntry].getName, List("VIEW"))
    val insertedRolePermission = this.dispatchHandler.execute(RolePermissionInsertCommand(insertedCompany.getCompanyId, insertedRole1, rolePermission)).result

    assertNotNull(insertedRolePermission)
    //assertTrue(this.liferayEntityComparator.equals(rolePermission, insertedRolePermission))

    val permissionList = this.dispatchHandler.execute(RolePermissionListCommand(insertedCompany.getCompanyId, insertedRole1, filterPermissionsWithNoAction = false)).result

    assertTrue(permissionList.contains(rolePermission.identifiedBy()))
    //assertTrue(this.liferayEntityComparator.equals(rolePermission, permissionList.get(rolePermission.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def updatePagePermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    val insertedRole2 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val rolePermission = new RolePermission(classOf[DLFileEntry].getName, List("VIEW"))
    val insertedRolePermission = this.dispatchHandler.execute(RolePermissionInsertCommand(insertedCompany.getCompanyId, insertedRole1, rolePermission)).result

    assertNotNull(insertedRolePermission)
    //assertTrue(this.liferayEntityComparator.equals(rolePermission, insertedRolePermission))

    insertedRolePermission.setActions(List("VIEW", "UPDATE"))

    val updatedRolePermissions = this.dispatchHandler.execute(UpdateCommand(insertedRolePermission)).result

    assertNotNull(updatedRolePermissions)
    //assertTrue(this.liferayEntityComparator.equals(insertedRolePermission, updatedRolePermissions))

    val permissionList = this.dispatchHandler.execute(RolePermissionListCommand(insertedCompany.getCompanyId, insertedRole1, filterPermissionsWithNoAction = false)).result

    assertTrue(permissionList.contains(rolePermission.identifiedBy()))
    //assertTrue(this.liferayEntityComparator.equals(updatedRolePermissions, permissionList.get(rolePermission.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def deletePagePermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    val insertedRole1 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    val insertedRole2 = this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val rolePermission = new RolePermission(classOf[DLFileEntry].getName, List("VIEW"))
    val insertedRolePermission = this.dispatchHandler.execute(RolePermissionInsertCommand(insertedCompany.getCompanyId, insertedRole1, rolePermission)).result

    assertNotNull(insertedRolePermission)

    this.dispatchHandler.execute(DeleteCommand(insertedRolePermission))

    val permissionList = this.dispatchHandler.execute(RolePermissionListCommand(insertedCompany.getCompanyId, insertedRole1, filterPermissionsWithNoAction = false)).result

    assertFalse(permissionList.contains(rolePermission.identifiedBy()))
  }


}
