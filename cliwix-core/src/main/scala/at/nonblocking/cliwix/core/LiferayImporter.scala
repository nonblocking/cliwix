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

package at.nonblocking.cliwix.core

import java.io.{FileNotFoundException, File}
import java.{util => jutil}

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.interceptor.ProcessingInterceptorDispatcher
import at.nonblocking.cliwix.core.filedata.FileDataResolver
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.core.transaction.CliwixTransaction
import at.nonblocking.cliwix.core.util._
import at.nonblocking.cliwix.core.validation.{CliwixValidationException, LiferayConfigValidator, ValidationError, XMLSchemaValidationEventHandler}
import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.io.FileUtils
import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.util.Random

sealed trait LiferayImporter {
  def importFromFile(xmlFile: File, reportFolder: File, fileDataResolver: FileDataResolver, importerConfig: LiferayImporterConfig): Unit
  def doImport(liferayConfig: LiferayConfig, fileDataResolver: FileDataResolver, importerConfig: LiferayImporterConfig): Unit
}

private[core] class LiferayImporterImpl extends LiferayImporter with LazyLogging with Reporting with TreeTypeUtils with ListTypeUtils with LiferayCacheUtil {

  @BeanProperty
  var handler: DispatchHandler = _

  @BeanProperty
  var liferayInfo: LiferayInfo = _

  @BeanProperty
  var cliwixTransaction: CliwixTransaction = _

  @BeanProperty
  var validators: Array[LiferayConfigValidator] = _

  @BeanProperty
  var processingInterceptor: ProcessingInterceptorDispatcher = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  @BeanProperty
  var resourceAwareXmlSerializer: ResourceAwareXmlSerializer = _

  override def importFromFile(xmlFile: File, reportFolder: File, fileDataResolver: FileDataResolver, importerConfig: LiferayImporterConfig) = {
    if (xmlFile == null || !xmlFile.exists()) throw new FileNotFoundException(xmlFile.getAbsolutePath)

    val baseFolder = if (reportFolder != null) reportFolder else xmlFile.getParentFile

    val timestamp = new jutil.Date()
    report.start(s"import_report.html", "Import Report")
    report.addMessage(s"Cliwix version: ${Cliwix.getVersion}.")
    report.addMessage(s"Liferay version: ${this.liferayInfo.getReleaseInfo}.")
    report.addMessage(s"Host: ${HostUtil.hostName}.")
    report.addMessage(s"Import started at ${Cliwix.formatDate(timestamp)}.")
    report.addBreak()

    val importInfo = new CliwixInfoProperties(baseFolder)
    importInfo.setConfigProperty(importerConfig.toString)

    val startTime = System.currentTimeMillis()

    try {
      baseFolder.mkdirs()

      val xmlSchemaValidationHandler = new XMLSchemaValidationEventHandler
      val liferayConfig = this.resourceAwareXmlSerializer.fromXML(xmlFile, xmlSchemaValidationHandler)

      if (xmlSchemaValidationHandler.validationErrors.nonEmpty) {
        addValidationErrorsToReport(xmlSchemaValidationHandler.validationErrors)
        throw new CliwixValidationException("XML Schema validation errors occurred. See report.")
      }

      doImport(liferayConfig, fileDataResolver, importerConfig)

      importInfo.setStateProperty(CliwixInfoProperties.STATE_SUCCESS)

    } catch {
      case e: Throwable =>
        report.setBottomSection()
        report.addError("Import failed!", e)
        importInfo.setStateProperty(CliwixInfoProperties.STATE_FAILED)
        importInfo.setErrorMessageProperty(e)
        throw e
    } finally {
      report.print(baseFolder)

      importInfo.setDurationProperty(System.currentTimeMillis() - startTime)
      importInfo.save()
    }
  }

  override def doImport(liferayConfig: LiferayConfig, fileDataResolver: FileDataResolver, importerConfig: LiferayImporterConfig) = {
    ExecutionContext.initFromImporterConfig(importerConfig, fileDataResolver)
    try {
      if (Cliwix.getProperty(Cliwix.PROPERTY_CLEAR_LIFERAY_CACHES_BEFORE_IMPORTEXPORT) == "true") clearLiferayCaches()
      if (Cliwix.getProperty(Cliwix.PROPERTY_DISABLED_LIFERAY_CACHES_DURING_IMPORTEXPORT) == "true") disableLiferayCaching()

      if (importerConfig.atomicTransaction) {
        this.cliwixTransaction.executeWithinLiferayTransaction(rollback = importerConfig.simulationMode)(doInternalImport(liferayConfig, importerConfig))
      } else {
        if (importerConfig.simulationMode) throw new CliwixException("Simuation mode can only be used with atomic transaction on!")
        doInternalImport(liferayConfig, importerConfig)
      }

    } finally {
      if (Cliwix.getProperty(Cliwix.PROPERTY_DISABLED_LIFERAY_CACHES_DURING_IMPORTEXPORT) == "true") enableLiferayCaching()

      ExecutionContext.destroy()
    }
  }

  def validateLiferayConfig(config: LiferayConfig) = {
    val validationErrors = this.validators.foldLeft(List[ValidationError]()) { (list, validator) =>
      list ++ validator.validate(config)
    }
    if (validationErrors.nonEmpty) {
      addValidationErrorsToReport(validationErrors)
      throw new CliwixValidationException("There were multiple input validation errors.")
    }
  }

  def addValidationErrorsToReport(errors: List[ValidationError]) = {
    errors.foreach { error =>
      val location =
        if (error.location != null) "(" + error.location + ")"
        else ""
      report.addError(s"Input validation error: ${error.message} $location.", null)
    }
  }

  private def doInternalImport(config: LiferayConfig, importerConfig: LiferayImporterConfig) = {
    assert(config != null, "config != null")

    val startTime = System.currentTimeMillis()
    validateLiferayConfig(config)

    if (importerConfig.overrideRootImportPolicy != null) {
      report.addMessage(s"Overriding existing root import policy with: ${importerConfig.overrideRootImportPolicy}")
      config.getCompanies.setImportPolicy(importerConfig.overrideRootImportPolicy)
    }

    if (config.getCompanies != null && config.getCompanies.getImportPolicy == null) {
      report.addWarning("No root import policy found. Setting it to ENFORCE.")
      config.getCompanies.setImportPolicy(IMPORT_POLICY.ENFORCE)
    }

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importCompanies(config.getCompanies, deferredDeletes)
    executeDeferredDeletes(deferredDeletes)

    val totalTime = (System.currentTimeMillis() - startTime) / 1000
    val totalMemory = Runtime.getRuntime.totalMemory() / (1024 * 1024)

    report.setBottomSection()
    report.addMessage(s"Import finished in $totalTime seconds.")
    report.addMessage(s"Total memory usage: $totalMemory MB.")
    report.addSuccess("Success.")
  }

  private def importCompanies(companies: Companies, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    val existingCompanies = this.handler.execute(CompanyListCommand(withConfiguration = true)).result
    processList[Company](companies, existingCompanies, -1, c => CompanyInsertCommand(c), deferredDeletes)

    safeForeach(companies) { c =>
      report.setSection(s"Company ${c.identifiedBy}")

      ExecutionContext.updateCompanyContext(c)

      if (c.getPortalPreferences != null) importPortalPreferences(c.getPortalPreferences, c)
      if (c.getUsers != null) importUsers(c.getUsers, c, companies.getImportPolicy, deferredDeletes)
      if (c.getUserGroups != null) importUserGroups(c.getUserGroups, c, companies.getImportPolicy, deferredDeletes)
      if (c.getRoles != null) importRoles(c.getRoles, c, companies.getImportPolicy, deferredDeletes)
      if (c.getOrganizations != null) importOrganizations(c.getOrganizations, c, companies.getImportPolicy, deferredDeletes)
      if (c.getRegularRoleAssignments != null) importRegularRoleAssignments(c.getRegularRoleAssignments, c, companies.getImportPolicy, deferredDeletes)
      if (c.getSites != null) importSites(c.getSites, c, companies.getImportPolicy, deferredDeletes)
    }
  }

  private def importPortalPreferences(portalPreferences: PortalPreferences, company: Company) = {
    val existingPortalPreferences = this.handler.execute(PortalPreferencesReadCommand(company.getCompanyId)).result
    assert(existingPortalPreferences != null, "Portal preferences exists")

    portalPreferences.copyIds(existingPortalPreferences)
    this.processingInterceptor.beforeEntityUpdate(portalPreferences, existingPortalPreferences, company.getCompanyId)
    if (!this.liferayEntityComparator.equals(existingPortalPreferences, portalPreferences)) {
      val diff = this.liferayEntityComparator.diff(existingPortalPreferences, portalPreferences).mkString(",")
      execute(UpdateCommand(portalPreferences), portalPreferences)
      report.addMessage(s"Updated portal preferences. Changes: $diff.")
    }
  }

  private def importUsers(users: Users, company: Company, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(users, parentPolicy)

    val existingUsers = this.handler.execute(UserListCommand(company.getCompanyId)).result
    processList[User](users, existingUsers, company.getCompanyId, u => UserInsertCommand(company.getCompanyId, u), deferredDeletes)
  }

  private def importUserGroups(userGroups: UserGroups, company: Company, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(userGroups, parentPolicy)

    val existingUserGroup = this.handler.execute(UserGroupListCommand(company.getCompanyId)).result
    processList[UserGroup](userGroups, existingUserGroup, company.getCompanyId, u => UserGroupInsertCommand(company.getCompanyId, u), deferredDeletes)
  }

  private def importOrganizations(organizations: Organizations, company: Company, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(organizations, parentPolicy)

    val existingOrganizations = new Organizations(this.handler.execute(OrganizationListCommand(company.getCompanyId)).result)
    processTree[Organization](organizations, existingOrganizations, company.getCompanyId,
      (org, parentOrg) => OrganizationInsertCommand(company.getCompanyId, org, parentOrg), deferredDeletes, deleteChildrenExplicitly = true)

    safeProcessRecursively(organizations) { org =>
      mergeImportPolicy(org.getOrganizationRoleAssignments, organizations.getImportPolicy)

      val existingRoleAssignments = this.handler.execute(OrganizationRoleAssignmentListCommand(org.getOrganizationId)).result
      processList[OrganizationRoleAssignment](org.getOrganizationRoleAssignments, existingRoleAssignments, company.getCompanyId, r => OrganizationRoleAssignmentInsertCommand(org.getOrganizationId, r), deferredDeletes)
    }
  }

  private def importRoles(roles: Roles, company: Company, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(roles, parentPolicy)

    val existingRoles = this.handler.execute(RoleListCommand(company.getCompanyId)).result
    processList[Role](roles, existingRoles, company.getCompanyId, r => RoleInsertCommand(company.getCompanyId, r), deferredDeletes)

    safeForeach(roles) { role =>
      mergeImportPolicy(role.getPermissions, roles.getImportPolicy)
      val existingPermissions = this.handler.execute(RolePermissionListCommand(company.getCompanyId, role, filterPermissionsWithNoAction = false)).result
      processList[RolePermission](role.getPermissions, existingPermissions, company.getCompanyId, rp => RolePermissionInsertCommand(company.getCompanyId, role, rp), deferredDeletes)
    }
  }

  private def importRegularRoleAssignments(roleAssignments: RegularRoleAssignments, company: Company, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(roleAssignments, parentPolicy)

    val existingRoleAssignments = this.handler.execute(RegularRoleAssignmentListCommand(company.getCompanyId)).result
    processList[RegularRoleAssignment](roleAssignments, existingRoleAssignments, company.getCompanyId, r => RegularRoleAssignmentInsertCommand(company.getCompanyId, r), deferredDeletes)

    //Make sure a valid admin user is set
    ExecutionContext.updateSecurityContext(failWhenNoAdminUserFound = true)
  }

  private def importSites(sites: Sites, company: Company, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    assert(company.getCompanyId != null)
    mergeImportPolicy(sites, parentPolicy)

    val existingSites = this.handler.execute(SiteListCommand(company.getCompanyId, withConfiguration = true)).result
    processList[Site](sites, existingSites, company.getCompanyId, s => SiteInsertCommand(company.getCompanyId, s), deferredDeletes)

    safeForeach(sites) { site =>
      report.setSubSection(s"Site ${site.identifiedBy}")

      ExecutionContext.updateGroupContext(site.getSiteId)

      if (site.getSiteRoleAssignments != null) importSiteRoleAssignments(site.getSiteRoleAssignments, company, site, sites.getImportPolicy, deferredDeletes)
      if (site.getSiteContent != null) importSiteContent(site.getSiteContent, company, site, sites.getImportPolicy, deferredDeletes)

      if (site.getPublicPages != null) importPageSetAndPages(site.getPublicPages, privatePages = false, company.getCompanyId, site, sites.getImportPolicy, deferredDeletes)
      if (site.getPrivatePages != null) importPageSetAndPages(site.getPrivatePages, privatePages = true, company.getCompanyId, site, sites.getImportPolicy, deferredDeletes)
    }
  }

  private def importSiteRoleAssignments(roleAssignments: SiteRoleAssignments, company: Company, site: Site, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) =  {
    assert(site.getSiteId != null)
    mergeImportPolicy(roleAssignments, parentPolicy)

    val existingRoleAssignments = this.handler.execute(SiteRoleAssignmentListCommand(site.getSiteId)).result
    processList[SiteRoleAssignment](roleAssignments, existingRoleAssignments, company.getCompanyId, r => SiteRoleAssignmentInsertCommand(site.getSiteId, r), deferredDeletes)
  }

  private def importSiteContent(siteContent: SiteContent, company: Company, site: Site, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) =  {
    assert(site.getSiteId != null)
    mergeImportPolicy(siteContent, parentPolicy)

    if (siteContent.getDocumentLibrary != null) importDocumentLibrary(siteContent.getDocumentLibrary, company.getCompanyId, site.getSiteId, siteContent.getImportPolicy, deferredDeletes)
    if (siteContent.getWebContent != null) importWebContent(siteContent.getWebContent, company.getCompanyId, site, siteContent.getImportPolicy, deferredDeletes)
  }

  private def importPageSetAndPages(pageSet: PageSet, privatePages: Boolean, companyId: Long, site: Site, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    assert(site.getSiteId != null)

    if (pageSet != null) {
      val existingPageSet = this.handler.execute(PageSetReadCommand(site.getSiteId, privatePages)).result

      if (existingPageSet != null) {
        pageSet.copyIds(existingPageSet)
        this.processingInterceptor.beforeEntityUpdate(pageSet, existingPageSet, companyId)
        if (!this.liferayEntityComparator.equals(existingPageSet, pageSet)) {
          val diff = this.liferayEntityComparator.diff(existingPageSet, pageSet).mkString(",")
          execute(UpdateCommand(pageSet), pageSet)
          report.addMessage(s"Updated ${if (privatePages) "private" else "public"} page with DB ID ${pageSet.getDbId}). Changes: $diff.")
        }
      } else {
        this.processingInterceptor.beforeEntityInsert(pageSet, companyId)
        execute(PageSetInsertCommand(site.getSiteId, pageSet, privatePages), pageSet)
        report.addMessage(s"Created ${if (privatePages) "private" else "public"} page set created with DB ID ${pageSet.getDbId}).")
      }

      //Pages
      val pages = pageSet.getPages
      val existingPages = new Pages()

      if (existingPageSet != null) {
        existingPages.setRootPages(this.handler.execute(PageListCommand(site.getSiteId, privatePages)).result)
      }

      mergeImportPolicy(pages, parentPolicy)

      processTree[Page](pages, existingPages, companyId, (page, parentPage) => PageInsertCommand(page, parentPage, pageSet), deferredDeletes)

      safeProcessRecursively(pages) { page =>
        importPermissions(page.getPermissions, companyId, pages.getImportPolicy,
          permissionListCommand = PagePermissionListCommand(companyId, page, filterPermissionsWithNoAction = false),
          createPermissionInsertCommand = permission => PagePermissionInsertCommand(companyId, permission, page),
          deferredDeletes, s" for Page('${page.identifiedBy()}')")
        importPortletConfigurations(page, companyId, pages.getImportPolicy, deferredDeletes)
      }
    }
  }

  private def importPortletConfigurations(page: Page, companyId: Long, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    assert(page.getPortletLayoutId != null)
    mergeImportPolicy(page.getPortletConfigurations, parentPolicy)

    val existingPortletConfiguration = this.handler.execute(PortletConfigurationListCommand(page.getPortletLayoutId)).result
    processList[PortletConfiguration](page.getPortletConfigurations, existingPortletConfiguration, companyId,
      pc => PortletConfigurationInsertCommand(page.getPortletLayoutId, pc), deferredDeletes,
      s" on Page('${page.identifiedBy}')")

    safeForeach(page.getPortletConfigurations) { pc =>
      importPermissions(pc.getPermissions, companyId, page.getPortletConfigurations.getImportPolicy,
        permissionListCommand = PortletPermissionListCommand(companyId, page, pc, filterPermissionsWithNoAction = false),
        createPermissionInsertCommand = permission => PortletPermissionInsertCommand(companyId, permission, page, pc),
        deferredDeletes, s" for Portlet('${pc.identifiedBy()}') on Page('${page.identifiedBy}')")
    }
  }

  private def importWebContent(webContent: WebContent, companyId: Long, site: Site, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(webContent, parentPolicy)

    if (webContent != null) {
      if (webContent.getStructures != null) importArticleStructures(webContent.getStructures, companyId, site, webContent.getImportPolicy, deferredDeletes)
      if (webContent.getTemplates != null) importArticleTemplates(webContent.getTemplates, companyId, site, webContent.getImportPolicy, deferredDeletes)
      if (webContent.getArticles != null) importArticles(webContent.getArticles, companyId, site, webContent.getImportPolicy, deferredDeletes)
    }
  }

  private def importArticleStructures(articleStructures: ArticleStructures, companyId: Long, site: Site, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(articleStructures, parentPolicy)

    val existingArticleStructures = new ArticleStructures(this.handler.execute(ArticleStructureListCommand(site.getSiteId)).result)
    processTree[ArticleStructure](articleStructures, existingArticleStructures, companyId,
      (structure, parentStructure) => ArticleStructureInsertCommand(companyId, site.getSiteId, structure, parentStructure),
      deferredDeletes, deleteChildrenExplicitly = true)

    safeProcessRecursively(articleStructures) { articleStructure =>
      importPermissions(articleStructure.getPermissions, companyId, articleStructures.getImportPolicy,
        permissionListCommand = ArticleStructurePermissionListCommand(companyId, articleStructure, filterPermissionsWithNoAction = false),
        createPermissionInsertCommand = permission => ArticleStructurePermissionInsertCommand(companyId, permission, articleStructure),
        deferredDeletes, s" for ArticleStructure('${articleStructure.identifiedBy()}')")
    }
  }

  private def importArticleTemplates(articleTemplates: ArticleTemplates, companyId: Long, site: Site, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(articleTemplates, parentPolicy)

    val existingArticleTemplates = this.handler.execute(ArticleTemplateListCommand(site.getSiteId)).result
    processList[ArticleTemplate](articleTemplates, existingArticleTemplates, companyId,
      template => ArticleTemplateInsertCommand(companyId, site.getSiteId, template), deferredDeletes)

    safeForeach(articleTemplates) { articleTemplate =>
      importPermissions(articleTemplate.getPermissions, companyId, articleTemplates.getImportPolicy,
        permissionListCommand = ArticleTemplatePermissionListCommand(companyId, articleTemplate, filterPermissionsWithNoAction = false),
        createPermissionInsertCommand = permission => ArticleTemplatePermissionInsertCommand(companyId, permission, articleTemplate),
        deferredDeletes, s" for ArticleTemplate('${articleTemplate.identifiedBy()}')")
    }
  }

  private def importArticles(articles: Articles, companyId: Long, site: Site, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(articles, parentPolicy)

    val existingArticles = this.handler.execute(ArticleListCommand(site.getSiteId)).result
    processList[Article](articles, existingArticles, companyId, a => ArticleInsertCommand(companyId, site.getSiteId, a), deferredDeletes)

    safeForeach(articles) { article =>
      importPermissions(article.getPermissions, companyId, articles.getImportPolicy,
        permissionListCommand = ArticlePermissionListCommand(companyId, article, filterPermissionsWithNoAction = false),
        createPermissionInsertCommand = permission => ArticlePermissionInsertCommand(companyId, permission, article),
        deferredDeletes, s" for Article('${article.identifiedBy()}')")
    }
  }

  private def importDocumentLibrary(documentLibrary: DocumentLibrary, companyId: Long, siteId: Long, parentPolicy: IMPORT_POLICY, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    mergeImportPolicy(documentLibrary, parentPolicy)

    val existingDocumentLibrary = new DocumentLibrary()
    existingDocumentLibrary.setRootItems(this.handler.execute(DocumentLibraryItemListCommand(siteId, null)).result)

    processTree[DocumentLibraryItem](documentLibrary, existingDocumentLibrary, companyId,
      (item, parentItem) => DocumentLibraryItemInsertCommand(companyId, siteId, item, parentItem.asInstanceOf[DocumentLibraryFolder]),
      deferredDeletes)

    if (documentLibrary != null) {
      importPermissions(documentLibrary.getPermissions, companyId, documentLibrary.getImportPolicy,
        permissionListCommand = DocumentLibraryPermissionListCommand(companyId, siteId, filterPermissionsWithNoAction = false),
        createPermissionInsertCommand = permission => DocumentLibraryPermissionInsertCommand(companyId, permission, siteId),
        deferredDeletes, "for DocumentLibrary")

      safeProcessRecursively(documentLibrary) {
        case folder: DocumentLibraryFolder =>
          importPermissions(folder.getPermissions, companyId, documentLibrary.getImportPolicy,
            permissionListCommand = DocumentLibraryFolderPermissionListCommand(companyId, folder, filterPermissionsWithNoAction = false),
            createPermissionInsertCommand = permission => DocumentLibraryFolderPermissionInsertCommand(companyId, permission, folder),
            deferredDeletes, s" for DocumentLibraryFolder('${folder.getPath}')")
        case file: DocumentLibraryFile =>
          importPermissions(file.getPermissions, companyId, documentLibrary.getImportPolicy,
            permissionListCommand = DocumentLibraryFilePermissionListCommand(companyId, file, filterPermissionsWithNoAction = false),
            createPermissionInsertCommand = permission => DocumentLibraryFilePermissionInsertCommand(companyId, permission, file),
            deferredDeletes, s" for DocumentLibraryFile('${file.getPath}')")
      }
    }
  }

  private def importPermissions(permissions: ResourcePermissions, companyId: Long, parentPolicy: IMPORT_POLICY,
                                permissionListCommand: ResourcePermissionListCommand, createPermissionInsertCommand: ResourcePermission => ResourcePermissionInsertCommand,
                                deferredDeletes: jutil.List[DeferredDeleteCommand[_]],
                                reportSuffix: String) = {

    mergeImportPolicy(permissions, parentPolicy)

    val existingPermissions = this.handler.execute(permissionListCommand).result
    processList(permissions, existingPermissions, companyId, createPermissionInsertCommand, deferredDeletes, reportSuffix)
  }

  private[core] def processList[T <: LiferayEntity : Manifest](targetList: ListType[T],
                                                               existingMap: jutil.Map[String, T],
                                                               companyId: Long,
                                                               createInsertCommand: T => Command[_],
                                                               deferredDeletes: jutil.List[DeferredDeleteCommand[_]],
                                                               reportSuffix: String = "") = {

    assert(targetList == null || targetList.getImportPolicy != null, "importPolicy not null: " + targetList)

    if (targetList != null) {
      this.processingInterceptor.beforeListImport(targetList, companyId)

      processRemove()
      processInsertUpdate()

      def processRemove() = {
        if (existingMap != null && targetList.getImportPolicy == IMPORT_POLICY.ENFORCE) {
          val keysToRemove =
            if (targetList.getList != null)
              existingMap.filter(e => !targetList.getList.exists(_.identifiedBy() == e._1)).keys
            else
              existingMap.keys.toList

          keysToRemove.foreach { key =>
            val entity = existingMap.get(key)
            executeDelete(DeleteCommand(entity), reportSuffix, deferredDeletes)
          }
        }
      }

      def processInsertUpdate() = {
        safeForeach(targetList) { entity =>
          if (existingMap.contains(entity.identifiedBy())) {
            val existing = existingMap.get(entity.identifiedBy())
            entity.copyIds(existing)
            this.processingInterceptor.beforeEntityUpdate(entity, existing, companyId)
            if (targetList.getImportPolicy != IMPORT_POLICY.INSERT && !this.liferayEntityComparator.equals(existing, entity)) {
              val diff = this.liferayEntityComparator.diff(existing, entity).mkString(",")
              execute(UpdateCommand(entity), entity)
              report.addMessage(s"Updated ${entityString(entity)} with DB ID ${entity.getDbId}$reportSuffix. Changes: $diff.")
            }
          } else {
            this.processingInterceptor.beforeEntityInsert(entity, companyId)
            execute(createInsertCommand(entity), entity)
            report.addMessage(s"Created ${entityString(entity)} with DB ID ${entity.getDbId}$reportSuffix.")
          }
        }
      }
    }
  }

  private[core] def processTree[T <: LiferayEntity : Manifest](targetTree: TreeType[T],
                                                               existingTree: TreeType[T],
                                                               companyId: Long,
                                                               createInsertCommand: (T, T) => Command[_],
                                                               deferredDeletes: jutil.List[DeferredDeleteCommand[_]],
                                                               deleteChildrenExplicitly: Boolean = false,
                                                               reportSuffix: String  = "") = {

    assert(targetTree == null || targetTree.getImportPolicy != null, "importPolicy not null: " + targetTree)

    if (targetTree != null) {
      this.processingInterceptor.beforeTreeImport(targetTree, companyId)

      val existingRootList = if (existingTree != null) existingTree.getRootItems else null

      //For trees it is necessary to remove items first, to be able to "move" things within the hierarchy
      processRemove(targetTree.getRootItems, existingRootList)
      processInsertUpdate(targetTree.getRootItems, existingRootList, null.asInstanceOf[T])

      def processRemove(targetList: jutil.List[T], existingList: jutil.List[T]): Unit = {
        if (existingList != null && targetTree.getImportPolicy == IMPORT_POLICY.ENFORCE) {
          existingList.foreach { existingEntity =>
            val targetEntity =
              if (targetList != null) targetList.find(_.identifiedBy() == existingEntity.identifiedBy())
              else None
            if (targetEntity.isEmpty) {
              executeDelete(DeleteCommand(existingEntity), reportSuffix, deferredDeletes)
              if (deleteChildrenExplicitly) {
                processRemove(null, existingTree.getSubItems(existingEntity))
              }
            } else {
              processRemove(targetTree.getSubItems(targetEntity.get), existingTree.getSubItems(existingEntity))
            }
          }
        }
      }

      def processInsertUpdate(targetList: jutil.List[T], existingList: jutil.List[T], parentItem: T): Unit = {
        if (targetList != null) targetList.foreach { entity =>
          val existingEntity =
            if (existingList != null) {
              existingList.find(_.identifiedBy() == entity.identifiedBy())
            } else {
              None
            }

          if (existingEntity.isEmpty) {
            this.processingInterceptor.beforeEntityInsert(entity, companyId)
            execute(createInsertCommand(entity, parentItem), entity)
            report.addMessage(s"Created ${entityString(entity)} with DB ID ${entity.getDbId}$reportSuffix.")

          } else {
            entity.copyIds(existingEntity.get)

            this.processingInterceptor.beforeEntityUpdate(entity, existingEntity.get, companyId)

            if (targetTree.getImportPolicy != IMPORT_POLICY.INSERT && !this.liferayEntityComparator.equals(existingEntity.get, entity)) {
              val diff = this.liferayEntityComparator.diff(existingEntity.get, entity).mkString(",")
              execute(UpdateCommand(entity), entity)
              report.addMessage(s"Updated  ${entityString(entity)} with DB ID ${entity.getDbId}$reportSuffix. Changes: $diff.")
            }
          }

          processInsertUpdate(targetTree.getSubItems(entity), if (existingEntity.isDefined) existingTree.getSubItems(existingEntity.get) else null, entity)
        }
      }

    }
  }

  private def execute(command: Command[_], updateEntity: LiferayEntity) = {
    val commandResult = this.handler.execute(command)
    if (updateEntity != null && commandResult != null && commandResult.result != null && commandResult.result.isInstanceOf[LiferayEntity]) {
      updateEntity.copyIds(commandResult.result.asInstanceOf[LiferayEntity])
    }
  }

  private def executeDelete[T <: LiferayEntity](command: DeleteCommand[T], reportSuffix: String, deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    try {
      val entity = command.entity
      this.handler.execute(command)
      report.addMessage(s"Removed ${entityString(entity)} with DB ID ${entity.getDbId}$reportSuffix.")
    } catch {
      case e: CliwixCommandExecutionException =>
        val cause = e.getCause
        val msg = cause.getClass.getName + ": " + cause.getMessage
        logger.info("Failed to execute delete: {}. Cause: {}. Retry it at the end of the import.", command, msg)
        deferredDeletes += DeferredDeleteCommand(
          ExecutionContext.currentCompany,
          ExecutionContext.currentGroupId,
          report.getCurrentSections,
          reportSuffix, command)
    }
  }

  private[core] def executeDeferredDeletes(deferredDeletes: jutil.List[DeferredDeleteCommand[_]]) = {
    val currentCompanyId = -1

    deferredDeletes.reverse.foreach{ deleteCommand =>
      report.setSections(deleteCommand.reportSections)
      if (deleteCommand.company != null && currentCompanyId != deleteCommand.company.getCompanyId) ExecutionContext.updateCompanyContext(deleteCommand.company)
      if (deleteCommand.groupId > 0) ExecutionContext.updateGroupContext(deleteCommand.groupId)

      val entity = deleteCommand.command.entity.asInstanceOf[LiferayEntity]

      try {
        this.handler.execute(deleteCommand.command)
        report.addMessage(s"Removed ${entityString(entity)} with DB ID ${entity.getDbId}${deleteCommand.reportSuffix}.")
      } catch {
        case e: CliwixCommandExecutionException =>
          if (ExecutionContext.flags.ignoreDeletionFailures) {
            val cause = e.getCause
            val msg = cause.getClass.getName + ": " + cause.getMessage
            if (msg.toLowerCase.contains("required")) {
              report.addWarning(s"Unable to remove ${entityString(entity)} with DB ID ${entity.getDbId}${deleteCommand.reportSuffix} because it required by some configuration not managed by Cliwix.")
            } else {
              report.addWarning(s"Unable to remove ${entityString(entity)} with DB ID ${entity.getDbId}${deleteCommand.reportSuffix} properly. The entity or some references will remain in the database.")
            }
          } else {
            throw e
          }
      }
    }
  }

  def mergeImportPolicy(nestedType: NestedType, parentPolicy: IMPORT_POLICY): Unit = {
    if (nestedType != null && nestedType.getImportPolicy == null) nestedType.setImportPolicy(parentPolicy)
  }

}

class LiferayImporterDummyImpl extends LiferayImporter {

  override def doImport(liferayConfig: LiferayConfig, fileDataResolver: FileDataResolver, importerConfig: LiferayImporterConfig): Unit = {}

  override def importFromFile(xmlFile: File, reportFolder: File, fileDataResolver: FileDataResolver, importerConfig: LiferayImporterConfig): Unit = {
    val baseFolder = xmlFile.getParentFile

    Thread.sleep(10000)

    FileUtils.touch(new File(baseFolder, "import-report.html"))

    val importInfo = new CliwixInfoProperties(baseFolder)
    importInfo.setConfigProperty(importerConfig.toString)

    if (Random.nextInt(100) > 50) {
      importInfo.setStateProperty(CliwixInfoProperties.STATE_FAILED)
      importInfo.setErrorMessageProperty(new RuntimeException("Dummy error message"))
    } else {
      importInfo.setStateProperty(CliwixInfoProperties.STATE_SUCCESS)
    }

    importInfo.setDurationProperty(Random.nextInt(1000000))

    importInfo.save()
  }
}

case class DeferredDeleteCommand[T <: LiferayEntity](company: Company, groupId: Long, reportSections: Array[String], reportSuffix: String, command: DeleteCommand[T])