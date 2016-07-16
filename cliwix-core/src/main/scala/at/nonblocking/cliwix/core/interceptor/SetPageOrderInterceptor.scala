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

import at.nonblocking.cliwix.core.util.TreeTypeUtils
import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

import java.{util=>jutil}

class SetPageOrderInterceptor extends TreeProcessingInterceptor[Page, Pages] with TreeTypeUtils with LazyLogging {

  override def beforeTreeImport(pages: Pages, companyId: Long) = {
    if (pages != null) {
      setPageOrder(pages.getRootPages)
      safeProcessRecursively(pages) { page =>
        setPageOrder(page.getSubPages)
      }
    }
  }

  private def setPageOrder(pages: jutil.List[Page]) = {
    if (pages != null) {
      for (i <- 0 until pages.size()) {
        pages.get(i).setPageOrder(i)
      }
    }
  }

}
