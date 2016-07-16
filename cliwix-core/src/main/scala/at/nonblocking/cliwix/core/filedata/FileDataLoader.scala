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

import java.io.{File, FileInputStream, InputStream}
import java.net.URI

sealed trait FileData {
  def getData: InputStream
  def getLastModified: Long
  def getSize: Long
}

sealed trait FileDataLoader {
  def getData(uri: URI): FileData
}

private[cliwix] class FileDataLocal(file: File) extends FileData {
  override def getData: InputStream = new FileInputStream(file)
  override def getSize: Long = file.length()
  override def getLastModified: Long = file.lastModified()
}

private[cliwix] class FileDataLoaderImpl extends FileDataLoader {

  override def getData(uri: URI) = {
    if (uri == null) {
      null
    } else {
      uri.getScheme match {
        case "file" =>
          val file = new File(uri)
          if (file.exists()) {
            new FileDataLocal(file)
          } else {
            null
          }
        //case "cliwix-remote":
      }
    }
  }


}
