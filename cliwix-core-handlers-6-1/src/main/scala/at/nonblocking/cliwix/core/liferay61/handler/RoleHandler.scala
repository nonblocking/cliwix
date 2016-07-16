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

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.core.util.ResourceAwareCollectionFactory
import at.nonblocking.cliwix.model._
import com.liferay.portal.NoSuchRoleException
import com.liferay.portal.service.RoleLocalService

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import java.{util=>jutil}

class RoleListHandler extends Handler[RoleListCommand, jutil.Map[String, Role]] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var resourceAwareCollectionFactory: ResourceAwareCollectionFactory = _

  private[core] override def handle(command: RoleListCommand): CommandResult[jutil.Map[String, Role]] = {
    val roles = this.roleService.getRoles(command.companyId)
    val resultMap = this.resourceAwareCollectionFactory.createMap[String, Role](roles.size())

    roles.foreach{ role =>
      val cliwixRole = this.converter.convertToCliwixRole(role)
      logger.debug("Exporting role: {}", cliwixRole.getName)
      resultMap.put(cliwixRole.identifiedBy, cliwixRole)
    }

    CommandResult(resultMap)
  }
}

class RoleInsertHandler extends Handler[RoleInsertCommand, Role] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: RoleInsertCommand): CommandResult[Role] = {
    val cliwixRole = command.role

    val defaultUser = ExecutionContext.securityContext.defaultUser

    logger.debug("Adding role: {}", cliwixRole)

    val liferayRole = this.roleService.createRole(-1)

    this.converter.mergeToLiferayRole(cliwixRole, liferayRole)

    val insertedRole = this.roleService.addRole(
      defaultUser.getUserId, defaultUser.getCompanyId,
      liferayRole.getName,
      liferayRole.getTitleMap,
      liferayRole.getDescriptionMap,
      liferayRole.getType)

    val insertedCliwixRole = this.converter.convertToCliwixRole(insertedRole)

    CommandResult(insertedCliwixRole)
  }
}

class RoleUpdateHandler extends Handler[UpdateCommand[Role], Role] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: UpdateCommand[Role]): CommandResult[Role] = {
    val cliwixRole = command.entity
    assert(cliwixRole.getRoleId != null, "roleId != null")

    logger.debug("Update role: {}", cliwixRole)

    val liferayRole = this.roleService.getRole(cliwixRole.getRoleId)

    this.converter.mergeToLiferayRole(cliwixRole, liferayRole)

    val updatedRole = this.roleService.updateRole(liferayRole)
    val updatedCliwixRole = this.converter.convertToCliwixRole(updatedRole)

    CommandResult(updatedCliwixRole)
  }
}

class RoleGetByIdHandler extends Handler[GetByDBIdCommand[Role], Role] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[Role]): CommandResult[Role] = {
    try {
      val role = this.roleService.getRole(command.dbId)
      val cliwixRole = this.converter.convertToCliwixRole(role)
      CommandResult(cliwixRole)
    } catch {
      case e: NoSuchRoleException =>
        logger.warn(s"Role with id ${command.dbId} not found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class RoleGetByIdentifierHandler extends Handler[GetByIdentifierOrPathCommand[Role], Role] {

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathCommand[Role]): CommandResult[Role] = {
    try {
      val role = this.roleService.getRole(command.companyId, command.identifierOrPath)
      val cliwixRole = this.converter.convertToCliwixRole(role)
      CommandResult(cliwixRole)
    } catch {
      case e: NoSuchRoleException =>
        logger.warn(s"Role with name ${command.identifierOrPath} not found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class RoleDeleteHandler extends Handler[DeleteCommand[Role], Role] {

  @BeanProperty
  var roleService: RoleLocalService = _

  private[core] override def handle(command: DeleteCommand[Role]): CommandResult[Role] = {
    logger.debug("Delete role: {}", command.entity)

    //In some Liferay versions deleteRole() returns a Role in some not,
    //so we must use reflection here
    val deleteRoleMethod = this.roleService.getClass.getMethod("deleteRole", classOf[Long])
    deleteRoleMethod.invoke(this.roleService, command.entity.getRoleId)
    CommandResult(null)
  }

}
