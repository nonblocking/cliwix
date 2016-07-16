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

package at.nonblocking.cliwix.core.liferay61.util

import java.{util => jutil}

import at.nonblocking.cliwix.core.handler.ErrorHandling
import at.nonblocking.cliwix.model.{MemberOrganization, MemberUser, MemberUserGroup}
import com.liferay.portal.service._

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

trait RoleAssignmentUtil {
  def getMemberUserGroupGroupIds(companyId: Long, memberUserGroups: jutil.List[MemberUserGroup]): List[Long]
  def getMemberUserGroupIds(companyId: Long, memberUserGroups: jutil.List[MemberUserGroup]): List[Long]
  def getMemberOrganizationGroupIds(companyId: Long, memberOrganizations: jutil.List[MemberOrganization]): List[Long]
  def getMemberUserIds(companyId: Long, memberUsers: jutil.List[MemberUser]): List[Long]
  def compatibleAddUserGroupRoles(userIds: Array[Long], siteId: Long, roleId: Long): Unit
}

class RoleAssignmentUtilImpl extends RoleAssignmentUtil with ErrorHandling {

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var userGroupRoleService: UserGroupRoleLocalService = _

  override def getMemberUserGroupGroupIds(companyId: Long, memberUserGroups: jutil.List[MemberUserGroup]) =
    memberUserGroups.map { memberUserGroup =>
      handleNoSuchUserGroup(memberUserGroup.getName) {
        this.userGroupService.getUserGroup(companyId, memberUserGroup.getName).getGroup
      }
    }
    .filter(_ != null)
    .map(_.getGroupId)
    .toList

  override def getMemberUserGroupIds(companyId: Long, memberUserGroups: jutil.List[MemberUserGroup]) =
    memberUserGroups.map { memberUserGroup =>
      handleNoSuchUserGroup(memberUserGroup.getName) {
        this.userGroupService.getUserGroup(companyId, memberUserGroup.getName)
      }
    }
    .filter(_ != null)
    .map(_.getUserGroupId)
    .toList

  override def getMemberOrganizationGroupIds(companyId: Long, memberOrganizations: jutil.List[MemberOrganization]) =
    memberOrganizations.map { memberUserOrg =>
      handleNoSuchOrganization(memberUserOrg.getName) {
        this.organizationService.getOrganization(companyId, memberUserOrg.getName).getGroup
      }
    }
    .filter(_ != null)
    .map(_.getGroupId)
    .toList

  override def getMemberUserIds(companyId: Long, memberUsers: jutil.List[MemberUser]) =
    memberUsers.map { memberUser =>
      handleNoSuchUser(memberUser.getScreenName) {
        this.userService.getUserByScreenName(companyId, memberUser.getScreenName)
      }
    }
    .filter(_ != null)
    .map(_.getUserId)
    .toList

  override def compatibleAddUserGroupRoles(userIds: Array[Long], siteId: Long, roleId: Long) = {
    //In some Liferay versions addUserGroupRoles() returns a list in some not,
    //so we must use reflection here
    val addUserGroupRolesMethod = this.userGroupRoleService.getClass.getMethod("addUserGroupRoles", classOf[Array[Long]], classOf[Long], classOf[Long])
    addUserGroupRolesMethod.invoke(this.userGroupRoleService, userIds.toArray, siteId.asInstanceOf[AnyRef], roleId.asInstanceOf[AnyRef])
  }

}
