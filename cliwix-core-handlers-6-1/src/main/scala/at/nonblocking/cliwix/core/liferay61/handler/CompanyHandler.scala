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

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.core.liferay61.util.{NativeSqlAccessUtil, PortalInstanceUtil}
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.model._
import com.liferay.portal.NoSuchCompanyException
import com.liferay.portal.kernel.events.SimpleAction
import com.liferay.portal.kernel.util.{InstancePool, PropsKeys, PropsUtil}
import com.liferay.portal.service.{AccountLocalService, CompanyLocalService, UserLocalService, VirtualHostLocalService}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import java.{util => jutil}

class CompanyListHandler extends Handler[CompanyListCommand, jutil.Map[String, Company]] {

  @BeanProperty
  var companyService: CompanyLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: CompanyListCommand): CommandResult[jutil.Map[String, Company]] = {
    val companies = this.companyService.getCompanies

    val cliwixCompanyMap = companies.map { c =>
      logger.debug("Export company with id={}", c.getCompanyId.toString)
      val company = this.converter.convertToCliwixCompany(c, command.withConfiguration)
      (company.identifiedBy, company)
    }

    CommandResult(new jutil.HashMap(cliwixCompanyMap.toMap))
  }
}

class CompanyInsertHandler extends Handler[CompanyInsertCommand, Company] {

  @BeanProperty
  var companyService: CompanyLocalService = _

  @BeanProperty
  var companyDefaultDataAction: SimpleAction = _

  @BeanProperty
  var accountService: AccountLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var portalInstanceUtil : PortalInstanceUtil = _

  val defaultWebId = PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID)

  private[core] override def handle(command: CompanyInsertCommand): CommandResult[Company] = {
    val cliwixCompany = command.company

    if (cliwixCompany.getCompanyConfiguration == null) throw new CliwixValidationException(s"No CompanyConfiguration provided for new company: ${cliwixCompany.getWebId}")
    if (cliwixCompany.getWebId == defaultWebId) throw new CliwixValidationException("Cannot insert default company, it must exist. Is the portal not initialized yet?")
    logger.debug("Adding company: {}", cliwixCompany)

    val liferayCompany = this.companyService.addCompany(
      cliwixCompany.getWebId, cliwixCompany.getCompanyConfiguration.getVirtualHost,
      cliwixCompany.getCompanyConfiguration.getMailDomain,
      null, false, 0, cliwixCompany.getCompanyConfiguration.getActive)

    initCompany(liferayCompany.getCompanyId)

    if (cliwixCompany.getCompanyConfiguration.getAccountName != null) {
      val liferayAccount = liferayCompany.getAccount
      liferayAccount.setName(cliwixCompany.getCompanyConfiguration.getAccountName)
      this.accountService.updateAccount(liferayAccount)
    }

    liferayCompany.setHomeURL(cliwixCompany.getCompanyConfiguration.getHomeUrl)
    val updatedLiferayCompany = this.companyService.updateCompany(liferayCompany)

    val defaultUser = liferayCompany.getDefaultUser
    defaultUser.setLanguageId(cliwixCompany.getCompanyConfiguration.getDefaultLocale)
    defaultUser.setTimeZoneId(cliwixCompany.getCompanyConfiguration.getDefaultTimezone)
    defaultUser.setGreeting(cliwixCompany.getCompanyConfiguration.getDefaultGreeting)
    this.userService.updateUser(defaultUser)

    val resultCompany = this.converter.convertToCliwixCompany(updatedLiferayCompany)
    resultCompany.getCompanyConfiguration.setVirtualHost(cliwixCompany.getCompanyConfiguration.getVirtualHost)
    CommandResult(resultCompany)
  }

  private val REQUIRED_EVENT_ADD_DEFAULT_DATA_ACTION = "com.liferay.portal.events.AddDefaultDataAction"

  private def initCompany(companyId: Long): Unit = {
    logger.info("Initializing company with DB ID: {}", String.valueOf(companyId))

    val startupEvents = {
      val events = PropsUtil.getArray(PropsKeys.APPLICATION_STARTUP_EVENTS)
      if (!events.contains(REQUIRED_EVENT_ADD_DEFAULT_DATA_ACTION)) Array(REQUIRED_EVENT_ADD_DEFAULT_DATA_ACTION) ++ events
      else events
    }

    startupEvents.foreach{ eventClass =>
      logger.debug("Executing startup event to init company: {}", eventClass)
      try {
        val eventInst = InstancePool.get(eventClass).asInstanceOf[SimpleAction]
        if (eventInst == null) throw new ClassNotFoundException(eventClass)
        eventInst.run(Array(String.valueOf(companyId)))
      } catch {
        case e: Throwable =>
          logger.error("Failed to execute startup event: " + eventClass, e)
      }
    }

    this.portalInstanceUtil.addPortalInstance(companyId)
  }

}

