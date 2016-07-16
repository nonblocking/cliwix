/*
 * Copyright (c) 2014-2016
 * nonblocking.at gmbh [http://www.nonblocking.at]
 *
 * This file is part of Cliwix.
 *
 * Cliwix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package at.nonblocking.cliwix.core.liferay61.handler

import java.io.{File, FileOutputStream, IOException}
import java.util.Calendar
import java.{util => jutil}

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.filedata.FileDataLoader
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.core.liferay61.util.NativeSqlAccessUtil
import at.nonblocking.cliwix.core.util.{AssetEntryUtil, TrashFilter}
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.core.{Cliwix, CliwixException, ExecutionContext}
import at.nonblocking.cliwix.model._
import com.liferay.portal.NoSuchRepositoryEntryException
import com.liferay.portal.kernel.dao.orm.{QueryUtil, Type}
import com.liferay.portal.kernel.exception.PortalException
import com.liferay.portal.kernel.util.MimeTypesUtil
import com.liferay.portal.service.UserLocalService
import com.liferay.portlet.documentlibrary.model.{DLFileEntry, DLFileEntryTypeConstants, DLFolderConstants}
import com.liferay.portlet.documentlibrary.service.{DLAppLocalService, DLFileEntryLocalService, DLFolderLocalService}
import com.liferay.portlet.documentlibrary.{DuplicateFileException, NoSuchFileEntryException, NoSuchFileException, NoSuchFolderException}
import org.apache.commons.io.{FilenameUtils, IOUtils}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class DocumentLibraryItemListHandler extends Handler[DocumentLibraryItemListCommand, jutil.List[DocumentLibraryItem]] {

  @BeanProperty
  var dlAppService: DLAppLocalService = _

  @BeanProperty
  var dlFileEntryService: DLFileEntryLocalService = _

  @BeanProperty
  var dlFolderEntryService: DLFolderLocalService = _

  @BeanProperty
  var assetEntryUtil: AssetEntryUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var trashFilter: TrashFilter = _

  private[core] override def handle(command: DocumentLibraryItemListCommand): CommandResult[jutil.List[DocumentLibraryItem]] = {
    def folderTree(parentFolder: DocumentLibraryFolder): List[DocumentLibraryFolder] = {
      val parentFolderId: Long = if (parentFolder != null) parentFolder.getFolderId else DLFolderConstants.DEFAULT_PARENT_FOLDER_ID

      val folders =
        this.dlFolderEntryService.getFolders(command.groupId, parentFolderId, false)
        .filter(trashFilter.isNotInTrash)

      folders.map { folder =>
        logger.debug("DL folder found: {}", folder.getName)
        val cliwixFolder = this.converter.convertToCliwixFolder(folder, parentFolder)
        val subFolders = folderTree(cliwixFolder)
        if (subFolders.nonEmpty) cliwixFolder.setSubItems(new jutil.ArrayList(subFolders))
        cliwixFolder
      }.toList
    }

    def addFileEntries(folders: List[DocumentLibraryFolder], path: String, exportOnlyFileDataLastModifiedTsAfter: Option[Long]): Unit = {
      folders.foreach { folder =>
        if (folder.getSubItems != null) addFileEntries(folder.getSubItems.map(_.asInstanceOf[DocumentLibraryFolder]).toList, path + "/" + folder.getName, exportOnlyFileDataLastModifiedTsAfter)

        val fileEntries =
          this.dlFileEntryService.getFileEntries(command.groupId, folder.getFolderId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null)
          .filter(trashFilter.isNotInTrash)

        if (fileEntries.nonEmpty && folder.getSubItems == null) folder.setSubItems(new jutil.ArrayList())

        val targetDir = if (command.dataDir != null) {
          val dir = new File(command.dataDir, path + "/" + folder.getName)
          dir.mkdirs()
          dir
        } else {
          null
        }

        fileEntries.foreach { file =>
          logger.debug("DL file entry found: {}", file.getTitle)

          val cliwixFile = this.converter.convertToCliwixFile(file, folder)
          cliwixFile.setAssetTags(this.assetEntryUtil.getTagListForAssetEntry(classOf[DLFileEntry].getName, file.getFileEntryId))

          try {
            if (targetDir != null && (exportOnlyFileDataLastModifiedTsAfter.isEmpty || cliwixFile.getFileDataUpdateTimestamp > exportOnlyFileDataLastModifiedTsAfter.get)) {
              val inStream = this.dlFileEntryService.getFileAsStream(0, file.getFileEntryId, file.getVersion, false)
              val target = new File(targetDir, cliwixFile.getFileDataName)

              logger.debug("Dumping DL file entry data to: {}", target.getAbsoluteFile)
              val outStream = new FileOutputStream(target)
              IOUtils.copy(inStream, outStream)
              inStream.close()
              outStream.close()
              target.setLastModified(cliwixFile.getFileDataUpdateTimestamp)
            }

            folder.getSubItems.add(cliwixFile)

          } catch {
            case e: NoSuchFileException =>
              val fullPath = folder.getPath + "/" + file.getTitle
              if (ExecutionContext.flags.skipCorruptDocuments) {
                report.addWarning(s"Skipping file because data cannot be read: $fullPath.")
              } else {
                throw new CliwixException(s"File data for entry '$fullPath' could not be found!")
              }
            case e: Throwable => throw e
          }
        }
      }
    }

    if (command.dataDir == null) {
      logger.debug("Export of file data is disabled!")
    }

    val folders = folderTree(null)

    val artificialRoot = new DocumentLibraryFolder("")
    artificialRoot.setFolderId(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID)
    artificialRoot.setPath("")
    artificialRoot.setSubItems(new jutil.ArrayList(folders))

    val exportOnlyFileDataLastModifiedTsAfter =
      if (command.exportOnlyFileDataLastModifiedWithinDays.isDefined) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -command.exportOnlyFileDataLastModifiedWithinDays.get)
        Some(cal.getTimeInMillis)
      } else {
        None
      }

    addFileEntries(List(artificialRoot), "", exportOnlyFileDataLastModifiedTsAfter)

    CommandResult(artificialRoot.getSubItems)
  }
}

class DocumentLibraryItemInsertHandler extends Handler[DocumentLibraryItemInsertCommand, DocumentLibraryItem] {

  @BeanProperty
  var dlAppService: DLAppLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var fileDataLoader: FileDataLoader = _

  @BeanProperty
  var assetEntryUtil: AssetEntryUtil = _

  @BeanProperty
  var nativeSqlAccessUtil: NativeSqlAccessUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: DocumentLibraryItemInsertCommand): CommandResult[DocumentLibraryItem] = {
    assert(command.item.getName != null, "name != null")

    assert(command.parentFolder == null || command.parentFolder.getFolderId != null, "parentFolderId != null")
    val parentFolderId: Long = if (command.parentFolder != null) command.parentFolder.getFolderId else DLFolderConstants.DEFAULT_PARENT_FOLDER_ID

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()

    command.item match {
      case cliwixFolder: DocumentLibraryFolder =>
        logger.debug("Adding folder: {}", cliwixFolder)

        val insertedFolder = this.dlAppService.addFolder(defaultUser.getUserId, command.groupId, parentFolderId,
          cliwixFolder.getName, cliwixFolder.getDescription, serviceContext)

        val insertedCliwixFolder = this.converter.convertToCliwixFolder(insertedFolder, command.parentFolder)
        CommandResult(insertedCliwixFolder)

      case cliwixFile: DocumentLibraryFile =>
        if (cliwixFile.getFileDataUri == null) throw new CliwixValidationException(s"No file data found for document library item: $cliwixFile")
        val fileData = this.fileDataLoader.getData(cliwixFile.getFileDataUri)
        if (fileData == null) throw new CliwixValidationException(s"No file data found for document library item: $cliwixFile")
        val inStream = fileData.getData

        val mimeTime = MimeTypesUtil.getContentType(cliwixFile.getFileDataName)

        logger.debug("Adding file: {}", cliwixFile)

        val description =
          if (cliwixFile.getDescription == null && Cliwix.getProperty(Cliwix.PROPERTY_STORE_ACTUAL_FILENAME_IN_DESCRIPTION) == "true") cliwixFile.getFileDataName
          else cliwixFile.getDescription

        val fileEntryTypeId = DLFileEntryTypeConstants.FILE_ENTRY_TYPE_ID_BASIC_DOCUMENT

        serviceContext.setAttribute("fileEntryTypeId", fileEntryTypeId)
        serviceContext.setCreateDate(new jutil.Date(fileData.getLastModified))
        serviceContext.setModifiedDate(serviceContext.getCreateDate)
        if (cliwixFile.getAssetTags != null) serviceContext.setAssetTagNames(cliwixFile.getAssetTags.toList.toArray)

        val insertedFile =
        try {
           this.dlAppService.addFileEntry(defaultUser.getUserId, command.groupId, parentFolderId,
              cliwixFile.getFileDataName, mimeTime, cliwixFile.getName, description,
              "Created by Cliwix", inStream, fileData.getSize, serviceContext)
        } catch {
          case e: DuplicateFileException =>
            //The file might be stored without the extension or with a different case
            val existingFileEntry = tryFindExistingFile(cliwixFile)
            if (existingFileEntry.isDefined) {
              val majorVersion = true
              this.dlAppService.updateFileEntry(defaultUser.getUserId, existingFileEntry.get.getFileEntryId,
                cliwixFile.getFileDataName, mimeTime, cliwixFile.getName, description,
                "Updated by Cliwix", majorVersion, inStream, fileData.getSize, serviceContext)
            } else {
              throw e
            }
        }

        inStream.close()

        val insertedCliwixFile = this.converter.convertToCliwixFile(insertedFile, command.parentFolder)
        insertedCliwixFile.setFileDataUri(cliwixFile.getFileDataUri)
        insertedCliwixFile.setAssetTags(cliwixFile.getAssetTags)
        CommandResult(insertedCliwixFile)
    }
  }

  private def tryFindExistingFile(cliwixFile: DocumentLibraryFile) = {
    //Try 1: Search case insensitive
    val fileEntryId1 = this.nativeSqlAccessUtil.scalarUniqueResult(
      s"SELECT fileEntryId FROM DLFileEntry where LOWER(title) = '${cliwixFile.getName.toLowerCase}'",
      Map("fileEntryId" -> Type.LONG))
    if (fileEntryId1.isDefined) {
      Some(this.dlAppService.getFileEntry(fileEntryId1.get.asInstanceOf[Long]))
    } else {
      //Try 2: Maybe it exists with the extension in the title
      val fileNameWithExt = cliwixFile.getName + "." + FilenameUtils.getExtension(cliwixFile.getFileDataName)
      val fileEntryId2 = this.nativeSqlAccessUtil.scalarUniqueResult(
        s"SELECT fileEntryId FROM DLFileEntry where LOWER(title) = '${fileNameWithExt.toLowerCase}'",
        Map("fileEntryId" -> Type.LONG))
      if (fileEntryId2.isDefined) {
        Some(this.dlAppService.getFileEntry(fileEntryId2.get.asInstanceOf[Long]))
      } else {
        None
      }
    }
  }
}

class DocumentLibraryItemUpdateHandler extends Handler[UpdateCommand[DocumentLibraryItem], DocumentLibraryItem] {

  @BeanProperty
  var dlAppService: DLAppLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var fileDataLoader: FileDataLoader = _

  @BeanProperty
  var assetEntryUtil: AssetEntryUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[DocumentLibraryItem]): CommandResult[DocumentLibraryItem] = {

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()

    command.entity match {
      case cliwixFolder: DocumentLibraryFolder =>
        assert(cliwixFolder.getFolderId != null, "folderId != null")

        val folder = this.dlAppService.getFolder(cliwixFolder.getFolderId)
        val parentFolderId = if (folder.getParentFolder != null) folder.getParentFolder.getFolderId else DLFolderConstants.DEFAULT_PARENT_FOLDER_ID

        logger.debug("Updating folder: {}", cliwixFolder)

        val updatedFolder = this.dlAppService.updateFolder(cliwixFolder.getFolderId, parentFolderId,
          cliwixFolder.getName, cliwixFolder.getDescription, serviceContext)

        val updatedCliwixFolder = this.converter.convertToCliwixFolder(updatedFolder, null)
        updatedCliwixFolder.setPath(cliwixFolder.getPath)
        CommandResult(updatedCliwixFolder)

      case cliwixFile: DocumentLibraryFile =>
        assert(cliwixFile.getFileId != null, "fileId != null")

        val file = this.dlAppService.getFileEntry(cliwixFile.getFileId)
        val mimeTime = MimeTypesUtil.getContentType(cliwixFile.getFileDataName)
        val fileData =
          if (cliwixFile.getFileDataUri != null) this.fileDataLoader.getData(cliwixFile.getFileDataUri)
          else null
        val fileSize = if (fileData != null) fileData.getSize else 0
        val lastModified = if (fileData != null) new jutil.Date(fileData.getLastModified) else null
        val inStream = if (fileData != null) fileData.getData else null
        val majorVersion = true

        logger.debug("Updating file: {}", cliwixFile)

        val description =
          if (cliwixFile.getDescription == null && Cliwix.getProperty(Cliwix.PROPERTY_STORE_ACTUAL_FILENAME_IN_DESCRIPTION) == "true") cliwixFile.getFileDataName
          else cliwixFile.getDescription

        val fileEntryTypeId = DLFileEntryTypeConstants.FILE_ENTRY_TYPE_ID_BASIC_DOCUMENT

        serviceContext.setAttribute("fileEntryTypeId", fileEntryTypeId)
        serviceContext.setModifiedDate(lastModified)
        if (cliwixFile.getAssetTags != null) serviceContext.setAssetTagNames(cliwixFile.getAssetTags.toList.toArray)

        val updatedFileEntry =
          try {
            this.dlAppService.updateFileEntry(defaultUser.getUserId, file.getFileEntryId,
              cliwixFile.getFileDataName, mimeTime, cliwixFile.getName, description,
              "Updated by Cliwix", majorVersion, inStream, fileSize, serviceContext)
          } catch {
            case e @ (_ : IOException | _ : PortalException) =>
              report.addWarning(s"Possible file corruption detected for file entry with name: ${cliwixFile.getName}. Performing delete and insert instead of update.")
              logger.warn(s"Possible file corruption detected for file entry with name: ${cliwixFile.getName}. Performing delete and insert instead of update.", e)
              this.dlAppService.deleteFileEntry(file.getFileEntryId)
              this.dlAppService.addFileEntry(defaultUser.getUserId, file.getGroupId, file.getFolderId,
                cliwixFile.getFileDataName, mimeTime, cliwixFile.getName, description,
                "Created by Cliwix", inStream, 0, serviceContext)
          }

        if (inStream != null) inStream.close()

        val updatedCliwixFile = this.converter.convertToCliwixFile(updatedFileEntry, null)
        updatedCliwixFile.setPath(cliwixFile.getPath)
        updatedCliwixFile.setFileDataUri(cliwixFile.getFileDataUri)
        updatedCliwixFile.setAssetTags(cliwixFile.getAssetTags)
        CommandResult(updatedCliwixFile)
    }
  }
}

class DocumentLibraryFolderGetByIdHandler extends Handler[GetByDBIdCommand[DocumentLibraryFolder], DocumentLibraryFolder] with DocumentLibraryUtils {

  @BeanProperty
  var dlAppService: DLAppLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[DocumentLibraryFolder]): CommandResult[DocumentLibraryFolder] = {
    try {
      val cliwixFolder =
        if (command.dbId == DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) {
          val serviceContext = ExecutionContext.createServiceContext()
          assert(serviceContext.getScopeGroupId > 0, "Execution context with valid site id")

          createRootFolder(serviceContext.getScopeGroupId)
        } else {
          getFolderRecursive(command.dbId, dlAppService, converter)
        }

      CommandResult(cliwixFolder)
    } catch {
      case e @ (_: NoSuchFolderException | _: NoSuchRepositoryEntryException) =>
        logger.warn(s"No folder with id ${command.dbId} found in document library!", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class DocumentLibraryFileGetByIdHandler extends Handler[GetByDBIdCommand[DocumentLibraryFile], DocumentLibraryFile] with DocumentLibraryUtils {

  @BeanProperty
  var dlAppService: DLAppLocalService = _

  @BeanProperty
  var assetEntryUtil: AssetEntryUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[DocumentLibraryFile]): CommandResult[DocumentLibraryFile] = {
    try {
      val file = this.dlAppService.getFileEntry(command.dbId)
      val cliwixFile = this.converter.convertToCliwixFile(file, getFolderRecursive(file.getFolderId, dlAppService, converter))
      cliwixFile.setAssetTags(this.assetEntryUtil.getTagListForAssetEntry(classOf[DLFileEntry].getName, file.getFileEntryId))
      CommandResult(cliwixFile)
    } catch {
      case e @ (_: NoSuchFileEntryException | _: NoSuchRepositoryEntryException) =>
        logger.warn(s"No file with id ${command.dbId} found in document library!", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class DocumentLibraryFolderGetByPathHandler extends Handler[GetByIdentifierOrPathWithinGroupCommand[DocumentLibraryFolder], DocumentLibraryFolder] with DocumentLibraryUtils {

  @BeanProperty
  var dlAppService: DLAppLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathWithinGroupCommand[DocumentLibraryFolder]): CommandResult[DocumentLibraryFolder] = {
    assert(command.identifierOrPath.startsWith("/"), "Document library path starts with a /")

    try {
      val cliwixFolder =
        if (command.identifierOrPath == "/") {
          createRootFolder(command.groupId)
        } else {
          val folders = command.identifierOrPath.split("/").filter(name => name != null && !name.isEmpty).toList
          getFolderByPath(folders, command.groupId, dlAppService, converter)
        }

      CommandResult(cliwixFolder)
    } catch {
      case e: NoSuchFolderException =>
        logger.warn(s"Folder ${command.identifierOrPath} not found in document library!", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class DocumentLibraryFileGetByPathHandler extends Handler[GetByIdentifierOrPathWithinGroupCommand[DocumentLibraryFile], DocumentLibraryFile] with DocumentLibraryUtils {

  @BeanProperty
  var dlAppService: DLAppLocalService = _

  @BeanProperty
  var assetEntryUtil: AssetEntryUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathWithinGroupCommand[DocumentLibraryFile]): CommandResult[DocumentLibraryFile] = {
    try {
      val pathParts = command.identifierOrPath.split("/").filter(name => name != null && !name.isEmpty).toList
      val folders = pathParts.init
      val fileName = pathParts.last
      val parentFolder = getFolderByPath(folders, command.groupId, dlAppService, converter)
      val file = this.dlAppService.getFileEntry(command.groupId, parentFolder.getFolderId, fileName)
      val cliwixFile = this.converter.convertToCliwixFile(file, getFolderRecursive(file.getFolderId, dlAppService, converter))
      cliwixFile.setAssetTags(this.assetEntryUtil.getTagListForAssetEntry(classOf[DLFileEntry].getName, file.getFileEntryId))
      CommandResult(cliwixFile)
    } catch {
      case e: NoSuchFolderException =>
        logger.warn(s"File ${command.identifierOrPath} not found in document library!", e)
        CommandResult(null)
      case e: NoSuchFileEntryException =>
        logger.warn(s"File ${command.identifierOrPath} not found in document library!", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class DocumentLibraryItemDeleteHandler extends Handler[DeleteCommand[DocumentLibraryItem], DocumentLibraryItem] {

  @BeanProperty
  var dlAppService: DLAppLocalService = _

  override private[core] def handle(command: DeleteCommand[DocumentLibraryItem]): CommandResult[DocumentLibraryItem] = {
    command.entity match {
      case folder: DocumentLibraryFolder =>
        logger.debug("Deleting folder: {}", folder)
        this.dlAppService.deleteFolder(folder.getFolderId)
        CommandResult(null)
      case file: DocumentLibraryFile =>
        logger.debug("Deleting file: {}", file)
        this.dlAppService.deleteFileEntry(file.getFileId)
        CommandResult(null)
    }
  }

}

sealed trait DocumentLibraryUtils {

  def getFolderByPath(folders: List[String], repositoryId: Long, dlAppService: DLAppLocalService, converter: LiferayEntityConverter): DocumentLibraryFolder = {
    def getFolder(folders: List[String], parentFolder: DocumentLibraryFolder): DocumentLibraryFolder = {
      if (folders.nonEmpty) {
        val parentFolderId: Long = if (parentFolder == null) DLFolderConstants.DEFAULT_PARENT_FOLDER_ID else parentFolder.getFolderId
        val folder = dlAppService.getFolder(repositoryId, parentFolderId, folders.head)
        val cliwixFolder = converter.convertToCliwixFolder(folder, parentFolder)
        getFolder(folders.tail, cliwixFolder)
      } else {
        parentFolder
      }
    }

    getFolder(folders, null)
  }

  def getFolderRecursive(folderId: Long, dlAppService: DLAppLocalService, converter: LiferayEntityConverter): DocumentLibraryFolder = {
    if (folderId == DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) {
      null
    } else {
      val folder = dlAppService.getFolder(folderId)
      val parent = getFolderRecursive(folder.getParentFolderId, dlAppService, converter)
      converter.convertToCliwixFolder(folder, parent)
    }
  }

  def createRootFolder(groupId: Long) = {
    val rootFolder = new DocumentLibraryFolder("Root")
    rootFolder.setFolderId(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID)
    rootFolder.setPath("/")
    rootFolder.setOwnerGroupId(groupId)
    rootFolder
  }

}
