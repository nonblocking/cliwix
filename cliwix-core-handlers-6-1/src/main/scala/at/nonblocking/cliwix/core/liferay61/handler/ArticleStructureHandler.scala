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

import java.{util => jutil}

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.ArticleStructure
import com.liferay.portlet.dynamicdatamapping
import com.liferay.portlet.journal.NoSuchStructureException
import com.liferay.portlet.journal.service.JournalStructureLocalService

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class ArticleStructureListHandler extends Handler[ArticleStructureListCommand, jutil.List[ArticleStructure]] {

  @BeanProperty
  var articleStructureService: JournalStructureLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: ArticleStructureListCommand): CommandResult[jutil.List[ArticleStructure]] = {
    val structures = this.articleStructureService.getStructures(command.groupId)

    def addSubStructures(articleStructure: ArticleStructure): Unit = {
      val subStructures =
        if (articleStructure.getStructureId != null)
          structures.filter(_.getParentStructureId == articleStructure.getStructureId)
        else
          structures.filter(s => s.getParentStructureId == null || s.getParentStructureId.isEmpty)

      if (subStructures.nonEmpty) {
        articleStructure.setSubStructures(new jutil.ArrayList())
        subStructures.foreach { subStructure =>
          val cliwixArticleStructure = this.converter.convertToCliwixArticleStructure(subStructure)
          logger.debug("Exporting article structure: {}", cliwixArticleStructure.getStructureId)
          addSubStructures(cliwixArticleStructure)
          articleStructure.getSubStructures.add(cliwixArticleStructure)
        }
      }
    }

    val fakeRootStructure = new ArticleStructure()
    addSubStructures(fakeRootStructure)

    CommandResult(if (fakeRootStructure.getSubStructures != null) fakeRootStructure.getSubStructures else Nil)
  }
}

class ArticleStructureInsertHandler extends Handler[ArticleStructureInsertCommand, ArticleStructure]  {

  @BeanProperty
  var articleStructureService: JournalStructureLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: ArticleStructureInsertCommand): CommandResult[ArticleStructure] = {
    assert(command.articleStructure != null, "articleStructure != null")

    val cliwixArticleStructure = command.articleStructure

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()
    val autoStructureId = false
    val parentStructureId = if (command.parentArticleStructure != null) command.parentArticleStructure.getStructureId else null
    val nameMap = this.converter.toLiferayTextMap(cliwixArticleStructure.getNames)
    val descriptionMap = this.converter.toLiferayTextMap(cliwixArticleStructure.getDescriptions)
    val defaultLocale = defaultUser.getLocale.getLanguage + "_" + defaultUser.getLocale.getCountry
    val xsd = this.converter.toLiferayRootXml(cliwixArticleStructure.getDynamicElements, defaultLocale)

    logger.debug("Insert article structure: {}", cliwixArticleStructure)

    val insertedStructure =
      handleInvalidStructureXsd(cliwixArticleStructure.getStructureId, cliwixArticleStructure.getDynamicElements) {
        handleDuplicateStructureElement(cliwixArticleStructure.getStructureId) {
          this.articleStructureService.addStructure(defaultUser.getUserId, command.groupId, cliwixArticleStructure.getStructureId, autoStructureId,
            parentStructureId, nameMap, descriptionMap, xsd, serviceContext)
        }
      }

    val insertedCliwixArticleStructure = this.converter.convertToCliwixArticleStructure(insertedStructure)
    CommandResult(insertedCliwixArticleStructure)
  }

}

class ArticleStructureUpdateHandler extends Handler[UpdateCommand[ArticleStructure], ArticleStructure] {

  @BeanProperty
  var articleStructureService: JournalStructureLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[ArticleStructure]): CommandResult[ArticleStructure] = {
    assert(command.entity.getDbId != null, "articleStructure.structureDbId != null")

    val cliwixArticleStructure = command.entity

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()
    val nameMap = this.converter.toLiferayTextMap(cliwixArticleStructure.getNames)
    val descriptionMap = this.converter.toLiferayTextMap(cliwixArticleStructure.getDescriptions)
    val defaultLocale = defaultUser.getLocale.getLanguage + "_" + defaultUser.getLocale.getCountry
    val xsd = this.converter.toLiferayRootXml(cliwixArticleStructure.getDynamicElements, defaultLocale)

    val existingArticleStructure = this.articleStructureService.getStructure(cliwixArticleStructure.getOwnerGroupId, cliwixArticleStructure.getStructureId)

    val parentStructureId = existingArticleStructure.getParentStructureId
    val groupId = existingArticleStructure.getGroupId

    logger.debug("Update article structure: {}", cliwixArticleStructure)

    val updatedStructure =
      handleInvalidStructureXsd(cliwixArticleStructure.getStructureId, cliwixArticleStructure.getDynamicElements) {
        handleDuplicateStructureElement(cliwixArticleStructure.getStructureId) {
          this.articleStructureService.updateStructure(groupId, cliwixArticleStructure.getStructureId,
            parentStructureId, nameMap, descriptionMap, xsd, serviceContext)
        }
      }

    val updatedCliwixArticleStructure = this.converter.convertToCliwixArticleStructure(updatedStructure)
    CommandResult(updatedCliwixArticleStructure)
  }
}

class ArticleStructureGetByIdHandler extends Handler[GetByDBIdCommand[ArticleStructure], ArticleStructure] {

  @BeanProperty
  var articleStructureService: JournalStructureLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[ArticleStructure]): CommandResult[ArticleStructure] = {
    try {
      val articleStructure = this.articleStructureService.getStructure(command.dbId)
      val cliwixArticleStructure = this.converter.convertToCliwixArticleStructure(articleStructure)
      CommandResult(cliwixArticleStructure)
    } catch {
      case e @ (_: NoSuchStructureException | _: dynamicdatamapping.NoSuchStructureException) =>
        logger.warn(s"No ArticleStructure with db id ${command.dbId} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class ArticleStructureGetByIdentifierHandler extends Handler[GetByIdentifierOrPathWithinGroupCommand[ArticleStructure], ArticleStructure] {

  @BeanProperty
  var articleStructureService: JournalStructureLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathWithinGroupCommand[ArticleStructure]): CommandResult[ArticleStructure] = {
    try {
      val articleStructure = this.articleStructureService.getStructure(command.groupId, command.identifierOrPath)
      val cliwixArticleStructure = this.converter.convertToCliwixArticleStructure(articleStructure)
      CommandResult(cliwixArticleStructure)
    } catch {
      case e @ (_: NoSuchStructureException | _: dynamicdatamapping.NoSuchStructureException) =>
        logger.warn(s"No ArticleStructure with id ${command.identifierOrPath} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class ArticleStructureDeleteHandler extends Handler[DeleteCommand[ArticleStructure], ArticleStructure] {

  @BeanProperty
  var articleStructureService: JournalStructureLocalService = _

  override private[core] def handle(command: DeleteCommand[ArticleStructure]): CommandResult[ArticleStructure] = {
    assert(command.entity.getDbId != null, "articleStructure.structureDbId != null")

    val cliwixArticleStructure = command.entity

    logger.debug("Delete article structure: {}", cliwixArticleStructure)

    this.articleStructureService.deleteStructure(cliwixArticleStructure.getOwnerGroupId, cliwixArticleStructure.getStructureId)
    CommandResult(null)
  }

}
