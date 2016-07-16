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
import at.nonblocking.cliwix.model.UserGroup
import com.liferay.portal.NoSuchUserGroupException
import com.liferay.portal.service._

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import java.{util=>jutil}

class UserGroupListHandler extends Handler[UserGroupListCommand, jutil.Map[String, UserGroup]] {

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var resourceAwareCollectionFactory: ResourceAwareCollectionFactory = _

  private[core] override def handle(command: UserGroupListCommand): CommandResult[jutil.Map[String, UserGroup]] = {
    val userGroups = this.userGroupService.getUserGroups(command.companyId)
    val resultMap = this.resourceAwareCollectionFactory.createMap[String, UserGroup](userGroups.size())

    userGroups.foreach { userGroup =>
      logger.debug("Export UserGroup: {}", userGroup.getName)
      val cliwixUserGroup = this.converter.convertToCliwixUserGroup(userGroup)
      resultMap.put(cliwixUserGroup.identifiedBy, cliwixUserGroup)
    }

    CommandResult(resultMap)
  }
}

class UserGroupInsertHandler extends Handler[UserGroupInsertCommand, UserGroup] {

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: UserGroupInsertCommand): CommandResult[UserGroup] = {
    val cliwixUserGroup = command.userGroup

    val defaultUser = ExecutionContext.securityContext.defaultUser

    logger.debug("Adding user group: {}", cliwixUserGroup)

    val insertedUserGroup = this.userGroupService.addUserGroup(defaultUser.getUserId, command.companyId,
      cliwixUserGroup.getName, cliwixUserGroup.getDescription)

    if (cliwixUserGroup.getMemberUsers != null) {
      val userIds = cliwixUserGroup.getMemberUsers.map { mu =>
        handleNoSuchUser(mu.getScreenName) {
          this.userService.getUserByScreenName(command.companyId, mu.getScreenName)
        }
      }
      .filter(_ != null)
      .map(_.getUserId)

      this.userService.addUserGroupUsers(insertedUserGroup.getUserGroupId, userIds.toArray)
    }

    val insertedCliwixUserGroup = this.converter.convertToCliwixUserGroup(insertedUserGroup)
    CommandResult(insertedCliwixUserGroup)
  }

}

class UserGroupUpdateHandler extends Handler[UpdateCommand[UserGroup], UserGroup] {

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: UpdateCommand[UserGroup]): CommandResult[UserGroup] = {
    val cliwixUserGroup = command.entity
    assert(cliwixUserGroup.getUserGroupId != null, "userGroupId != null")

    logger.debug("Updating user group: {}", cliwixUserGroup)

    val userGroup = this.userGroupService.getUserGroup(cliwixUserGroup.getUserGroupId)
    this.converter.mergeToLiferayUserGroup(cliwixUserGroup, userGroup)
    val updatedUserGroup = this.getUserGroupService.updateUserGroup(userGroup)

    if (cliwixUserGroup.getMemberUsers != null) {
      val existingUserIds =
        this.userService.getUserGroupUsers(userGroup.getUserGroupId).map(_.getUserId)

      val userIds = cliwixUserGroup.getMemberUsers.map { mu =>
        handleNoSuchUser(mu.getScreenName) {
          this.userService.getUserByScreenName(updatedUserGroup.getCompanyId, mu.getScreenName)
        }
      }
      .filter(_ != null)
      .map(_.getUserId)

      val addedUserIds = userIds.filter(userId => !existingUserIds.contains(userId))
      val removedUserIds = existingUserIds.filter(userId => !userIds.contains(userId))

      this.userService.addUserGroupUsers(updatedUserGroup.getUserGroupId, addedUserIds.toArray)
      this.userService.unsetUserGroupUsers(updatedUserGroup.getUserGroupId, removedUserIds.toArray)
    }

    val updatedCliwixUserGroup = this.converter.convertToCliwixUserGroup(updatedUserGroup)
    CommandResult(updatedCliwixUserGroup)
  }
}

class UserGroupGetByIdHandler extends Handler[GetByDBIdCommand[UserGroup], UserGroup] {

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[UserGroup]): CommandResult[UserGroup] = {
    try {
      val userGroup = this.userGroupService.getUserGroup(command.dbId)
      val cliwixUserGroup = this.converter.convertToCliwixUserGroup(userGroup)
      CommandResult(cliwixUserGroup)
    } catch {
      case e: NoSuchUserGroupException =>
        logger.warn(s"UserGroup with id ${command.dbId} not found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class UserGroupGetByIdentifierHandler extends Handler[GetByIdentifierOrPathCommand[UserGroup], UserGroup] {

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathCommand[UserGroup]): CommandResult[UserGroup] = {
    try {
      val userGroup = this.userGroupService.getUserGroup(command.companyId, command.identifierOrPath)
      val cliwixUserGroup = this.converter.convertToCliwixUserGroup(userGroup)
      CommandResult(cliwixUserGroup)
    } catch {
      case e: NoSuchUserGroupException =>
        logger.warn(s"UserGroup with name ${command.identifierOrPath} not found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class UserGroupDeleteHandler extends Handler[DeleteCommand[UserGroup], UserGroup] {

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  private[core] override def handle(command: DeleteCommand[UserGroup]): CommandResult[UserGroup] = {
    logger.debug("Deleting user group: {}", command.entity)

    //In some Liferay versions deleteUserGroup() returns a UserGroup in some not,
    //so we must use reflection here
    val deleteUserGroupMethod = this.userGroupService.getClass.getMethod("deleteUserGroup", classOf[Long])
    deleteUserGroupMethod.invoke(this.userGroupService, command.entity.getUserGroupId)
    CommandResult(null)
  }

}