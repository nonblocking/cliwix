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
import at.nonblocking.cliwix.core.util.ResourceAwareCollectionFactory
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.model._
import com.liferay.portal.model.RoleConstants
import com.liferay.portal.service._
import java.{util => jutil}

import at.nonblocking.cliwix.core.liferay61.util.RoleAssignmentUtil

import scala.collection.JavaConversions._
import scala.beans.BeanProperty

class SiteRoleAssignmentListHandler extends Handler[SiteRoleAssignmentListCommand, jutil.Map[String, SiteRoleAssignment]] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var resourceAwareCollectionFactory: ResourceAwareCollectionFactory = _

  private[core] override def handle(command: SiteRoleAssignmentListCommand): CommandResult[jutil.Map[String, SiteRoleAssignment]] = {
    val site = this.groupService.getGroup(command.siteId)
    val roles = this.roleService
      .getRoles(site.getCompanyId)
      .filter(_.getType == RoleConstants.TYPE_SITE)

    val resultMap = this.resourceAwareCollectionFactory.createMap[String, SiteRoleAssignment](roles.length)

    roles
      .map(r => this.converter.convertToCliwixSiteRoleAssignment(r, site))
      .filter(ra =>
        ra.getMemberUsers != null || ra.getMemberUserGroups != null
      )
      .foreach(
        ra => resultMap.put(ra.identifiedBy(), ra)
      )

    CommandResult(resultMap)
  }
}

class SiteRoleAssignmentInsertHandler extends Handler[SiteRoleAssignmentInsertCommand, SiteRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var userGroupGroupRoleService: UserGroupGroupRoleLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var userGroupRoleService: UserGroupRoleLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: SiteRoleAssignmentInsertCommand): CommandResult[SiteRoleAssignment] = {
    val roleAssignment = command.roleAssignment
    logger.debug("Insert site role assignment: {}", roleAssignment)

    val site = this.groupService.getGroup(command.siteId)
    val role = this.roleService.getRole(site.getCompanyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_SITE) throw new CliwixValidationException(s"Role ${role.getName} is not of type SITE!")

    if (roleAssignment.getMemberUserGroups != null) {
      val userGroupIds = this.roleAssignmentUtil.getMemberUserGroupIds(site.getCompanyId, roleAssignment.getMemberUserGroups)
      this.userGroupGroupRoleService.addUserGroupGroupRoles(userGroupIds.toArray, site.getGroupId, role.getRoleId)
    }

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(site.getCompanyId, roleAssignment.getMemberUsers)
      this.roleAssignmentUtil.compatibleAddUserGroupRoles(userIds.toArray, site.getGroupId, role.getRoleId)
    }

    roleAssignment.setOwnerGroupId(site.getGroupId)

    CommandResult(roleAssignment)
  }
}

class SiteRoleAssignmentUpdateHandler extends Handler[UpdateCommand[SiteRoleAssignment], SiteRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var userGroupGroupRoleService: UserGroupGroupRoleLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var userGroupRoleService: UserGroupRoleLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: UpdateCommand[SiteRoleAssignment]): CommandResult[SiteRoleAssignment] = {
    val roleAssignment = command.entity
    assert(roleAssignment != null)
    logger.debug("Update site role assignment: {}", roleAssignment)

    val site = this.groupService.getGroup(roleAssignment.getOwnerGroupId)
    val role = this.roleService.getRole(site.getCompanyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_SITE) throw new CliwixValidationException(s"Role ${role.getName} is not of type SITE!")

    val existingUserGroupIds = this.userGroupGroupRoleService
      .getUserGroupGroupRolesByGroupAndRole(site.getGroupId, role.getRoleId)
      .map(_.getUserGroupId)
    val existingUserIds = this.userGroupRoleService
      .getUserGroupRolesByGroupAndRole(site.getGroupId, role.getRoleId)
      .map(_.getUserId)

    if (roleAssignment.getMemberUserGroups != null) {
      val userGroupIds = this.roleAssignmentUtil.getMemberUserGroupIds(site.getCompanyId, roleAssignment.getMemberUserGroups)
      val newUserGroupIds = userGroupIds.filter(!existingUserGroupIds.contains(_))
      val removedUserGroupIds = existingUserGroupIds.filter(!userGroupIds.contains(_))
      this.userGroupGroupRoleService.addUserGroupGroupRoles(newUserGroupIds.toArray, site.getGroupId, role.getRoleId)
      this.userGroupGroupRoleService.deleteUserGroupGroupRoles(removedUserGroupIds.toArray, site.getGroupId, role.getRoleId)
    } else {
      this.userGroupGroupRoleService.deleteUserGroupGroupRoles(existingUserGroupIds.toArray, site.getGroupId, role.getRoleId)
    }

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(site.getCompanyId, roleAssignment.getMemberUsers)
      val newUserIds = userIds.filter(!existingUserIds.contains(_))
      val removedUserIds = existingUserIds.filter(!userIds.contains(_))
      this.roleAssignmentUtil.compatibleAddUserGroupRoles(newUserIds.toArray, site.getGroupId, role.getRoleId)
      this.userGroupRoleService.deleteUserGroupRoles(removedUserIds.toArray, site.getGroupId, role.getRoleId)
    } else {
      this.userGroupRoleService.deleteUserGroupRoles(existingUserIds.toArray, site.getGroupId, role.getRoleId)
    }

    CommandResult(roleAssignment)
  }
}

class SiteRoleAssignmentDeleteHandler extends Handler[DeleteCommand[SiteRoleAssignment], SiteRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var userGroupGroupRoleService: UserGroupGroupRoleLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var userGroupRoleService: UserGroupRoleLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: DeleteCommand[SiteRoleAssignment]): CommandResult[SiteRoleAssignment] = {
    val roleAssignment = command.entity
    assert(roleAssignment != null)
    logger.debug("Delete site role assignment: {}", roleAssignment)

    val site = this.groupService.getGroup(roleAssignment.getOwnerGroupId)
    val role = this.roleService.getRole(site.getCompanyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_SITE) throw new CliwixValidationException(s"Role ${role.getName} is not of type SITE!")

    if (roleAssignment.getMemberUserGroups != null) {
      val userGroupIds = this.roleAssignmentUtil.getMemberUserGroupIds(site.getCompanyId, roleAssignment.getMemberUserGroups)
      this.userGroupGroupRoleService.deleteUserGroupGroupRoles(userGroupIds.toArray, site.getGroupId, role.getRoleId)
    }

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(site.getCompanyId, roleAssignment.getMemberUsers)
      this.userGroupRoleService.deleteUserGroupRoles(userIds.toArray, site.getGroupId, role.getRoleId)
    }

    CommandResult(null)
  }
}
