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

import at.nonblocking.cliwix.core.{ExecutionContext, Reporting}
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.model.Site
import com.liferay.portal.model.VirtualHost
import com.liferay.portal.{model => liferay, NoSuchVirtualHostException, NoSuchGroupException}
import com.liferay.portal.service._
import com.liferay.portal.service.persistence.GroupUtil
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class SiteListHandler extends Handler[SiteListCommand, jutil.Map[String, Site]] {

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: SiteListCommand): CommandResult[jutil.Map[String, Site]] = {
    def isGlobalSite(site: liferay.Group) = site.getName == String.valueOf(command.companyId)

    val sites = this.groupService
      .getCompanyGroups(command.companyId, 0, this.groupService.getCompanyGroupsCount(command.companyId))
      .filter(s => s.isSite && !s.isControlPanel && !isGlobalSite(s))

    val cliwixSiteMap = sites.map { s =>
      logger.debug("Export site: {}", s.getName)
      val site = this.converter.convertToCliwixSite(s, command.withConfiguration)

      (site.identifiedBy(), site)
    }

    CommandResult(new jutil.HashMap(cliwixSiteMap.toMap))
  }
}

class SiteInsertHandler extends Handler[SiteInsertCommand, Site] with SiteUtil {

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var layoutSetService: LayoutSetLocalService = _

  @BeanProperty
  var virtualHostService: VirtualHostLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: SiteInsertCommand): CommandResult[Site] = {
    val cliwixSite = command.site

    if (cliwixSite.getSiteConfiguration == null) throw new CliwixValidationException(s"No SiteConfiguration provided for new site: ${cliwixSite.getName}")

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()

    logger.debug("Adding site: {}", cliwixSite)

    val insertedSite = this.groupService.addGroup(defaultUser.getUserId, classOf[liferay.Group].getName, -1,
      cliwixSite.getName, cliwixSite.getSiteConfiguration.getDescription, cliwixSite.getSiteConfiguration.getMembershipType.getType,
      cliwixSite.getSiteConfiguration.getFriendlyURL, true, true, serviceContext)

    createUpdateRemoveVirtualHosts(insertedSite.getCompanyId, insertedSite.getGroupId, cliwixSite, this.layoutSetService, this.virtualHostService)

    if (cliwixSite.getSiteMembers != null) {
      if (cliwixSite.getSiteMembers.getMemberUserGroups != null) cliwixSite.getSiteMembers.getMemberUserGroups.map(_.getName).foreach { userGroupName =>
        handleNoSuchUserGroup(userGroupName) {
          val userGroup = this.userGroupService.getUserGroup(command.companyId, userGroupName)
          GroupUtil.addUserGroup(insertedSite.getGroupId, userGroup)
        }
      }
      if (cliwixSite.getSiteMembers.getMemberUsers != null) cliwixSite.getSiteMembers.getMemberUsers.map(_.getScreenName).foreach { userScreenName =>
        handleNoSuchUser(userScreenName) {
          val user = this.userService.getUserByScreenName(command.companyId, userScreenName)
          GroupUtil.addUser(insertedSite.getGroupId, user)
        }
      }

      if (cliwixSite.getSiteMembers.getMemberOrganizations != null) cliwixSite.getSiteMembers.getMemberOrganizations.map(_.getName).foreach { orgName =>
        handleNoSuchOrganization(orgName) {
          val organization = this.organizationService.getOrganization(command.companyId, orgName)
          GroupUtil.addOrganization(insertedSite.getGroupId, organization)
        }
      }
    }

    val insertedCliwixSite = this.converter.convertToCliwixSite(insertedSite)
    CommandResult(insertedCliwixSite)
  }

}

class SiteUpdateHandler extends Handler[UpdateCommand[Site], Site] with SiteUtil {

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var layoutSetService: LayoutSetLocalService = _

