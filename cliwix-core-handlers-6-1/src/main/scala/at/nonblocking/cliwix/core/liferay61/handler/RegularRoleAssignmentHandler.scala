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
import com.liferay.portal.{model => liferay}
import com.liferay.portal.service._
import java.{util => jutil}

import at.nonblocking.cliwix.core.liferay61.util.RoleAssignmentUtil

import scala.collection.JavaConversions._
import scala.beans.BeanProperty

class RegularRoleAssignmentListHandler extends Handler[RegularRoleAssignmentListCommand, jutil.Map[String, RegularRoleAssignment]] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var resourceAwareCollectionFactory: ResourceAwareCollectionFactory = _

  private[core] override def handle(command: RegularRoleAssignmentListCommand): CommandResult[jutil.Map[String, RegularRoleAssignment]] = {
    val roles = this.roleService
      .getRoles(command.companyId)
      .filter(_.getType == RoleConstants.TYPE_REGULAR)

    val resultMap = this.resourceAwareCollectionFactory.createMap[String, RegularRoleAssignment](roles.length)

    val roleAssignments = roles
      .map(
        this.converter.convertToCliwixRegularRoleAssignment
      )
      .filter(ra =>
        ra.getMemberUserGroups != null || ra.getMemberUsers != null || ra.getMemberOrganizations != null
      )
      .foreach(
        ra => resultMap.put(ra.identifiedBy(), ra)
      )

    CommandResult(resultMap)
  }
}

class RegularRoleAssignmentInsertHandler extends Handler[RegularRoleAssignmentInsertCommand, RegularRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: RegularRoleAssignmentInsertCommand): CommandResult[RegularRoleAssignment] = {
    val roleAssignment = command.roleAssignment
    assert(roleAssignment != null)
    logger.debug("Insert regular role assignment: {}", roleAssignment)

    val role = this.roleService.getRole(command.companyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_REGULAR) throw new CliwixValidationException(s"Role ${role.getName} is not of type REGULAR!")

    if (roleAssignment.getMemberUserGroups != null) {
      val userGroupGroupIds = this.roleAssignmentUtil.getMemberUserGroupGroupIds(command.companyId, roleAssignment.getMemberUserGroups)
      this.groupService.addRoleGroups(role.getRoleId, userGroupGroupIds.toArray)
    }

    if (roleAssignment.getMemberOrganizations != null) {
      val orgGroupIds = this.roleAssignmentUtil.getMemberOrganizationGroupIds(command.companyId, roleAssignment.getMemberOrganizations)
      this.groupService.addRoleGroups(role.getRoleId, orgGroupIds.toArray)
    }

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(command.companyId, roleAssignment.getMemberUsers)
      this.userService.addRoleUsers(role.getRoleId, userIds.toArray)
    }

    roleAssignment.setOwnerCompanyId(role.getCompanyId)

    CommandResult(roleAssignment)
  }
}

class RegularRoleAssignmentUpdateHandler extends Handler[UpdateCommand[RegularRoleAssignment], RegularRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: UpdateCommand[RegularRoleAssignment]): CommandResult[RegularRoleAssignment] = {
    val roleAssignment = command.entity
    assert(roleAssignment != null)
    logger.debug("Update regular role assignment: {}", roleAssignment)

    val role = this.roleService.getRole(roleAssignment.getOwnerCompanyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_REGULAR) throw new CliwixValidationException(s"Role ${role.getName} is not of type REGULAR!")

    val existingRoleUserIds = this.userService.getRoleUsers(role.getRoleId).map(_.getUserId)
    val existingRoleUserGroupGroupIds = this.groupService.getRoleGroups(role.getRoleId)
      .filter(_.getClassName == classOf[liferay.UserGroup].getName)
      .map(_.getGroupId)
    val existingRoleOrgGroupIds = this.groupService.getRoleGroups(role.getRoleId)
      .filter(_.getClassName == classOf[liferay.Organization].getName)
      .map(_.getGroupId)

    if (roleAssignment.getMemberUserGroups != null) {
      val userGroupGroupIds = this.roleAssignmentUtil.getMemberUserGroupGroupIds(roleAssignment.getOwnerCompanyId, roleAssignment.getMemberUserGroups)
      val newUserGroupGroupIds = userGroupGroupIds.filter(!existingRoleUserGroupGroupIds.contains(_))
      val removedUserGroupGroupIds = existingRoleUserGroupGroupIds.filter(!userGroupGroupIds.contains(_))

      this.groupService.addRoleGroups(role.getRoleId, newUserGroupGroupIds.toArray)
      this.groupService.unsetRoleGroups(role.getRoleId, removedUserGroupGroupIds.toArray)
    } else {
      this.groupService.unsetRoleGroups(role.getRoleId, existingRoleUserGroupGroupIds.toArray)
    }

    if (roleAssignment.getMemberOrganizations != null) {
      val orgGroupIds = this.roleAssignmentUtil.getMemberOrganizationGroupIds(roleAssignment.getOwnerCompanyId, roleAssignment.getMemberOrganizations)
      val newOrgGroupIds = orgGroupIds.filter(!existingRoleOrgGroupIds.contains(_))
      val removedOrgGroupIds = existingRoleOrgGroupIds.filter(!orgGroupIds.contains(_))

      this.groupService.addRoleGroups(role.getRoleId, newOrgGroupIds.toArray)
      this.groupService.unsetRoleGroups(role.getRoleId, removedOrgGroupIds.toArray)
    } else {
      this.groupService.unsetRoleGroups(role.getRoleId, existingRoleOrgGroupIds.toArray)
    }

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(roleAssignment.getOwnerCompanyId, roleAssignment.getMemberUsers)
      val newOrgGroupIds = userIds.filter(!existingRoleUserIds.contains(_))
      val removedOrgGroupIds = existingRoleUserIds.filter(!userIds.contains(_))

      this.userService.addRoleUsers(role.getRoleId, newOrgGroupIds.toArray)
      this.userService.unsetRoleUsers(role.getRoleId, removedOrgGroupIds.toArray)
    } else {
      this.userService.unsetRoleUsers(role.getRoleId, existingRoleUserIds.toArray)
    }

    CommandResult(roleAssignment)
  }
}

class RegularRoleAssignmentDeleteHandler extends Handler[DeleteCommand[RegularRoleAssignment], RegularRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: DeleteCommand[RegularRoleAssignment]): CommandResult[RegularRoleAssignment] = {
    val roleAssignment = command.entity
    assert(roleAssignment != null)
    logger.debug("Delete regular role assignment: {}", roleAssignment)

    val role = this.roleService.getRole(roleAssignment.getOwnerCompanyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_REGULAR) throw new CliwixValidationException(s"Role ${role.getName} is not of type REGULAR!")

    if (roleAssignment.getMemberUserGroups != null) {
      val userGroupGroupIds = this.roleAssignmentUtil.getMemberUserGroupGroupIds(roleAssignment.getOwnerCompanyId, roleAssignment.getMemberUserGroups)
      this.groupService.unsetRoleGroups(role.getRoleId, userGroupGroupIds.toArray)
    }

    if (roleAssignment.getMemberOrganizations != null) {
      val orgGroupIds = this.roleAssignmentUtil.getMemberOrganizationGroupIds(roleAssignment.getOwnerCompanyId, roleAssignment.getMemberOrganizations)
      this.groupService.unsetRoleGroups(role.getRoleId, orgGroupIds.toArray)
    }

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(roleAssignment.getOwnerCompanyId, roleAssignment.getMemberUsers)
      this.userService.unsetRoleUsers(role.getRoleId, userIds.toArray)
    }

    CommandResult(null)
  }
}
