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

package at.nonblocking.cliwix.core.interceptor

import at.nonblocking.cliwix.core.Reporting
import at.nonblocking.cliwix.core.expression.{ExpressionResolver, ExpressionGenerator}
import at.nonblocking.cliwix.core.util.{ListTypeUtils, GroupUtil}
import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class ReplaceDocumentUrlsInArticlesInterceptor extends ListProcessingInterceptor[Article, Articles] with ListTypeUtils with ReplaceDocumentUrlsUtil with Reporting with LazyLogging {

  @BeanProperty
  var expressionGenerator: ExpressionGenerator = _

  @BeanProperty
  var expressionResolver: ExpressionResolver = _

  @BeanProperty
  var groupUtil: GroupUtil = _

  override def afterEntityExport(article: Article, companyId: Long) = {
    article match {
      case cliwixStaticArticle: StaticArticle =>
        cliwixStaticArticle.getContents.foreach(xmlContent => xmlContent.setXml(createDocumentUrlExpressions(xmlContent.getXml, this.groupUtil, this.expressionGenerator)))
      case cliwixTemplateDrivenArticle: TemplateDrivenArticle =>
        cliwixTemplateDrivenArticle.setDynamicElements(createDocumentUrlExpressions(cliwixTemplateDrivenArticle.getDynamicElements, this.groupUtil, this.expressionGenerator))
    }
  }

  override def beforeEntityInsert(entity: Article, companyId: Long) = resolveDocumentUrlExpressionsInArticle(entity, companyId)

  override def beforeEntityUpdate(entity: Article, existingEntity: Article, companyId: Long) = resolveDocumentUrlExpressionsInArticle(entity, companyId)

  private def resolveDocumentUrlExpressionsInArticle(article: Article, companyId: Long) = {
    article match {
      case cliwixStaticArticle: StaticArticle =>
        cliwixStaticArticle.getContents.foreach { xmlContent =>
          xmlContent.setXml(resolveDocumentUrlExpressions(companyId, xmlContent.getXml, this.expressionResolver, s"Article with id ${article.getArticleId}"))
        }
      case cliwixTemplateDrivenArticle: TemplateDrivenArticle =>
        cliwixTemplateDrivenArticle.setDynamicElements(resolveDocumentUrlExpressions(companyId, cliwixTemplateDrivenArticle.getDynamicElements, this.expressionResolver, s"Article with id ${article.getArticleId}"))
    }
  }

}
