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

import java.{util => jutil}

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.ResourcePermission
import com.liferay.counter.service.CounterLocalService
import com.liferay.portal.{model => liferay}
import com.liferay.portal.service._

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class ResourcePermissionListHandler extends Handler[ResourcePermissionListCommand, jutil.Map[String, ResourcePermission]] {

  @BeanProperty
  var resourcePermissionService: ResourcePermissionLocalService = _

  @BeanProperty
  var resourceActionService: ResourceActionLocalService = _

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: ResourcePermissionListCommand): CommandResult[jutil.Map[String, ResourcePermission]] = {
    val scopeId = command match {
      case dlListCommand: DocumentLibraryPermissionListCommand => liferay.ResourceConstants.SCOPE_GROUP
      case _ => liferay.ResourceConstants.SCOPE_INDIVIDUAL
    }

    val permissions = this.resourcePermissionService.getResourcePermissions(
      command.companyId, command.resourceName, scopeId, command.resourcePrimKey)

    val cliwixPermissionMap = permissions
      .filter(_.getRoleId > 0)
      .map { permission =>
        val roleName = this.roleService.getRole(permission.getRoleId).getName
        val resourceActionList = this.resourceActionService.getResourceActions(permission.getName)
        val cliwixPerm = this.converter.convertToCliwixResourcePermission(permission, roleName, resourceActionList.toList)

        logger.debug("Export permission for role: {}", cliwixPerm.getRole)
        (cliwixPerm.identifiedBy, cliwixPerm)
      }

    val cliwixPermissionMapNoEmptyActions =
      if (command.filterPermissionsWithNoAction) cliwixPermissionMap.filter(_._2.getActions.nonEmpty)
      else cliwixPermissionMap

    CommandResult(new jutil.HashMap(cliwixPermissionMapNoEmptyActions.toMap))
  }
}

class ResourcePermissionInsertHandler extends Handler[ResourcePermissionInsertCommand, ResourcePermission] {

  @BeanProperty
  var resourcePermissionService: ResourcePermissionLocalService = _

  @BeanProperty
  var resourceActionService: ResourceActionLocalService = _

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var counterService: CounterLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: ResourcePermissionInsertCommand): CommandResult[ResourcePermission] = {
    val cliwixPermission = command.permission

    val roleName = cliwixPermission.getRole
    handleNoSuchRole(roleName) {
      val role = this.roleService.getRole(command.companyId, roleName)

      val resourceActionList = this.resourceActionService.getResourceActions(command.resourceName)
      val resourcePermissionId = this.counterService.increment(classOf[liferay.ResourcePermission].getName)

      val scopeId = command match {
        case dlInsertCommand: DocumentLibraryPermissionInsertCommand => liferay.ResourceConstants.SCOPE_GROUP
        case _ => liferay.ResourceConstants.SCOPE_INDIVIDUAL
      }

      logger.debug("Adding permission: {}", cliwixPermission)

      val resourcePermission = this.resourcePermissionService.createResourcePermission(resourcePermissionId)
      resourcePermission.setCompanyId(command.companyId)
      resourcePermission.setName(command.resourceName)
      resourcePermission.setPrimKey(command.resourcePrimKey)
      resourcePermission.setScope(scopeId)
      resourcePermission.setRoleId(role.getRoleId)

      this.converter.mergeToLiferayPermission(cliwixPermission.getActions, resourcePermission, resourceActionList.toList,
        actionNameNotFound => handleNoSuchAction(actionNameNotFound, command.resourceName))

      val insertedResourcePermission = this.resourcePermissionService.addResourcePermission(resourcePermission)

      val insertedCliwixPermission = this.converter.convertToCliwixResourcePermission(insertedResourcePermission, roleName, resourceActionList.toList)
      CommandResult(insertedCliwixPermission)
    }
  }

}

class ResourcePermissionUpdateHandler extends Handler[UpdateCommand[ResourcePermission], ResourcePermission] {

  @BeanProperty
  var resourcePermissionService: ResourcePermissionLocalService = _

  @BeanProperty
  var resourceActionService: ResourceActionLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[ResourcePermission]): CommandResult[ResourcePermission] = {
    val cliwixPermission = command.entity
    assert(cliwixPermission.getResourcePermissionId != null, "resourcePermissionId != null")

    val resourcePermission = this.resourcePermissionService.getResourcePermission(cliwixPermission.getResourcePermissionId)
    val resourceActionList = this.resourceActionService.getResourceActions(resourcePermission.getName)

    this.converter.mergeToLiferayPermission(cliwixPermission.getActions, resourcePermission, resourceActionList.toList,
      actionNameNotFound => handleNoSuchAction(actionNameNotFound, resourcePermission.getName))

    logger.debug("Updating permission: {}", cliwixPermission)

    val updatedResourcePermission = this.resourcePermissionService.updateResourcePermission(resourcePermission)

    val updatedCliwixPermission = this.converter.convertToCliwixResourcePermission(updatedResourcePermission, cliwixPermission.getRole, resourceActionList.toList)
    CommandResult(updatedCliwixPermission)
  }

}

class ResourcePermissionDeleteHandler extends Handler[DeleteCommand[ResourcePermission], ResourcePermission] {

  @BeanProperty
  var resourcePermissionService: ResourcePermissionLocalService = _

  override private[core] def handle(command: DeleteCommand[ResourcePermission]): CommandResult[ResourcePermission] = {
    logger.debug("Deleting permission: {}", command.entity)

    //In some Liferay versions deleteResourcePermission() returns a ResourcePermission in some not,
    //so we must use reflection here
    val deletePermissionMethod = this.resourcePermissionService.getClass.getMethod("deleteResourcePermission", classOf[Long])
    deletePermissionMethod.invoke(this.resourcePermissionService, command.entity.getResourcePermissionId)

    CommandResult(null)
  }

}
