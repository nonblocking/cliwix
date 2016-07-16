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
import at.nonblocking.cliwix.model.PortalPreferences
import com.liferay.portal.kernel.util.UnicodeProperties
import com.liferay.portal.service.{CompanyLocalService, PortalPreferencesLocalService}
import com.liferay.portal.util.PortletKeys

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class PortalPreferencesReadHandler extends Handler[PortalPreferencesReadCommand, PortalPreferences] {

  @BeanProperty
  var portalPreferencesService: PortalPreferencesLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: PortalPreferencesReadCommand): CommandResult[PortalPreferences] = {
    val prefs = this.portalPreferencesService
      .getPortalPreferenceses(0, this.portalPreferencesService.getPortalPreferencesesCount)
      .filter(p => p.getOwnerType == PortletKeys.PREFS_OWNER_TYPE_COMPANY && p.getOwnerId == command.companyId)

    val pref = if (prefs.size > 0) this.converter.convertToCliwixPortalPreferences(prefs(0)) else null

    logger.debug("Export portal preferences: {}", pref)

    CommandResult(pref)
  }
}

class PortalPreferencesUpdateHandler extends Handler[UpdateCommand[PortalPreferences], PortalPreferences] {

  @BeanProperty
  var portalPreferencesService: PortalPreferencesLocalService = _

  @BeanProperty
  var companyLocalService: CompanyLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: UpdateCommand[PortalPreferences]): CommandResult[PortalPreferences] = {
    val cliwixPortalPreferences = command.entity
    assert(cliwixPortalPreferences.getPortalPreferencesId != null, "portalPreferencesId != null")

    logger.debug("Updating portalPreferences: {}", cliwixPortalPreferences)

    val portalPreferences = this.portalPreferencesService.getPortalPreferences(cliwixPortalPreferences.getPortalPreferencesId)
    this.converter.mergeToLiferayPortalPreferences(cliwixPortalPreferences, portalPreferences)
    val updatedPortalPreferences = this.portalPreferencesService.updatePortalPreferences(portalPreferences)

    //Update locale and other settings (LDAP)
    val companyId = portalPreferences.getOwnerId
    val unicodeProperties = new UnicodeProperties()
    cliwixPortalPreferences.getPreferences.foreach(p => unicodeProperties.put(p.getName, p.getValue))
    this.companyLocalService.updatePreferences(companyId, unicodeProperties)

    val updatedCliwixPortalPreferences = this.converter.convertToCliwixPortalPreferences(updatedPortalPreferences)
    CommandResult(updatedCliwixPortalPreferences)
  }
}

class PortalPreferencesDeleteHandler extends Handler[DeleteCommand[PortalPreferences], PortalPreferences] {

  @BeanProperty
  var portalPreferencesService: PortalPreferencesLocalService = _

  override private[core] def handle(command: DeleteCommand[PortalPreferences]): CommandResult[PortalPreferences] = {
    logger.debug("Deleting portalPreferences: {}", command.entity)

    //In some Liferay versions deletePortalPreferences() returns an object in some not,
    //so we must use reflection here
    val deletePortalPreferencesMethod = this.portalPreferencesService.getClass.getMethod("deletePortalPreferences", classOf[Long])
    deletePortalPreferencesMethod.invoke(this.portalPreferencesService, command.entity.getPortalPreferencesId)

    CommandResult(null)
  }

}