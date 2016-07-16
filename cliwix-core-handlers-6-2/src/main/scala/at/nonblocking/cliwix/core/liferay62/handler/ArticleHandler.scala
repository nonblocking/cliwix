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

import java.util.Date

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.{LiferayDate, LiferayEntityConverter}
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.core.liferay61.handler.ArticleChecker
import at.nonblocking.cliwix.core.util.AssetEntryUtil
import at.nonblocking.cliwix.model.{Article, StaticArticle, TemplateDrivenArticle}

import com.liferay.portlet.journal.model.JournalArticle
import com.liferay.portlet.journal.service.JournalArticleLocalService

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class ArticleInsertHandler extends Handler[ArticleInsertCommand, Article] with ArticleChecker {

  @BeanProperty
  var articleService: JournalArticleLocalService = _

  @BeanProperty
  var assetEntryUtil: AssetEntryUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: ArticleInsertCommand): CommandResult[Article] = {
    val cliwixArticle = command.article

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()
    if (cliwixArticle.getAssetTags != null) serviceContext.setAssetTagNames(cliwixArticle.getAssetTags.toList.toArray)
    val autoArticleId = false
    val classNameId = 0
    val classPK = 0
    val version = 1.0
    val titleMap = this.converter.toLiferayTextMap(cliwixArticle.getTitles)
    val descriptionMap = this.converter.toLiferayTextMap(cliwixArticle.getSummaries)
    val layoutUuid = null
    val displayDate = if (cliwixArticle.getDisplayDate == null) new Date() else cliwixArticle.getDisplayDate
    val liferayDisplayDate = this.converter.toLiferayDate(displayDate, defaultUser.getTimeZone)
    val neverExpire = cliwixArticle.getExpirationDate == null
    val liferayExpirationDate = this.converter.toLiferayDate(cliwixArticle.getExpirationDate, defaultUser.getTimeZone)
    val neverReview = true
    val liferayReviewDate = LiferayDate()
    //Folders are not yet supported
    val folderId = 0

    logger.debug("Adding article: {}", cliwixArticle)

    cliwixArticle match {
      case cliwixStaticArticle: StaticArticle =>
        val structureId = null
        val templateId = null
        val content = this.converter.toLiferayXmlContent(cliwixStaticArticle.getContents, cliwixArticle.getDefaultLocale)

        checkContent(content)

        val insertedArticle = this.articleService.addArticle(defaultUser.getUserId, command.groupId, folderId, classNameId, classPK,
          cliwixArticle.getArticleId, autoArticleId, version, titleMap, descriptionMap,
          content, cliwixArticle.getType, structureId, templateId, layoutUuid,
          liferayDisplayDate.month, liferayDisplayDate.day, liferayDisplayDate.year, liferayDisplayDate.hour, liferayDisplayDate.minute,
          liferayExpirationDate.month, liferayExpirationDate.day, liferayExpirationDate.year, liferayExpirationDate.hour, liferayExpirationDate.minute,
          neverExpire,
          liferayReviewDate.month, liferayReviewDate.day, liferayReviewDate.year, liferayReviewDate.hour, liferayReviewDate.minute,
          neverReview,
          true, false, null, null, null, null, serviceContext)

        val insertedCliwixArticle = this.converter.convertToCliwixStaticArticle(insertedArticle)
        insertedCliwixArticle.setAssetTags(cliwixArticle.getAssetTags)
        CommandResult(insertedCliwixArticle)

      case cliwixTemplateDrivenArticle: TemplateDrivenArticle =>
        val structureId = cliwixTemplateDrivenArticle.getStructureId
        val templateId = cliwixTemplateDrivenArticle.getTemplateId
        val content = this.converter.toLiferayRootXml(cliwixTemplateDrivenArticle.getDynamicElements, cliwixArticle.getDefaultLocale)

        checkContent(content)

        val insertedArticle =
          handleNoSuchStructure(structureId){
            handleNoSuchTemplate(templateId){
              handleInvalidArticleContent(cliwixTemplateDrivenArticle.getArticleId, content) {
                this.articleService.addArticle(defaultUser.getUserId, command.groupId, folderId, classNameId, classPK,
                  cliwixArticle.getArticleId, autoArticleId, version, titleMap, descriptionMap,
                  content, cliwixArticle.getType, structureId, templateId, layoutUuid,
                  liferayDisplayDate.month, liferayDisplayDate.day, liferayDisplayDate.year, liferayDisplayDate.hour, liferayDisplayDate.minute,
                  liferayExpirationDate.month, liferayExpirationDate.day, liferayExpirationDate.year, liferayExpirationDate.hour, liferayExpirationDate.minute,
                  neverExpire,
                  liferayReviewDate.month, liferayReviewDate.day, liferayReviewDate.year, liferayReviewDate.hour, liferayReviewDate.minute,
                  neverReview,
                  true, false, null, null, null, null, serviceContext)
              }
            }
        }

        val insertedCliwixArticle = this.converter.convertToCliwixTemplateDrivenArticle(insertedArticle)
        insertedCliwixArticle.setAssetTags(cliwixArticle.getAssetTags)
        CommandResult(insertedCliwixArticle)
    }
  }

}

