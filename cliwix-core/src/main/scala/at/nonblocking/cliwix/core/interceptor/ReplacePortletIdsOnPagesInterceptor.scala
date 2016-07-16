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


import at.nonblocking.cliwix.core.util.{ListTypeUtils, PortletUtil}
import at.nonblocking.cliwix.model.{Pages, PageSetting, Page}
import com.liferay.portal.model.PortletConstants
import com.liferay.util.PwdGenerator
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Removes unused portlet configurations from the page and replaces _INSTANCE_ qualifiers by a simple number.
 */
class ReplacePortletIdsOnPagesInterceptor extends TreeProcessingInterceptor[Page, Pages] with ListTypeUtils with LazyLogging {

  val COLUMN_PREFIX = "column-"

  @BeanProperty
  var portletUtil: PortletUtil = _

  override def beforeEntityInsert(page: Page, companyId: Long) = {
    val portletsOnPage = if (page.getPageSettings != null) getPortletIds(page.getPageSettings.toList) else Nil
    val portletsOnPageWithNumberQualifier = portletsOnPage.filter(_.contains("#"))

    removeAllNonExistingIds(page, portletsOnPage)

    val replacementMap = mutable.HashMap[String, String]()

    if (portletsOnPageWithNumberQualifier.nonEmpty) {
      portletsOnPageWithNumberQualifier.foreach { id =>
        val baseId = id.split("#")(0)
        val newPortletId = baseId + PortletConstants.INSTANCE_SEPARATOR + generateInstanceQualifier
        logger.info(s"Replacing portlet id $id through new $newPortletId on page ${page.identifiedBy()}")
        replacementMap += (id -> newPortletId)
      }
    }

    addNonQualifiedPortletsWhichAreInstanciable(page, portletsOnPage, replacementMap)

    replacePortletIds(page, replacementMap.toMap)
  }

  override def beforeEntityUpdate(page: Page, existingPage: Page, companyId: Long) = {
    val portletsOnPage = if (page.getPageSettings != null) getPortletIds(page.getPageSettings.toList) else Nil
    val portletsOnPageWithNumberQualifier = portletsOnPage.filter(_.contains("#"))

    removeAllNonExistingIds(page, portletsOnPage)

    val replacementMap = mutable.HashMap[String, String]()

    if (portletsOnPageWithNumberQualifier.nonEmpty) {
      findPortletIdMatchesOnExistingPage(page, existingPage, replacementMap)

      portletsOnPageWithNumberQualifier.foreach { id =>
        if (!replacementMap.contains(id)) {
          val baseId = id.split("#")(0)
          val newPortletId = baseId + PortletConstants.INSTANCE_SEPARATOR + generateInstanceQualifier
          logger.info(s"Replacing portlet id $id through new $newPortletId on page ${page.identifiedBy()}")
          replacementMap += (id -> newPortletId)
        }
      }
    }

    addNonQualifiedPortletsWhichAreInstanciable(page, portletsOnPage, replacementMap)

    replacePortletIds(page, replacementMap.toMap)
  }

  override def afterEntityExport(page: Page, companyId: Long) = {
    val portletsOnPage = if (page.getPageSettings != null) getPortletIds(page.getPageSettings.toList)
    else Nil

    removeAllNonExistingIds(page, portletsOnPage)
    replaceInstanceQualifiers(page, portletsOnPage)
  }

