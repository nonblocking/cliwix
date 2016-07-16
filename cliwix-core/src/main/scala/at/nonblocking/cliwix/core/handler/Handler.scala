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

package at.nonblocking.cliwix.core.handler

import java.lang.reflect.InvocationTargetException

import at.nonblocking.cliwix.core._
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.command.CommandResult
import at.nonblocking.cliwix.core.transaction.CliwixTransaction
import at.nonblocking.cliwix.core.validation.CliwixValidationException
import com.liferay.portal.{NoSuchOrganizationException, NoSuchRoleException, NoSuchUserException, NoSuchUserGroupException}
import com.liferay.portlet.{dynamicdatamapping, journal}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

/**
 * Base class for all command handlers
 */
abstract class Handler[C <: Command[R]: Manifest, R: Manifest] extends ErrorHandling with LazyLogging {

  @throws[CliwixCommandExecutionException]
  final def execute(command: C): CommandResult[R] = exceptionTranslation(command)(handle(command))

  private[core] def handle(command: C): CommandResult[R]

  def canHandle(command: Command[_]) = manifest[C].runtimeClass.isAssignableFrom(command.getClass) && manifest[R].runtimeClass.isAssignableFrom(command.resultType)

}

trait ErrorHandling extends Reporting {

  private[core] def exceptionTranslation[T <: CommandResult[_]](command: Command[_])(closure: => T): T = {
    try {
      closure
    } catch {
      case e: CliwixCommandExecutionException => throw e
      case e: InvocationTargetException =>
        throw new CliwixCommandExecutionException(command, e.getCause)
      case e: Exception =>
        throw new CliwixCommandExecutionException(command, e)
    }
  }

  private[core] def handleNoSuchRole[T](roleName: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e: NoSuchRoleException =>
        if (ExecutionContext.flags.ignoreNonExistingRoles) {
          report.addWarning(s"Role $roleName does not exist!")
          null.asInstanceOf[T]
        } else {
          throw new CliwixValidationException(s"Role $roleName does not exist!", e)
        }
      case e: Throwable => throw e
    }
  }

  private[core] def handleNoSuchUserGroup[T](groupName: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e: NoSuchUserGroupException =>
        if (ExecutionContext.flags.ignoreNonExistingUserGroups) {
          report.addWarning(s"UserGroup $groupName does not exist!")
          null.asInstanceOf[T]
        } else {
          throw new CliwixValidationException(s"User group $groupName does not exist!", e)
        }
      case e: Throwable => throw e
    }
  }

  private[core] def handleNoSuchOrganization[T](orgName: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e: NoSuchOrganizationException =>
        if (ExecutionContext.flags.ignoreNonExistingOrganizations) {
          report.addWarning(s"Organization $orgName does not exist!")
          null.asInstanceOf[T]
        } else {
          throw new CliwixValidationException(s"Organization $orgName does not exist!", e)
        }
      case e: Throwable => throw e
    }
  }

  private[core] def handleNoSuchUser[T](userScreenName: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e: NoSuchUserException =>
        if (ExecutionContext.flags.ignoreNonExistingUsers) {
          report.addWarning(s"User with screenName $userScreenName does not exist!")
          null.asInstanceOf[T]
        } else {
          throw new CliwixValidationException(s"User with screen name $userScreenName does not exist!", e)
        }
      case e: Throwable => throw e
    }
  }

  private[core] def handleNoSuchAction(actionId: String, resourceName: String): Unit = {
    if (ExecutionContext.flags.ignoreNonExistingResourceActions) {
      report.addWarning(s"No action $actionId is defined for resource type $resourceName!")
    } else {
      throw new CliwixValidationException(s"No action $actionId is defined for resource type $resourceName")
    }
  }

  private[core] def handleNoSuchCountryCode(countryCode: String): Unit = {
    if (ExecutionContext.flags.ignoreNonExistingCountries) {
      report.addWarning(s"No country code $countryCode found!")
    } else {
      throw new CliwixValidationException(s"No country code $countryCode found!")
    }
  }

  private[core] def handleNoSuchRegionCode(regionCode: String): Unit = {
    if (ExecutionContext.flags.ignoreNonExistingRegions) {
      report.addWarning(s"No region code $regionCode found!")
    } else {
      throw new CliwixValidationException(s"No region code $regionCode found!")
    }
  }

  private[core] def handleNoSuchStructure[T](structureId: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e @ (_: journal.NoSuchStructureException | _: dynamicdatamapping.NoSuchStructureException) =>
        throw new CliwixValidationException(s"A web content structure with id '$structureId' doesn't exist!", e)
      case e: Throwable => throw e
    }
  }

  private[core] def handleNoSuchTemplate[T](templateId: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e @ (_: journal.NoSuchTemplateException | _: dynamicdatamapping.NoSuchTemplateException) =>
        throw new CliwixValidationException(s"A web content template with id '$templateId' doesn't exist!", e)
      case e: Throwable => throw e
    }
  }

  private[core] def handleInvalidArticleContent[T](articleId: String, content: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e: journal.ArticleContentException =>
        throw new CliwixValidationException(s"Invalid article content in article '$articleId': $content", e)
      case e: Throwable => throw e
    }
  }

  private[core] def handleInvalidStructureXsd[T](structureId: String, xml: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e @ (_: journal.StructureXsdException | _: dynamicdatamapping.StructureXsdException) =>
        throw new CliwixValidationException(s"Invalid dynamic element XML in structure '$structureId': $xml", e)
      case e: Throwable => throw e
    }
  }

  private[core] def handleDuplicateStructureElement[T](structureId: String)(closure: => T): T = {
    try {
      closure
    } catch {
      case e: journal.DuplicateStructureElementException =>
        throw new CliwixValidationException(s"Duplicate dynamic-element found in structure: $structureId!", e)
      case e: Throwable => throw e
    }
  }
}

class DispatchHandler extends LazyLogging {

  @BeanProperty
  var handlers: java.util.List[Handler[_, _]] = _

  @BeanProperty
  var cliwixTransaction: CliwixTransaction = _

  def execute[T](command: Command[T]): CommandResult[T] = {
    def executeCommand(handler: Handler[_, _]) =
      this.cliwixTransaction.executeWithinLiferayTransaction() {
        handler.asInstanceOf[Handler[Command[T], T]].execute(command)
      }

    for (h <- handlers) {
      if (h.canHandle(command)) {
        logger.debug("Executing command: {}", command.desc)
        val result = executeCommand(h)
        assert(result != null, s"Result of handler ${h.getClass.getName} != null")
        logger.debug("Execution result: {}", String.valueOf(result.result))
        return result
      }
    }

    throw new CliwixException("No handler found for command: " + command.getClass.getName)
  }

}

