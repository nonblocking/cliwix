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

class OrganizationRoleAssignmentListHandler extends Handler[OrganizationRoleAssignmentListCommand, jutil.Map[String, OrganizationRoleAssignment]] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var resourceAwareCollectionFactory: ResourceAwareCollectionFactory = _

  private[core] override def handle(command: OrganizationRoleAssignmentListCommand): CommandResult[jutil.Map[String, OrganizationRoleAssignment]] = {
    val org = this.organizationService.getOrganization(command.organizationId)
    val roles = this.roleService
      .getRoles(org.getCompanyId)
      .filter(_.getType == RoleConstants.TYPE_ORGANIZATION)

    val resultMap = this.resourceAwareCollectionFactory.createMap[String, OrganizationRoleAssignment](roles.length)

    val roleAssignments = roles
      .map(r => this.converter.convertToCliwixOrganizationRoleAssignment(r, org))
      .filter(ra => ra.getMemberUsers != null)
      .foreach(ra => resultMap.put(ra.identifiedBy(), ra))

    CommandResult(resultMap)
  }
}

class OrganizationRoleAssignmentInsertHandler extends Handler[OrganizationRoleAssignmentInsertCommand, OrganizationRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var userGroupRoleService: UserGroupRoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: OrganizationRoleAssignmentInsertCommand): CommandResult[OrganizationRoleAssignment] = {
    val roleAssignment = command.roleAssignment
    logger.debug("Insert organization role assignment: {}", roleAssignment)

    val org = this.organizationService.getOrganization(command.organizationId)
    val role = this.roleService.getRole(org.getCompanyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_ORGANIZATION) throw new CliwixValidationException(s"Role ${role.getName} is not of type ORGANIZATION!")

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(org.getCompanyId, roleAssignment.getMemberUsers)
      this.roleAssignmentUtil.compatibleAddUserGroupRoles(userIds.toArray, org.getGroupId, role.getRoleId)
    }

    roleAssignment.setOrganizationId(org.getOrganizationId)
    roleAssignment.setOwnerGroupId(org.getGroupId)

    CommandResult(roleAssignment)
  }
}

class OrganizationRoleAssignmentUpdateHandler extends Handler[UpdateCommand[OrganizationRoleAssignment], OrganizationRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var userGroupRoleService: UserGroupRoleLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: UpdateCommand[OrganizationRoleAssignment]): CommandResult[OrganizationRoleAssignment] = {
    val roleAssignment = command.entity
    assert(roleAssignment != null)
    logger.debug("Update organization role assignment: {}", roleAssignment)

    val org = this.organizationService.getOrganization(roleAssignment.getOrganizationId)
    val role = this.roleService.getRole(org.getCompanyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_ORGANIZATION) throw new CliwixValidationException(s"Role ${role.getName} is not of type ORGANIZATION!")

    val existingUserIds = this.userGroupRoleService
      .getUserGroupRolesByGroupAndRole(org.getGroupId, role.getRoleId)
      .map(_.getUserId)

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(org.getCompanyId, roleAssignment.getMemberUsers)
      val newUserIds = userIds.filter(!existingUserIds.contains(_))
      val removedUserIds = existingUserIds.filter(!userIds.contains(_))
      this.roleAssignmentUtil.compatibleAddUserGroupRoles(newUserIds.toArray, org.getGroupId, role.getRoleId)
      this.userGroupRoleService.deleteUserGroupRoles(removedUserIds.toArray, org.getGroupId, role.getRoleId)
    } else {
      this.userGroupRoleService.deleteUserGroupRoles(existingUserIds.toArray, org.getGroupId, role.getRoleId)
    }

    CommandResult(roleAssignment)
  }
}

class OrganizationRoleAssignmentDeleteHandler extends Handler[DeleteCommand[OrganizationRoleAssignment], OrganizationRoleAssignment] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var userGroupRoleService: UserGroupRoleLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var roleAssignmentUtil: RoleAssignmentUtil = _

  private[core] override def handle(command: DeleteCommand[OrganizationRoleAssignment]): CommandResult[OrganizationRoleAssignment] = {
    val roleAssignment = command.entity
    assert(roleAssignment != null)
    logger.debug("Delete organization role assignment: {}", roleAssignment)

    val org = this.organizationService.getOrganization(roleAssignment.getOrganizationId)
    val role = this.roleService.getRole(org.getCompanyId, roleAssignment.getRoleName)
    if (role.getType != RoleConstants.TYPE_ORGANIZATION) throw new CliwixValidationException(s"Role ${role.getName} is not of type ORGANIZATION!")

    if (roleAssignment.getMemberUsers != null) {
      val userIds = this.roleAssignmentUtil.getMemberUserIds(org.getCompanyId, roleAssignment.getMemberUsers)
      this.userGroupRoleService.deleteUserGroupRoles(userIds.toArray, org.getGroupId, role.getRoleId)
    }

    CommandResult(null)
  }
}
