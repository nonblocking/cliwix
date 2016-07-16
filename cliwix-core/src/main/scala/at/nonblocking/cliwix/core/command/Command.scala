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

package at.nonblocking.cliwix.core.command

import at.nonblocking.cliwix.core._
import at.nonblocking.cliwix.model._
import java.io.File

import com.liferay.portal.model.{PortletConstants, Layout}
import com.liferay.portlet.documentlibrary.model.{DLFileEntry, DLFolder}

import java.{util=>jutil}

import com.liferay.portlet.journal.model.{JournalTemplate, JournalStructure, JournalArticle}

abstract class Command[R: Manifest] extends Serializable {
  def resultType = manifest[R].runtimeClass
  def desc: String
}

/* List Commands */

case class CompanyListCommand(withConfiguration: Boolean) extends Command[jutil.Map[String, Company]] {
  override def desc = "List all companies"
}

case class RoleListCommand(companyId: Long) extends Command[jutil.Map[String, Role]] {
  override def desc = s"List all roles of company with DB ID: $companyId"
}

case class UserGroupListCommand(companyId: Long) extends Command[jutil.Map[String, UserGroup]] {
  override def desc = s"List all user groups of company with DB ID: $companyId"
}

case class OrganizationListCommand(companyId: Long) extends Command[jutil.List[Organization]] {
  override def desc = s"List all organizations of company with DB ID: $companyId"
}

case class UserListCommand(companyId: Long) extends Command[jutil.Map[String, User]] {
  override def desc = s"List all users of company with DB ID: $companyId"
}

case class PortalPreferencesReadCommand(companyId: Long) extends Command[PortalPreferences] {
  override def desc = s"Read portal preferences of company with DB ID: $companyId"
}

case class SiteListCommand(companyId: Long, withConfiguration: Boolean) extends Command[jutil.Map[String, Site]] {
  override def desc = s"List all sites of company with DB ID: $companyId"
}

case class PageSetReadCommand(groupId: Long, privatePages: Boolean) extends Command[PageSet] {
  override def desc = if (privatePages) s"Read the private page set of group with DB ID $groupId"
  else s"Read the public page set of group with ID: $groupId"
}

case class PageListCommand(groupId: Long, privatePages: Boolean) extends Command[jutil.List[Page]] {
  override def desc = if (privatePages) s"List the private pages of group with DB ID $groupId"
  else s"List the public pages of group with ID: $groupId"
}

case class PortletConfigurationListCommand(portletLayoutId: Long) extends Command[jutil.Map[String, PortletConfiguration]] {
  override def desc = s"List all portlets on page with layout ID: $portletLayoutId"
}

case class RolePermissionListCommand(companyId: Long, role: Role, filterPermissionsWithNoAction: Boolean) extends Command[jutil.Map[String, RolePermission]] {
  override def desc = s"List all permissions for role: ${role.getName}"
}

abstract class ResourcePermissionListCommand(val companyId: Long, val resourceName: String, val resourcePrimKey: String, val filterPermissionsWithNoAction: Boolean) extends Command[jutil.Map[String, ResourcePermission]] {
  override def desc = s"List all permissions with name '$resourceName' and primary key '$resourcePrimKey'"
}
case class PagePermissionListCommand(override val companyId: Long, page: Page, override val filterPermissionsWithNoAction: Boolean)
  extends ResourcePermissionListCommand(companyId, classOf[Layout].getName, page.getPortletLayoutId.toString, filterPermissionsWithNoAction)

case class PortletPermissionListCommand(override val companyId: Long, page: Page, portletConfiguration: PortletConfiguration, override val filterPermissionsWithNoAction: Boolean)
  extends ResourcePermissionListCommand(companyId, portletConfiguration.getBasePortletId, page.getPortletLayoutId + PortletConstants.LAYOUT_SEPARATOR + portletConfiguration.getPortletId, filterPermissionsWithNoAction)

