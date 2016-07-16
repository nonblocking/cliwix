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

import java.util.{UUID, Date}
import java.{util => jutil}

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.core.util.ResourceAwareCollectionFactory
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.model.{GENDER, User}
import com.liferay.portal.kernel.util.{PropsKeys, PropsUtil}
import com.liferay.portal.service._
import com.liferay.portal._

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class UserListHandler extends Handler[UserListCommand, jutil.Map[String, User]] {

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var resourceAwareCollectionFactory: ResourceAwareCollectionFactory = _

  val pagingSize = 100

  private[core] override def handle(command: UserListCommand): CommandResult[jutil.Map[String, User]] = {
    val count = this.userService.getCompanyUsersCount(command.companyId)
    val resultMap = this.resourceAwareCollectionFactory.createMap[String, User](count)

    for (page <- 0 to (count / pagingSize)) {
      val users = this.userService.getCompanyUsers(command.companyId, page * pagingSize, (page + 1) * pagingSize)

      users
        .filter(!_.isDefaultUser)
        .foreach { u =>
          logger.debug("Export User: {}", u.getScreenName)
          val cliwixUser = this.converter.convertToCliwixUser(u)
          resultMap.put(cliwixUser.identifiedBy, cliwixUser)
        }
    }

    CommandResult(resultMap)
  }
}

class UserInsertHandler extends Handler[UserInsertCommand, User] {

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var contactService: ContactLocalService = _

  @BeanProperty
  var companyService: CompanyLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: UserInsertCommand): CommandResult[User] = {
    val cliwixUser = command.user

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()

    val screenName = cliwixUser.getScreenName.toLowerCase
    if (screenName != cliwixUser.getScreenName) {
      report.addWarning(s"User screen name changed to lower case: $screenName")
    }

    val dummyPwd = "___dummy___"
    val prefixId = 0
    val suffixId = 0
    val male = cliwixUser.getGender == GENDER.M
    val liferayBirthDate = this.converter.toLiferayDate(cliwixUser.getBirthDate, defaultUser.getTimeZone)
    val facebookId = 0
    val openId = ""

    logger.debug("Adding user: {}", cliwixUser)

    val liferayUser =
      try {
        this.userService.addUser(defaultUser.getUserId,
          command.companyId, false, dummyPwd, dummyPwd, false, screenName, cliwixUser.getEmailAddress,
          facebookId, openId, defaultUser.getLocale,
          cliwixUser.getFirstName, cliwixUser.getMiddleName, cliwixUser.getLastName,
          prefixId, suffixId, male, liferayBirthDate.month, liferayBirthDate.day, liferayBirthDate.year, cliwixUser.getJobTitle,
          null, null, null, null,
          false, serviceContext)
    } catch {
      case e: UserScreenNameException =>
        throw new CliwixValidationException(s"Invalid screen name: $screenName", e)
      case e: GroupFriendlyURLException =>
        if (e.getType == LayoutFriendlyURLException.DUPLICATE)
          throw new CliwixValidationException(s"Screen name '$screenName' conflicts with an existing groups with friendly URL '/$screenName' and cannot be imported!", e)
        else
          throw e
      case e @ (_: UserEmailAddressException | _: DuplicateUserEmailAddressException ) =>
        val company = this.companyService.getCompanyById(command.companyId)
        val emailRequired = PropsUtil.get(PropsKeys.USERS_EMAIL_ADDRESS_REQUIRED) == "true"
        val newEmailAddress =
          if (emailRequired) "cliwix_" + UUID.randomUUID().toString.replaceAll("-", "") + "@" + company.getMx
          else null

        val userWithGeneratedEmailAddress = this.userService.addUser(defaultUser.getUserId,
          command.companyId, false, dummyPwd, dummyPwd, false, screenName, newEmailAddress,
          facebookId, openId, defaultUser.getLocale,
          cliwixUser.getFirstName, cliwixUser.getMiddleName, cliwixUser.getLastName,
          prefixId, suffixId, male, liferayBirthDate.month, liferayBirthDate.day, liferayBirthDate.year, cliwixUser.getJobTitle,
          null, null, null, null,
          false, serviceContext)

        report.addWarning(s"Invalid or duplicate email address '${cliwixUser.getEmailAddress}' for user '${cliwixUser.getScreenName}' detected. " +
          s"Generated random address: ${userWithGeneratedEmailAddress.getEmailAddress}. Please check!")
        cliwixUser.setEmailAddress(userWithGeneratedEmailAddress.getEmailAddress)
        userWithGeneratedEmailAddress
    }

    val liferayContact = liferayUser.getContact

    this.converter.mergeToLiferayUser(cliwixUser, liferayUser, liferayContact)

    //Prevent password reset on first login
    liferayUser.setPasswordReset(false)
    liferayUser.setLastLoginDate(new Date())

    val insertedUser = this.userService.updateUser(liferayUser)
    this.contactService.updateContact(liferayContact)

    val insertedCliwixUser = this.converter.convertToCliwixUser(insertedUser)
    CommandResult(insertedCliwixUser)
  }
}

class UserUpdateHandler extends Handler[UpdateCommand[User], User] {

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var contactService: ContactLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: UpdateCommand[User]): CommandResult[User] = {
    val cliwixUser = command.entity
    assert(cliwixUser.getUserId != null, "userId != null")

    val liferayUser = this.userService.getUser(cliwixUser.getUserId)
    val liferayContact = liferayUser.getContact

    this.converter.mergeToLiferayUser(cliwixUser, liferayUser, liferayContact)

    logger.debug("Update user: {}", cliwixUser)

    val updatedUser = this.userService.updateUser(liferayUser)
    this.contactService.updateContact(liferayContact)

    val updatedCliwixUser = this.converter.convertToCliwixUser(updatedUser)
    CommandResult(updatedCliwixUser)
  }
}


class UserGetByIdHandler extends Handler[GetByDBIdCommand[User], User] {

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[User]): CommandResult[User] = {
    try {
      val user = this.userService.getUser(command.dbId)
      val cliwixUser = this.converter.convertToCliwixUser(user)
      CommandResult(cliwixUser)
    } catch {
      case e: NoSuchUserException =>
        logger.warn(s"No user with id ${command.dbId} found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class UserGetByIdentifierHandler extends Handler[GetByIdentifierOrPathCommand[User], User] {

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathCommand[User]): CommandResult[User] = {
    try {
      val user = this.userService.getUserByScreenName(command.companyId, command.identifierOrPath)
      val cliwixUser = this.converter.convertToCliwixUser(user)
      CommandResult(cliwixUser)
    } catch {
      case e: NoSuchUserException =>
        logger.warn(s"No user with screenName ${command.identifierOrPath} found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }
}


class UserDeleteHandler extends Handler[DeleteCommand[User], User] {

  @BeanProperty
  var userService: UserLocalService = _

  private[core] override def handle(command: DeleteCommand[User]): CommandResult[User] = {
    logger.debug("Delete user: {}", command.entity)

    val cliwixUser = command.entity

    //In some Liferay versions deleteUser() returns a User in some not,
    //so we must use reflection here
    val deleteUserMethod = this.userService.getClass.getMethod("deleteUser", classOf[Long])
    deleteUserMethod.invoke(this.userService, cliwixUser.getUserId)

    if (cliwixUser.getUserId == ExecutionContext.securityContext.adminUser.getUserId) {
      //The current admin user has been deleted, switch to a new one
      ExecutionContext.updateSecurityContext(failWhenNoAdminUserFound = false)
    }

    CommandResult(null)
  }

}