class ArticleUpdateHandler extends Handler[UpdateCommand[Article], Article] with ArticleChecker {

  @BeanProperty
  var articleService: JournalArticleLocalService = _

  @BeanProperty
  var assetEntryUtil: AssetEntryUtil = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[Article]): CommandResult[Article] = {
    val cliwixArticle = command.entity
    val article = this.articleService.getArticle(cliwixArticle.getArticleDbId)

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val serviceContext = ExecutionContext.createServiceContext()
    if (cliwixArticle.getAssetTags != null) serviceContext.setAssetTagNames(cliwixArticle.getAssetTags.toList.toArray)
    val titleMap = this.converter.toLiferayTextMap(cliwixArticle.getTitles)
    val descriptionMap = this.converter.toLiferayTextMap(cliwixArticle.getSummaries)
    val displayDate = if (cliwixArticle.getDisplayDate == null) new Date() else cliwixArticle.getDisplayDate
    val liferayDisplayDate = this.converter.toLiferayDate(displayDate, defaultUser.getTimeZone)
    val neverExpire = cliwixArticle.getExpirationDate == null
    val liferayExpirationDate = this.converter.toLiferayDate(cliwixArticle.getExpirationDate, defaultUser.getTimeZone)
    val neverReview = true
    val liferayReviewDate = LiferayDate()
    //Folders are not yet supported
    val folderId = 0

    logger.debug("Updating article: {}", cliwixArticle)

    cliwixArticle match {
      case cliwixStaticArticle: StaticArticle =>
        val content = this.converter.toLiferayXmlContent(cliwixStaticArticle.getContents, cliwixArticle.getDefaultLocale)

        checkContent(content)

        val updatedArticle = this.articleService.updateArticle(defaultUser.getUserId, article.getGroupId,
          folderId, article.getArticleId, article.getVersion,
          titleMap, descriptionMap, content, cliwixArticle.getType, null, null, article.getLayoutUuid,
          liferayDisplayDate.month, liferayDisplayDate.day, liferayDisplayDate.year, liferayDisplayDate.hour, liferayDisplayDate.minute,
          liferayExpirationDate.month, liferayExpirationDate.day, liferayExpirationDate.year, liferayExpirationDate.hour, liferayExpirationDate.minute,
          neverExpire,
          liferayReviewDate.month, liferayReviewDate.day, liferayReviewDate.year, liferayReviewDate.hour, liferayReviewDate.minute,
          neverReview,
          true, false, null, null, null, null, serviceContext)

        val updatedCliwixArticle = this.converter.convertToCliwixStaticArticle(updatedArticle)
        updatedCliwixArticle.setAssetTags(cliwixArticle.getAssetTags)
        CommandResult(updatedCliwixArticle)

      case cliwixTemplateDrivenArticle: TemplateDrivenArticle =>
        val structureId = cliwixTemplateDrivenArticle.getStructureId
        val templateId = cliwixTemplateDrivenArticle.getTemplateId
        val content = this.converter.toLiferayRootXml(cliwixTemplateDrivenArticle.getDynamicElements, cliwixArticle.getDefaultLocale)

        checkContent(content)

        val updatedArticle =
          handleNoSuchStructure(structureId) {
            handleNoSuchTemplate(templateId) {
              handleInvalidArticleContent(cliwixTemplateDrivenArticle.getArticleId, content) {
                this.articleService.updateArticle(defaultUser.getUserId, article.getGroupId,
                  folderId, article.getArticleId, article.getVersion,
                  titleMap, descriptionMap, content, cliwixArticle.getType, structureId, templateId, article.getLayoutUuid,
                  liferayDisplayDate.month, liferayDisplayDate.day, liferayDisplayDate.year, liferayDisplayDate.hour, liferayDisplayDate.minute,
                  liferayExpirationDate.month, liferayExpirationDate.day, liferayExpirationDate.year, liferayExpirationDate.hour, liferayExpirationDate.minute,
                  neverExpire,
                  liferayReviewDate.month, liferayReviewDate.day, liferayReviewDate.year, liferayReviewDate.hour, liferayReviewDate.minute,
                  neverReview,
                  true, false, null, null, null, null, serviceContext)
              }
            }
          }

        val updatedCliwixArticle = this.converter.convertToCliwixTemplateDrivenArticle(updatedArticle)
        updatedCliwixArticle.setAssetTags(cliwixArticle.getAssetTags)
        CommandResult(updatedCliwixArticle)

    }
  }
}