case class ArticlePermissionListCommand(override val companyId: Long, article: Article, override val filterPermissionsWithNoAction: Boolean)
  extends ResourcePermissionListCommand(companyId, classOf[JournalArticle].getName, String.valueOf(article.getResourcePrimKey), filterPermissionsWithNoAction)

case class ArticleStructurePermissionListCommand(override val companyId: Long, articleStructure: ArticleStructure, override val filterPermissionsWithNoAction: Boolean)
  extends ResourcePermissionListCommand(companyId, classOf[JournalStructure].getName, String.valueOf(articleStructure.getStructureDbId), filterPermissionsWithNoAction)

case class ArticleTemplatePermissionListCommand(override val companyId: Long, articleTemplate: ArticleTemplate, override val filterPermissionsWithNoAction: Boolean)
  extends ResourcePermissionListCommand(companyId, classOf[JournalTemplate].getName, String.valueOf(articleTemplate.getTemplateDbId), filterPermissionsWithNoAction)

case class DocumentLibraryFolderPermissionListCommand(override val companyId: Long, dlFolder: DocumentLibraryFolder, override val filterPermissionsWithNoAction: Boolean)
  extends ResourcePermissionListCommand(companyId, classOf[DLFolder].getName, dlFolder.getFolderId.toString, filterPermissionsWithNoAction)

case class DocumentLibraryFilePermissionListCommand(override val companyId: Long, dlFile: DocumentLibraryFile, override val filterPermissionsWithNoAction: Boolean)
  extends ResourcePermissionListCommand(companyId, classOf[DLFileEntry].getName, dlFile.getFileId.toString, filterPermissionsWithNoAction)

case class DocumentLibraryPermissionListCommand(override val companyId: Long, groupId: Long, override val filterPermissionsWithNoAction: Boolean)
  extends ResourcePermissionListCommand(companyId, "com.liferay.portlet.documentlibrary", groupId.toString, filterPermissionsWithNoAction)

case class ArticleListCommand(groupId: Long) extends Command[jutil.Map[String, Article]] {
  override def desc = s"List articles of group with ID: $groupId"
}

case class ArticleStructureListCommand(groupId: Long) extends Command[jutil.List[ArticleStructure]] {
  override def desc = s"List article structures of group with ID: $groupId"
}

case class ArticleTemplateListCommand(groupId: Long) extends Command[jutil.Map[String, ArticleTemplate]] {
  override def desc = s"List article templates of group with ID: $groupId"
}

case class DocumentLibraryItemListCommand(groupId: Long, dataDir: File, exportOnlyFileDataLastModifiedWithinDays: Option[Int] = None) extends Command[jutil.List[DocumentLibraryItem]] {
  override def desc = s"List document library entries of the group with ID: $groupId"
}

case class RegularRoleAssignmentListCommand(companyId: Long) extends Command[jutil.Map[String, RegularRoleAssignment]] {
  override def desc = s"List role assignments of company with ID: $companyId"
}

case class OrganizationRoleAssignmentListCommand(organizationId: Long) extends Command[jutil.Map[String, OrganizationRoleAssignment]] {
  override def desc = s"List role assignments of organization with ID: $organizationId"
}

case class SiteRoleAssignmentListCommand(siteId: Long) extends Command[jutil.Map[String, SiteRoleAssignment]] {
  override def desc = s"List role assignments of site with ID: $siteId"
}

/* Insert Commands */

case class CompanyInsertCommand(company: Company) extends Command[Company] {
  override def desc = s"Create new company: ${company.toString}"
}

case class UserInsertCommand(companyId: Long, user: User) extends Command[User] {
  override def desc = s"Create new user: '${user.toString}' for company with DB ID: $companyId"
}

case class UserGroupInsertCommand(companyId: Long, userGroup: UserGroup) extends Command[UserGroup] {
  override def desc = s"Create new user group: '${userGroup.toString}' for company with DB ID: $companyId"
}

case class RoleInsertCommand(companyId: Long, role: Role) extends Command[Role] {
  override def desc = s"Create new role: '${role.toString}' for company with DB ID: $companyId"
}

