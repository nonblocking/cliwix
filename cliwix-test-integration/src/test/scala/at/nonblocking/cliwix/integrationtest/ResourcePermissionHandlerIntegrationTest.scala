package at.nonblocking.cliwix.integrationtest

import java.io.{File, PrintWriter}

import at.nonblocking.cliwix.core.{ExecutionContextFlags, ExecutionContext}
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model._
import org.junit.Assert._
import org.junit.{After, Before, Test}
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
@ExplicitExecutionContext
class ResourcePermissionHandlerIntegrationTest {

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
  def insertPagePermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setPageOrder(0)
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    val permission = new ResourcePermission(role1.getName, List("VIEW", "ADD_DISCUSSION", "DELETE"))
    val insertedPagePermission = this.dispatchHandler.execute(
      PagePermissionInsertCommand(insertedCompany.getCompanyId, permission, insertedPage)).result

    assertNotNull(insertedPagePermission)
    //assertEquals(permission, insertedPagePermission)

    val permissionList = dispatchHandler.execute(PagePermissionListCommand(insertedCompany.getCompanyId, insertedPage, filterPermissionsWithNoAction = false)).result

    assertTrue(permissionList.contains(permission.identifiedBy()))
    //assertEquals(permission, permissionList.get(permission.identifiedBy()).get)
  }

  @Test
  @TransactionalRollback
  def updatePagePermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")

    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setPageOrder(0)
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    val permission = new ResourcePermission(role1.getName, List("VIEW", "ADD_DISCUSSION", "DELETE"))
    val insertedPagePermission = this.dispatchHandler.execute(PagePermissionInsertCommand(insertedCompany.getCompanyId, permission, insertedPage)).result

    insertedPagePermission.setActions(List("VIEW", "UPDATE"))

    val updatedPagePermissions = this.dispatchHandler.execute(UpdateCommand(insertedPagePermission)).result

    //assertEquals(insertedPagePermission, updatedPagePermissions)

    val permissionList = dispatchHandler.execute(PagePermissionListCommand(insertedCompany.getCompanyId, insertedPage, filterPermissionsWithNoAction = false)).result

    assertTrue(permissionList.contains(permission.identifiedBy()))
    //assertEquals(updatedPagePermissions, permissionList.get(permission.identifiedBy()).get)
  }

  @Test
  @TransactionalRollback
  def deletePagePermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")

    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setPageOrder(0)

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    val permission = new ResourcePermission(role1.getName, List("VIEW", "ADD_DISCUSSION", "DELETE"))
    val insertedPagePermission = this.dispatchHandler.execute(PagePermissionInsertCommand(insertedCompany.getCompanyId, permission, insertedPage)).result

    insertedPagePermission.setActions(List("VIEW", "UPDATE"))

    this.dispatchHandler.execute(DeleteCommand(insertedPagePermission))

    val permissionList = dispatchHandler.execute(PagePermissionListCommand(insertedCompany.getCompanyId, insertedPage, filterPermissionsWithNoAction = false)).result

    assertFalse(permissionList.contains(permission.identifiedBy()))
  }

  @Test
  @TransactionalRollback
  def insertPortletPermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val publicPageSet = this.dispatchHandler.execute(PageSetReadCommand(insertedSite.getSiteId, privatePages = false)).result

    val page = new Page(PAGE_TYPE.PORTLET, "/test1", List(new LocalizedTextContent("de_DE", "Test1")))
    page.setPageOrder(0)
    page.setDescriptions(List(new LocalizedTextContent("en_US", "First test page")))

    val insertedPage = this.dispatchHandler.execute(PageInsertCommand(page, null, publicPageSet)).result

    val pc = new PortletConfiguration("23_INSTANCE_fooo", List(new Preference("foo", "bar"), new Preference("foo2", "bar2")))
    pc.setBasePortletId("23")

    val insertedPC = this.dispatchHandler.execute(PortletConfigurationInsertCommand(insertedPage.getPortletLayoutId, pc)).result

    val permission = new ResourcePermission(role1.getName, List("VIEW", "PERMISSIONS", "ADD_TO_PAGE"))

    val insertedPortletPermission = this.dispatchHandler.execute(PortletPermissionInsertCommand(insertedCompany.getCompanyId, permission, insertedPage, insertedPC)).result

    assertNotNull(insertedPortletPermission)
    //assertEquals(permission, insertedPortletPermission)

    val permissionList = dispatchHandler.execute(PortletPermissionListCommand(insertedCompany.getCompanyId, insertedPage, insertedPC, filterPermissionsWithNoAction = false)).result

    assertTrue(permissionList.contains(permission.identifiedBy()))
    //assertEquals(permission, permissionList.get(permission.identifiedBy()).get)
  }

  @Test
  @TransactionalRollback
  def insertDocumentLibraryPermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val permission = new ResourcePermission(role1.getName, List("VIEW", "DELETE"))
    val insertedDocumentLibraryPermission = this.dispatchHandler.execute(DocumentLibraryPermissionInsertCommand(insertedCompany.getCompanyId, permission, insertedSite.getSiteId)).result

    assertNotNull(insertedDocumentLibraryPermission)

    val permissionList = dispatchHandler.execute(DocumentLibraryPermissionListCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, filterPermissionsWithNoAction = false)).result

    assertTrue(permissionList.contains(permission.identifiedBy()))
  }

  @Test
  @TransactionalRollback
  def insertDocumentLibraryFolderPermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder1 = new DocumentLibraryFolder("folder1")
    val insertedFolder1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder1, null)).result

    val permission = new ResourcePermission(role1.getName, List("VIEW", "DELETE"))
    val insertedFolderPermission = this.dispatchHandler.execute(DocumentLibraryFolderPermissionInsertCommand(
      insertedCompany.getCompanyId, permission, insertedFolder1.asInstanceOf[DocumentLibraryFolder]))
      .result

    assertNotNull(insertedFolderPermission)

    val permissionList = dispatchHandler.execute(DocumentLibraryFolderPermissionListCommand(
      insertedCompany.getCompanyId, insertedFolder1.asInstanceOf[DocumentLibraryFolder], filterPermissionsWithNoAction = false))
      .result

    assertTrue(permissionList.contains(permission.identifiedBy()))
  }

  @Test
  @TransactionalRollback
  def insertDocumentLibraryFilePermissionsTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder1 = new DocumentLibraryFolder("folder1")

    val textFile = File.createTempFile("test1", ".txt")
    val writer = new PrintWriter(textFile)
    writer.write("Hello World")
    writer.close()
    val file1 = new DocumentLibraryFile("test1", "test1.txt")
    file1.setFileDataUri(textFile.toURI)

    val insertedFolder1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder1, null)).result.asInstanceOf[DocumentLibraryFolder]
    val insertedFile1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, file1, insertedFolder1)).result

    val permission = new ResourcePermission(role1.getName, List("VIEW", "DELETE"))
    val insertedFilePermission = this.dispatchHandler.execute(DocumentLibraryFilePermissionInsertCommand(
      insertedCompany.getCompanyId, permission, insertedFile1.asInstanceOf[DocumentLibraryFile]))
      .result

    assertNotNull(insertedFilePermission)

    val permissionList = dispatchHandler.execute(DocumentLibraryFilePermissionListCommand(
      insertedCompany.getCompanyId, insertedFile1.asInstanceOf[DocumentLibraryFile], filterPermissionsWithNoAction = false))
      .result

    assertTrue(permissionList.contains(permission.identifiedBy()))
  }

  @Test
  @TransactionalRollback
  def insertArticlePermissionTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val role1 = new Role("TEST_ROLE1")
    val role2 = new Role("TEST_ROLE2")

    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role1)).result
    this.dispatchHandler.execute(RoleInsertCommand(insertedCompany.getCompanyId, role2)).result

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("22233", "de_DE",
      List(new LocalizedTextContent("de_DE", "First Article")),
      List(new LocalizedXmlContent("en_GB", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artikel</h2><p>test</p>")))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article)).result

    val permission = new ResourcePermission(role1.getName, List("VIEW", "DELETE"))
    val insertedArticlePermission = this.dispatchHandler.execute(ArticlePermissionInsertCommand(insertedCompany.getCompanyId, permission, insertedArticle)).result

    assertNotNull(insertedArticlePermission)

    val permissionList = dispatchHandler.execute(ArticlePermissionListCommand(insertedCompany.getCompanyId, insertedArticle, filterPermissionsWithNoAction = false)).result

    assertTrue(permissionList.contains(permission.identifiedBy()))
  }

}
