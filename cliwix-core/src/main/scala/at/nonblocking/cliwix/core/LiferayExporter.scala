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

import java.io.{File, FileOutputStream}
import java.{util => jutil}

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.interceptor.ProcessingInterceptorDispatcher
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.core.transaction.CliwixTransaction
import at.nonblocking.cliwix.core.util._
import at.nonblocking.cliwix.model._
import at.nonblocking.cliwix.model.xml.CliwixXmlSerializer
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.io.{FileUtils, IOUtils}

import scala.beans.BeanProperty
import scala.util.Random
import scala.collection.JavaConversions._

sealed trait LiferayExporter {
  def export(exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String): LiferayConfig
  def exportToFile(exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String, xmlFileName: String): Unit
}

private[core] class LiferayExporterImpl extends LiferayExporter with LazyLogging with Reporting with TreeTypeUtils with ListTypeUtils with LiferayCacheUtil {

  val DEFAULT_IMPORT_POLICY = IMPORT_POLICY.UPDATE_INSERT

  @BeanProperty
  var handler: DispatchHandler = _

  @BeanProperty
  var liferayInfo: LiferayInfo = _

  @BeanProperty
  var cliwixTransaction: CliwixTransaction = _

  @BeanProperty
  var processingInterceptor: ProcessingInterceptorDispatcher = _

  @BeanProperty
  var resourceAwareXmlSerializer: ResourceAwareXmlSerializer = _

  val hrDateFormat = new java.text.SimpleDateFormat("yyy-MM-dd HH:mm:ss (zzz)")

  override def exportToFile(exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String, xmlFileName: String): Unit = {

    val timestamp = new jutil.Date()
    report.start(s"export-report.html", "Export Report")
    report.addMessage(s"Cliwix version: ${Cliwix.getVersion}.")
    report.addMessage(s"Liferay version: ${this.liferayInfo.getReleaseInfo}.")
    report.addMessage(s"Host: ${HostUtil.hostName}.")
    report.addMessage(s"Export started at ${hrDateFormat.format(timestamp)}.")
    report.addBreak()

    val exportInfo = new CliwixInfoProperties(baseFolder)
    exportInfo.setConfigProperty(exporterConfig.toString)

    val startTime = System.currentTimeMillis()

    try {
      baseFolder.mkdirs()
      val outputFile = new File(baseFolder, xmlFileName)

      val liferayConfig = export(exporterConfig, baseFolder, exportDocumentsFolderName)

      val out = new FileOutputStream(outputFile)
      this.resourceAwareXmlSerializer.writeXML(liferayConfig, out)
      out.close()

      dumpSchemaFiles(baseFolder)

      exportInfo.setStateProperty(CliwixInfoProperties.STATE_SUCCESS)

    } catch {
      case e: Throwable =>
        report.setBottomSection()
        report.addError("Export failed.", e)
        exportInfo.setStateProperty(CliwixInfoProperties.STATE_FAILED)
        exportInfo.setErrorMessageProperty(e)
        throw e
    } finally {
      report.print(baseFolder)

      exportInfo.setDurationProperty(System.currentTimeMillis() - startTime)
      exportInfo.save()
    }
  }

  private def dumpSchemaFiles(targetDir: File) = {
    val cliwixSchemaFileInput = CliwixXmlSerializer.getSchema(CliwixXmlSerializer.getCliwixSchemaName)
    val xmlSchemaFileInput = CliwixXmlSerializer.getSchema("xml.xsd")

    val cliwixSchemaFileOutput = new FileOutputStream(new File(targetDir, CliwixXmlSerializer.getCliwixSchemaName))
    val xmlSchemaFileOutput = new FileOutputStream(new File(targetDir, "xml.xsd"))

    IOUtils.copy(cliwixSchemaFileInput, cliwixSchemaFileOutput)
    IOUtils.copy(xmlSchemaFileInput, xmlSchemaFileOutput)

    cliwixSchemaFileInput.close()
    xmlSchemaFileInput.close()
    cliwixSchemaFileOutput.close()
    xmlSchemaFileOutput.close()
  }

  override def export(exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String): LiferayConfig = {
    ExecutionContext.initFromExporterConfig(exporterConfig)
    try {
      if (Cliwix.getProperty(Cliwix.PROPERTY_CLEAR_LIFERAY_CACHES_BEFORE_IMPORTEXPORT) == "true") clearLiferayCaches()
      if (Cliwix.getProperty(Cliwix.PROPERTY_DISABLED_LIFERAY_CACHES_DURING_IMPORTEXPORT) == "true") disableLiferayCaching()

      doExport(exporterConfig, baseFolder, exportDocumentsFolderName)

    } finally {
      if (Cliwix.getProperty(Cliwix.PROPERTY_DISABLED_LIFERAY_CACHES_DURING_IMPORTEXPORT) == "true") enableLiferayCaching()

      ExecutionContext.destroy()
    }
  }

  private def doExport(exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String): LiferayConfig = {
    assert(baseFolder != null, "baseFolder != null")

    val startTime = System.currentTimeMillis()

    baseFolder.mkdirs()

    val liferayConfig = new LiferayConfig
    liferayConfig.setSourceVersion(this.liferayInfo.getVersion)

    val companies = addCompanies(liferayConfig, exporterConfig)
    safeForeach(companies)(addCompanyData(_, exporterConfig, baseFolder, exportDocumentsFolderName))

    val totalTime = (System.currentTimeMillis() - startTime) / 1000
    val totalMemory = Runtime.getRuntime.totalMemory() / (1024 * 1024)

    report.setBottomSection()
    report.addMessage(s"Export finished in $totalTime seconds.")
    report.addMessage(s"Total memory usage: $totalMemory MB.")
    report.addSuccess("Success.")

    liferayConfig
  }

  private def addCompanies(liferayConfig: LiferayConfig, exporterConfig: LiferayExporterConfig) = {
    val withCompanyConfiguration = exporterConfig.filter.exportEntitiesOf(classOf[CompanyConfiguration])
    val companyList = this.handler
      .execute(CompanyListCommand(withCompanyConfiguration)).result
      .filter(c => exporterConfig.filter.exportEntityInstance(c._2))

    val companies = new Companies(MapValuesListWrapper(companyList))
    companies.setImportPolicy(DEFAULT_IMPORT_POLICY)
    liferayConfig.setCompanies(companies)
    report.addMessage(s"${companyList.size} Companies found.")

    safeForeach(companies)(c => this.processingInterceptor.afterEntityExport(c, c.getCompanyId))
    this.processingInterceptor.afterListExport(companies, -1)
    companies
  }

  private def addCompanyData(company: Company, exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String) = {
    report.setSection(s"Company ${company.identifiedBy}")

    ExecutionContext.updateCompanyContext(company)

    if (exporterConfig.filter.exportEntitiesOf(classOf[PortalPreferences])) addPortalPreferences(company)
    if (exporterConfig.filter.exportEntitiesOf(classOf[User])) addUsers(company)
    if (exporterConfig.filter.exportEntitiesOf(classOf[UserGroup])) addUserGroups(company)
    if (exporterConfig.filter.exportEntitiesOf(classOf[Role])) addRoles(company)
    if (exporterConfig.filter.exportEntitiesOf(classOf[Organization])) addOrganizations(company, exporterConfig)
    if (exporterConfig.filter.exportEntitiesOf(classOf[RegularRoleAssignment])) addRegularRoleAssignments(company)

    if (exporterConfig.filter.exportEntitiesOf(classOf[Site]))
      addSites(company, exporterConfig, baseFolder, exportDocumentsFolderName)
  }

  private def addRoles(company: Company) = {
    val roleMap = this.handler.execute(RoleListCommand(company.getCompanyId)).result
    val roles = new Roles(MapValuesListWrapper(roleMap))
    company.setRoles(roles)

    safeForeach(roles) { r =>
      val permissionsMap = this.handler.execute(RolePermissionListCommand(company.getCompanyId, r, filterPermissionsWithNoAction = true)).result
      val permissions = new RolePermissions(MapValuesListWrapper(permissionsMap))
      r.setPermissions(permissions)

      safeForeach(permissions)(p => this.processingInterceptor.afterEntityExport(p, company.getCompanyId))
      this.processingInterceptor.afterListExport(permissions, company.getCompanyId)
      this.processingInterceptor.afterEntityExport(r, company.getCompanyId)
    }
    this.processingInterceptor.afterListExport(roles, company.getCompanyId)
    report.addMessage(s"${roleMap.size} roles exported.")
  }

  private def addUserGroups(company: Company) = {
    val userGroupMap = this.handler.execute(UserGroupListCommand(company.getCompanyId)).result
    val userGroups = new UserGroups(MapValuesListWrapper(userGroupMap))
    company.setUserGroups(userGroups)

    safeForeach(userGroups)(g => this.processingInterceptor.afterEntityExport(g, company.getCompanyId))
    this.processingInterceptor.afterListExport(userGroups, company.getCompanyId)
    report.addMessage(s"${userGroupMap.size} user groups exported.")
  }

  private def addRegularRoleAssignments(company: Company) = {
    val roleAssignmentsMap = this.handler.execute(RegularRoleAssignmentListCommand(company.getCompanyId)).result
    val roleAssignments = new RegularRoleAssignments(MapValuesListWrapper(roleAssignmentsMap))
    company.setRegularRoleAssignments(roleAssignments)

    safeForeach(roleAssignments)(r => this.processingInterceptor.afterEntityExport(r, company.getCompanyId))
    this.processingInterceptor.afterListExport(roleAssignments, company.getCompanyId)
    report.addMessage(s"${roleAssignmentsMap.size} regular roles assignments exported.")
  }

  private def addOrganizations(company: Company, exporterConfig: LiferayExporterConfig) = {
    val organizationList = this.handler.execute(OrganizationListCommand(company.getCompanyId)).result
    val organizations = new Organizations(organizationList)
    company.setOrganizations(organizations)

    var numOrgs = 0
    safeProcessRecursively(organizations){ o =>
      this.processingInterceptor.afterEntityExport(o, company.getCompanyId)
      numOrgs = numOrgs + 1
    }
    this.processingInterceptor.afterTreeExport(organizations, company.getCompanyId)
    report.addMessage(s"$numOrgs organizations exported.")

    if (exporterConfig.filter.exportEntitiesOf(classOf[OrganizationRoleAssignment])) {
      safeProcessRecursively(organizations) { o =>
        addOrganizationRoleAssignments(o)
      }
    }
  }

  private def addOrganizationRoleAssignments(org: Organization) = {
    val roleAssignmentsMap = this.handler.execute(OrganizationRoleAssignmentListCommand(org.getOrganizationId)).result
    val roleAssignments = new OrganizationRoleAssignments(MapValuesListWrapper(roleAssignmentsMap))
    org.setOrganizationRoleAssignments(roleAssignments)

    safeForeach(roleAssignments)(r => this.processingInterceptor.afterEntityExport(r, org.getOwnerCompanyId))
    this.processingInterceptor.afterListExport(roleAssignments, org.getOwnerCompanyId)
    report.addMessage(s"${roleAssignmentsMap.size} role assignments exported for organization: ${org.getName}.")
  }

  private def addUsers(company: Company) = {
    val userMap = this.handler.execute(UserListCommand(company.getCompanyId)).result
    val users = new Users(MapValuesListWrapper(userMap))
    company.setUsers(users)

    safeForeach(users)(u => this.processingInterceptor.afterEntityExport(u, company.getCompanyId))
    this.processingInterceptor.afterListExport(users, company.getCompanyId)
    report.addMessage(s"${userMap.size} users exported.")
  }

  private def addPortalPreferences(company: Company) = {
    val prefs = this.handler.execute(PortalPreferencesReadCommand(company.getCompanyId)).result
    company.setPortalPreferences(prefs)
    report.addMessage(s"Portal preferences exported.")
    this.processingInterceptor.afterEntityExport(prefs, company.getCompanyId)
  }

  private def addSites(company: Company, exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String) = {
    val withSiteConfiguration = exporterConfig.filter.exportEntitiesOf(classOf[SiteConfiguration])
    val siteMap = this.handler
      .execute(SiteListCommand(company.getCompanyId, withSiteConfiguration)).result
      .filter(s => exporterConfig.filter.exportEntityInstance(s._2))

    if (siteMap.nonEmpty) {
      val sites = new Sites(MapValuesListWrapper(siteMap))
      company.setSites(sites)

      safeForeach(sites) { site =>
        report.setSubSection(s"Site ${site.identifiedBy}")

        ExecutionContext.updateGroupContext(site.getSiteId)

        this.processingInterceptor.afterEntityExport(site, company.getCompanyId)

        if (exporterConfig.filter.exportEntitiesOf(classOf[SiteRoleAssignment])) addSiteRoleAssignments(site)

        addSiteContent(company, site, exporterConfig, baseFolder, exportDocumentsFolderName)

        if (exporterConfig.filter.exportEntitiesOf(classOf[PageSet])) addPublicPages(company, site)
        if (exporterConfig.filter.exportEntitiesOf(classOf[PageSet])) addPrivatePages(company, site)
      }
    }
  }

  private def addSiteRoleAssignments(site: Site) = {
    val roleAssignmentsMap = this.handler.execute(SiteRoleAssignmentListCommand(site.getSiteId)).result
    val roleAssignments = new SiteRoleAssignments(MapValuesListWrapper(roleAssignmentsMap))
    site.setSiteRoleAssignments(roleAssignments)

    safeForeach(roleAssignments)(r => this.processingInterceptor.afterEntityExport(r, site.getOwnerCompanyId))
    this.processingInterceptor.afterListExport(roleAssignments, site.getOwnerCompanyId)
    report.addMessage(s"${roleAssignmentsMap.size} site assignments exported for site: ${site.getName}.")
  }

  private def addPublicPages(company: Company, site: Site) = {
    val pageSet = this.handler.execute(PageSetReadCommand(site.getSiteId, privatePages = false)).result
    if (pageSet != null) {
      val pageList = this.handler.execute(PageListCommand(site.getSiteId, privatePages = false)).result
      val pages = new Pages(pageList)
      pageSet.setPages(pages)
      site.setPublicPages(pageSet)

      var numPages = 0

      safeProcessRecursively(pages) { page =>
        addPagePortletsAndPermissions(company, page)
        numPages = numPages + 1
      }

      safeProcessRecursively(pages)(p => this.processingInterceptor.afterEntityExport(p, company.getCompanyId))
      this.processingInterceptor.afterTreeExport(pages, company.getCompanyId)
      report.addMessage(s"$numPages public pages exported.")
    }
  }

  private def addPrivatePages(company: Company, site: Site) = {
    val pageSet = this.handler.execute(PageSetReadCommand(site.getSiteId, privatePages = true)).result
    if (pageSet != null) {
      val pageList = this.handler.execute(PageListCommand(site.getSiteId, privatePages = true)).result
      val pages = new Pages(pageList)
      pageSet.setPages(pages)
      site.setPrivatePages(pageSet)

      var numPages = 0

      safeProcessRecursively(pages) { page =>
        addPagePortletsAndPermissions(company, page)
        numPages = numPages + 1
      }

      safeProcessRecursively(pages)(p => this.processingInterceptor.afterEntityExport(p, company.getCompanyId))
      this.processingInterceptor.afterTreeExport(pages, company.getCompanyId)
      report.addMessage(s"$numPages private pages exported.")
    }
  }

  private def addPagePortletsAndPermissions(company: Company, page: Page) = {
    val pagePermissionMap = this.handler.execute(PagePermissionListCommand(company.getCompanyId, page, filterPermissionsWithNoAction = true)).result
    page.setPermissions(new ResourcePermissions(MapValuesListWrapper(pagePermissionMap)))

    val portletConfigurationsMap = this.handler.execute(PortletConfigurationListCommand(page.getPortletLayoutId)).result
    val portletConfigurations = new PortletConfigurations(MapValuesListWrapper(portletConfigurationsMap))
    page.setPortletConfigurations(portletConfigurations)

    safeForeach(portletConfigurations) { portletConfiguration =>
      val portletPermissionMap = this.handler.execute(PortletPermissionListCommand(company.getCompanyId, page, portletConfiguration, filterPermissionsWithNoAction = true)).result
      portletConfiguration.setPermissions(new ResourcePermissions(MapValuesListWrapper(portletPermissionMap)))
    }

    safeForeach(page.getPortletConfigurations)(c => this.processingInterceptor.afterEntityExport(c, company.getCompanyId))
    this.processingInterceptor.afterListExport(portletConfigurations, company.getCompanyId)
  }

  private def addSiteContent(company: Company, site: Site, exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String) = {
    val siteContent = new SiteContent
    site.setSiteContent(siteContent)

    if (exporterConfig.filter.exportEntitiesOf(classOf[WebContent])) addWebContent(company, site, siteContent, exporterConfig)
    if (exporterConfig.filter.exportEntitiesOf(classOf[DocumentLibrary]) && exportDocumentsFolderName != null)
      addSiteDocumentLibrary(company, site, siteContent, exporterConfig, baseFolder, exportDocumentsFolderName)
  }

  private def addWebContent(company: Company, site: Site, siteContent: SiteContent, exporterConfig: LiferayExporterConfig) = {
    val webContent = new WebContent
    siteContent.setWebContent(webContent)

    addArticleStructures(company, webContent, site)
    addArticleTemplates(company, webContent, site)
    addArticles(company, webContent, site)
  }

  private def addArticleStructures(company: Company, webContent: WebContent, site: Site) = {
    val articleStructures = this.handler.execute(ArticleStructureListCommand(site.getSiteId)).result
    val cliwixArticleStructures = new ArticleStructures(articleStructures)
    webContent.setStructures(cliwixArticleStructures)

    var numStructures = 0
    safeProcessRecursively(cliwixArticleStructures){ structure =>
      val articleStructurePermissionMap = this.handler.execute(ArticleStructurePermissionListCommand(company.getCompanyId, structure, filterPermissionsWithNoAction = true)).result
      structure.setPermissions(new ResourcePermissions(MapValuesListWrapper(articleStructurePermissionMap)))

      this.processingInterceptor.afterEntityExport(structure, company.getCompanyId)
      numStructures = numStructures + 1
    }
    this.processingInterceptor.afterTreeExport(cliwixArticleStructures, company.getCompanyId)
    report.addMessage(s"$numStructures article structures exported.")
  }

  private def addArticleTemplates(company: Company, webContent: WebContent, site: Site) = {
    val articleTemplatesMap = this.handler.execute(ArticleTemplateListCommand(site.getSiteId)).result
    val articleTemplates = new ArticleTemplates(MapValuesListWrapper(articleTemplatesMap))
    webContent.setTemplates(articleTemplates)

    safeForeach(articleTemplates) { template =>
      val articleTemplatePermissionMap = this.handler.execute(ArticleTemplatePermissionListCommand(company.getCompanyId, template, filterPermissionsWithNoAction = true)).result
      template.setPermissions(new ResourcePermissions(MapValuesListWrapper(articleTemplatePermissionMap)))
      this.processingInterceptor.afterEntityExport(template, company.getCompanyId)
    }

    this.processingInterceptor.afterListExport(articleTemplates, company.getCompanyId)
    report.addMessage(s"${articleTemplates.getTemplates.size} article templates exported.")
  }

  private def addArticles(company: Company, webContent: WebContent, site: Site) = {
    val articleMap = this.handler.execute(ArticleListCommand(site.getSiteId)).result
    val articles = new Articles(MapValuesListWrapper(articleMap))
    webContent.setArticles(articles)

    safeForeach(articles) { article =>
      val articlePermissionMap = this.handler.execute(ArticlePermissionListCommand(company.getCompanyId, article, filterPermissionsWithNoAction = true)).result
      article.setPermissions(new ResourcePermissions(MapValuesListWrapper(articlePermissionMap)))
      this.processingInterceptor.afterEntityExport(article, company.getCompanyId)
    }

    this.processingInterceptor.afterListExport(articles, company.getCompanyId)
    report.addMessage(s"${articles.getArticles.size} articles exported.")
  }

  private def addSiteDocumentLibrary(company: Company, site: Site, siteContent: SiteContent, exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String) = {
    assert(exportDocumentsFolderName != null)

    val companyExportFolderName = exportDocumentsFolderName + "/Company_" + company.getWebId + "/Site_" + site.getName
    val companyExportFolder = new File(baseFolder, companyExportFolderName)
    val dlItemList = this.handler.execute(DocumentLibraryItemListCommand(site.getSiteId, companyExportFolder, exporterConfig.exportOnlyFileDataLastModifiedWithinDays)).result

    val documentLibrary = new DocumentLibrary(companyExportFolderName, dlItemList)
    siteContent.setDocumentLibrary(documentLibrary)

    val documentLibraryPermissionMap = this.handler.execute(DocumentLibraryPermissionListCommand(company.getCompanyId, site.getSiteId, filterPermissionsWithNoAction = true)).result
    documentLibrary.setPermissions(new ResourcePermissions(MapValuesListWrapper(documentLibraryPermissionMap)))

    var numFolders = 1 //1 -> Root folder
    var numFiles = 0

    safeProcessRecursively(documentLibrary) {
      case folder: DocumentLibraryFolder =>
        val folderPermissionMap = this.handler.execute(DocumentLibraryFolderPermissionListCommand(company.getCompanyId, folder, filterPermissionsWithNoAction = true)).result
        folder.setPermissions(new ResourcePermissions(MapValuesListWrapper(folderPermissionMap)))

        numFolders = numFolders + 1

      case file: DocumentLibraryFile =>
        val filePermissionMap = this.handler.execute(DocumentLibraryFilePermissionListCommand(company.getCompanyId, file, filterPermissionsWithNoAction = true)).result
        file.setPermissions(new ResourcePermissions(MapValuesListWrapper(filePermissionMap)))

        numFiles = numFiles + 1
    }

    safeProcessRecursively(documentLibrary)(p => this.processingInterceptor.afterEntityExport(p, company.getCompanyId))
    this.processingInterceptor.afterTreeExport(documentLibrary, company.getCompanyId)
    report.addMessage(s"$numFiles files in $numFolders folders exported.")
  }
}

class LiferayExporterDummyImpl extends LiferayExporter {

  override def export(exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String): LiferayConfig = new LiferayConfig

  override def exportToFile(exporterConfig: LiferayExporterConfig, baseFolder: File, exportDocumentsFolderName: String, xmlFileName: String): Unit = {

    Thread.sleep(10000)

    FileUtils.touch(new File(baseFolder, "export-report.html"))

    val exportInfo = new CliwixInfoProperties(baseFolder)
    exportInfo.setConfigProperty(exporterConfig.toString)

    if (Random.nextInt(100) > 50) {
      exportInfo.setStateProperty(CliwixInfoProperties.STATE_FAILED)
      exportInfo.setErrorMessageProperty(new RuntimeException("Dummy error message"))
    } else {
      exportInfo.setStateProperty(CliwixInfoProperties.STATE_SUCCESS)
    }

    exportInfo.setDurationProperty(Random.nextInt(1000000))

    exportInfo.save()
  }
}
