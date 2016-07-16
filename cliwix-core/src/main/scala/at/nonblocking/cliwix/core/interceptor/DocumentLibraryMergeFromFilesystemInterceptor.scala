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

package at.nonblocking.cliwix.core.interceptor

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.filedata.FileDataItem
import at.nonblocking.cliwix.core.util.ListTypeUtils
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

import java.{util=>jutil}

import scala.collection.JavaConversions._

class DocumentLibraryMergeFromFilesystemInterceptor extends TreeProcessingInterceptor[DocumentLibraryItem, DocumentLibrary] with LazyLogging with ListTypeUtils {

  override def beforeTreeImport(dl: DocumentLibrary, companyId: Long) {
    if (dl != null && dl.getMergeFromFileSystem != null && dl.getMergeFromFileSystem) {
      assert(ExecutionContext.fileDataResolver != null)

      val mediaDir = ExecutionContext.fileDataResolver.getRoot.getSubItemByRelativePath(dl.getFileDataFolder)
      if (mediaDir.isEmpty) throw new CliwixValidationException(s"File data root folder not found: ${dl.getFileDataFolder}")

      val fakeRootDir = new DocumentLibraryFolder()
      fakeRootDir.setSubItems(dl.getRootItems)

      mergeDirectory(fakeRootDir, mediaDir.get)

      dl.setRootItems(fakeRootDir.getSubItems)
    }
  }

  private def mergeDirectory(dlFolder: DocumentLibraryFolder, fileDataFolder: FileDataItem) {

    val fileEntries = fileDataFolder
      .listFiles()
      .filter(!_.getName.startsWith("."))

    if (fileEntries.length > 0 && dlFolder.getSubItems == null) {
      dlFolder.setSubItems(new jutil.ArrayList[DocumentLibraryItem]())
    }

    fileEntries.foreach { file =>
      if (file.isDirectory) {
        val existingFolderEntry = dlFolder.getSubItems
          .filter(_.isInstanceOf[DocumentLibraryFolder])
          .find(_.getName == file.getName)

        if (existingFolderEntry.isDefined) {
          mergeDirectory(existingFolderEntry.get.asInstanceOf[DocumentLibraryFolder], file)
        } else {
          val newFolderEntry = new DocumentLibraryFolder(file.getName)
          dlFolder.getSubItems.add(newFolderEntry)
          mergeDirectory(newFolderEntry, file)
        }

      } else {
        val existingFileEntry = dlFolder.getSubItems
          .filter(_.isInstanceOf[DocumentLibraryFile])
          .find(_.getName == file.getName)

        if (existingFileEntry.isEmpty) {
          val name = stripExtension(file.getName)
          val newFileEntry = new DocumentLibraryFile(name, file.getName)
          dlFolder.getSubItems.add(newFileEntry)
        }
      }
    }
  }

  private def stripExtension(name: String) =
    if (name.lastIndexOf('.') != -1) name.substring(0, name.lastIndexOf('.'))
    else name

}
