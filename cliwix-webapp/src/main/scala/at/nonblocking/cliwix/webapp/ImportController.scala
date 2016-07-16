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

import java.io.{Serializable, File, FileInputStream}
import javax.inject.Inject
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import at.nonblocking.cliwix.core.filedata.FileDataResolverFileSystemImpl
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.core.{CliwixException, CliwixInfoProperties, LiferayImporterConfig}
import at.nonblocking.cliwix.model.IMPORT_POLICY
import com.typesafe.scalalogging.slf4j.LazyLogging
import net.lingala.zip4j.core.ZipFile
import org.apache.commons.io.{FileUtils, IOUtils}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import scala.beans.BeanProperty


@Controller
@RequestMapping(value = Array("/services/imports"))
class ImportController extends ControllerDefaults with LazyLogging {

  val MODE_UPLOAD = "upload"
  val MODE_DIRECTORY = "directory"

  @BeanProperty
  @Inject
  var cliwixCoreHolder: CliwixCoreHolder = _

  @BeanProperty
  @Inject
  var taskExecutor: SingleTaskExecutor = _

  private var lastTaskInfo: TaskInfo = _

  @RequestMapping(method = Array(RequestMethod.POST))
  def startImport(request: HttpServletRequest, @RequestBody settings: ImportSettings): ImportResult = {
    checkPermission(request)

    if (this.taskExecutor.isTaskRunning) throw new IllegalStateException(this.webappConfig.getMessage("error.only.one.importexport.at.a.time"))

    val liferayImport = this.cliwixCoreHolder.getCliwix.getImporter

    val importId = System.currentTimeMillis().toString
    lastTaskInfo = new TaskInfo(importId.toLong, getUsername(request), getClient(request), getRemoteIPAddress(request))

    val importDir = new File(this.webappConfig.getImportDirectory, importId)
    if (importDir.exists()) FileUtils.forceDelete(importDir)
    val tmpImportDir = new File(this.webappConfig.getImportDirectory, "_tmp_" + importId)
    tmpImportDir.mkdirs()
    logger.info(s"Import workspace: ${importDir.getAbsolutePath}")

    try {
      if (settings.getMode == MODE_UPLOAD) {
        copyUploadToImportDir(settings, tmpImportDir)
      } else {
        copyServerDirectoryContentToImportDir(settings, tmpImportDir)
      }

      removeInfoFileIfExists(tmpImportDir)
      FileUtils.moveDirectory(tmpImportDir, importDir)

      val xmlFile = new File(importDir, WebappConfig.MAIN_LIFERAY_CONFIG_FILE_NAME)
      if (!xmlFile.exists()) {
        throw new CliwixValidationException(this.webappConfig.getMessage("error.import.no.liferay.config.xml.found"))
      }

      val fileDataResolver = new FileDataResolverFileSystemImpl(importDir)

      val importConfig = new LiferayImporterConfig(
        atomicTransaction = settings.atomicTransaction,
        simulationMode = settings.simulationMode,
        overrideRootImportPolicy = settings.overrideRootImportPolicy,
        ignoreNonExistingUserGroups = settings.ignoreNonExistingUsersGroupsOrgs,
        ignoreNonExistingOrganizations = settings.ignoreNonExistingUsersGroupsOrgs,
        ignoreNonExistingUsers = settings.ignoreNonExistingUsersGroupsOrgs,
        ignoreNonExistingResourceActions = settings.ignoreNonExistingResourceActions,
        ignoreInvalidDocumentReferences = settings.ignoreInvalidDocumentReferences,
        ignoreDeletionFailures = settings.ignoreDeletionFailures)

      this.taskExecutor.execute(lastTaskInfo,
        () => {
          try {
            liferayImport.importFromFile(xmlFile, importDir, fileDataResolver, importConfig)
          } finally {
            enrichInfoProperties(lastTaskInfo, importDir)
          }
        })

    } catch {
      case e: Throwable =>
        logger.info("Upload error: Removing import dir: {}", importDir.getAbsolutePath)
        FileUtils.deleteDirectory(importDir)
        throw e
    }

    val result = new ImportResult
    result.setImportId(importId)
    result
  }

