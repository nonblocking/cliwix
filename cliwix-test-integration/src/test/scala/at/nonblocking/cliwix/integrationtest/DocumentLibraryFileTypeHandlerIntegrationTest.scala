package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.LiferayInfo
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.model.DocumentLibraryFileType
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty

@RunWith(classOf[CliwixIntegrationTestRunner])
class DocumentLibraryFileTypeHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayInfo: LiferayInfo= _

  @Test
  @TransactionalRollback
  def getByIdAndGetByNaturalIdentifierTest() {
    val dummyCompanyId = -1
    val defaultGroupId = 0

    val fileTypeFromIdentifier = this.dispatchHandler.execute(GetByIdentifierOrPathWithinGroupCommand("Basic Document", dummyCompanyId, defaultGroupId, classOf[DocumentLibraryFileType])).result

    assertNotNull(fileTypeFromIdentifier)
    assertNotNull(fileTypeFromIdentifier.getFileEntryTypeId)
    if (this.liferayInfo.getBaseVersion == "6.1") {
      assertEquals("Basic Document", fileTypeFromIdentifier.getFileEntryTypeKey)
    } else {
      assertEquals("BASIC-DOCUMENT", fileTypeFromIdentifier.getFileEntryTypeKey)
    }

    val fileTypeFromId = this.dispatchHandler.execute(GetByDBIdCommand(fileTypeFromIdentifier.getFileEntryTypeId, classOf[DocumentLibraryFileType])).result

    assertNotNull(fileTypeFromId)
    assertEquals(fileTypeFromIdentifier.getFileEntryTypeId, fileTypeFromId.getFileEntryTypeId)
    if (this.liferayInfo.getBaseVersion == "6.1") {
      assertEquals("Basic Document", fileTypeFromId.getFileEntryTypeKey)
    } else {
      assertEquals("BASIC-DOCUMENT", fileTypeFromId.getFileEntryTypeKey)
    }
  }


}
