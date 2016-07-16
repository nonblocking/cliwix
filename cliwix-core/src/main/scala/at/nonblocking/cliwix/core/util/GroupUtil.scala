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

import at.nonblocking.cliwix.core.CliwixException
import at.nonblocking.cliwix.model._
import com.liferay.portal.service.GroupLocalService

import com.liferay.portal.{model => liferay, NoSuchGroupException}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty

trait GroupUtil {
  def getLiferayEntityForGroupId(groupId: Long): Option[(Class[_ <: LiferayEntity], Long)]
}

private[core] class GroupUtilImpl extends GroupUtil with LazyLogging {

  @BeanProperty
  var groupService: GroupLocalService = _

  /*
    Possible group classNames:

    com.liferay.portal.model.Company
    com.liferay.portal.model.Group
    com.liferay.portal.model.LayoutPrototype
    com.liferay.portal.model.LayoutSetPrototype
    com.liferay.portal.model.Organization
    com.liferay.portal.model.User
    com.liferay.portal.model.UserGroup
    com.liferay.portal.model.UserPersonalSite
   */

  val companyClassName = classOf[liferay.Company].getName
  val siteClassName = classOf[liferay.Group].getName
  val organizationClassName = classOf[liferay.Organization].getName
  val userClassName = classOf[liferay.User].getName
  val userGroupClassName = classOf[liferay.UserGroup].getName

  def getLiferayEntityForGroupId(groupId: Long): Option[(Class[_ <: LiferayEntity], Long)] = {
    try {
      val group = this.groupService.getGroup(groupId)

      group.getClassName match {
        case `companyClassName` => Some(classOf[Company], group.getClassPK)
        case `siteClassName` =>  Some(classOf[Site], group.getClassPK)
        case `organizationClassName` =>  Some(classOf[Organization], group.getClassPK)
        case `userClassName` =>  Some(classOf[User], group.getClassPK)
        case `userGroupClassName` =>  Some(classOf[UserGroup], group.getClassPK)
        case _ => throw new CliwixException(s"Group class ${group.getClassName} cannot be handled!")
      }
    } catch {
      case e: NoSuchGroupException =>
        logger.warn(s"No group with id $groupId found")
        None
    }
  }


}
