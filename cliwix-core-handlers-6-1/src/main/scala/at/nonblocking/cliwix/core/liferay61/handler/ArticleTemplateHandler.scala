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
import at.nonblocking.cliwix.core.util.ResourceAwareCollectionFactory
import at.nonblocking.cliwix.model.ArticleTemplate
import com.liferay.portlet.journal.service.JournalTemplateLocalService
import com.liferay.portlet.{journal, dynamicdatamapping}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class ArticleTemplateListHandler extends Handler[ArticleTemplateListCommand, jutil.Map[String, ArticleTemplate]] {

  @BeanProperty
  var articleTemplateService: JournalTemplateLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  @BeanProperty
  var resourceAwareCollectionFactory: ResourceAwareCollectionFactory = _

  private[core] override def handle(command: ArticleTemplateListCommand): CommandResult[jutil.Map[String, ArticleTemplate]] = {
    val templates = this.articleTemplateService.getTemplates(command.groupId)
    val resultMap = this.resourceAwareCollectionFactory.createMap[String, ArticleTemplate](templates.size())

    templates.foreach { template =>
      val cliwixArticleTemplate = this.converter.convertToCliwixArticleTemplate(template)
      logger.debug("Exporting article template: {}", cliwixArticleTemplate.getTemplateId)
      resultMap.put(cliwixArticleTemplate.identifiedBy, cliwixArticleTemplate)
    }

    CommandResult(resultMap)
  }
}

class ArticleTemplateInsertHandler extends Handler[ArticleTemplateInsertCommand, ArticleTemplate]  {

  @BeanProperty
  var articleTemplateService: JournalTemplateLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: ArticleTemplateInsertCommand): CommandResult[ArticleTemplate] = {
    assert(command.articleTemplate != null, "articleTemplate != null")

    val cliwixArticleTemplate = command.articleTemplate

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()
    val autoTemplateId = false
    val nameMap = this.converter.toLiferayTextMap(cliwixArticleTemplate.getNames)
    val descriptionMap = this.converter.toLiferayTextMap(cliwixArticleTemplate.getDescriptions)
    val formatScript = false
    val cachable = true

    logger.debug("Insert article template: {}", cliwixArticleTemplate)

    val insertedArticleTemplate = this.articleTemplateService.addTemplate(defaultUser.getUserId, command.groupId,
      cliwixArticleTemplate.getTemplateId, autoTemplateId, cliwixArticleTemplate.getStructureId, nameMap, descriptionMap,
      cliwixArticleTemplate.getScript, formatScript, cliwixArticleTemplate.getLanguage,
      cachable, false, null, null, serviceContext)

    val insertedCliwixArticleTemplate = this.converter.convertToCliwixArticleTemplate(insertedArticleTemplate)
    CommandResult(insertedCliwixArticleTemplate)
  }

}

class ArticleTemplateUpdateHandler extends Handler[UpdateCommand[ArticleTemplate], ArticleTemplate] {

  @BeanProperty
  var articleTemplateService: JournalTemplateLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[ArticleTemplate]): CommandResult[ArticleTemplate] = {
    assert(command.entity.getDbId != null, "articleTemplate.templateDbId != null")

    val cliwixArticleTemplate = command.entity

    val serviceContext = ExecutionContext.createServiceContext()
    val nameMap = this.converter.toLiferayTextMap(cliwixArticleTemplate.getNames)
    val descriptionMap = this.converter.toLiferayTextMap(cliwixArticleTemplate.getDescriptions)
    val formatScript = false

    val existingTemplate = this.articleTemplateService.getTemplate(cliwixArticleTemplate.getOwnerGroupId, cliwixArticleTemplate.getTemplateId)
    val groupId = existingTemplate.getGroupId
    val cachable = existingTemplate.getCacheable

    logger.debug("Update article template: {}", cliwixArticleTemplate)

    val updatedArticleTemplate = this.articleTemplateService.updateTemplate(groupId, cliwixArticleTemplate.getTemplateId,
      cliwixArticleTemplate.getStructureId, nameMap, descriptionMap,
      cliwixArticleTemplate.getScript, formatScript, cliwixArticleTemplate.getLanguage,
      cachable, false, null, null, serviceContext)

    val updatedCliwixArticleTemplate = this.converter.convertToCliwixArticleTemplate(updatedArticleTemplate)
    CommandResult(updatedCliwixArticleTemplate)
  }
}

class ArticleTemplateGetByIdHandler extends Handler[GetByDBIdCommand[ArticleTemplate], ArticleTemplate] {

  @BeanProperty
  var articleTemplateService: JournalTemplateLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[ArticleTemplate]): CommandResult[ArticleTemplate] = {
    try {
      val existingArticleTemplate = this.articleTemplateService.getTemplate(command.dbId)
      val cliwixArticleTemplate = this.converter.convertToCliwixArticleTemplate(existingArticleTemplate)
      CommandResult(cliwixArticleTemplate)
    } catch {
      case e @ (_: journal.NoSuchTemplateException | _: dynamicdatamapping.NoSuchTemplateException) =>
        logger.warn(s"No ArticleTemplate with db id ${command.dbId} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class ArticleTemplateGetByIdentifierHandler extends Handler[GetByIdentifierOrPathWithinGroupCommand[ArticleTemplate], ArticleTemplate] {

  @BeanProperty
  var articleTemplateService: JournalTemplateLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathWithinGroupCommand[ArticleTemplate]): CommandResult[ArticleTemplate] = {
    try {
      val existingArticleTemplate = this.articleTemplateService.getTemplate(command.groupId, command.identifierOrPath)
      val cliwixArticleTemplate = this.converter.convertToCliwixArticleTemplate(existingArticleTemplate)
      CommandResult(cliwixArticleTemplate)
    } catch {
      case e @ (_: journal.NoSuchTemplateException | _: dynamicdatamapping.NoSuchTemplateException) =>
        logger.warn(s"No ArticleTemplate with id ${command.identifierOrPath} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class ArticleTemplateDeleteHandler extends Handler[DeleteCommand[ArticleTemplate], ArticleTemplate] {

  @BeanProperty
  var articleTemplateService: JournalTemplateLocalService = _

  override private[core] def handle(command: DeleteCommand[ArticleTemplate]): CommandResult[ArticleTemplate] = {
    assert(command.entity.getDbId != null, "articleTemplate.templateDbId != null")

    val cliwixArticleTemplate = command.entity

    logger.debug("Delete article template: {}", command.entity)

    this.articleTemplateService.deleteTemplate(cliwixArticleTemplate.getOwnerGroupId, cliwixArticleTemplate.getTemplateId)
    CommandResult(null)
  }

}