class CompanyUpdateHandler extends Handler[UpdateCommand[Company], Company] {

  @BeanProperty
  var companyService: CompanyLocalService = _

  @BeanProperty
  var accountService: AccountLocalService = _

  @BeanProperty
  var virtualHostService: VirtualHostLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: UpdateCommand[Company]): CommandResult[Company] = {
    assert(command.entity.getCompanyId != null, "companyId != null")
    val cliwixCompany = command.entity

    if (cliwixCompany.getCompanyConfiguration == null) {
      logger.debug("No CompanyConfiguration found, don't update company: {}", cliwixCompany)
      CommandResult(cliwixCompany)

    } else {

      logger.debug("Updating company: {}", cliwixCompany)

      val liferayCompany = this.companyService.getCompanyById(cliwixCompany.getCompanyId)
      liferayCompany.setMx(cliwixCompany.getCompanyConfiguration.getMailDomain)
      liferayCompany.setActive(cliwixCompany.getCompanyConfiguration.getActive)
      liferayCompany.setHomeURL(cliwixCompany.getCompanyConfiguration.getHomeUrl)
      val updatedLiferayCompany = this.companyService.updateCompany(liferayCompany)

      val vHost = this.virtualHostService.getVirtualHost(cliwixCompany.getCompanyId, 0)
      vHost.setHostname(cliwixCompany.getCompanyConfiguration.getVirtualHost)
      this.virtualHostService.updateVirtualHost(vHost)

      if (cliwixCompany.getCompanyConfiguration.getAccountName != null) {
        val liferayAccount = updatedLiferayCompany.getAccount
        liferayAccount.setName(cliwixCompany.getCompanyConfiguration.getAccountName)
        this.accountService.updateAccount(liferayAccount)
      }

      val defaultUser = updatedLiferayCompany.getDefaultUser
      defaultUser.setLanguageId(cliwixCompany.getCompanyConfiguration.getDefaultLocale)
      defaultUser.setTimeZoneId(cliwixCompany.getCompanyConfiguration.getDefaultTimezone)
      defaultUser.setGreeting(cliwixCompany.getCompanyConfiguration.getDefaultGreeting)
      this.userService.updateUser(defaultUser)

      val resultCompany = this.converter.convertToCliwixCompany(updatedLiferayCompany)
      CommandResult(resultCompany)
    }
  }
}

class CompanyGetByIdHandler extends Handler[GetByDBIdCommand[Company], Company] {

  @BeanProperty
  var companyService: CompanyLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: GetByDBIdCommand[Company]): CommandResult[Company] = {
    try {
      val company = this.companyService.getCompanyById(command.dbId)
      CommandResult(this.converter.convertToCliwixCompany(company))
    } catch {
      case e: NoSuchCompanyException =>
        logger.warn(s"No Company with id ${command.dbId} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }
}

class CompanyGetByIdentifierHandler extends Handler[GetByIdentifierOrPathCommand[Company], Company] {

  @BeanProperty
  var companyService: CompanyLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: GetByIdentifierOrPathCommand[Company]): CommandResult[Company] = {
    try {
      val company = this.companyService.getCompanyByWebId(command.identifierOrPath)
      CommandResult(this.converter.convertToCliwixCompany(company))
    } catch {
      case e: NoSuchCompanyException =>
        logger.warn(s"No Company with webId ${command.identifierOrPath} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }
}

class CompanyDeleteHandler extends Handler[DeleteCommand[Company], Company] {

  @BeanProperty
  var companyService: CompanyLocalService = _

  @BeanProperty
  var portalInstanceUtil: PortalInstanceUtil = _

  @BeanProperty
  var nativeSqlAccessUtil: NativeSqlAccessUtil = _

  private[core] override def handle(command: DeleteCommand[Company]): CommandResult[Company] = {
    logger.debug("Deleting company: {}", command.entity)

    val cliwixCompany = command.entity

    //In some Liferay versions deleteCompany() returns a Company in some not,
    //so we must use reflection here
    val deleteCompanyMethod = this.companyService.getClass.getMethod("deleteCompany", classOf[Long])
    deleteCompanyMethod.invoke(this.companyService, cliwixCompany.getCompanyId)

    this.nativeSqlAccessUtil.eraseAllCompanyData(cliwixCompany.getCompanyId)
    this.portalInstanceUtil.removePortalInstance(cliwixCompany.getCompanyId)

    CommandResult(null)
  }

}
