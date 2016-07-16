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

package at.nonblocking.cliwix.webapp

import java.io.{FileInputStream, File}
import java.util.{ResourceBundle, Properties}
import javax.inject.Named

import at.nonblocking.cliwix.core.{Cliwix, CliwixException}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._

sealed trait WebappConfig {
  def getProperty(key: String): String
  def getMessage(key: String): String
  def getMessage(key: String, params: Any*): String
  def getWorkspaceDirectory: File
  def getExportDirectory: File
  def getImportDirectory: File
  def getTmpDirectory: File
}

object WebappConfig {
  val PROPERTY_CREATE_DUMMY_CORE_IF_NO_LIFERAY_FOUND = "cliwix.webapp.createDummyCoreIfNoLiferayFound"
  val PROPERTY_ENABLE_SECURITY = "cliwix.webapp.enableSecurity"

  val PROPERTIES_ALLOWED_TO_OVERRIDE = List(
    PROPERTY_ENABLE_SECURITY
  )

  val CLIWIX_WORKSPACE_PROPERTY = "CLIWIX_WORKSPACE"
  val MAIN_LIFERAY_CONFIG_FILE_NAME = "liferay-config.xml"
}

@Named
class WebappConfigImpl extends WebappConfig with LazyLogging {

  private val properties = new Properties()
  properties.load(getClass.getResourceAsStream("/cliwix-webapp.properties"))

  private val messages = ResourceBundle.getBundle("cliwix-webapp-messages")
  assert(messages != null)

  private val workspaceDirectory = new File(System.getProperty(WebappConfig.CLIWIX_WORKSPACE_PROPERTY))
  private val exportDirectory =  new File(this.workspaceDirectory, "export")
  private val importDirectory = new File(this.workspaceDirectory, "import")
  private val tmpDirectory = new File(this.workspaceDirectory, "tmp")

  try {
    this.workspaceDirectory.mkdirs()
    this.exportDirectory.mkdir()
    this.importDirectory.mkdir()
    this.tmpDirectory.mkdir()
  } catch {
    case e: Throwable => new CliwixException(s"Unable to create workspace directory ${this.workspaceDirectory.getAbsolutePath}")
  }

  System.setProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY, workspaceDirectory.getAbsolutePath)

  val overridePropertiesFile = new File(workspaceDirectory, Cliwix.OVERRIDE_PROPERTIES_FILENAME)
  if (overridePropertiesFile.exists()) {
    val inputStream = new FileInputStream(overridePropertiesFile)
    val overrideProperties = new Properties()
    overrideProperties.load(inputStream)
    inputStream.close()
    overrideProperties.foreach{ case (key, value) =>
      if (WebappConfig.PROPERTIES_ALLOWED_TO_OVERRIDE.contains(key)) {
        logger.debug("Overriding webapp property '{}' with value '{}'", key, value)
        properties.put(key, value)
      }
    }
  }

  override def getProperty(key: String) = this.properties.getProperty(key)

  override def getMessage(key: String) = this.messages.getString(key)

  override def getMessage(key: String, params: Any*) = this.messages.getString(key).format(params:_*)

  override def getWorkspaceDirectory = this.workspaceDirectory

  override def getExportDirectory = this.exportDirectory

  override def getImportDirectory = this.importDirectory

  override def getTmpDirectory = this.tmpDirectory
}
