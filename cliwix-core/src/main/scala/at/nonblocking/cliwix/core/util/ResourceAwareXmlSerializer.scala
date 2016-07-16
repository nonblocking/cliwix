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

package at.nonblocking.cliwix.core.util

import java.io.{File, OutputStream}
import javax.xml.bind.ValidationEventHandler

import at.nonblocking.cliwix.core._
import at.nonblocking.cliwix.model.LiferayConfig
import at.nonblocking.cliwix.model.xml.CliwixXmlSerializer
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty

/**
 * Can load huge XML files by using MapDb disk persistence for potential huge lists
 */
sealed trait ResourceAwareXmlSerializer {

  def fromXML(xmlFile: File, validationListener: ValidationEventHandler = null): LiferayConfig

  def writeXML(liferayConfig: LiferayConfig, outputStream: OutputStream): Unit

}

private[core] class ResourceAwareXmlSerializerImpl extends ResourceAwareXmlSerializer with LazyLogging {

  @BeanProperty
  var resourceAwareCollectionFactory: ResourceAwareCollectionFactory = _

  val availableRamMB = Runtime.getRuntime.maxMemory() / 1024 / 1024
  val xmlOverheadFactor = 5

  override def fromXML(xmlFile: File, validationListener: ValidationEventHandler) = {
    val fileSizeMB = xmlFile.length() / 1024 / 1024
    val enableListDiskCaching = fileSizeMB * xmlOverheadFactor > availableRamMB

    if (enableListDiskCaching) {
      logger.info(s"Loading large XML file with $fileSizeMB MB: Enabling disk caching of lists.")
      val listener = new UseMapDbCollectionForPotentialHugeListsUnmarshalListener(getPotentialHugeListClasses, this.resourceAwareCollectionFactory)
      CliwixXmlSerializer.fromXML(xmlFile, listener, validationListener)
    } else {
      CliwixXmlSerializer.fromXML(xmlFile, null, validationListener)
    }
  }

  private def getPotentialHugeListClasses = {
    val listClassNames = Cliwix.getProperty(Cliwix.PROPERTY_POTENTIAL_HUGE_LISTS).split("[,;]")
    listClassNames.map(name => Class.forName(MODEL_PACKAGE_NAME + "." + name)).toList
  }

  override def writeXML(liferayConfig: LiferayConfig, outputStream: OutputStream) = CliwixXmlSerializer.toXML(liferayConfig, outputStream)

}