  private def removeInfoFileIfExists(importDir: File) = {
    val infoFile = new File(importDir, CliwixInfoProperties.FILE_NAME)
    if (infoFile.exists()) {
      infoFile.delete()
    }
  }

  @RequestMapping(value = Array("/{importId}/status"), method = Array(RequestMethod.GET))
  def importStatus(request: HttpServletRequest, @PathVariable("importId") importId: String) = {
    checkPermission(request)

    if (importId == null || !isAllDigits(importId)) throw new CliwixNotFoundException(s"Import with id $importId not found!")

    val importDir = new File(this.webappConfig.getImportDirectory, importId)
    if (!importDir.exists()) throw new CliwixNotFoundException(s"Import with id $importId not found!")

    val importInfo = new CliwixInfoProperties(importDir)
    if (importInfo.exists) {
      new ImportStatus(importInfo.getStateProperty)
    } else if (this.taskExecutor.isTaskRunning && this.taskExecutor.getCurrentTask.id == importId.toLong) {
      new ImportStatus(CliwixInfoProperties.STATE_PROCESSING)
    } else {
      new ImportStatus(CliwixInfoProperties.STATE_FAILED)
    }
  }

  @RequestMapping(method = Array(RequestMethod.GET))
  def listImports(request: HttpServletRequest,
                  @RequestParam(value = "start", required = false) startStr: String,
                  @RequestParam(value = "limit", required = false) limitStr: String): Imports = {

    checkPermission(request)

    val start = if (startStr == null || !isAllDigits(startStr)) 0 else startStr.toInt
    val limit = if (limitStr == null || !isAllDigits(limitStr)) DEFAULT_LIST_LIMIT else limitStr.toInt
    val end = start + limit

    val importDirectories = this.webappConfig.getImportDirectory.listFiles()
      .filter(f => f.isDirectory && isAllDigits(f.getName))
      .sortBy(_.getName)
      .reverse

    val importsTotal = importDirectories.length

    val imports = importDirectories
      .slice(start, end)
      .map { importDir =>
        val imp = new Import
        imp.setId(importDir.getName.toLong)
        imp.setReportExists(importDir.listFiles().exists(f => f.getName.startsWith("import") && f.getName.endsWith(".html")))

        val importInfo = new CliwixInfoProperties(importDir)
        if (importInfo.exists) {
          imp.setUser(importInfo.getProperty(INFO_PROPERTY_USER))
          imp.setClient(importInfo.getProperty(INFO_PROPERTY_CLIENT))
          imp.setClientIP(importInfo.getProperty(INFO_PROPERTY_CLIENT_IP))
          imp.setState(importInfo.getStateProperty)
          imp.setErrorMessage(importInfo.getErrorMessageProperty)
          imp.setDurationMs(importInfo.getDurationProperty)
        } else if (this.taskExecutor.isTaskRunning && this.taskExecutor.getCurrentTask.id == imp.getId) {
          imp.setState(CliwixInfoProperties.STATE_PROCESSING)
          imp.setUser(this.taskExecutor.getCurrentTask.user)
          imp.setClient(this.taskExecutor.getCurrentTask.client)
          imp.setClientIP(this.taskExecutor.getCurrentTask.clientIP)
          imp.setDurationMs(System.currentTimeMillis() - this.taskExecutor.getCurrentTask.startTime)
        } else {
          imp.setState(CliwixInfoProperties.STATE_FAILED)
        }

        imp
    }

    val result = new Imports
    result.setList(imports)
    result.setTotal(importsTotal)
    result.setStart(start)
    result
  }

