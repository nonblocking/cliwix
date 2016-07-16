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

package at.nonblocking.cliwix.core.liferay62.handler

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.core.liferay62.util.Liferay62StructureXsdUtil
import at.nonblocking.cliwix.model.ArticleStructure
import com.liferay.portlet.dynamicdatamapping.NoSuchStructureException
import com.liferay.portlet.dynamicdatamapping.service.DDMStructureLocalService
import com.liferay.portlet.journal.model.JournalStructureAdapter
import com.liferay.portlet.journal.service.JournalStructureLocalService

import scala.beans.BeanProperty

class ArticleStructureInsertHandler extends Handler[ArticleStructureInsertCommand, ArticleStructure]  {

  @BeanProperty
  var articleStructureService: JournalStructureLocalService = _

  @BeanProperty
  var liferay62StructureXsdUtil: Liferay62StructureXsdUtil = _

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

    //see #119
    val fixedXsd = this.liferay62StructureXsdUtil.moveMetaDataAfterChildDynamicElements(xsd)

    logger.debug("Insert article structure: {}", cliwixArticleStructure)

    val insertedStructure =
      handleInvalidStructureXsd(cliwixArticleStructure.getStructureId, cliwixArticleStructure.getDynamicElements) {
        handleDuplicateStructureElement(cliwixArticleStructure.getStructureId) {
          this.articleStructureService.addStructure(defaultUser.getUserId, command.groupId, cliwixArticleStructure.getStructureId, autoStructureId,
            parentStructureId, nameMap, descriptionMap, fixedXsd, serviceContext)
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
  var liferay62StructureXsdUtil: Liferay62StructureXsdUtil = _

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

    //see #119
    val fixedXsd = this.liferay62StructureXsdUtil.moveMetaDataAfterChildDynamicElements(xsd)

    val existingArticleStructure = this.articleStructureService.getStructure(cliwixArticleStructure.getOwnerGroupId, cliwixArticleStructure.getStructureId)

    val parentStructureId = existingArticleStructure.getParentStructureId
    val groupId = existingArticleStructure.getGroupId

    logger.debug("Update article structure: {}", cliwixArticleStructure)

    val updatedStructure =
      handleInvalidStructureXsd(cliwixArticleStructure.getStructureId, cliwixArticleStructure.getDynamicElements) {
        handleDuplicateStructureElement(cliwixArticleStructure.getStructureId) {
          this.articleStructureService.updateStructure(groupId, cliwixArticleStructure.getStructureId,
            parentStructureId, nameMap, descriptionMap, fixedXsd, serviceContext)
        }
      }

    val updatedCliwixArticleStructure = this.converter.convertToCliwixArticleStructure(updatedStructure)
    CommandResult(updatedCliwixArticleStructure)
  }
}

class ArticleStructureGetByIdHandler extends Handler[GetByDBIdCommand[ArticleStructure], ArticleStructure] {

  @BeanProperty
  var ddmStructureLocalService: DDMStructureLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[ArticleStructure]): CommandResult[ArticleStructure] = {
    try {
      val existingDDMStructure = this.ddmStructureLocalService.getStructure(command.dbId)
      val existingArticleStructure = new JournalStructureAdapter(existingDDMStructure)
      val cliwixArticleStructure = this.converter.convertToCliwixArticleStructure(existingArticleStructure)
      CommandResult(cliwixArticleStructure)
    } catch {
      case e: NoSuchStructureException =>
        logger.warn(s"No ArticleStructure with db id ${command.dbId} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}
