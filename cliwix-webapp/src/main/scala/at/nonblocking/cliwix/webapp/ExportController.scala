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

package at.nonblocking.cliwix.webapp

import java.io.{Serializable, FileInputStream, File}
import javax.inject.Inject
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import at.nonblocking.cliwix.core.util.HostUtil
import at.nonblocking.cliwix.core._
import at.nonblocking.cliwix.model
import com.typesafe.scalalogging.slf4j.LazyLogging
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.util.Zip4jConstants
import org.apache.commons.io.{FileUtils, IOUtils}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import scala.beans.BeanProperty
import scala.collection.mutable

import java.{util=>jutil}

@Controller
@RequestMapping(value = Array("/services/exports"))
class ExportController extends ControllerDefaults with LazyLogging {

  @BeanProperty
  @Inject
  var cliwixCoreHolder: CliwixCoreHolder = _

  @BeanProperty
  @Inject
  var taskExecutor: SingleTaskExecutor = _

  private val DOCUMENT_EXPORT_DIR = "assets"

  private var lastTaskInfo: TaskInfo = _

  @RequestMapping(method = Array(RequestMethod.POST))
  def startExport(request: HttpServletRequest, @RequestBody settings: ExportSettings): ExportResult = {
    checkPermission(request)

    if (this.taskExecutor.isTaskRunning) {
      throw new IllegalStateException(this.webappConfig.getMessage("error.only.one.importexport.at.a.time"))
    }

    val liferayExporter = this.cliwixCoreHolder.getCliwix.getExporter

    val exportId = System.currentTimeMillis().toString
    lastTaskInfo = new TaskInfo(exportId.toLong, getUsername(request), getClient(request), getRemoteIPAddress(request))

    val exportDir = new File(this.webappConfig.getExportDirectory, exportId)

    exportDir.mkdirs()

    logger.info(s"Exporting config to directory: ${exportDir.getAbsolutePath}")

    val includeList = new mutable.MutableList[Class[_]]

    if (settings.exportPortalInstanceConfiguration) {
      includeList += classOf[model.CompanyConfiguration]
      includeList += classOf[model.PortalPreferences]
    }
    if (settings.exportUsers) includeList += classOf[model.User]
    if (settings.exportUserGroups) includeList += classOf[model.UserGroup]
    if (settings.exportRoles) {
      includeList += classOf[model.Role]
      includeList += classOf[model.RegularRoleAssignment]
      includeList += classOf[model.SiteRoleAssignment]
      includeList += classOf[model.OrganizationRoleAssignment]
    }
    if (settings.exportOrganizations) includeList += classOf[model.Organization]

    includeList += classOf[model.Site]
    if (settings.exportSiteConfiguration) includeList += classOf[model.SiteConfiguration]
    if (settings.exportWebContent) includeList += classOf[model.WebContent]
    if (settings.exportDocumentLibrary) includeList += classOf[model.DocumentLibrary]
    if (settings.exportPages) includeList += classOf[model.PageSet]

    val companyFilter =
      if (settings.companyFilter != null && settings.companyFilter.nonEmpty) settings.companyFilter.split(',')
      else null
    val siteFilter =
      if (settings.siteFilter != null && settings.siteFilter.nonEmpty) settings.siteFilter.split(',')
      else null
    val exportOnlyFileDataLastModifiedWithinDays =
      if (settings.exportOnlyFileDataLastModifiedWithinDays > 0) Some(settings.exportOnlyFileDataLastModifiedWithinDays)
      else None

    val config = new LiferayExporterConfig(
      new LiferayEntityFilterInclude(includeList.toList, companyFilter, siteFilter),
      settings.skipCorruptDocuments,
      exportOnlyFileDataLastModifiedWithinDays)

    logger.info(s"Export config: $config")

    this.taskExecutor.execute(lastTaskInfo,
      () => {
        try {
          liferayExporter.exportToFile(config, exportDir, DOCUMENT_EXPORT_DIR, WebappConfig.MAIN_LIFERAY_CONFIG_FILE_NAME)
        } finally {
          enrichInfoProperties(lastTaskInfo, exportDir)
        }
    })

    val result = new ExportResult
    result.setExportId(exportId)
    result
  }

