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

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.ArticleTemplate
import com.liferay.portlet.dynamicdatamapping.NoSuchTemplateException
import com.liferay.portlet.dynamicdatamapping.service.DDMTemplateLocalService
import com.liferay.portlet.journal.model.JournalTemplateAdapter

import scala.beans.BeanProperty

class ArticleTemplateGetByIdHandler extends Handler[GetByDBIdCommand[ArticleTemplate], ArticleTemplate] {

  @BeanProperty
  var ddmTemplateLocalService: DDMTemplateLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[ArticleTemplate]): CommandResult[ArticleTemplate] = {
    try {
      val existingDDMTemplate = this.ddmTemplateLocalService.getTemplate(command.dbId)
      val existingArticleTemplate = new JournalTemplateAdapter(existingDDMTemplate)
      val cliwixArticleTemplate = this.converter.convertToCliwixArticleTemplate(existingArticleTemplate)
      CommandResult(cliwixArticleTemplate)
    } catch {
      case e: NoSuchTemplateException =>
        logger.warn(s"No ArticleTemplate with db id ${command.dbId} found")
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}
