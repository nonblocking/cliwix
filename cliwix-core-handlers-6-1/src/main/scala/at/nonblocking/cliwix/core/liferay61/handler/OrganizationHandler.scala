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

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.core.util.CountryAndRegionUtil
import at.nonblocking.cliwix.model._
import com.liferay.portal.kernel.util.{GetterUtil, PropsKeys, PropsUtil}
import com.liferay.portal.model.OrganizationConstants
import com.liferay.portal.service._
import com.liferay.portal.NoSuchRoleException

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class OrganizationListHandler extends Handler[OrganizationListCommand, jutil.List[Organization]] {

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: OrganizationListCommand): CommandResult[jutil.List[Organization]] = {
    def addSubOrganizations(parent: Organization, parentId: Long): Unit = {
      val organizations = this.organizationService.getOrganizations(command.companyId, parentId)
      if (organizations.size() > 0) {
        parent.setSubOrganizations(new jutil.ArrayList[Organization]())

        organizations.foreach { org =>
          logger.debug("Export Organization: ", org.getName)

          val cliwixOrg = this.converter.convertToCliwixOrganization(org)
          addSubOrganizations(cliwixOrg, cliwixOrg.getOrganizationId)

          parent.getSubOrganizations.add(cliwixOrg)
        }
      }
    }

    val fakeRootOrganization = new Organization("")
    addSubOrganizations(fakeRootOrganization, OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID)

    CommandResult(if (fakeRootOrganization.getSubOrganizations != null) fakeRootOrganization.getSubOrganizations else Nil)
  }
}

class OrganizationInsertHandler extends Handler[OrganizationInsertCommand, Organization] {

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var countryAndRegionUtil: CountryAndRegionUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: OrganizationInsertCommand): CommandResult[Organization] = {
    assert(command.parentOrganization == null || command.parentOrganization.getOrganizationId != null, "parentOrganization.getOrganizationId != null")
    val cliwixOrganization = command.organization

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val parentOrganizationId: Long = if (command.parentOrganization != null) command.parentOrganization.getOrganizationId else OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID
    val recursable = true

    val country = this.countryAndRegionUtil.getCountryForA2Code(cliwixOrganization.getCountryCode)
    val region = this.countryAndRegionUtil.getRegionForRegionCode(country, cliwixOrganization.getRegionCode)
    val countryId = if (country.isDefined) country.get.getCountryId else 0
    val regionId = if (region.isDefined) region.get.getRegionId else 0
    val serviceContext = ExecutionContext.createServiceContext()
    val statusId = GetterUtil.getInteger(PropsUtil.get(PropsKeys.SQL_DATA_COM_LIFERAY_PORTAL_MODEL_LISTTYPE_ORGANIZATION_STATUS))

    if (cliwixOrganization.getCountryCode != null && country.isEmpty) handleNoSuchCountryCode(cliwixOrganization.getCountryCode)
    if (cliwixOrganization.getRegionCode != null && region.isEmpty) handleNoSuchRegionCode(cliwixOrganization.getRegionCode)

    logger.debug("Adding organization: {}", cliwixOrganization)

    val insertedOrganization = this.organizationService.addOrganization(
      defaultUser.getUserId, parentOrganizationId,
      cliwixOrganization.getName, cliwixOrganization.getType, recursable,
      regionId, countryId, statusId, null, false, serviceContext)

    if (cliwixOrganization.getOrganizationMembers != null && cliwixOrganization.getOrganizationMembers.getMemberUsers != null) {
      val userIds = cliwixOrganization.getOrganizationMembers.getMemberUsers.map { mu =>
        handleNoSuchUser(mu.getScreenName) {
          this.userService.getUserByScreenName(insertedOrganization.getCompanyId, mu.getScreenName)
        }
      }
      .filter(_ != null)
      .map(_.getUserId)

      this.userService.addOrganizationUsers(insertedOrganization.getOrganizationId, userIds.toArray)
    }

    val insertedCliwixOrganization = this.converter.convertToCliwixOrganization(insertedOrganization)
    CommandResult(insertedCliwixOrganization)
  }
}

