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

import at.nonblocking.cliwix.core._
import at.nonblocking.cliwix.core.expression.{ExpressionGenerator, ExpressionResolver}
import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._
import scala.beans.BeanProperty

/**
 * Replace document Urls in page settings if this page is of type URL.
 */
class ReplaceDocumentUrlsInPageSettingsInterceptor extends TreeProcessingInterceptor[Page, Pages] with ReplaceDocumentUrlsUtil with Reporting with LazyLogging {

  @BeanProperty
  var expressionGenerator: ExpressionGenerator = _

  @BeanProperty
  var expressionResolver: ExpressionResolver = _

  @BeanProperty
  var groupUtil: GroupUtil = _

  override def afterEntityExport(page: Page, companyId: Long) = {
    if (page != null && page.getPageType == PAGE_TYPE.URL && page.getPageSettings != null) {
      page.getPageSettings
        .filter(_.getKey.equalsIgnoreCase("url"))
        .foreach{ ps =>
          ps.setValue(createDocumentUrlExpressions(ps.getValue, this.groupUtil, this.expressionGenerator))
        }
    }
  }

  override def beforeEntityInsert(page: Page, companyId: Long) = {
    resolveDocumentUrlExpressionsInPageSettings(page, companyId)
  }

  override def beforeEntityUpdate(page: Page, existingPage: Page, companyId: Long) = {
    resolveDocumentUrlExpressionsInPageSettings(page, companyId)
  }

  private def resolveDocumentUrlExpressionsInPageSettings(page: Page, companyId: Long) = {
    if (page != null && page.getPageType == PAGE_TYPE.URL && page.getPageSettings != null) {
      page.getPageSettings
        .filter(_.getKey.equalsIgnoreCase("url"))
        .foreach{ ps =>
          ps.setValue(resolveDocumentUrlExpressions(companyId, ps.getValue, this.expressionResolver, s"URL of page with DB ID: ${page.getPageId}"))
        }
    }
  }

}