case class OrganizationInsertCommand(companyId: Long, organization: Organization, parentOrganization: Organization) extends Command[Organization] {
  override def desc = s"Create new organization: '${organization.toString}' for company with DB ID: $companyId"
}

case class SiteInsertCommand(companyId: Long, site: Site) extends Command[Site] {
  override def desc = s"Create new site: '${site.toString}' for company with DB ID: $companyId"
}

case class PageSetInsertCommand(groupId: Long, pageSet: PageSet, privatePageSet: Boolean) extends Command[PageSet] {
  override def desc = s"Create new ${if (privatePageSet) "private" else "public"}page set for group with DB ID: $groupId"
}

case class PageInsertCommand(page: Page, parentPage: Page, pageSet: PageSet) extends Command[Page] {
  override def desc = s"Create new page for page set with id '${pageSet.getPageSetId}': ${page.toString}"
}

case class PortletConfigurationInsertCommand(portletLayoutId: Long, portletConfiguration: PortletConfiguration) extends Command[PortletConfiguration] {
  override def desc = s"Create new portlet configuration for layout with ID '$portletLayoutId': ${portletConfiguration.toString}"
}

case class RolePermissionInsertCommand(companyId: Long, role: Role, permission: RolePermission) extends Command[RolePermission] {
  override def desc = s"Create new company permission for role '${role.getName}' and resourceName '${permission.getResourceName}' in company with DB ID '$companyId': ${permission.toString}"
}

abstract class ResourcePermissionInsertCommand(val companyId: Long, val permission: ResourcePermission, val resourceName: String, val resourcePrimKey: String) extends Command[ResourcePermission] {
  override def desc = s"Create new permission for role '${permission.getRole}' and resource '$resourceName' in company with DB ID '$companyId': ${permission.toString}"
}
case class PagePermissionInsertCommand(override val companyId: Long, override val permission: ResourcePermission, page: Page)
  extends ResourcePermissionInsertCommand(companyId, permission, classOf[Layout].getName, page.getPortletLayoutId.toString)

case class PortletPermissionInsertCommand(override val companyId: Long, override val permission: ResourcePermission, page: Page, portletConfiguration: PortletConfiguration)
  extends ResourcePermissionInsertCommand(companyId, permission, portletConfiguration.getBasePortletId, page.getPortletLayoutId + PortletConstants.LAYOUT_SEPARATOR + portletConfiguration.getPortletId)

case class ArticlePermissionInsertCommand(override val companyId: Long, override val permission: ResourcePermission, article: Article)
  extends ResourcePermissionInsertCommand(companyId, permission, classOf[JournalArticle].getName, String.valueOf(article.getResourcePrimKey))

case class ArticleStructurePermissionInsertCommand(override val companyId: Long, override val permission: ResourcePermission,  articleStructure: ArticleStructure)
  extends ResourcePermissionInsertCommand(companyId, permission, classOf[JournalStructure].getName, String.valueOf(articleStructure.getStructureDbId))

case class ArticleTemplatePermissionInsertCommand(override val companyId: Long, override val permission: ResourcePermission, articleTemplate: ArticleTemplate)
  extends ResourcePermissionInsertCommand(companyId, permission, classOf[JournalTemplate].getName, String.valueOf(articleTemplate.getTemplateDbId))

case class DocumentLibraryFolderPermissionInsertCommand(override val companyId: Long, override val permission: ResourcePermission, dlFolder: DocumentLibraryFolder)
  extends ResourcePermissionInsertCommand(companyId, permission, classOf[DLFolder].getName, dlFolder.getFolderId.toString)

case class DocumentLibraryFilePermissionInsertCommand(override val companyId: Long, override val permission: ResourcePermission, dlFile: DocumentLibraryFile)
  extends ResourcePermissionInsertCommand(companyId, permission, classOf[DLFileEntry].getName, dlFile.getFileId.toString)

case class DocumentLibraryPermissionInsertCommand(override val companyId: Long, override val permission: ResourcePermission, groupId: Long)
  extends ResourcePermissionInsertCommand(companyId, permission, "com.liferay.portlet.documentlibrary", groupId.toString)

