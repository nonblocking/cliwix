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

import at.nonblocking.cliwix.core.filedata.FileDataItem
import at.nonblocking.cliwix.core.{Reporting, ExecutionContext}
import at.nonblocking.cliwix.core.util.ListTypeUtils
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._
import java.{util=>jutil}

class SetFileDataOnFileEntriesInterceptor extends TreeProcessingInterceptor[DocumentLibraryItem, DocumentLibrary] with Reporting with LazyLogging with ListTypeUtils {

  override def beforeTreeImport(dl: DocumentLibrary, companyId: Long) {
    assert(ExecutionContext.fileDataResolver != null)

    val mediaDir = ExecutionContext.fileDataResolver.getRoot.getSubItemByRelativePath(dl.getFileDataFolder)
    if (mediaDir.isEmpty) throw new CliwixValidationException(s"File data root folder not found: ${dl.getFileDataFolder}")

    processRecursively(dl.getRootItems, mediaDir)(_.getSubItems) { (item, parentFileDataItem)  =>
      item match {
        case dlFile: DocumentLibraryFile =>
          val fileDataItem =
            if (parentFileDataItem.isDefined) parentFileDataItem.get.getSubItemByRelativePath(dlFile.getFileDataName)
            else None

          if (fileDataItem.isEmpty) {
            report.addWarning(s"No file data found for file: ${dlFile.getName}. If this file is new, import will fail.")
            dlFile.setFileDataUpdateTimestamp(0)
          } else {
            dlFile.setFileDataUpdateTimestamp(fileDataItem.get.lastModified())
            dlFile.setFileDataUri(ExecutionContext.fileDataResolver.getURI(fileDataItem.get))
          }
        case _ =>
      }
    }
  }

  private def processRecursively(items: jutil.List[DocumentLibraryItem], parentFileDataItem: Option[FileDataItem])(subItems: DocumentLibraryItem => jutil.List[DocumentLibraryItem])
                           (closure: (DocumentLibraryItem, Option[FileDataItem]) => Unit): Unit =
    if (items != null) items.foreach { it =>
      closure(it, parentFileDataItem)
      if (it.isInstanceOf[DocumentLibraryFolder]) {

        val fileDataItemFolder =
          if (parentFileDataItem.isDefined) parentFileDataItem.get.getSubItemByRelativePath(it.getName)
          else None

        processRecursively(subItems(it), fileDataItemFolder)(subItems)(closure)
      }
    }

}
