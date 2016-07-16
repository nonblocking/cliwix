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

import java.io.{FileInputStream, IOException, FileOutputStream, File}
import java.util.Properties
import javax.servlet.{ServletContextEvent, ServletContextListener}

import org.apache.commons.io.IOUtils

class StartupListener extends ServletContextListener {

  private val HOME_DIRECTORY = System.getProperty("user.home").replaceAll("\\\\", "/")

  //Don't use slf4j logging here, because logback requires the CLIWIX_WORKSPACE property and would be initialized to early!

  override def contextInitialized(sce: ServletContextEvent): Unit = {
    if (System.getenv(WebappConfig.CLIWIX_WORKSPACE_PROPERTY) != null) {
      //Copy environment variable to system property
      System.setProperty(WebappConfig.CLIWIX_WORKSPACE_PROPERTY, System.getenv(WebappConfig.CLIWIX_WORKSPACE_PROPERTY))
    }

    if (System.getProperty(WebappConfig.CLIWIX_WORKSPACE_PROPERTY) == null) {
      val defaultWD = HOME_DIRECTORY + "/.cliwix"
      println(s"Setting workspace directory to: $defaultWD. Change it by setting a environment variable or system property CLIWIX_WORKSPACE.")
      System.setProperty(WebappConfig.CLIWIX_WORKSPACE_PROPERTY, defaultWD)
    } else {
      println("Found system property CLIWIX_WORKSPACE=" + System.getProperty(WebappConfig.CLIWIX_WORKSPACE_PROPERTY))
    }

    copyCliwixConfigurationOverrideTemplateIfNotExists()

    configureLogging()
  }

  def configureLogging() = {
    val workspaceDir = new File(System.getProperty(WebappConfig.CLIWIX_WORKSPACE_PROPERTY))
    val propertyFile = new File(workspaceDir, "cliwix.properties")
    val properties = new Properties()

    safeAccess(propertyFile.getAbsolutePath, new FileInputStream(propertyFile)) { propertyFileIS =>
      properties.load(propertyFileIS)
    }

    val configFile =
      if (properties.getProperty("cliwix.enableDebugLog") != null) "logback_debug.xml"
      else "logback_prod.xml"

    System.setProperty("logback.configurationFile", configFile)
  }

  def copyCliwixConfigurationOverrideTemplateIfNotExists() = {
    val workspaceDir = new File(System.getProperty(WebappConfig.CLIWIX_WORKSPACE_PROPERTY))
    if (!workspaceDir.exists()) workspaceDir.mkdirs()

    val propertyFile = new File(workspaceDir, "cliwix.properties")
    if (!propertyFile.exists()) {
      val templateIS = Thread.currentThread().getContextClassLoader.getResourceAsStream("cliwix-overrides-template.properties")
      if (templateIS != null) {
        println("Creating empty Cliwix property file: " + propertyFile.getAbsolutePath)
        safeCreate(propertyFile.getAbsolutePath, new FileOutputStream(propertyFile)) { propertyFileOS =>
          IOUtils.copy(templateIS, propertyFileOS)
        }
      } else {
        println("Error: No 'cliwix-overrides-template.properties' found in classpath!")
      }
    }
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {}


  private def safeAccess(path: String, instantiator: => FileInputStream)(closure: FileInputStream => Unit) = {
    val fis =
      try {
        instantiator
      } catch {
        case e: Throwable =>
          println(s"ERROR: Failed to access resource: $path")
          null
      }
    if (fis != null) {
      try {
        closure(fis)
      } catch {
        case e: Throwable =>
          println(s"ERROR: Failed to access resource: $path")
      } finally {
        try { fis.close() } catch { case _: Throwable => }
      }
    }
  }

  private def safeCreate(path: String, instantiator: => FileOutputStream)(closure: FileOutputStream => Unit) = {
    val fos =
      try {
        instantiator
      } catch {
        case e: Throwable =>
          println(s"ERROR: Failed to create file $path. Does the server have sufficient access rights?")
          null
      }
    if (fos != null) {
      try {
        closure(fos)
      } catch {
        case e: Throwable =>
          println(s"ERROR: Failed to create file $path. Does the server have sufficient access rights?")
      } finally {
        try { fos.close() } catch { case _: Throwable => }
      }
    }
  }
}
