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

import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.model.Portlet
import com.liferay.portal.model.PortletConstants
import com.liferay.portal.service.PortletLocalService

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

sealed trait PortletUtil {
  def getAllPortlets: List[Portlet]
  def getPortletById(portletId: String): Option[Portlet]
}

private[core] class PortletUtilImpl extends PortletUtil {

  @BeanProperty
  var portletService: PortletLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override def getAllPortlets = this.portletService.getPortlets.map(this.converter.convertToCliwixPortlet).toList

  override def getPortletById(portletId: String) = {
    val id = if (portletId.contains(PortletConstants.INSTANCE_SEPARATOR)) portletId.split(PortletConstants.INSTANCE_SEPARATOR)(0)
      else portletId
    val portlet = this.portletService.getPortletById(id)

    if (portlet == null) None
    else Some(this.converter.convertToCliwixPortlet(portlet))
  }

}
