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

import java.io.{FileInputStream, File}
import java.util.{Date, Properties}

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil
import com.liferay.portal.kernel.util.ReleaseInfo
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext

import scala.collection.JavaConversions._

/**
 * The Cliwix core
 */
sealed trait Cliwix {
  def getExporter: LiferayExporter
  def getImporter: LiferayImporter
  def getLiferayInfo: LiferayInfo
  def getLiferayAuthenticator: LiferayAuthenticator
}

object Cliwix extends LazyLogging {

  val CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY = "CLIWIX_CUSTOM_CONFIGURATION"

  val PROPERTY_VERSION = "cliwix.version"
  val PROPERTY_SUPPORTED_LIFERAY_VERSIONS = "cliwix.supportedLiferayVersions"
  val PROPERTY_STORE_ACTUAL_FILENAME_IN_DESCRIPTION = "cliwix.storeActualFileNameInDescription"
  val PROPERTY_IGNORE_REGULAR_ROLE_ASSIGNMENTS = "cliwix.ignoreRegularRoleAssignments"
  val PROPERTY_POTENTIAL_HUGE_LISTS = "cliwix.potentialHugeLists"
  val PROPERTY_CLEAR_LIFERAY_CACHES_BEFORE_IMPORTEXPORT = "cliwix.clearLiferayCachesBeforeImportExport"
  val PROPERTY_DISABLED_LIFERAY_CACHES_DURING_IMPORTEXPORT = "cliwix.disableLiferayCachesDuringImportExport"

  val OVERRIDE_PROPERTIES_FILENAME = "cliwix.properties"

  val PROPERTIES_ALLOWED_TO_OVERRIDE = List(
    PROPERTY_STORE_ACTUAL_FILENAME_IN_DESCRIPTION,
    PROPERTY_IGNORE_REGULAR_ROLE_ASSIGNMENTS,
    PROPERTY_CLEAR_LIFERAY_CACHES_BEFORE_IMPORTEXPORT,
    PROPERTY_DISABLED_LIFERAY_CACHES_DURING_IMPORTEXPORT
  )

  private val IMPL_CLASS_NAME = "at.nonblocking.cliwix.core.CliwixImpl"

  private val properties = new Properties()
  properties.load(getClass.getResourceAsStream("/cliwix-core.properties"))

  private val dateFormat = new java.text.SimpleDateFormat("yyy-MM-dd HH:mm:ss (zzz)")

  private var instance: Cliwix = _

  private var overridePropertiesMerged = false

  @throws[CliwixLiferayNotFoundException]
  @throws[CliwixLiferayNotSupportedException]
  @throws[CliwixLiferayNotReadyException]
  def apply(createDummyIfNoLiferayFound: Boolean = false): Cliwix = {
    if (this.instance != null) {
      this.instance
    } else {

      mergeOverrideProperties()

      try {
        val implClass = Class.forName(IMPL_CLASS_NAME)
        this.instance = implClass.newInstance().asInstanceOf[Cliwix]
        this.instance
      } catch {
        case e: NoClassDefFoundError =>
          if (createDummyIfNoLiferayFound) {
            logger.error("No Liferay API found in classpath! Switching to Dummy implementation.")
            new CliwixDummyImpl
          } else {
            throw new CliwixLiferayNotFoundException
          }
      }
    }
  }

  def getProperty(key: String) = this.properties.getProperty(key)

  def formatDate(date: Date) = dateFormat.format(date)

  def getVersion = getProperty(Cliwix.PROPERTY_VERSION)

  private def mergeOverrideProperties() = {
    if (!overridePropertiesMerged && System.getProperty(CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY) != null) {
      val overridePropertiesFile = new File(System.getProperty(CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY) + File.separator + Cliwix.OVERRIDE_PROPERTIES_FILENAME)
      if (overridePropertiesFile.exists()) {
        val inputStream = new FileInputStream(overridePropertiesFile)
        val overrideProperties = new Properties()
        overrideProperties.load(inputStream)
        inputStream.close()
        overrideProperties.foreach{ case (key, value) =>
          if (Cliwix.PROPERTIES_ALLOWED_TO_OVERRIDE.contains(key)) {
            logger.debug("Overriding core property '{}' with value '{}'", key, value)
            properties.put(key, value)
          }
        }
      }
      overridePropertiesMerged = true
    }
  }
}

/*
 * Default impl
 */
class CliwixImpl extends Cliwix {

  private val cliwixCoreContext = "/META-INF/cliwix-core-context.xml"
  private val cliwixCoreHandlersContext = "classpath*:META-INF/cliwix-core-liferay-%s-context.xml"

  private val context: ApplicationContext = createContext()

  override def getExporter = getContext.getBean(classOf[LiferayExporter])

  override def getImporter = getContext.getBean(classOf[LiferayImporter])

  override def getLiferayInfo = getContext.getBean(classOf[LiferayInfo])

  def getLiferayAuthenticator = getContext.getBean(classOf[LiferayAuthenticator])

  private[core] def getContext = this.context

  private def createContext() = {
    if (!checkLiferaySupported()) {
      throw new CliwixLiferayNotSupportedException()
    } else if (!checkLiferayStarted()) {
      throw new CliwixLiferayNotReadyException
    } else {
      val liferayHandlerContextConfiguration = cliwixCoreHandlersContext.format(liferayBaseVersion.replace(".", "_"))
      new ClassPathXmlApplicationContext(cliwixCoreContext, liferayHandlerContextConfiguration)
    }
  }

  private def checkLiferayStarted() = {
    PortalBeanLocatorUtil.getBeanLocator != null
  }

  private def checkLiferaySupported() = {
    val supportedVersions = Cliwix.getProperty(Cliwix.PROPERTY_SUPPORTED_LIFERAY_VERSIONS).split("[,;]")
    supportedVersions.contains(liferayBaseVersion)
  }

  private def liferayBaseVersion = ReleaseInfo.getVersion.split("\\.").slice(0, 2).mkString(".")
}

class CliwixDummyImpl extends Cliwix {
  override def getExporter: LiferayExporter = new LiferayExporterDummyImpl

  override def getImporter: LiferayImporter = new LiferayImporterDummyImpl

  override def getLiferayInfo: LiferayInfo =
    new LiferayInfo {
      override def getReleaseInfo: String = "Liferay 6.1.2-Dummy"
      override def getVersion: String = "6.1.2-Dummy"
      override def getBaseVersion: String = "6.1"
    }

  def getLiferayAuthenticator = new LiferayAuthenticatorDummyImpl

}