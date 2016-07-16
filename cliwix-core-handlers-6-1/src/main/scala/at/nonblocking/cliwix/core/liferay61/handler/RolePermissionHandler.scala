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
import at.nonblocking.cliwix.model.{RolePermission, ResourcePermission}
import com.liferay.counter.service.CounterLocalService
import com.liferay.portal.kernel.dao.orm.QueryUtil
import com.liferay.portal.{model => liferay}
import com.liferay.portal.service._

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class RolePermissionListHandler extends Handler[RolePermissionListCommand, jutil.Map[String, RolePermission]] {

  @BeanProperty
  var resourcePermissionService: ResourcePermissionLocalService = _

  @BeanProperty
  var resourceActionService: ResourceActionLocalService = _

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: RolePermissionListCommand): CommandResult[jutil.Map[String, RolePermission]] = {
    assert(command.role != null, "command.role != null")

    val permissions = this.resourcePermissionService.getRoleResourcePermissions(command.role.getRoleId,
      Array(liferay.ResourceConstants.SCOPE_COMPANY), QueryUtil.ALL_POS, QueryUtil.ALL_POS)

    val cliwixPermissionMap = permissions
      .filter(_.getRoleId > 0)
      .map { permission =>
        val resourceActionList = this.resourceActionService.getResourceActions(permission.getName)
        val cliwixPerm = this.converter.convertToCliwixRolePermission(permission, command.role.getRoleId, resourceActionList.toList)

        logger.debug("Export permissions for resource: {}", permission.getName)
        (cliwixPerm.identifiedBy, cliwixPerm)
      }

    val cliwixPermissionMapNoEmptyActions =
      if (command.filterPermissionsWithNoAction) cliwixPermissionMap.filter(_._2.getActions.nonEmpty)
      else cliwixPermissionMap

    CommandResult(new jutil.HashMap(cliwixPermissionMapNoEmptyActions.toMap))
  }
}

class RolePermissionInsertHandler extends Handler[RolePermissionInsertCommand, RolePermission] {

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

  override private[core] def handle(command: RolePermissionInsertCommand): CommandResult[RolePermission] = {
    assert(command.role != null, "command.role != null")

    val cliwixPermission = command.permission

    val resourceActionList = this.resourceActionService.getResourceActions(cliwixPermission.getResourceName)
    val resourcePermissionId = this.counterService.increment(classOf[liferay.ResourcePermission].getName)

    logger.debug("Adding permission: {}", cliwixPermission)

    val resourcePermission = this.resourcePermissionService.createResourcePermission(resourcePermissionId)
    resourcePermission.setCompanyId(command.companyId)
    resourcePermission.setName(cliwixPermission.getResourceName)
    resourcePermission.setPrimKey(command.companyId.toString)
    resourcePermission.setScope(liferay.ResourceConstants.SCOPE_COMPANY)
    resourcePermission.setRoleId(command.role.getRoleId)

    this.converter.mergeToLiferayPermission(cliwixPermission.getActions, resourcePermission, resourceActionList.toList,
      actionNameNotFound => handleNoSuchAction(actionNameNotFound, cliwixPermission.getResourceName))

    val insertedResourcePermission = this.resourcePermissionService.addResourcePermission(resourcePermission)

    val insertedCliwixPermission = this.converter.convertToCliwixRolePermission(insertedResourcePermission, command.role.getRoleId, resourceActionList.toList)
    CommandResult(insertedCliwixPermission)
  }
}

class RolePermissionUpdateHandler extends Handler[UpdateCommand[RolePermission], RolePermission] {

  @BeanProperty
  var resourcePermissionService: ResourcePermissionLocalService = _

  @BeanProperty
  var resourceActionService: ResourceActionLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[RolePermission]): CommandResult[RolePermission] = {
    val cliwixPermission = command.entity
    assert(cliwixPermission.getResourcePermissionId != null, "resourcePermissionId != null")

    val resourcePermission = this.resourcePermissionService.getResourcePermission(cliwixPermission.getResourcePermissionId)
    val resourceActionList = this.resourceActionService.getResourceActions(resourcePermission.getName)

    this.converter.mergeToLiferayPermission(cliwixPermission.getActions, resourcePermission, resourceActionList.toList,
      actionNameNotFound => handleNoSuchAction(actionNameNotFound, resourcePermission.getName))

    logger.debug("Updating permission: {}", cliwixPermission)

    val updatedResourcePermission = this.resourcePermissionService.updateResourcePermission(resourcePermission)

    val updatedCliwixPermission = this.converter.convertToCliwixRolePermission(updatedResourcePermission, cliwixPermission.getRoleId, resourceActionList.toList)
    CommandResult(updatedCliwixPermission)
  }

}

class RolePermissionDeleteHandler extends Handler[DeleteCommand[RolePermission], RolePermission] {

  @BeanProperty
  var resourcePermissionService: ResourcePermissionLocalService = _

  override private[core] def handle(command: DeleteCommand[RolePermission]): CommandResult[RolePermission] = {
    assert(command.entity.getResourcePermissionId != null, "resourcePermissionId != null")

    logger.debug("Deleting permission: {}", command.entity)

    //In some Liferay versions deleteResourcePermission() returns a ResourcePermission in some not,
    //so we must use reflection here
    val deletePermissionMethod = this.resourcePermissionService.getClass.getMethod("deleteResourcePermission", classOf[Long])
    deletePermissionMethod.invoke(this.resourcePermissionService, command.entity.getResourcePermissionId)

    CommandResult(null)
  }

}