  @RequestMapping(value = Array("/{importId}/report"), method = Array(RequestMethod.GET),
    produces = Array("text/html; charset=utf-8", "application/json"))
  def report(request: HttpServletRequest, response: HttpServletResponse, @PathVariable("importId") importId: String): Unit = {
    checkPermission(request)

    if (!isAllDigits(importId)) {
      throw new IllegalArgumentException(this.webappConfig.getMessage("error.import.not.found", importId))
    }

    val importDir = new File(this.webappConfig.getImportDirectory, importId)
    if (!importDir.exists()) {
      throw new CliwixException(this.webappConfig.getMessage("error.import.not.found", importId))
    }

    val reportFile = importDir.listFiles().find(f => f.getName.startsWith("import") && f.getName.endsWith(".html"))
    if (reportFile.isEmpty) {
      throw new CliwixNotFoundException(this.webappConfig.getMessage("error.report.file.not.found"))
    }

    val reportFileIS = new FileInputStream(reportFile.get)
    IOUtils.copy(reportFileIS, response.getOutputStream)
    reportFileIS.close()
  }

  @RequestMapping(value = Array("/{importId}"), method = Array(RequestMethod.DELETE))
  def delete(request: HttpServletRequest, @PathVariable("importId") importId: String): Unit = {
    checkPermission(request)

    if (!isAllDigits(importId)) {
      throw new IllegalArgumentException(this.webappConfig.getMessage("error.import.not.found", importId))
    }

    val importDir = new File(this.webappConfig.getImportDirectory, importId)
    if (!importDir.exists()) {
      throw new CliwixException(this.webappConfig.getMessage("error.import.not.found", importId))
    }

    FileUtils.deleteDirectory(importDir)
  }

  private def copyUploadToImportDir(settings: ImportSettings, importDir: File) = {
    if (settings.getTmpFileName == null || settings.getTmpFileName.isEmpty) {
      throw new CliwixValidationException(this.webappConfig.getMessage("error.import.file.upload.failed"))
    }

    val tmpFile = new File(this.webappConfig.getTmpDirectory, settings.getTmpFileName)
    if (!tmpFile.exists()) {
      throw new CliwixValidationException(this.webappConfig.getMessage("error.import.file.upload.failed"))
    }

    if (tmpFile.getName.toLowerCase.endsWith(".zip")) {
      val zipFile = new ZipFile(tmpFile)
      zipFile.extractAll(importDir.getAbsolutePath)
      FileUtils.deleteQuietly(tmpFile)
    } else if (tmpFile.getName.toLowerCase.endsWith(".xml")) {
      FileUtils.moveFile(tmpFile, new File(importDir, WebappConfig.MAIN_LIFERAY_CONFIG_FILE_NAME))
    }
  }

  private def copyServerDirectoryContentToImportDir(settings: ImportSettings, importDir: File) = {
    if (settings.getDirectory == null || settings.getDirectory.isEmpty) {
      throw new CliwixValidationException(this.webappConfig.getMessage("error.import.no.valid.server.directory", settings.getDirectory))
    }

    val dir = new File(settings.getDirectory)
    if (!dir.exists()) {
      throw new CliwixValidationException(this.webappConfig.getMessage("error.import.no.valid.server.directory", settings.getDirectory))
    }

    FileUtils.copyDirectory(dir, importDir)
  }

}

class ImportSettings extends Serializable {

  @BeanProperty
  var mode: String = _

  @BeanProperty
  var directory: String = _

  @BeanProperty
  var tmpFileName: String = _

  @BeanProperty
  var atomicTransaction: Boolean = _

  @BeanProperty
  var simulationMode: Boolean = _

  @BeanProperty
  var overrideRootImportPolicy: IMPORT_POLICY = _

  @BeanProperty
  var ignoreNonExistingUsersGroupsOrgs: Boolean = _

  @BeanProperty
  var ignoreNonExistingResourceActions: Boolean = _

  @BeanProperty
  var ignoreInvalidDocumentReferences: Boolean = _

  @BeanProperty
  var ignoreDeletionFailures: Boolean = _

}

class ImportStatus(s: String) extends Serializable {

  @BeanProperty
  var status: String = s

}

class ImportResult extends Serializable {

  @BeanProperty
  var importId: String = _

}

class Imports extends Serializable {

  @BeanProperty
  var total: Int = _

  @BeanProperty
  var start: Int = _

  @BeanProperty
  var list: Array[Import] = _

}

class Import extends Serializable {

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

