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

package at.nonblocking.cliwix.core.validation

import at.nonblocking.cliwix.core.{LiferayInfo, Cliwix}
import at.nonblocking.cliwix.core.util.ListTypeUtils
import at.nonblocking.cliwix.model.LiferayConfig

import scala.beans.BeanProperty
import scala.collection.mutable

class ArticleTemplateStructureIdRequiredIn61LiferayConfigValidator extends LiferayConfigValidator with ListTypeUtils {

  @BeanProperty
  var liferayInfo: LiferayInfo = _

  override def validate(liferayConfig: LiferayConfig): List[ValidationError] = {
    assert(liferayConfig != null, "liferayConfig != null")

    val messages = new mutable.MutableList[ValidationError]()

    if (this.liferayInfo.getBaseVersion == "6.1") {
      safeForeach(liferayConfig.getCompanies) { company =>
        safeForeach(company.getSites) { site =>
          if (site.getSiteContent != null && site.getSiteContent.getWebContent != null) {
            safeForeach(site.getSiteContent.getWebContent.getTemplates) { articleTemplate =>
              if (articleTemplate.getStructureId == null) {
                messages += new ValidationError("ArticleTemplate.structureId is mandatory in Liferay 6.1!", s"ArticleTemplate: ${articleTemplate.identifiedBy()}", null)
              }
            }
          }
        }
      }
    }

    messages.toList
  }


}
