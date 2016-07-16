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

package at.nonblocking.cliwix.core.converter

import java.util.{Date, TimeZone}
import java.{util => jutil}

import at.nonblocking.cliwix.model._
import com.liferay.portal.kernel.repository.{model => liferayRepository}
import com.liferay.portal.model.ResourceAction
import com.liferay.portal.{model => liferay}
import com.liferay.portlet.documentlibrary.{model => documentLibrary}
import com.liferay.portlet.journal.{model => liferayJournal}

private[core] trait LiferayEntityConverter {
  def convertToCliwixCompany(company: liferay.Company, withConfiguration: Boolean = true): Company
  def convertToCliwixUser(user: liferay.User): User
  def convertToCliwixOrganization(org: liferay.Organization): Organization
  def convertToCliwixOrganizationRoleAssignment(role: liferay.Role, org: liferay.Organization): OrganizationRoleAssignment
  def convertToCliwixRole(role: liferay.Role): Role
  def convertToCliwixRegularRoleAssignment(role: liferay.Role): RegularRoleAssignment
  def convertToCliwixUserGroup(userGroup: liferay.UserGroup): UserGroup
  def convertToCliwixPortlet(portlet: liferay.Portlet): Portlet
  def convertToCliwixPortalPreferences(prefs: liferay.PortalPreferences): PortalPreferences
  def convertToCliwixSite(site: liferay.Group, withConfiguration: Boolean = true, withMembers: Boolean = true): Site
  def convertToCliwixSiteRoleAssignment(role: liferay.Role, site: liferay.Group): SiteRoleAssignment
  def convertToCliwixPageSet(layoutSet: liferay.LayoutSet): PageSet
  def convertToCliwixPage(layout: liferay.Layout): Page
  def convertToCliwixPortletConfiguration(prefs: liferay.PortletPreferences): PortletConfiguration
  def convertToCliwixResourcePermission(permission: liferay.ResourcePermission, roleName: String, resourceActionList: List[liferay.ResourceAction]): ResourcePermission
  def convertToCliwixRolePermission(permission: liferay.ResourcePermission, roleId: Long, resourceActionList: List[ResourceAction]): RolePermission
  def convertToCliwixStaticArticle(article: liferayJournal.JournalArticle): StaticArticle
  def convertToCliwixTemplateDrivenArticle(article: liferayJournal.JournalArticle): TemplateDrivenArticle
  def convertToCliwixArticleStructure(articleStructure: liferayJournal.JournalStructure): ArticleStructure
  def convertToCliwixArticleTemplate(articleTemplate: liferayJournal.JournalTemplate): ArticleTemplate
  def convertToCliwixFolder(folder: documentLibrary.DLFolder, parent: DocumentLibraryFolder): DocumentLibraryFolder
  def convertToCliwixFolder(folder: liferayRepository.Folder, parent: DocumentLibraryFolder): DocumentLibraryFolder
  def convertToCliwixFile(file: documentLibrary.DLFileEntry, parent: DocumentLibraryFolder): DocumentLibraryFile
  def convertToCliwixFile(file: liferayRepository.FileEntry, parent: DocumentLibraryFolder): DocumentLibraryFile

  def mergeToLiferayRole(cliwixRole: Role, role: liferay.Role): Unit
  def mergeToLiferayUserGroup(cliwixUserGroup: UserGroup, userGroup: liferay.UserGroup): Unit
  def mergeToLiferayUser(cliwixUser: User, user: liferay.User, contact: liferay.Contact): Unit
  def mergeToLiferayPortalPreferences(cliwixPortalPreferences: PortalPreferences, portalPrefernces: liferay.PortalPreferences): Unit
  def mergeToLiferaySiteGroup(cliwixSite: Site, group: liferay.Group): Unit
  def mergeToLiferayLayoutSet(cliwixPageSet: PageSet, layoutSet: liferay.LayoutSet): Unit
  def mergeToLiferayLayout(cliwixPage: Page, layout: liferay.Layout): Unit
  def mergeToLiferayPortletPreferences(cliwixPortletConfiguration: PortletConfiguration, portletPreferences: liferay.PortletPreferences): Unit
  def mergeToLiferayPermission(actions: jutil.List[String], permission: liferay.ResourcePermission, resourceActionList: List[liferay.ResourceAction], actionNotFound: String => Unit = null): Unit
  def toLiferayTextMap(textList: jutil.List[LocalizedTextContent]): jutil.Map[jutil.Locale, String]
  def toLiferayXmlContent(xmlList: jutil.List[LocalizedXmlContent], defaultLocale: String): String
  def toLiferayRootXml(xmlString: String, defaultLocale: String): String
  def toLiferayDate(date: Date, userTimeZone: TimeZone): LiferayDate

}

