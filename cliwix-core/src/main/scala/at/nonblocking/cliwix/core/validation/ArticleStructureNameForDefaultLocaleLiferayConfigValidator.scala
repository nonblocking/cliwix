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

import at.nonblocking.cliwix.core.util.{TreeTypeUtils, ListTypeUtils}
import at.nonblocking.cliwix.model.{ArticleStructure, LiferayConfig}

import scala.collection.JavaConversions._
import scala.collection.mutable

class ArticleStructureNameForDefaultLocaleLiferayConfigValidator extends LiferayConfigValidator with ListTypeUtils with TreeTypeUtils {

  override def validate(liferayConfig: LiferayConfig): List[ValidationError] = {
    assert(liferayConfig != null, "liferayConfig != null")

    val messages = new mutable.MutableList[ValidationError]()

    safeForeach(liferayConfig.getCompanies) { company =>
      if (company.getCompanyConfiguration != null) {
        val defaultLocale = company.getCompanyConfiguration.getDefaultLocale

        safeForeach(company.getSites) { site =>
          if (site.getSiteContent != null && site.getSiteContent.getWebContent != null) {
            safeProcessRecursively(site.getSiteContent.getWebContent.getStructures) { articleStructure =>
              checkArticleStructure(articleStructure, defaultLocale, messages)
            }
          }
        }
      }
    }

    messages.toList
  }

  private def checkArticleStructure(articleStructure: ArticleStructure, defaultLocale: String, messages: mutable.MutableList[ValidationError]) = {
    val nameWithDefaultLocale = articleStructure.getNames.find(_.getLocale == defaultLocale)
    if (nameWithDefaultLocale.isEmpty) {
      messages += new ValidationError("A name with the default locale must exist!", s"ArticleStructure: ${articleStructure.identifiedBy()}", null)
    }
  }

}
