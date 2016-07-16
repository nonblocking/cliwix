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

import at.nonblocking.cliwix.core.filedata.FileDataResolver
import at.nonblocking.cliwix.model.Company
import com.liferay.portal.model.User
import com.liferay.portal.service.ServiceContext

case class ExecutionContextSecurityContext(defaultUser: User, adminUser: User)

trait ExecutionContextSecurityHandler {
  def updateSecurityContext(company: Company, failWhenNoAdminUserFound: Boolean): ExecutionContextSecurityContext
}

case class ExecutionContextFlags (
  //Export
  skipCorruptDocuments: Boolean = false,

  //Import
  ignoreNonExistingRoles: Boolean = false,
  ignoreNonExistingUserGroups: Boolean = false,
  ignoreNonExistingOrganizations: Boolean = false,
  ignoreNonExistingUsers: Boolean = false,
  ignoreNonExistingCountries: Boolean = false,
  ignoreNonExistingRegions: Boolean = false,
  ignoreNonExistingResourceActions: Boolean = false,
  ignoreInvalidDocumentReferences: Boolean = false,
  ignoreDeletionFailures: Boolean = false
)

object ExecutionContext {

  private var _executionContextSecurityHandler: ExecutionContextSecurityHandler = _
  private var _securityContext: ExecutionContextSecurityContext = _
  private var _flags: ExecutionContextFlags = _
  private var _fileDataResolver: FileDataResolver = _
  private var _groupId: Long = _
  private var _company: Company = _

  private var _initialized = false

  def init(fileDataResolver: FileDataResolver = null, flags: ExecutionContextFlags = ExecutionContextFlags()) = {
    if (_initialized) {
      throw new CliwixException("Cliwix import/export is already running!")
    }

    this._fileDataResolver = fileDataResolver
    this._flags = flags

    this._initialized = true
  }

  def initFromExporterConfig(exporterConfig: LiferayExporterConfig) = {
    init(flags = ExecutionContextFlags(skipCorruptDocuments = exporterConfig.skipCorruptDocuments))
  }

  def initFromImporterConfig(importerConfig: LiferayImporterConfig, fileDataResolver: FileDataResolver = null) = {
    init(fileDataResolver,
      ExecutionContextFlags(
        ignoreNonExistingRoles = importerConfig.ignoreNonExistingRoles,
        ignoreNonExistingUserGroups = importerConfig.ignoreNonExistingUserGroups,
        ignoreNonExistingOrganizations = importerConfig.ignoreNonExistingOrganizations,
        ignoreNonExistingUsers = importerConfig.ignoreNonExistingUsers,
        ignoreNonExistingResourceActions = importerConfig.ignoreNonExistingResourceActions,
        ignoreNonExistingCountries = importerConfig.ignoreNonExistingCountries,
        ignoreNonExistingRegions = importerConfig.ignoreNonExistingRegions,
        ignoreInvalidDocumentReferences = importerConfig.ignoreInvalidDocumentReferences,
        ignoreDeletionFailures = importerConfig.ignoreDeletionFailures)
    )
  }

  def securityContext = this._securityContext
  def flags = this._flags

  def currentCompany = _company
  def currentGroupId = _groupId

  def fileDataResolver = _fileDataResolver

  def executionContextSecurityHandler_=(executionContextSecurityHandler: ExecutionContextSecurityHandler): Unit = {
    this._executionContextSecurityHandler = executionContextSecurityHandler
  }

  def updateCompanyContext(company: Company) = {
    this._company = company
    this._groupId = -1

    updateSecurityContext()
  }

  def updateSecurityContext(failWhenNoAdminUserFound: Boolean = true) = {
    this._securityContext = this._executionContextSecurityHandler.updateSecurityContext(_company, failWhenNoAdminUserFound)
  }

  def updateGroupContext(groupId: Long) {
    _groupId = groupId
  }

  def createServiceContext(): ServiceContext = {
    val serviceContext = new ServiceContext
    serviceContext.setCompanyId(_company.getCompanyId)
    serviceContext.setScopeGroupId(_groupId)
    serviceContext.setUserId(if (this._securityContext.defaultUser != null) this._securityContext.defaultUser.getUserId else 0)
    serviceContext
  }

  def destroy() = {
    this._securityContext = null
    this._flags = null
    this._company = null
    this._groupId = -1
    this._fileDataResolver = null
    this._initialized = false
  }
}

class ExecutionContextDependencySetter {
  def setExecutionContextSecurityHandler(executionContextSecurityHandler: ExecutionContextSecurityHandler): Unit = {
    ExecutionContext.executionContextSecurityHandler_=(executionContextSecurityHandler)
  }
}