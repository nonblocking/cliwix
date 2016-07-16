package at.nonblocking.cliwix.integrationtest

import java.io.{File, PrintWriter}

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model._
import com.liferay.portlet.documentlibrary.model.DLFolderConstants
import org.apache.commons.io.FileUtils
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.{Before, Test}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
class DocumentLibraryItemHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  val fileDataDirIn = new File("target/filedata_in")
  val fileDataDirOut = new File("target/filedata_out")

  @Before
  def before(): Unit = {
    FileUtils.deleteDirectory(fileDataDirIn)
    fileDataDirIn.mkdirs()
  }

  @Test
  @TransactionalRollback
  def documentLibraryItemInsertTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder1 = new DocumentLibraryFolder("folder1")

    val textFile = new File(fileDataDirIn, "test1.txt")
    val writer = new PrintWriter(textFile)
    writer.write("Hello World")
    writer.close()
    val file1 = new DocumentLibraryFile("test1", "test1.txt")
    file1.setFileDataUri(textFile.toURI)
    file1.setAssetTags(List("tag1", "tag2"))

    val insertedFolder1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder1, null)).result.asInstanceOf[DocumentLibraryFolder]
    val insertedFile1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, file1, insertedFolder1)).result

    assertTrue(this.liferayEntityComparator.equals(folder1, insertedFolder1))
    assertEquals("/folder1", insertedFolder1.getPath)
    assertTrue(insertedFolder1.getOwnerGroupId > 0)
    assertTrue(this.liferayEntityComparator.equals(insertedFile1, file1))
    assertEquals("/folder1/test1", insertedFile1.getPath)
    assertTrue(insertedFile1.getOwnerGroupId > 0)

    val dlEntries = dispatchHandler.execute(DocumentLibraryItemListCommand(insertedSite.getSiteId, fileDataDirOut)).result

    assertTrue(dlEntries.size == 1)
    assertTrue(this.liferayEntityComparator.equals(folder1, dlEntries(0)))
    assertTrue(this.liferayEntityComparator.equals(dlEntries(0).getSubItems()(0), file1))

    val outFile = new File(fileDataDirOut, "folder1/test1.txt")
    assertTrue(outFile.exists())
    assertEquals("Hello World", FileUtils.readFileToString(outFile))
  }

  @Test
  @TransactionalRollback
  def documentLibraryInsertFileInRootFolderTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val textFile = new File(fileDataDirIn, "test1.txt")
    val writer = new PrintWriter(textFile)
    writer.write("Hello World")
    writer.close()
    val file1 = new DocumentLibraryFile("test1", "test1.txt")
    file1.setFileDataUri(textFile.toURI)
    file1.setAssetTags(List("tag1", "tag2"))

    val insertedFile1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, file1, null)).result

    assertTrue(this.liferayEntityComparator.equals(insertedFile1, file1))
    assertEquals("/test1", insertedFile1.getPath)
    assertTrue(insertedFile1.getOwnerGroupId > 0)

    val dlEntries = dispatchHandler.execute(DocumentLibraryItemListCommand(insertedSite.getSiteId, fileDataDirOut)).result

    assertTrue(dlEntries.size == 1)
    assertTrue(this.liferayEntityComparator.equals(dlEntries(0), file1))

    val outFile = new File(fileDataDirOut, "/test1.txt")
    assertTrue(outFile.exists())
    assertEquals("Hello World", FileUtils.readFileToString(outFile))
  }

  @Test
  @TransactionalRollback
  def documentLibraryItemUpdateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder1 = new DocumentLibraryFolder("folder1")

    val textFile = new File(fileDataDirIn, "test1.txt")
    val writer = new PrintWriter(textFile)
    writer.write("Hello World")
    writer.close()
    val file1 = new DocumentLibraryFile("test1", "test1.txt")
    file1.setFileDataUri(textFile.toURI)
    file1.setAssetTags(List("tag1", "tag2"))

    val insertedFolder1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder1, null)).result.asInstanceOf[DocumentLibraryFolder]
    val insertedFile1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, file1, insertedFolder1)).result

    insertedFolder1.setDescription("Updated Folder")

    val writer2 = new PrintWriter(textFile)
    writer2.write("Hello World2")
    writer2.close()

    insertedFile1.setDescription("Updated File")
    insertedFile1.asInstanceOf[DocumentLibraryFile].setAssetTags(List("tag3", "tag4"))

    val updatedFolder1 = this.dispatchHandler.execute(UpdateCommand(insertedFolder1)).result
    val updatedFile1 = this.dispatchHandler.execute(UpdateCommand(insertedFile1)).result

    assertTrue(this.liferayEntityComparator.equals(insertedFolder1, updatedFolder1))
    assertEquals("/folder1", updatedFolder1.getPath)
    assertTrue(updatedFolder1.getOwnerGroupId > 0)
    assertTrue(this.liferayEntityComparator.equals(updatedFile1, insertedFile1))
    assertEquals("/folder1/test1", updatedFile1.getPath)
    assertTrue(updatedFile1.getOwnerGroupId > 0)

    val dlEntries = dispatchHandler.execute(DocumentLibraryItemListCommand(insertedSite.getSiteId, fileDataDirOut)).result

    assertTrue(dlEntries.size == 1)
    assertTrue(this.liferayEntityComparator.equals(insertedFolder1, dlEntries(0)))
    assertTrue(this.liferayEntityComparator.equals(dlEntries(0).getSubItems()(0), insertedFile1))

    val outFile = new File(fileDataDirOut, "folder1/test1.txt")
    assertTrue(outFile.exists())
    assertEquals("Hello World2", FileUtils.readFileToString(outFile))
  }

  @Test
  @TransactionalRollback
  def documentLibraryItemUpdateDifferentCaseTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder1 = new DocumentLibraryFolder("folder1")

    val textFile = new File(fileDataDirIn, "test1.txt")
    val writer = new PrintWriter(textFile)
    writer.write("Hello World")
    writer.close()
    val file1 = new DocumentLibraryFile("test1", "test1.txt")
    file1.setFileDataUri(textFile.toURI)
    file1.setAssetTags(List("tag1", "tag2"))

    val insertedFolder1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder1, null)).result.asInstanceOf[DocumentLibraryFolder]
    val insertedFile1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, file1, insertedFolder1)).result

    insertedFolder1.setDescription("Updated Folder")

    val writer2 = new PrintWriter(textFile)
    writer2.write("Hello World2")
    writer2.close()

    insertedFile1.setDescription("Updated File")
    insertedFile1.asInstanceOf[DocumentLibraryFile].setAssetTags(List("tag3", "tag4"))

    val updatedFolder1 = this.dispatchHandler.execute(UpdateCommand(insertedFolder1)).result
    val updatedFile1 = this.dispatchHandler.execute(UpdateCommand(insertedFile1)).result

    assertTrue(this.liferayEntityComparator.equals(insertedFolder1, updatedFolder1))
    assertEquals("/folder1", updatedFolder1.getPath)
    assertTrue(updatedFolder1.getOwnerGroupId > 0)
    assertTrue(this.liferayEntityComparator.equals(updatedFile1, insertedFile1))
    assertEquals("/folder1/test1", updatedFile1.getPath)
    assertTrue(updatedFile1.getOwnerGroupId > 0)

    val dlEntries = dispatchHandler.execute(DocumentLibraryItemListCommand(insertedSite.getSiteId, fileDataDirOut)).result

    assertTrue(dlEntries.size == 1)
    assertTrue(this.liferayEntityComparator.equals(insertedFolder1, dlEntries(0)))
    assertTrue(this.liferayEntityComparator.equals(dlEntries(0).getSubItems()(0), insertedFile1))

    val outFile = new File(fileDataDirOut, "folder1/test1.txt")
    assertTrue(outFile.exists())
    assertEquals("Hello World2", FileUtils.readFileToString(outFile))
  }

  @Test
  @TransactionalRollback
  def documentLibraryItemGetByIdTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder1 = new DocumentLibraryFolder("folder1")

    val textFile = new File(fileDataDirIn, "test1.txt")
    val writer = new PrintWriter(textFile)
    writer.write("Hello World")
    writer.close()
    val file1 = new DocumentLibraryFile("test1", "test1.txt")
    file1.setFileDataUri(textFile.toURI)
    file1.setAssetTags(List("tag1", "tag2"))

    val insertedFolder1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder1, null)).result.asInstanceOf[DocumentLibraryFolder]
    val insertedFile1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, file1, insertedFolder1)).result.asInstanceOf[DocumentLibraryFile]

    val folder = this.dispatchHandler.execute(GetByDBIdCommand(insertedFolder1.getFolderId, classOf[DocumentLibraryFolder])).result
    val file = this.dispatchHandler.execute(GetByDBIdCommand(insertedFile1.getFileId, classOf[DocumentLibraryFile])).result

    assertNotNull(folder)
    assertNotNull(file)
    assertEquals("/folder1", folder.getPath)
    assertEquals("/folder1/test1", file.getPath)
    assertTrue(this.liferayEntityComparator.equals(folder, insertedFolder1))
    assertTrue(this.liferayEntityComparator.equals(file, insertedFile1))
  }

  @Test
  @TransactionalRollback
  def documentLibraryItemGetByIdRootFolderTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder = this.dispatchHandler.execute(GetByDBIdCommand(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, classOf[DocumentLibraryFolder])).result

    assertNotNull(folder)
    assertEquals(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, folder.getFolderId)
    assertEquals("/", folder.getPath)
  }

  @Test
  @TransactionalRollback
  def documentLibraryItemGetByNaturalIdentifierTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder1 = new DocumentLibraryFolder("folder1")
    val folder2 = new DocumentLibraryFolder("folder2")

    val textFile = new File(fileDataDirIn, "test1.txt")
    val writer = new PrintWriter(textFile)
    writer.write("Hello World")
    writer.close()
    val file1 = new DocumentLibraryFile("test1", "test1.txt")
    file1.setFileDataUri(textFile.toURI)
    file1.setAssetTags(List("tag1", "tag2"))

    val insertedFolder1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder1, null)).result.asInstanceOf[DocumentLibraryFolder]
    val insertedFolder2 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder2, insertedFolder1)).result.asInstanceOf[DocumentLibraryFolder]
    val insertedFile1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, file1, insertedFolder2)).result.asInstanceOf[DocumentLibraryFile]

    assertEquals("/folder1/folder2", insertedFolder2.getPath)
    assertEquals("/folder1/folder2/test1", insertedFile1.getPath)

    val folder = this.dispatchHandler.execute(GetByIdentifierOrPathWithinGroupCommand(insertedFolder2.getPath, insertedCompany.getCompanyId, insertedSite.getSiteId, classOf[DocumentLibraryFolder])).result
    val file = this.dispatchHandler.execute(GetByIdentifierOrPathWithinGroupCommand(insertedFile1.getPath, insertedCompany.getCompanyId, insertedSite.getSiteId, classOf[DocumentLibraryFile])).result

    assertNotNull(folder)
    assertEquals("/folder1/folder2", folder.getPath)
    assertNotNull(file)
    assertEquals("/folder1/folder2/test1", file.getPath)
    assertTrue(this.liferayEntityComparator.equals(folder, insertedFolder2))
    assertTrue(this.liferayEntityComparator.equals(file, insertedFile1))
  }

  @Test
  def documentLibraryItemGetByNaturalIdentifierRootFolderTest() {

    val folder = this.dispatchHandler.execute(GetByIdentifierOrPathWithinGroupCommand("/", -1, -1, classOf[DocumentLibraryFolder])).result

    assertNotNull(folder)
    assertEquals(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, folder.getFolderId)
    assertEquals("/", folder.getPath)
  }

  @Test
  @TransactionalRollback
  def documentLibraryItemDeleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val folder1 = new DocumentLibraryFolder("folder1")

    val textFile = new File(fileDataDirIn, "test1.txt")
    val writer = new PrintWriter(textFile)
    writer.write("Hello World")
    writer.close()
    val file1 = new DocumentLibraryFile("test1", "test1.txt")
    file1.setFileDataUri(textFile.toURI)
    file1.setAssetTags(List("tag1", "tag2"))

    val insertedFolder1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, folder1, null)).result.asInstanceOf[DocumentLibraryFolder]
    val insertedFile1 = this.dispatchHandler.execute(DocumentLibraryItemInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, file1, insertedFolder1)).result.asInstanceOf[DocumentLibraryFile]

    this.dispatchHandler.execute(DeleteCommand(insertedFile1))

    val dlEntries1 = dispatchHandler.execute(DocumentLibraryItemListCommand(insertedSite.getSiteId, fileDataDirOut)).result
    assertTrue(dlEntries1(0).getSubItems == null)

    this.dispatchHandler.execute(DeleteCommand(insertedFolder1))

    val dlEntries2 = dispatchHandler.execute(DocumentLibraryItemListCommand(insertedSite.getSiteId, fileDataDirOut)).result
    assertTrue(dlEntries2.size == 0)
  }

}
