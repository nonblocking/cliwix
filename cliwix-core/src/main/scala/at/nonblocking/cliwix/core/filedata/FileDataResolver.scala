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

package at.nonblocking.cliwix.core.filedata

import java.io.File
import java.net.URI

trait FileDataItem {
  def listFiles(): Array[FileDataItem]
  def exists(): Boolean
  def isDirectory: Boolean
  def getName: String
  def lastModified(): Long
  def getUnderlying: AnyRef

  def getSubItemByRelativePath(name: String): Option[FileDataItem] = {
    val parts = name.replaceAll("\\\\", "/").split("/")

    if (parts.nonEmpty) {
      var current = this
      var i = 0
      while (current != null && i < parts.length) {
        val sub = current.listFiles().find(_.getName == parts(i))
        current = if (sub.isDefined) sub.get else null
        i = i + 1
      }

      Option(current)
    } else {
      None
    }
  }
}

private[cliwix] trait FileDataResolver {
  def getRoot: FileDataItem
  def getURI(item: FileDataItem): URI
}

private[cliwix] class FileDataResolverFileSystemImpl(rootFolder: File) extends  FileDataResolver {

  override def getRoot: FileDataItem = new FileWrapper(rootFolder)

  override def getURI(item: FileDataItem) = item.getUnderlying.asInstanceOf[File].toURI

  private class FileWrapper(underlying: File) extends FileDataItem {
    override def listFiles(): Array[FileDataItem] = underlying.listFiles().map(f => new FileWrapper(f))
    override def getName: String = underlying.getName
    override def lastModified(): Long = underlying.lastModified()
    override def getUnderlying = underlying
    override def isDirectory = underlying.isDirectory
    override def exists() = underlying.exists()
  }
}