  @BeanProperty
  var virtualHostService: VirtualHostLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[Site]): CommandResult[Site] = {
    val cliwixSite = command.entity
    assert(cliwixSite.getSiteId != null, "siteId != null")

    logger.debug("Updating site: {}", cliwixSite)

    val site = this.groupService.getGroup(cliwixSite.getSiteId)
    this.converter.mergeToLiferaySiteGroup(cliwixSite, site)
    val updatedSite = this.groupService.updateGroup(site)

    createUpdateRemoveVirtualHosts(updatedSite.getCompanyId, updatedSite.getGroupId, cliwixSite, this.layoutSetService, this.virtualHostService)

    if (cliwixSite.getSiteMembers != null) {
      val existingUserGroupNames = GroupUtil.getUserGroups(updatedSite.getGroupId).map(_.getName)
      val existingUserScreenNames = GroupUtil.getUsers(updatedSite.getGroupId).map(_.getScreenName)
      val existingOrgNames = GroupUtil.getOrganizations(updatedSite.getGroupId).map(_.getName)

      if (cliwixSite.getSiteMembers.getMemberUserGroups != null) cliwixSite.getSiteMembers.getMemberUserGroups.map(_.getName).foreach { userGroupName =>
        if (!existingUserGroupNames.contains(userGroupName)) {
          handleNoSuchUserGroup(userGroupName) {
            val userGroup = this.userGroupService.getUserGroup(updatedSite.getCompanyId, userGroupName)
            GroupUtil.addUserGroup(updatedSite.getGroupId, userGroup)
          }
        }
      }
      existingUserGroupNames.foreach { userGroupName =>
        if (cliwixSite.getSiteMembers.getMemberUserGroups == null || !cliwixSite.getSiteMembers.getMemberUserGroups.exists(_.getName == userGroupName)) {
          val userGroup = this.userGroupService.getUserGroup(updatedSite.getCompanyId, userGroupName)
          GroupUtil.removeUserGroup(updatedSite.getGroupId, userGroup)
        }
      }

      if (cliwixSite.getSiteMembers.getMemberUsers != null) cliwixSite.getSiteMembers.getMemberUsers.map(_.getScreenName).foreach { userScreenName =>
        if (!existingUserScreenNames.contains(userScreenName)) {
          handleNoSuchUser(userScreenName) {
            val user = this.userService.getUserByScreenName(updatedSite.getCompanyId, userScreenName)
            GroupUtil.addUser(updatedSite.getGroupId, user)
          }
        }
      }
      existingUserScreenNames.foreach { userScreenName =>
        if (cliwixSite.getSiteMembers.getMemberUsers == null || !cliwixSite.getSiteMembers.getMemberUsers.exists(_.getScreenName == userScreenName)) {
          val user = this.userService.getUserByScreenName(updatedSite.getCompanyId, userScreenName)
          GroupUtil.removeUser(updatedSite.getGroupId, user)
        }
      }

      if (cliwixSite.getSiteMembers.getMemberOrganizations != null) cliwixSite.getSiteMembers.getMemberOrganizations.map(_.getName).foreach { orgName =>
        if (!existingOrgNames.contains(orgName)) {
          handleNoSuchOrganization(orgName) {
            val organization = this.organizationService.getOrganization(updatedSite.getCompanyId, orgName)
            GroupUtil.addOrganization(updatedSite.getGroupId, organization)
          }
        }
      }
      existingOrgNames.foreach { orgName =>
        if (cliwixSite.getSiteMembers.getMemberOrganizations == null || !cliwixSite.getSiteMembers.getMemberOrganizations.exists(_.getName == orgName)) {
          val organization = this.organizationService.getOrganization(updatedSite.getCompanyId, orgName)
          GroupUtil.removeOrganization(updatedSite.getGroupId, organization)
        }
      }
    }

    val updatedCliwixSite = this.converter.convertToCliwixSite(updatedSite)
    CommandResult(updatedCliwixSite)
  }
}

class SiteGetByIdHandler extends Handler[GetByDBIdCommand[Site], Site] {

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[Site]): CommandResult[Site] = {
    try {
      val site = this.groupService.getGroup(command.dbId)
      assert(site.isSite, s"group with id ${command.dbId} is a site")
      val cliwixSite = this.converter.convertToCliwixSite(site)
      CommandResult(cliwixSite)
    } catch {
      case e: NoSuchGroupException =>
        logger.warn(s"No Site with id ${command.dbId} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }
}

class SiteGetByIdentifierHandler extends Handler[GetByIdentifierOrPathCommand[Site], Site] {

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathCommand[Site]): CommandResult[Site] = {
    try {
      val site = this.groupService.getGroup(command.companyId, command.identifierOrPath)
      assert(site.isSite, s"group with name ${command.identifierOrPath} is a site")
      val cliwixSite = this.converter.convertToCliwixSite(site)
      CommandResult(cliwixSite)
    } catch {
      case e: NoSuchGroupException =>
        logger.warn(s"No Site with name ${command.identifierOrPath} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }
}

class SiteDeleteHandler extends Handler[DeleteCommand[Site], Site]  {

  @BeanProperty
  var groupService: GroupLocalService = _

  override private[core] def handle(command: DeleteCommand[Site]): CommandResult[Site] = {
    logger.debug("Deleting site: {}", command.entity)

    //In some Liferay versions deleteGroup() returns a Group in some not,
    //so we must use reflection here
    val deleteGroupMethod = this.groupService.getClass.getMethod("deleteGroup", classOf[Long])
    deleteGroupMethod.invoke(this.groupService, command.entity.getSiteId)
    CommandResult(null)
  }
}

sealed trait SiteUtil extends Reporting with LazyLogging {

  def createUpdateRemoveVirtualHosts(companyId: Long, siteId: Long, cliwixSite: Site,
                                     layoutSetService: LayoutSetLocalService,
                                     virtualHostService: VirtualHostLocalService): Unit = {
    if (cliwixSite.getSiteConfiguration != null) {
      updateVirtualHost(companyId, siteId, cliwixSite.getSiteConfiguration.getVirtualHostPublicPages, privatePages = false, layoutSetService, virtualHostService)
      updateVirtualHost(companyId, siteId, cliwixSite.getSiteConfiguration.getVirtualHostPrivatePages, privatePages = true, layoutSetService, virtualHostService)
    }
  }

  private def updateVirtualHost(companyId: Long, siteId: Long, hostName: String, privatePages: Boolean,
                                layoutSetService: LayoutSetLocalService,
                                virtualHostService: VirtualHostLocalService) = {
    val layoutSet = layoutSetService.getLayoutSet(siteId, privatePages)
    if (layoutSet != null || hostName != null) {
      val layoutSetId =
        if (layoutSet == null)
          layoutSetService.addLayoutSet(siteId, privatePages).getLayoutSetId
        else
          layoutSet.getLayoutSetId

      if (hostName != null && !hostName.isEmpty) {
        report.addMessage(s"Changing virtual host of site to: $hostName")
        virtualHostService.updateVirtualHost(companyId, layoutSetId, hostName)
      } else {
        try {
          val existingVirtualHost = virtualHostService.getVirtualHost(companyId, layoutSetId)
          report.addMessage("Removing virtual host of site")

          //In some Liferay versions deleteVirtualHost() returns a VirtualHost in some not,
          //so we must use reflection here
          val deleteVirtualHostMethod = virtualHostService.getClass.getMethod("deleteVirtualHost", classOf[VirtualHost])
          deleteVirtualHostMethod.invoke(virtualHostService, existingVirtualHost)

        } catch {
          case e: NoSuchVirtualHostException =>
        }
      }
    }
  }
}