class OrganizationUpdateHandler extends Handler[UpdateCommand[Organization], Organization] {

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var countryAndRegionUtil: CountryAndRegionUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[Organization]): CommandResult[Organization] = {
    val cliwixOrganization = command.entity

    val organization = this.organizationService.getOrganization(cliwixOrganization.getOrganizationId)
    val country = this.countryAndRegionUtil.getCountryForA2Code(cliwixOrganization.getCountryCode)
    val region = this.countryAndRegionUtil.getRegionForRegionCode(country, cliwixOrganization.getRegionCode)
    val countryId = if (country.isDefined) country.get.getCountryId else 0
    val regionId = if (region.isDefined) region.get.getRegionId else 0
    val serviceContext = ExecutionContext.createServiceContext()

    if (cliwixOrganization.getCountryCode != null && country.isEmpty) handleNoSuchCountryCode(cliwixOrganization.getCountryCode)
    if (cliwixOrganization.getRegionCode != null && region.isEmpty) handleNoSuchRegionCode(cliwixOrganization.getRegionCode)

    logger.debug("Updating organization: {}", cliwixOrganization)

    val updatedOrganization = this.organizationService.updateOrganization(
      organization.getCompanyId, organization.getOrganizationId, organization.getParentOrganizationId,
      cliwixOrganization.getName, cliwixOrganization.getType, organization.getRecursable,
      regionId, countryId, organization.getStatusId, null, false, serviceContext)

    if (cliwixOrganization.getOrganizationMembers != null && cliwixOrganization.getOrganizationMembers.getMemberUsers != null) {
      val existingUserIds = this.userService.getOrganizationUserIds(updatedOrganization.getOrganizationId)

      val userIds = cliwixOrganization.getOrganizationMembers.getMemberUsers.map { mu =>
        handleNoSuchUser(mu.getScreenName) {
          this.userService.getUserByScreenName(updatedOrganization.getCompanyId, mu.getScreenName)
        }
      }
      .filter(_ != null)
      .map(_.getUserId)

      val addedUserIds = userIds.filter(userId => !existingUserIds.contains(userId))
      val removedUserIds = existingUserIds.filter(userId => !userIds.contains(userId))

      this.userService.addOrganizationUsers(updatedOrganization.getOrganizationId, addedUserIds.toArray)
      this.userService.unsetOrganizationUsers(updatedOrganization.getOrganizationId, removedUserIds.toArray)
    }

    val updatedCliwixOrganization = this.converter.convertToCliwixOrganization(updatedOrganization)
    CommandResult(updatedCliwixOrganization)
  }

}

class OrganizationGetByIdHandler extends Handler[GetByDBIdCommand[Organization], Organization] {

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[Organization]): CommandResult[Organization] = {
    try {
      val organization = this.organizationService.getOrganization(command.dbId)
      val cliwixOrganization = this.converter.convertToCliwixOrganization(organization)
      CommandResult(cliwixOrganization)
    } catch {
      case e: NoSuchRoleException =>
        logger.warn(s"No organization with id ${command.dbId} found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class OrganizationGetByIdentifierHandler extends Handler[GetByIdentifierOrPathCommand[Organization], Organization] {

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathCommand[Organization]): CommandResult[Organization] = {
    try {
      val organization = this.organizationService.getOrganization(command.companyId, command.identifierOrPath)
      val cliwixOrganization = this.converter.convertToCliwixOrganization(organization)
      CommandResult(cliwixOrganization)
    } catch {
      case e: NoSuchRoleException =>
        logger.warn(s"No organization with path ${command.identifierOrPath} found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class OrganizationDeleteHandler extends Handler[DeleteCommand[Organization], Organization] {

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  override private[core] def handle(command: DeleteCommand[Organization]): CommandResult[Organization] = {
    logger.debug("Deleting organization: {}", command.entity)

    //In some Liferay versions deleteOrganization() returns an Organization in some not,
    //so we must use reflection here
    val deleteOrganizationMethod = this.organizationService.getClass.getMethod("deleteOrganization", classOf[Long])
    deleteOrganizationMethod.invoke(this.organizationService, command.entity.getOrganizationId)
    CommandResult(null)
  }

}