  @RequestMapping(value = Array("/{exportId}/status"), method = Array(RequestMethod.GET))
  def exportStatus(request: HttpServletRequest, @PathVariable("exportId") exportId: String) = {
    checkPermission(request)

    if (exportId == null || !isAllDigits(exportId)) throw new CliwixNotFoundException(s"Export with id $exportId not found!")

    val exportDir = new File(this.webappConfig.getExportDirectory, exportId)
    if (!exportDir.exists()) throw new CliwixNotFoundException(s"Export with id $exportId not found!")


    val exportInfo = new CliwixInfoProperties(exportDir)
    if (exportInfo.exists) {
      new ExportStatus(exportInfo.getStateProperty)
    } else if (this.taskExecutor.isTaskRunning && this.taskExecutor.getCurrentTask.id == exportId.toLong) {
      new ExportStatus(CliwixInfoProperties.STATE_PROCESSING)
    } else {
      new ExportStatus(CliwixInfoProperties.STATE_FAILED)
    }
  }

  @RequestMapping(method = Array(RequestMethod.GET))
  def listExports(request: HttpServletRequest,
                  @RequestParam(value = "start", required =  false) startStr: String,
                  @RequestParam(value = "limit", required =  false) limitStr: String): Exports = {

    checkPermission(request)

    val start = if (startStr == null || !isAllDigits(startStr)) 0 else startStr.toInt
    val limit = if (limitStr == null || !isAllDigits(limitStr)) DEFAULT_LIST_LIMIT else limitStr.toInt
    val end = start + limit

    val exportDirectories = this.webappConfig.getExportDirectory.listFiles()
      .filter(f => f.isDirectory && isAllDigits(f.getName))
      .sortBy(_.getName)
      .reverse

    val exportsTotal = exportDirectories.length

    val exports = exportDirectories
      .slice(start, end)
      .map{ exportDir =>
        val export = new Export
        export.setId(exportDir.getName.toLong)
        export.setReportExists(exportDir.listFiles().exists(f => f.getName.startsWith("export") && f.getName.endsWith(".html")))

        val exportInfo = new CliwixInfoProperties(exportDir)
        if (exportInfo.exists) {
          export.setUser(exportInfo.getProperty(INFO_PROPERTY_USER))
          export.setClient(exportInfo.getProperty(INFO_PROPERTY_CLIENT))
          export.setClientIP(exportInfo.getProperty(INFO_PROPERTY_CLIENT_IP))
          export.setState(exportInfo.getStateProperty)
          export.setErrorMessage(exportInfo.getErrorMessageProperty)
          export.setDurationMs(exportInfo.getDurationProperty)
        } else if (this.taskExecutor.isTaskRunning && this.taskExecutor.getCurrentTask.id == export.getId) {
          export.setState(CliwixInfoProperties.STATE_PROCESSING)
          export.setUser(this.taskExecutor.getCurrentTask.user)
          export.setClient(this.taskExecutor.getCurrentTask.client)
          export.setClientIP(this.taskExecutor.getCurrentTask.clientIP)
          export.setDurationMs(System.currentTimeMillis() - this.taskExecutor.getCurrentTask.startTime)
        } else {
          export.setState(CliwixInfoProperties.STATE_FAILED)
        }

        export
    }

    val result = new Exports
    result.setList(exports)
    result.setTotal(exportsTotal)
    result.setStart(start)
    result
  }

  @RequestMapping(value = Array("/{exportId}/report"), method = Array(RequestMethod.GET),
    produces = Array("text/html; charset=utf-8", "application/json"))
  def report(request: HttpServletRequest, response: HttpServletResponse, @PathVariable("exportId") exportId: String): Unit = {
    checkPermission(request)

    if (!isAllDigits(exportId)) {
      throw new IllegalArgumentException(this.webappConfig.getMessage("error.export.not.found", exportId))
    }

    val exportDir = new File(this.webappConfig.getExportDirectory, exportId)
    if (!exportDir.exists()) {
      throw new CliwixException(this.webappConfig.getMessage("error.export.not.found", exportId))
    }

    val reportFile = exportDir.listFiles().find(f => f.getName.startsWith("export") && f.getName.endsWith(".html"))
    if (reportFile.isEmpty) {
      throw new CliwixNotFoundException(this.webappConfig.getMessage("error.report.file.not.found"))
    }

    val reportFileIS = new FileInputStream(reportFile.get)
    IOUtils.copy(reportFileIS, response.getOutputStream)
    reportFileIS.close()
  }

