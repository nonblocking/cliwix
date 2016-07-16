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

package at.nonblocking.cliwix.core.liferay61.converter

import java.util.regex.Pattern
import java.util.{Calendar, Date, GregorianCalendar, TimeZone}
import java.{util => jutil}

import at.nonblocking.cliwix.core.liferay61.util.NativeSqlAccessUtil
import at.nonblocking.cliwix.core.{Cliwix, Reporting}
import at.nonblocking.cliwix.core.converter.{LiferayDate, LiferayEntityConverter}
import at.nonblocking.cliwix.core.util.CountryAndRegionUtil
import at.nonblocking.cliwix.model._
import com.liferay.portal.kernel.dao.orm.Type
import com.liferay.portal.kernel.repository.{model => liferayRepository}
import com.liferay.portal.model.{LayoutSet, PortletConstants, RoleConstants}
import com.liferay.portal.service._
import com.liferay.portal.service.persistence.GroupUtil
import com.liferay.portal.{model => liferay}
import com.liferay.portlet.documentlibrary.{model => documentLibrary}
import com.liferay.portlet.journal.{model => liferayJournal}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.xml.{Unparsed, XML}

private[core] class LiferayEntityConverter61 extends LiferayEntityConverter with Reporting {

  @BeanProperty
  var countryAndRegionUtil: CountryAndRegionUtil = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var userGroupRoleService: UserGroupRoleLocalService = _

  @BeanProperty
  var userGroupService: UserGroupLocalService = _

  @BeanProperty
  var userGroupGroupRoleService: UserGroupGroupRoleLocalService = _

  @BeanProperty
  var organizationService: OrganizationLocalService = _

  @BeanProperty
  var layoutSetService: LayoutSetLocalService = _

  @BeanProperty
  var nativeSqlAccessUtil: NativeSqlAccessUtil = _

  override def convertToCliwixCompany(company: liferay.Company, withConfiguration: Boolean): Company = {
    val accountName = if (company.getAccount != null) company.getAccount.getName else null

    val cliwixCompanyConfiguration =
      if (withConfiguration) {
        val cliwixCompanyConfiguration = new CompanyConfiguration(
          company.getVirtualHostname,
          company.getMx,
          company.getDefaultUser.getLanguageId,
          company.getDefaultUser.getTimeZoneId)
        cliwixCompanyConfiguration.setAccountName(accountName)
        cliwixCompanyConfiguration.setActive(company.isActive)
        cliwixCompanyConfiguration.setHomeUrl(trim(company.getHomeURL))
        cliwixCompanyConfiguration.setDefaultGreeting(trim(company.getDefaultUser.getGreeting))
        cliwixCompanyConfiguration
      } else null

    val cliwixCompany = new Company(company.getWebId, cliwixCompanyConfiguration)
    cliwixCompany.setCompanyId(company.getCompanyId)
    cliwixCompany.setCompanyGroupId(company.getGroup.getGroupId)

    cliwixCompany
  }

  override def convertToCliwixUser(user: liferay.User): User = {
    def getPassword = new Password(user.isPasswordEncrypted, user.getPassword)
    def getGender = if (user.isFemale) GENDER.F else GENDER.M
    def getTimezoneString = if (user.getTimeZone != null) user.getTimeZone.getID else null

    val cliwixUser = new User(user.getScreenName, user.getEmailAddress, getPassword,
      trim(user.getJobTitle), trim(user.getFirstName), trim(user.getMiddleName), trim(user.getLastName),
      user.getBirthday, getGender, user.getLanguageId, getTimezoneString, trim(user.getGreeting))

    cliwixUser.setUserId(user.getUserId)
    cliwixUser.setOwnerCompanyId(user.getCompanyId)
    cliwixUser.setUserGroupId(user.getGroupId)

    cliwixUser
  }

  override def mergeToLiferayUser(cliwixUser: User, user: liferay.User, contact: liferay.Contact) = {
    user.setEmailAddress(cliwixUser.getEmailAddress)
    user.setJobTitle(cliwixUser.getJobTitle)
    user.setFirstName(cliwixUser.getFirstName)
    user.setMiddleName(cliwixUser.getMiddleName)
    user.setLastName(cliwixUser.getLastName)
    user.setLanguageId(cliwixUser.getLanguage)
    user.setTimeZoneId(cliwixUser.getTimezone)
    user.setGreeting(cliwixUser.getGreeting)

    if (cliwixUser.getPassword != null) {
      user.setPasswordEncrypted(cliwixUser.getPassword.getEncrypted)
      user.setPassword(cliwixUser.getPassword.getPassword)
    }

    contact.setFirstName(cliwixUser.getFirstName)
    contact.setMiddleName(cliwixUser.getMiddleName)
    contact.setLastName(cliwixUser.getLastName)
    contact.setBirthday(cliwixUser.getBirthDate)
    contact.setJobTitle(cliwixUser.getJobTitle)

    if (cliwixUser.getGender != null) {
      contact.setMale(cliwixUser.getGender == GENDER.M)
    }
  }

  override def convertToCliwixOrganization(org: liferay.Organization) = {
    //val memberUsers = this.userService.getOrganizationUsers(org.getOrganizationId).map(u => new MemberUser(u.getScreenName))
    val memberUsers = nativeSQLGetOrgUsers(org)

    val organizationMembers =
      if (memberUsers.nonEmpty) new OrganizationMembers(memberUsers)
      else null

    val cliwixOrg = new Organization(org.getName, organizationMembers)

    val country = this.countryAndRegionUtil.getCountryForId(org.getCountryId)
    val region = this.countryAndRegionUtil.getRegionForId(org.getRegionId)
    val countryA2Code = if (country.isDefined) country.get.getA2 else null
    val regionCode = if (region.isDefined) region.get.getRegionCode else null
    cliwixOrg.setCountryCode(countryA2Code)
    cliwixOrg.setRegionCode(regionCode)

    cliwixOrg.setType(org.getType)
    cliwixOrg.setOrganizationId(org.getOrganizationId)
    cliwixOrg.setOwnerCompanyId(org.getCompanyId)
    cliwixOrg.setOrganizationGroupId(org.getGroupId)

    cliwixOrg
  }

  /*
  * Safe method to get all org users.
  * UserService.getOrganizationUsers() throws an exception if a userId in the Users_Orgs table no longer exists.
  */
  private def nativeSQLGetOrgUsers(org: liferay.Organization) = {
    this.nativeSqlAccessUtil.scalarList(
        "select u.screenName from Users_Orgs uo, User_ u where uo.userId = u.userId and uo.organizationId = " + org.getOrganizationId,
        Map("screenName" -> Type.STRING))
      .map(screenName => new MemberUser(screenName.toString))
  }

  override def convertToCliwixOrganizationRoleAssignment(role: liferay.Role, org: liferay.Organization) = {
    val roleAssignment = new OrganizationRoleAssignment(role.getName)

    val memberUsers = this.userGroupRoleService
      .getUserGroupRolesByGroupAndRole(org.getGroupId, role.getRoleId)
      .map { ugr =>
        try {
          new MemberUser(ugr.getUser.getScreenName)
        } catch {
          case e: Throwable =>
            report.addWarning(s"Some users for organization role ${role.getName} couldn't be exported. Reason: ${e.getMessage}")
            null
        }
      }
      .filter(_ != null)

    if (memberUsers.nonEmpty) roleAssignment.setMemberUsers(memberUsers)

    roleAssignment.setOwnerGroupId(org.getGroupId)
    roleAssignment.setOrganizationId(org.getOrganizationId)

    roleAssignment
  }

  override def convertToCliwixRole(role: liferay.Role) = {
    val cliwixRole = new Role(role.getName)

    cliwixRole.setType(toCliwixRoleType(role.getType))
    cliwixRole.setTitles(toCliwixLocalizedTextContentList(role.getTitleMap))
    cliwixRole.setDescriptions(toCliwixLocalizedTextContentList(role.getDescriptionMap))
    cliwixRole.setRoleId(role.getRoleId)
    cliwixRole.setOwnerCompanyId(role.getCompanyId)

    cliwixRole
  }

  override def convertToCliwixRegularRoleAssignment(role: liferay.Role) = {
    val classNameUserGroup = classOf[liferay.UserGroup].getName
    val classNameOrganization = classOf[liferay.Organization].getName

    val roleAssignment = new RegularRoleAssignment(role.getName)

    roleAssignment.setMemberUserGroups(new jutil.ArrayList[MemberUserGroup]())
    roleAssignment.setMemberOrganizations(new jutil.ArrayList[MemberOrganization]())
    val groups = this.groupService.getRoleGroups(role.getRoleId)
    groups.foreach(g => g.getClassName match {
      case `classNameUserGroup` =>
        try {
          val userGroup = this.userGroupService.getUserGroup(g.getClassPK)
          roleAssignment.getMemberUserGroups.add(new MemberUserGroup(userGroup.getName))
        } catch {
          case e: Throwable =>
            report.addWarning(s"Some user groups for role ${role.getName} couldn't be exported. Reason: ${e.getMessage}")
        }
      case `classNameOrganization` =>
        try {
          val org = this.organizationService.getOrganization(g.getClassPK)
          roleAssignment.getMemberOrganizations.add(new MemberOrganization(org.getName))
        } catch {
          case e: Throwable =>
            report.addWarning(s"Some organizations for role ${role.getName} couldn't be exported. Reason: ${e.getMessage}")
        }
      case _ =>
    })
    if (roleAssignment.getMemberUserGroups.isEmpty) {
      roleAssignment.setMemberUserGroups(null)
    }
    if (roleAssignment.getMemberOrganizations.isEmpty) {
      roleAssignment.setMemberOrganizations(null)
    }

    //val memberUsers = this.userService.getRoleUsers(role.getRoleId).map(u => new MemberUser(u.getScreenName))
    val memberUsers = nativeSQLGetRoleUsers(role)
    if (memberUsers.nonEmpty) {
      roleAssignment.setMemberUsers(memberUsers)
    }

    roleAssignment.setOwnerCompanyId(role.getCompanyId)

    roleAssignment
  }

  /*
  * Safe method to get all role users.
  * UserService.getRoleUsers() throws an exception if a userId in the Users_Roles table no longer exists
  */
  private def nativeSQLGetRoleUsers(role: liferay.Role) = {
    this.nativeSqlAccessUtil.scalarList(
        "select u.screenName from Users_Roles ur, User_ u where ur.userId = u.userId and ur.roleId = " + role.getRoleId,
        Map("screenName" -> Type.STRING))
      .map(screenName => new MemberUser(screenName.toString))
  }

  override def mergeToLiferayRole(cliwixRole: Role, role: liferay.Role) = {
    role.setName(cliwixRole.getName)
    role.setType(toLiferayRoleType(cliwixRole.getType))
    role.setTitleMap(toLiferayTextMap(cliwixRole.getTitles))
    role.setDescriptionMap(toLiferayTextMap(cliwixRole.getDescriptions))
  }

  override def convertToCliwixUserGroup(userGroup: liferay.UserGroup): UserGroup = {
    //val memberUsers = this.userService.getUserGroupUsers(userGroup.getUserGroupId).map(u => new MemberUser(u.getScreenName))
    val memberUsers = nativeSQLGetUserGroupUsers(userGroup)

    val cliwixUserGroup = new UserGroup

    cliwixUserGroup.setName(userGroup.getName)
    cliwixUserGroup.setDescription(trim(userGroup.getDescription))
    if (memberUsers.nonEmpty) {
      cliwixUserGroup.setMemberUsers(memberUsers)
    }

    cliwixUserGroup.setUserGroupId(userGroup.getUserGroupId)
    cliwixUserGroup.setOwnerCompanyId(userGroup.getCompanyId)
    cliwixUserGroup.setUserGroupGroupId(userGroup.getGroup.getGroupId)

    cliwixUserGroup
  }

  /*
   * Safe method to get all usergroup users.
   * UserService.getUserGroupUsers() throws an exception if a userId in the Users_UserGroups table no longer exists
   */
  private def nativeSQLGetUserGroupUsers(userGroup: liferay.UserGroup) = {
    this.nativeSqlAccessUtil.scalarList(
        "select u.screenName from Users_UserGroups ug, User_ u where ug.userId = u.userId and ug.userGroupId = " + userGroup.getUserGroupId,
        Map("screenName" -> Type.STRING))
      .map(screenName => new MemberUser(screenName.toString))
  }

  override def mergeToLiferayUserGroup(cliwixUserGroup: UserGroup, userGroup: liferay.UserGroup) = {
    userGroup.setName(cliwixUserGroup.getName)
    userGroup.setDescription(cliwixUserGroup.getDescription)
  }

  override def convertToCliwixPortlet(portlet: liferay.Portlet) = {
    val cliwixPortlet = new Portlet(portlet.getPortletId, portlet.getDisplayName, portlet.isInstanceable)
    cliwixPortlet.setPortletDbId(portlet.getPrimaryKey)
    cliwixPortlet
  }

  override def convertToCliwixPortalPreferences(prefs: liferay.PortalPreferences): PortalPreferences = {
    val cliwixPreferences = toCliwixPreferences(prefs.getPreferences)
    val cliwixPortalPreferences = new PortalPreferences(cliwixPreferences)
    cliwixPortalPreferences.setPortalPreferencesId(prefs.getPortalPreferencesId)

    cliwixPortalPreferences
  }

  override def mergeToLiferayPortalPreferences(cliwixPortalPreferences: PortalPreferences, portalPreferences: liferay.PortalPreferences) = {
    val preferencesList = if (cliwixPortalPreferences.getPreferences != null) cliwixPortalPreferences.getPreferences.toList else Nil
    portalPreferences.setPreferences(toPortletPreferencesXml(preferencesList))
  }

  override def convertToCliwixSite(site: liferay.Group, withConfiguration: Boolean, withMembers: Boolean): Site = {
    val cliwixSiteConfiguration =
      if (withConfiguration) {
        val cliwixSiteConfiguration = new SiteConfiguration(site.getFriendlyURL, SITE_MEMBERSHIP_TYPE.fromType(site.getType))
        cliwixSiteConfiguration.setActive(site.isActive)
        cliwixSiteConfiguration.setDescription(trim(site.getDescription))
        cliwixSiteConfiguration.setVirtualHostPublicPages(getVirtualHost(site.getCompanyId, site.getGroupId, privatePages = false))
        cliwixSiteConfiguration.setVirtualHostPrivatePages(getVirtualHost(site.getCompanyId, site.getGroupId, privatePages = true))
        cliwixSiteConfiguration
      } else null

    val cliwixSiteMembers =
      if (withMembers) {
        val cliwixSiteMembers = new SiteMembers()


        val memberOrganizations = this.organizationService.getGroupOrganizations(site.getGroupId).map(o => new MemberOrganization(o.getName))
        if (memberOrganizations.nonEmpty) cliwixSiteMembers.setMemberOrganizations(memberOrganizations)

        val memberUserGroups = GroupUtil.getUserGroups(site.getGroupId()).map(g => new MemberUserGroup(g.getName))
        if (memberUserGroups.nonEmpty) cliwixSiteMembers.setMemberUserGroups(memberUserGroups)

        val memberUsers = this.userService.getGroupUsers(site.getGroupId).map(u => new MemberUser(u.getScreenName))
        if (memberUsers.nonEmpty) cliwixSiteMembers.setMemberUsers(memberUsers)

        cliwixSiteMembers
      } else null

    val cliwixSite = new Site(site.getName, cliwixSiteConfiguration, cliwixSiteMembers)
    cliwixSite.setSiteId(site.getGroupId)
    cliwixSite.setOwnerCompanyId(site.getCompanyId)

    cliwixSite
  }

  override def convertToCliwixSiteRoleAssignment(role: liferay.Role, site: liferay.Group) = {
    val roleAssignment = new SiteRoleAssignment(role.getName)

    val memberUserGroups = this.userGroupGroupRoleService
      .getUserGroupGroupRolesByGroupAndRole(site.getGroupId, role.getRoleId)
      .map{ ugr =>
        try {
          new MemberUserGroup(this.userGroupService.getUserGroup(ugr.getUserGroupId).getName)
        } catch {
          case e: Throwable =>
            report.addWarning(s"Some user groups for site role ${role.getName} couldn't be exported. Reason: ${e.getMessage}")
            null
        }
      }
      .filter(_ != null)

    if (memberUserGroups.nonEmpty) roleAssignment.setMemberUserGroups(memberUserGroups)

    val memberUsers = this.userGroupRoleService
      .getUserGroupRolesByGroupAndRole(site.getGroupId, role.getRoleId)
      .map{ ugr =>
        try {
          new MemberUser(ugr.getUser.getScreenName)
        } catch {
          case e: Throwable =>
            report.addWarning(s"Some users for site role ${role.getName} couldn't be exported. Reason: ${e.getMessage}")
            null
        }
      }
      .filter(_ != null)

    if (memberUsers.nonEmpty) roleAssignment.setMemberUsers(memberUsers)

    roleAssignment.setOwnerGroupId(site.getGroupId)
    roleAssignment
  }

  override def mergeToLiferaySiteGroup(cliwixSite: Site, group: liferay.Group) = {
    group.setName(cliwixSite.getName)
    group.setSite(true)
    if (cliwixSite.getSiteConfiguration != null) {
      group.setActive(cliwixSite.getSiteConfiguration.isActive)
      group.setFriendlyURL(cliwixSite.getSiteConfiguration.getFriendlyURL)
      group.setDescription(cliwixSite.getSiteConfiguration.getDescription)
      group.setType(cliwixSite.getSiteConfiguration.getMembershipType.getType)
    }
  }

  override def convertToCliwixPageSet(layoutSet: liferay.LayoutSet): PageSet = {
    val cliwixPageSet = new PageSet()

    cliwixPageSet.setDefaultThemeId(trim(layoutSet.getThemeId))
    cliwixPageSet.setDefaultColorSchemeId(trim(layoutSet.getColorSchemeId))
    cliwixPageSet.setCss(trim(layoutSet.getCss))

    cliwixPageSet.setPrivatePageSet(layoutSet.isPrivateLayout)
    cliwixPageSet.setPageSetId(layoutSet.getLayoutSetId)
    cliwixPageSet.setOwnerGroupId(layoutSet.getGroupId)

    cliwixPageSet
  }

  override def mergeToLiferayLayoutSet(cliwixPageSet: PageSet, layoutSet: liferay.LayoutSet) = {
    layoutSet.setThemeId(cliwixPageSet.getDefaultThemeId)
    layoutSet.setColorSchemeId(cliwixPageSet.getDefaultColorSchemeId)
    layoutSet.setCss(cliwixPageSet.getCss)
  }

  override def convertToCliwixPage(layout: liferay.Layout): Page = {
    val names = toCliwixLocalizedTextContentList(layout.getNameMap)
    val titles = toCliwixLocalizedTextContentList(layout.getTitleMap)
    val descriptions = toCliwixLocalizedTextContentList(layout.getDescriptionMap)
    val keywordsList = toCliwixLocalizedTextContentList(layout.getKeywordsMap)
    val robotsList = toCliwixLocalizedTextContentList(layout.getRobotsMap)

    val cliwixPage = new Page(PAGE_TYPE.fromType(layout.getType), layout.getFriendlyURL, names)

    cliwixPage.setHidden(layout.isHidden)
    cliwixPage.setHtmlTitles(titles)
    cliwixPage.setDescriptions(descriptions)
    cliwixPage.setKeywordsList(keywordsList)
    cliwixPage.setRobotsList(robotsList)
    cliwixPage.setThemeId(trim(layout.getThemeId))
    cliwixPage.setColorSchemeId(trim(layout.getColorSchemeId))
    cliwixPage.setCss(trim(layout.getCss))
    cliwixPage.setPageSettings(toCliwixPageSettings(layout.getTypeSettings))

    cliwixPage.setPageId(layout.getLayoutId)
    cliwixPage.setPortletLayoutId(layout.getPlid)
    cliwixPage.setOwnerGroupId(layout.getGroupId)
    cliwixPage.setPageOrder(layout.getPriority)

    val prefix = if (layout.isPrivateLayout) "privatePages:" else "publicPages:"
    cliwixPage.setPath(prefix + cliwixPage.getFriendlyUrl)

    cliwixPage
  }

  override def mergeToLiferayLayout(cliwixPage: Page, layout: liferay.Layout) = {
    layout.setFriendlyURL(cliwixPage.getFriendlyUrl)
    layout.setType(cliwixPage.getPageType.getType)
    layout.setHidden(cliwixPage.getHidden != null && cliwixPage.getHidden)
    layout.setThemeId(cliwixPage.getThemeId)
    layout.setColorSchemeId(cliwixPage.getColorSchemeId)
    layout.setCss(cliwixPage.getCss)

    layout.setNameMap(toLiferayTextMap(cliwixPage.getNames))
    layout.setTitleMap(toLiferayTextMap(cliwixPage.getHtmlTitles))
    layout.setDescriptionMap(toLiferayTextMap(cliwixPage.getDescriptions))
    layout.setKeywordsMap(toLiferayTextMap(cliwixPage.getKeywordsList))
    layout.setRobotsMap(toLiferayTextMap(cliwixPage.getRobotsList))

    layout.setTypeSettings(toLiferayTypeSettings(cliwixPage.getPageSettings))

    layout.setPriority(cliwixPage.getPageOrder)
  }

  override def convertToCliwixPortletConfiguration(prefs: liferay.PortletPreferences): PortletConfiguration = {
    val cliwixPreferences = toCliwixPreferences(prefs.getPreferences)
    val portletId = prefs.getPortletId
    val basePortletId = portletId.split(PortletConstants.INSTANCE_SEPARATOR)(0)

    val portletConfiguration = new PortletConfiguration(portletId, cliwixPreferences)
    portletConfiguration.setBasePortletId(basePortletId)
    portletConfiguration.setPortletPreferencesId(prefs.getPortletPreferencesId)

    portletConfiguration
  }

  override def mergeToLiferayPortletPreferences(cliwixPortletConfiguration: PortletConfiguration, portletPreferences: liferay.PortletPreferences) = {
    val preferencesList = if (cliwixPortletConfiguration.getPreferences != null) cliwixPortletConfiguration.getPreferences.toList else Nil
    portletPreferences.setPreferences(toPortletPreferencesXml(preferencesList))
  }

  override def convertToCliwixStaticArticle(article: liferayJournal.JournalArticle): StaticArticle = {
    val titles = toCliwixLocalizedTextContentList(article.getTitleMap)
    val contents = toLocalizedXmlContentList(article.getContent)

    val cliwixArticle = new StaticArticle(article.getArticleId, article.getDefaultLocale, titles, contents)

    mapArticleGenericFields(article, cliwixArticle)

    cliwixArticle
  }

  override def convertToCliwixTemplateDrivenArticle(article: liferayJournal.JournalArticle): TemplateDrivenArticle = {
    val titles = toCliwixLocalizedTextContentList(article.getTitleMap)
    val dynamicElementsXml = removeRootElement(article.getContent)

    val cliwixArticle = new TemplateDrivenArticle(article.getArticleId, article.getDefaultLocale, titles,
      article.getStructureId, article.getTemplateId, dynamicElementsXml)

    mapArticleGenericFields(article, cliwixArticle)

    cliwixArticle
  }

  private def mapArticleGenericFields(article: liferayJournal.JournalArticle, cliwixArticle: Article) = {
    cliwixArticle.setType(article.getType)
    cliwixArticle.setDisplayDate(article.getDisplayDate)
    cliwixArticle.setExpirationDate(article.getExpirationDate)
    cliwixArticle.setSummaries(toCliwixLocalizedTextContentList(article.getDescriptionMap))
    cliwixArticle.setArticleDbId(article.getPrimaryKey)
    cliwixArticle.setResourcePrimKey(article.getResourcePrimKey)
    cliwixArticle.setOwnerGroupId(article.getGroupId)
  }

  override def convertToCliwixArticleStructure(articleStructure: liferayJournal.JournalStructure) = {
    val names = toCliwixLocalizedTextContentList(articleStructure.getNameMap)
    val descriptions = toCliwixLocalizedTextContentList(articleStructure.getDescriptionMap)
    val dynamicElements = removeRootElement(articleStructure.getXsd)

    val cliwixArticleStructure = new ArticleStructure(articleStructure.getStructureId, names, dynamicElements)
    cliwixArticleStructure.setDescriptions(descriptions)

    cliwixArticleStructure.setStructureDbId(articleStructure.getId)
    cliwixArticleStructure.setOwnerGroupId(articleStructure.getGroupId)

    cliwixArticleStructure
  }

  override def convertToCliwixArticleTemplate(articleTemplate: liferayJournal.JournalTemplate) = {
    val names = toCliwixLocalizedTextContentList(articleTemplate.getNameMap)
    val descriptions = toCliwixLocalizedTextContentList(articleTemplate.getDescriptionMap)
    val language = articleTemplate.getLangType
    val script = articleTemplate.getXsl

    val cliwixArticleTemplate = new ArticleTemplate(articleTemplate.getTemplateId, names, language, script)
    cliwixArticleTemplate.setDescriptions(descriptions)
    cliwixArticleTemplate.setStructureId(articleTemplate.getStructureId)

    cliwixArticleTemplate.setTemplateDbId(articleTemplate.getId)
    cliwixArticleTemplate.setOwnerGroupId(articleTemplate.getGroupId)

    cliwixArticleTemplate
  }

  override def convertToCliwixFolder(folder: liferayRepository.Folder, parent: DocumentLibraryFolder): DocumentLibraryFolder =
    convertToCliwixFolder(folder.getModel.asInstanceOf[documentLibrary.DLFolder], parent)

  override def convertToCliwixFolder(folder: documentLibrary.DLFolder, parent: DocumentLibraryFolder): DocumentLibraryFolder = {
    val cliwixFolder = new DocumentLibraryFolder(folder.getName)

    cliwixFolder.setDescription(trim(folder.getDescription))
    cliwixFolder.setFolderId(folder.getFolderId)
    cliwixFolder.setPath(if (parent == null) "/" + cliwixFolder.getName else parent.getPath + "/" + cliwixFolder.getName)
    cliwixFolder.setOwnerGroupId(folder.getGroupId)

    cliwixFolder
  }

  override def convertToCliwixFile(file: liferayRepository.FileEntry, parent: DocumentLibraryFolder): DocumentLibraryFile =
    convertToCliwixFile(file.getModel.asInstanceOf[documentLibrary.DLFileEntry], parent)

  override def convertToCliwixFile(file: documentLibrary.DLFileEntry, parent: DocumentLibraryFolder): DocumentLibraryFile = {
    val name = file.getTitle.trim
    val fileDataName =
      if (trim(file.getExtension) != null && !name.endsWith("." + file.getExtension)) name + "." + file.getExtension
      else name

    val cliwixFile = new DocumentLibraryFile(file.getTitle, fileDataName)
    cliwixFile.setDescription(trim(file.getDescription))
    cliwixFile.setFileId(file.getFileEntryId)
    cliwixFile.setFileDataUpdateTimestamp(file.getModifiedDate.getTime)
    cliwixFile.setPath(if (parent == null) "/" + cliwixFile.getName else parent.getPath + "/" + cliwixFile.getName)
    cliwixFile.setOwnerGroupId(file.getGroupId)

    if (trim(file.getExtension) != null && Cliwix.getProperty(Cliwix.PROPERTY_STORE_ACTUAL_FILENAME_IN_DESCRIPTION) == "true") {
      if (cliwixFile.getDescription != null && cliwixFile.getDescription.endsWith("." + file.getExtension)) {
        cliwixFile.setFileDataName(cliwixFile.getDescription)
        cliwixFile.setDescription(null)
      }
    }

    cliwixFile
  }

  override def convertToCliwixResourcePermission(permission: liferay.ResourcePermission, roleName: String, resourceActionList: List[liferay.ResourceAction]) = {
    val cliwixPermission = new ResourcePermission(roleName, new jutil.ArrayList())
    cliwixPermission.setResourcePermissionId(permission.getResourcePermissionId)

    for (action <- resourceActionList) {
      if ((permission.getActionIds & action.getBitwiseValue) == action.getBitwiseValue) {
        cliwixPermission.getActions.add(action.getActionId)
      }
    }

    cliwixPermission
  }

  override def convertToCliwixRolePermission(permission: liferay.ResourcePermission, roleId: Long, resourceActionList: List[liferay.ResourceAction]) = {
    val cliwixPermission = new RolePermission(permission.getName, new jutil.ArrayList())
    cliwixPermission.setResourcePermissionId(permission.getResourcePermissionId)
    cliwixPermission.setRoleId(roleId)

    for (action <- resourceActionList) {
      if ((permission.getActionIds & action.getBitwiseValue) == action.getBitwiseValue) {
        cliwixPermission.getActions.add(action.getActionId)
      }
    }

    cliwixPermission
  }

  override def mergeToLiferayPermission(actions: jutil.List[String], permission: liferay.ResourcePermission, resourceActionList: List[liferay.ResourceAction], actionNotFound: String => Unit = null) = {
    var actionIdsBitWise = 0L

    if (actions != null) actions.foreach { actionName =>
      val resourceAction = resourceActionList.find(_.getActionId == actionName)
      if (resourceAction.isEmpty) {
        if (actionNotFound != null) {
          actionNotFound(actionName)
        }
      } else {
        actionIdsBitWise = actionIdsBitWise | resourceAction.get.getBitwiseValue
      }
    }

    permission.setActionIds(actionIdsBitWise)
  }

  override def toLiferayDate(date: Date, userTimeZone: TimeZone): LiferayDate = {
    assert(userTimeZone != null, "userTimeZone != null")
    if (date != null) {
      val cal = new GregorianCalendar(userTimeZone)
      cal.setTime(date)
      val year = cal.get(Calendar.YEAR)
      val month = cal.get(Calendar.MONTH)
      val day = cal.get(Calendar.DATE)
      val hour = cal.get(Calendar.HOUR_OF_DAY)
      val minute = cal.get(Calendar.MINUTE)
      val second = cal.get(Calendar.SECOND)
      new LiferayDate(year, month, day, hour, minute, second)
    } else {
      LiferayDate()
    }
  }

  override def toLiferayTextMap(textList: jutil.List[LocalizedTextContent]): jutil.Map[jutil.Locale, String] = {
    if (textList == null || textList.size() == 0) {
      null
    } else {
      val map = textList.map { t =>
        (fromLocaleTag(t.getLocale), t.getText)
      }.toMap[jutil.Locale, String]
      map
    }
  }

  override def toLiferayXmlContent(xmlList: jutil.List[LocalizedXmlContent], defaultLocale: String): String = {
    def xmlStaticContent(content: LocalizedXmlContent) = <static-content language-id={content.getLocale}>{Unparsed("<![CDATA[%s]]>".format(content.getXml))}</static-content>

    if (xmlList == null || xmlList.size() == 0) {
      null
    } else {
      val availableLocales = xmlList.map(_.getLocale).mkString(",")
      val xml = <root available-locales={availableLocales} default-locale={defaultLocale}>{xmlList.map(xmlStaticContent)}</root>
      xml.toString()
    }
  }

  override def toLiferayRootXml(xmlString: String, defaultLocale: String): String = {
    val localeRegex = Pattern.compile("(?:locale|language-id)=\\\"([^\\\"]{5})\\\"", Pattern.MULTILINE)

    val availableLocales = mutable.Set[String]()
    val matcher = localeRegex.matcher(xmlString)
    while (matcher.find()) {
      availableLocales += matcher.group(1)
    }

    "<root available-locales=\"" + availableLocales.mkString(",") + "\" default-locale=\"" + defaultLocale + "\">" + xmlString + "</root>"
  }

  protected def toCliwixLocalizedTextContentList(textMap: jutil.Map[jutil.Locale, String]): jutil.List[LocalizedTextContent] = {
    if (textMap == null || textMap.isEmpty) {
      null
    } else {
      val list = textMap.filter(!_._2.isEmpty).map { case (lang, text) =>
        new LocalizedTextContent(toLocaleTag(lang), text)
      }.toList
      if (list.isEmpty) null
      else list
    }
  }

  protected def toLocaleTag(locale: jutil.Locale) = locale.getLanguage + "_" + locale.getCountry

  protected def fromLocaleTag(tag: String) = {
    val parts = tag.split("_")
    assert(parts.length == 2, "valid locale representation")
    new jutil.Locale(parts(0), parts(1))
  }

  protected def toLocalizedXmlContentList(str: String): jutil.List[LocalizedXmlContent] = {
    if (str == null || str.isEmpty) {
      null

    } else {
      val list = new mutable.MutableList[LocalizedXmlContent]

      val xml = XML.loadString(str)
      (xml \ "static-content").foreach { staticContent =>
        list += new LocalizedXmlContent((staticContent \ "@language-id").text, staticContent.text)
      }

      list
    }
  }

  protected def getVirtualHost(companyId: Long, siteId: Long, privatePages: Boolean): String = {
    val layoutSet: LayoutSet = this.layoutSetService.getLayoutSet(siteId, privatePages)
    if (layoutSet != null) {
      val virtualHost = layoutSet.getVirtualHostname
      if (virtualHost.isEmpty) null
      else virtualHost
    } else null
  }

  protected def removeRootElement(xmlString: String) = {
    if (xmlString == null || xmlString.isEmpty) {
      ""
    } else {

      val rootStartTagStartIndex = xmlString.indexOf("<root")
      if (rootStartTagStartIndex > -1) {
        val rootStartTagEndIndex = xmlString.indexOf(">", rootStartTagStartIndex + 4)
        val rootEndTagStartIndex = xmlString.indexOf("</root")

        xmlString.substring(rootStartTagEndIndex + 1, rootEndTagStartIndex)

      } else {

        xmlString
      }
    }
  }

  protected def toCliwixPreferences(xmlString: String): jutil.List[Preference] = {
    val cliwixPreferences = new mutable.MutableList[Preference]

    if (xmlString != null && !xmlString.isEmpty) {
      val xml = XML.loadString(xmlString)

      (xml \ "preference").foreach { pref =>
        def values = (pref \ "value").map(_.text)
        if (values.size == 1) {
          cliwixPreferences += new Preference((pref \ "name").text, values.head)
        } else if (values.size > 1) {
          cliwixPreferences += new Preference((pref \ "name").text, values)
        }
      }
    }

    cliwixPreferences
  }

  protected def toPortletPreferencesXml(preferences: List[Preference]) = {
    def valueXml(value: String) = <value>{value}</value>
    def valuesXml(values: jutil.List[String]) = values.map(valueXml)
    def prefXml(pref: Preference) =
      if (pref.getValues != null) <preference><name>{pref.getName}</name>{valuesXml(pref.getValues)}</preference>
      else <preference><name>{pref.getName}</name>{valueXml(pref.getValue)}</preference>

    val preferencesXml = <portlet-preferences>{preferences.map(prefXml)}</portlet-preferences>
    preferencesXml.toString()
  }

  protected def toCliwixPageSettings(str: String): jutil.List[PageSetting] = {
    if (str == null || str.isEmpty) {
      null
    } else {
      val settingList = str.split("\n").map { s =>
        val parts = s.split("=")
        new PageSetting(parts(0), if (parts.length > 1) parts(1) else "")
      }.toList
      settingList
    }
  }

  protected def toLiferayTypeSettings(pageSettings: jutil.List[PageSetting]) = {
    if (pageSettings == null || pageSettings.size() == 0) {
      null
    } else {
      pageSettings.map(s => s.getKey + "=" + s.getValue).mkString("\n")
    }
  }

  protected def toLiferayRoleType(roleType: ROLE_TYPE) = {
    roleType match {
      case ROLE_TYPE.SITE => RoleConstants.TYPE_SITE
      case ROLE_TYPE.ORGANIZATION => RoleConstants.TYPE_ORGANIZATION
      case _ => RoleConstants.TYPE_REGULAR
    }
  }

  protected def toCliwixRoleType(roleType: Int) = {
    roleType match {
      case RoleConstants.TYPE_SITE => ROLE_TYPE.SITE
      case RoleConstants.TYPE_ORGANIZATION => ROLE_TYPE.ORGANIZATION
      case _ => ROLE_TYPE.REGULAR
    }
  }

  protected def trim(s: String) = {
    if (s == null) null
    else if (s.trim.length == 0) null
    else s.trim
  }

}
