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

import at.nonblocking.cliwix.core.command.{GetByIdentifierOrPathWithinGroupCommand, CommandResult, GetByDBIdCommand}
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.DocumentLibraryFileType
import com.liferay.portlet.documentlibrary.NoSuchFileEntryTypeException
import com.liferay.portlet.documentlibrary.service.DLFileEntryTypeLocalService

import scala.beans.BeanProperty

class DocumentLibraryFileTypeGetByIdHandler extends Handler[GetByDBIdCommand[DocumentLibraryFileType], DocumentLibraryFileType] {

  @BeanProperty
  var dlFileEntryTypeService: DLFileEntryTypeLocalService = _

  override private[core] def handle(command: GetByDBIdCommand[DocumentLibraryFileType]): CommandResult[DocumentLibraryFileType] = {
    try {
      val fileEntryType = this.dlFileEntryTypeService.getFileEntryType(command.dbId)
      val cliwixFileType = new DocumentLibraryFileType(fileEntryType.getFileEntryTypeId, fileEntryType.getName, fileEntryType.getGroupId)
      CommandResult(cliwixFileType)
    } catch {
      case e: NoSuchFileEntryTypeException =>
        logger.warn(s"No file entry type with id ${command.dbId} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class DocumentLibraryFileTypeGetByIdentifierHandler extends Handler[GetByIdentifierOrPathWithinGroupCommand[DocumentLibraryFileType], DocumentLibraryFileType] {

  @BeanProperty
  var dlFileEntryTypeService: DLFileEntryTypeLocalService = _

  override private[core] def handle(command: GetByIdentifierOrPathWithinGroupCommand[DocumentLibraryFileType]): CommandResult[DocumentLibraryFileType] = {
    try {
      val fileEntryType = this.dlFileEntryTypeService.getFileEntryType(command.groupId, command.identifierOrPath)
      val cliwixFileType = new DocumentLibraryFileType(fileEntryType.getFileEntryTypeId, fileEntryType.getName, fileEntryType.getGroupId)
      CommandResult(cliwixFileType)
    } catch {
      case e: NoSuchFileEntryTypeException =>
        logger.warn(s"No file entry type with name ${command.identifierOrPath} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}