  @RequestMapping(value = Array("/{exportId}/zip"), method = Array(RequestMethod.GET),
    produces = Array("application/zip", "application/json"))
  def downloadZIP(request: HttpServletRequest, response: HttpServletResponse, @PathVariable("exportId") exportId: String): Unit = {
    checkPermission(request)

    if (!isAllDigits(exportId)) {
      throw new IllegalArgumentException(this.webappConfig.getMessage("error.export.not.found", exportId))
    }

    val exportDir = new File(this.webappConfig.getExportDirectory, exportId)
    if (!exportDir.exists()) {
      throw new CliwixException(this.webappConfig.getMessage("error.export.not.found", exportId))
    }

    val tmpFile = File.createTempFile("cliwix_export", ".zip")
    tmpFile.delete() //Must not exist
    val zipFile = new ZipFile(tmpFile)

    val zipParams = new ZipParameters()
    zipParams.setCompressionMethod(Zip4jConstants.COMP_DEFLATE)
    zipParams.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST)
    zipParams.setIncludeRootFolder(false)

    zipFile.addFolder(exportDir, zipParams)

    val exportTimestamp = new jutil.Date(exportId.toLong)
    val fileName = "export_" + HostUtil.hostName + "_" + Cliwix.formatDate(exportTimestamp).replaceAll(":", "").replaceAll(" ", "-") + ".zip"

    response.setHeader("Content-Disposition", "attachment; filename=" + fileName)
    response.setHeader("Content-Length", tmpFile.length().toString)

    IOUtils.copy(new FileInputStream(tmpFile), response.getOutputStream)
    tmpFile.delete()
  }

  @RequestMapping(value = Array("/{exportId}"), method = Array(RequestMethod.DELETE))
  def delete(request: HttpServletRequest, @PathVariable("exportId") exportId: String): Unit = {
    checkPermission(request)

    if (!isAllDigits(exportId)) {
      throw new IllegalArgumentException(this.webappConfig.getMessage("error.export.not.found", exportId))
    }

    val exportDir = new File(this.webappConfig.getExportDirectory, exportId)
    if (!exportDir.exists()) {
      throw new CliwixException(this.webappConfig.getMessage("error.export.not.found", exportId))
    }

    FileUtils.deleteDirectory(exportDir)
  }

}

class ExportSettings extends Serializable {

  @BeanProperty
  var companyFilter: String = _

  @BeanProperty
  var exportPortalInstanceConfiguration: Boolean = _

  @BeanProperty
  var siteFilter: String = _

  @BeanProperty
  var exportSiteConfiguration: Boolean = _

  @BeanProperty
  var exportUsers: Boolean = _

  @BeanProperty
  var exportUserGroups: Boolean = _

  @BeanProperty
  var exportRoles: Boolean = _

  @BeanProperty
  var exportOrganizations: Boolean = _

  @BeanProperty
  var exportPages: Boolean = _

  @BeanProperty
  var exportWebContent: Boolean = _

  @BeanProperty
  var exportDocumentLibrary: Boolean = _

  @BeanProperty
  var skipCorruptDocuments: Boolean = _

  @BeanProperty
  var exportOnlyFileDataLastModifiedWithinDays: Int = _
}

class ExportStatus(s: String) extends Serializable {

  @BeanProperty
  var status: String = s

}

class ExportResult extends Serializable {

  @BeanProperty
  var exportId: String = _

}

class Exports extends Serializable {

  @BeanProperty
  var total: Int = _

  @BeanProperty
  var start: Int = _

  @BeanProperty
  var list: Array[Export] = _

}

class Export extends Serializable {

  @BeanProperty
  var id: Long = _

  @BeanProperty
  var user: String = _

  @BeanProperty
  var client: String = _

  @BeanProperty
  var clientIP: String = _

  @BeanProperty
  var state: String = _

  @BeanProperty
  var errorMessage: String = _

  @BeanProperty
  var reportExists: Boolean = _

  @BeanProperty
  var durationMs: Long = _

}

