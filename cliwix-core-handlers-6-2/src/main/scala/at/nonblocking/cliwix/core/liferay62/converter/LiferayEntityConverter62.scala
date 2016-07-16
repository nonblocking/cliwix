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

package at.nonblocking.cliwix.core.liferay62.converter

import at.nonblocking.cliwix.core.liferay61.converter.LiferayEntityConverter61
import at.nonblocking.cliwix.model.{MemberUser, MemberUserGroup, _}
import com.liferay.portal.service.UserGroupLocalService
import com.liferay.portal.{model => liferay}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

private[core] class LiferayEntityConverter62 extends LiferayEntityConverter61 {

  override protected def getVirtualHost(companyId: Long, siteId: Long, privatePages: Boolean): String = {
    val layoutSet: liferay.LayoutSet = this.layoutSetService.getLayoutSet(siteId, privatePages)
    if (layoutSet != null) {
      //In 6.2 it is necessary to reset the cache
      layoutSet.setVirtualHostname(null)

      val virtualHost = layoutSet.getVirtualHostname
      if (virtualHost.isEmpty) null
      else virtualHost
    } else null
  }

}