case class ArticleInsertCommand(companyId: Long, groupId: Long, article: Article) extends Command[Article] {
  override def desc = s"Create new article with ID '${article.getArticleId}' within group with DB ID: $groupId: $article"
}

case class ArticleStructureInsertCommand(companyId: Long, groupId: Long, articleStructure: ArticleStructure, parentArticleStructure: ArticleStructure) extends Command[ArticleStructure] {
  override def desc = s"Create new article structure with ID '${articleStructure.getStructureId}' within group with DB ID: $groupId: ${articleStructure.toString}"
}

case class ArticleTemplateInsertCommand(companyId: Long, groupId: Long, articleTemplate: ArticleTemplate) extends Command[ArticleTemplate] {
  override def desc = s"Create new article with ID '${articleTemplate.getTemplateId}' within group with DB ID '$groupId': ${articleTemplate.toString}"
}

case class DocumentLibraryItemInsertCommand(companyId: Long, groupId: Long, item: DocumentLibraryItem, parentFolder: DocumentLibraryFolder) extends Command[DocumentLibraryItem] {
  override def desc = s"Create new document library item '${item.getName}' within group with DB ID '$groupId': ${item.toString}"
}

case class RegularRoleAssignmentInsertCommand(companyId: Long, roleAssignment: RegularRoleAssignment) extends Command[RegularRoleAssignment] {
  override def desc = s"Create new assignments for role ${roleAssignment.getRoleName} in company with ID: $companyId"
}

case class OrganizationRoleAssignmentInsertCommand(organizationId: Long, roleAssignment: OrganizationRoleAssignment) extends Command[OrganizationRoleAssignment] {
  override def desc = s"Create new assignments for role ${roleAssignment.getRoleName} in organization with ID: $organizationId"
}

case class SiteRoleAssignmentInsertCommand(siteId: Long, roleAssignment: SiteRoleAssignment) extends Command[SiteRoleAssignment] {
  override def desc = s"Create new assignments for role ${roleAssignment.getRoleName} in site with ID: $siteId"
}

/* Generic Commands */

case class GetByDBIdCommand[T <: LiferayEntity : Manifest](dbId: Long, entityClass: Class[T]) extends Command[T] {
  override def resultType = entityClass
  override def desc = s"Reading ${entityClass.getSimpleName} with DB ID: $dbId"
}

case class GetByIdentifierOrPathCommand[T <: CompanyMember : Manifest](identifierOrPath: String, companyId: Long, entityClass: Class[T]) extends Command[T] {
  override def resultType = entityClass
  override def desc =
    if (entityClass.isAssignableFrom(classOf[LiferayEntityWithUniquePathIdentifier])) {
      s"Loading '${entityClass.getSimpleName}' with path '$identifierOrPath'"
    } else {
      s"Loading '${entityClass.getSimpleName}' with identifier '$identifierOrPath'"
    }
  }

case class GetByIdentifierOrPathWithinGroupCommand[T <: GroupMember : Manifest](identifierOrPath: String, companyId: Long, groupId: Long, entityClass: Class[T]) extends Command[T] {
  override def resultType = entityClass
  override def desc =
    if (entityClass.isAssignableFrom(classOf[LiferayEntityWithUniquePathIdentifier])) {
      s"Loading '${entityClass.getSimpleName}' with path '$identifierOrPath' within group '$groupId'"
    } else {
      s"Loading '${entityClass.getSimpleName}' with identifier '$identifierOrPath' within group '$groupId'"
    }
}

case class UpdateCommand[T <: LiferayEntity : Manifest](entity: T) extends Command[T] {
  override def resultType = entity.getClass
  override def desc = s"Update ${entityString(entity)} with DB ID: ${entity.getDbId}"
}

case class DeleteCommand[T <: LiferayEntity : Manifest](entity: T) extends Command[T] {
  override def resultType = entity.getClass
  override def desc = s"Delete ${entityString(entity)} with DB ID: ${entity.getDbId}"
}


