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

package at.nonblocking.cliwix.core

import java.io.{FileOutputStream, FileInputStream, File}
import java.util.Properties

class CliwixInfoProperties(baseFolder: File) extends Properties {
  assert(baseFolder.isDirectory || !baseFolder.exists(), "baseFolder is directory")

  if (!baseFolder.exists()) baseFolder.mkdirs()

  private val propertyFile = new File(baseFolder, CliwixInfoProperties.FILE_NAME)
  if (propertyFile.exists()) {
    val is = new FileInputStream(this.propertyFile)
    load(is)
    is.close()
  }

  def save() = {
    val os = new FileOutputStream(this.propertyFile)
    store(os, null)
    os.close()
  }

  def exists = this.propertyFile.exists()

  def setConfigProperty(configObject: Any) = setProperty(CliwixInfoProperties.PROPERTY_CONFIG, configObject.toString)
  def setStateProperty(state: String) = setProperty(CliwixInfoProperties.PROPERTY_STATE, state)
  def setErrorMessageProperty(throwable: Throwable) = {
    val message = if (throwable.getMessage != null) throwable.getMessage
      else if (throwable.getCause != null && throwable.getCause.getMessage != null) throwable.getCause.getMessage
      else throwable.getClass.getName
    setProperty(CliwixInfoProperties.PROPERTY_ERROR_MESSAGE, message)
  }
  def setDurationProperty(durationMs: Long) = setProperty(CliwixInfoProperties.PROPERTY_DURATION_MS, durationMs.toString)

  def getStateProperty = getProperty(CliwixInfoProperties.PROPERTY_STATE)
  def getErrorMessageProperty = getProperty(CliwixInfoProperties.PROPERTY_ERROR_MESSAGE)
  def getDurationProperty = {
    val durationStr = getProperty(CliwixInfoProperties.PROPERTY_DURATION_MS)
    if (durationStr != null) {
      durationStr.toLong
    } else {
      -1L
    }
  }
}

object CliwixInfoProperties {
  val FILE_NAME = ".info.cliwix"

  val PROPERTY_CONFIG = "config"
  val PROPERTY_STATE = "state"
  val PROPERTY_ERROR_MESSAGE = "error.message"
  val PROPERTY_DURATION_MS = "duration.ms"

  val STATE_SUCCESS = "success"
  val STATE_FAILED = "failed"
  val STATE_PROCESSING = "processing"
}
