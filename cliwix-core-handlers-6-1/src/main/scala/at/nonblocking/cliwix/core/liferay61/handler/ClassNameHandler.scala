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

import at.nonblocking.cliwix.core.command.{GetByIdentifierOrPathCommand, CommandResult, GetByDBIdCommand}
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.ClassName
import com.liferay.portal.NoSuchClassNameException
import com.liferay.portal.service.ClassNameLocalService

import scala.beans.BeanProperty

class ClassNameGetByIdHandler extends Handler[GetByDBIdCommand[ClassName], ClassName] {

  @BeanProperty
  var classNameService: ClassNameLocalService = _

  override private[core] def handle(command: GetByDBIdCommand[ClassName]): CommandResult[ClassName] = {
    try {
      val className = this.classNameService.getClassName(command.dbId)
      val cliwixClassName = new ClassName(className.getClassNameId, className.getClassName)
      CommandResult(cliwixClassName)
    } catch {
      case e: NoSuchClassNameException =>
        logger.warn(s"No className with id ${command.dbId} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class ClassNameGetByIdentifierHandler extends Handler[GetByIdentifierOrPathCommand[ClassName], ClassName] {

  @BeanProperty
  var classNameService: ClassNameLocalService = _

  override private[core] def handle(command: GetByIdentifierOrPathCommand[ClassName]): CommandResult[ClassName] = {
    try {
      val className = this.classNameService.getClassName(command.identifierOrPath)
      val cliwixClassName = new ClassName(className.getClassNameId, className.getClassName)
      CommandResult(cliwixClassName)
    } catch {
      case e: NoSuchClassNameException =>
        logger.warn(s"No className with name ${command.identifierOrPath} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}