  private def findPortletIdMatchesOnExistingPage(page: Page, existingPage: Page, replacementMap: mutable.HashMap[String, String]) = {
    if (existingPage.getPageSettings != null) {
      val columns = page.getPageSettings.filter(_.getKey.startsWith(COLUMN_PREFIX)).map(_.getKey)

      columns.foreach { column =>
        val newColumn = page.getPageSettings.find(_.getKey == column)
        val existingColumn = existingPage.getPageSettings.find(_.getKey == column)

        if (newColumn.isDefined && existingColumn.isDefined) {
          val newInstanciablePortlets = newColumn.get.getValue.split(",").filter(id => id != null && !id.isEmpty && id.contains("#"))
          val existingInstanciablePortlets = existingColumn.get.getValue.split(",").filter(id => id != null && !id.isEmpty  && id.contains(PortletConstants.INSTANCE_SEPARATOR))
          val usedExistingIds = mutable.MutableList[String]()

          for (id <- newInstanciablePortlets) {
            val baseId = id.split("#")(0)
            val existingId = existingInstanciablePortlets.find(id => id.startsWith(baseId + PortletConstants.INSTANCE_SEPARATOR) && !usedExistingIds.contains(id))
            if (existingId.isDefined) {
              logger.info(s"Replacing portlet id $id through existing id ${existingId.get} on page ${page.identifiedBy()}")
              replacementMap += (id -> existingId.get)
              usedExistingIds += existingId.get
            }
          }
        }
      }
    }
  }

  private def addNonQualifiedPortletsWhichAreInstanciable(page: Page, portletsOnPage: List[String], replacementMap: mutable.HashMap[String, String]) = {
    portletsOnPage.filter(!_.contains("#")).foreach{ id =>
      val portlet = this.portletUtil.getPortletById(id)
      if (portlet.isDefined && portlet.get.isInstanceable) {
        val newPortletId = id + PortletConstants.INSTANCE_SEPARATOR + generateInstanceQualifier
        logger.info(s"Replacing portlet id $id through $newPortletId on page ${page.identifiedBy()}")
        replacementMap += (id -> newPortletId)
      }
    }
  }

  private def generateInstanceQualifier = PwdGenerator.getPassword(PwdGenerator.KEY1 + PwdGenerator.KEY2 + PwdGenerator.KEY3, 12)

  private def replaceInstanceQualifiers(page: Page, portletsOnPage: List[String]) = {
    val replacementMap = mutable.HashMap[String, String]()
    portletsOnPage.filter(_.contains(PortletConstants.INSTANCE_SEPARATOR)).foreach{ id =>
      val baseId = id.split(PortletConstants.INSTANCE_SEPARATOR)(0)
      val nr = replacementMap.values.count(_.startsWith(baseId)) + 1
      val newPortletId = baseId + "#" + nr
      logger.info(s"Replacing portlet id $id through $newPortletId on page ${page.identifiedBy()}")
      replacementMap += (id -> newPortletId)
    }

    replacePortletIds(page, replacementMap.toMap)
  }

  private def getPortletIds(pageSettings: List[PageSetting]) =
    pageSettings.filter(_.getKey.startsWith(COLUMN_PREFIX)).flatMap(_.getValue.split("[,]")).filter(p => p != null && !p.isEmpty)

  private def replacePortletIds(page: Page, replacementMap: Map[String, String]) = replacementMap.foreach{ case(originalId, newId) =>
    if (page.getPageSettings != null) page.getPageSettings
      .filter(_.getKey.startsWith(COLUMN_PREFIX))
      .foreach { ps =>
        ps.setValue(ps.getValue.split(",").map{ id =>
          if (id.trim == originalId) newId
          else id
        }
        .mkString(","))
      }
    if (page.getPortletConfigurations != null) {
      page.getPortletConfigurations.getList
        .find(_.getPortletId == originalId)
        .foreach(pc => pc.setPortletId(newId))
    }

    safeForeach(page.getPortletConfigurations){ pc =>
      if (pc.getPortletId == originalId) pc.setPortletId(newId)
    }
  }

  private def removeAllNonExistingIds(page: Page, portletsOnPage: List[String]) = {
    if (page.getPortletConfigurations != null && page.getPortletConfigurations.getList != null) {
      val unusedPortlets = page.getPortletConfigurations.getList.filter(pc => !portletsOnPage.contains(pc.getPortletId))
      for (portletToRemove <- unusedPortlets) {
        logger.info(s"Removing unsued portlet configuration ${portletToRemove.getPortletId} on page ${page.identifiedBy()}")
        page.getPortletConfigurations.getList.remove(portletToRemove)
      }
    }
  }

}
