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

package at.nonblocking.cliwix.core.liferay61.handler

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.PortletConfiguration
import com.liferay.portal.service._
import com.liferay.portal.util.PortletKeys

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import java.{util=>jutil}

class PortletConfigurationListHandler extends Handler[PortletConfigurationListCommand, jutil.Map[String, PortletConfiguration]] {

  @BeanProperty
  var portletPreferencesService: PortletPreferencesLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: PortletConfigurationListCommand): CommandResult[jutil.Map[String, PortletConfiguration]] = {
    val prefs = this.portletPreferencesService.getPortletPreferencesByPlid(command.portletLayoutId)

    val cliwixPrefsMap = prefs
      .filter(_.getOwnerType == PortletKeys.PREFS_OWNER_TYPE_LAYOUT)
      .map { p =>

        val cliwixPortletConfiguration = this.converter.convertToCliwixPortletConfiguration(p)

        logger.debug("Export portlet configuration: {}", cliwixPortletConfiguration)
        (cliwixPortletConfiguration.identifiedBy, cliwixPortletConfiguration)
      }

    CommandResult(new jutil.HashMap(cliwixPrefsMap.toMap))
  }
}

class PortletConfigurationInsertHandler extends Handler[PortletConfigurationInsertCommand, PortletConfiguration] {

  @BeanProperty
  var portletPreferencesService: PortletPreferencesLocalService = _

  @BeanProperty
  var layoutService: LayoutLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: PortletConfigurationInsertCommand): CommandResult[PortletConfiguration] = {
    val cliwixPortletConfiguration = command.portletConfiguration

    val page = this.layoutService.getLayout(command.portletLayoutId)
    val ownerId = PortletKeys.PREFS_OWNER_ID_DEFAULT
    val ownerType = PortletKeys.PREFS_OWNER_TYPE_LAYOUT
    val portlet = null

    logger.debug("Adding portlet configuration: {}", cliwixPortletConfiguration)

    val insertedPortletPreferences = this.portletPreferencesService.addPortletPreferences(page.getCompanyId, ownerId, ownerType,
      command.portletLayoutId, cliwixPortletConfiguration.getPortletId, portlet, null)

    this.converter.mergeToLiferayPortletPreferences(cliwixPortletConfiguration, insertedPortletPreferences)

    val updatedPortletPreferences = this.portletPreferencesService.updatePortletPreferences(insertedPortletPreferences)

    val insertedCliwixPortletConfiguration = this.converter.convertToCliwixPortletConfiguration(updatedPortletPreferences)
    CommandResult(insertedCliwixPortletConfiguration)
  }

}

class PortletConfigurationUpdateHandler extends Handler[UpdateCommand[PortletConfiguration], PortletConfiguration] {

  @BeanProperty
  var portletPreferencesService: PortletPreferencesLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[PortletConfiguration]): CommandResult[PortletConfiguration] = {
    val cliwixPortletConfiguration = command.entity
    assert(cliwixPortletConfiguration.getPortletPreferencesId != null)

    logger.debug("Updating portlet configuration: {}", cliwixPortletConfiguration)

    val portletPreferences = this.portletPreferencesService.getPortletPreferences(cliwixPortletConfiguration.getPortletPreferencesId)

    this.converter.mergeToLiferayPortletPreferences(cliwixPortletConfiguration, portletPreferences)

    val updatedPortletPreferences = this.portletPreferencesService.updatePortletPreferences(portletPreferences)

    val updatedCliwixPortletConfiguration = this.converter.convertToCliwixPortletConfiguration(updatedPortletPreferences)
    CommandResult(updatedCliwixPortletConfiguration)
  }

}

class PortletConfigurationDeleteHandler extends Handler[DeleteCommand[PortletConfiguration], PortletConfiguration] {

  @BeanProperty
  var portletPreferencesService: PortletPreferencesLocalService = _

  override private[core] def handle(command: DeleteCommand[PortletConfiguration]): CommandResult[PortletConfiguration] = {
    logger.debug("Deleting portlet configuration: {}", command.entity)

    //In some Liferay versions deletePortalPreferences() returns an object in some not,
    //so we must use reflection here
    val deletePortletPreferencesMethod = this.portletPreferencesService.getClass.getMethod("deletePortletPreferences", classOf[Long])
    deletePortletPreferencesMethod.invoke(this.portletPreferencesService, command.entity.getPortletPreferencesId)

    CommandResult(null)
  }

}