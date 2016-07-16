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

import at.nonblocking.cliwix.core.util.ListTypeUtils
import at.nonblocking.cliwix.model.{IMPORT_POLICY, LiferayConfig}
import com.liferay.portal.kernel.util.{PropsUtil, PropsKeys}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable
import scala.collection.JavaConversions._

private[core] class DefaultCompanyLiferayConfigValidator extends LiferayConfigValidator with ListTypeUtils with LazyLogging {

  val DEFAULT_VIRTUAL_HOST = "localhost"

  override def validate(liferayConfig: LiferayConfig): List[ValidationError] = {
    assert(liferayConfig != null, "liferayConfig != null")

    if (!DefaultCompanyLiferayConfigValidator.disabled) {
      val messages = new mutable.MutableList[ValidationError]()
      val defaultCompanyWebId = PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID)

      if (liferayConfig.getCompanies != null && liferayConfig.getCompanies.getImportPolicy == IMPORT_POLICY.ENFORCE) {
        if (liferayConfig.getCompanies.getList != null && !liferayConfig.getCompanies.getList.exists(_.getWebId == defaultCompanyWebId)) {
          messages += new ValidationError(s" Default company (webId='$defaultCompanyWebId') is required if import policy is ENFORCE!", "", null)
        }
      }

      safeForeach(liferayConfig.getCompanies) { company =>
        if (company.getWebId == defaultCompanyWebId
          && company.getCompanyConfiguration != null && company.getCompanyConfiguration.getVirtualHost != DEFAULT_VIRTUAL_HOST) {
          messages += new ValidationError(s"Virtual host of the default company (webId='$defaultCompanyWebId') must be 'localhost'", s"Company:${company.getWebId}", null)
        } else if (company.getWebId != defaultCompanyWebId
          && company.getCompanyConfiguration != null && company.getCompanyConfiguration.getVirtualHost == DEFAULT_VIRTUAL_HOST) {
          messages += new ValidationError(s"Virtual host 'localhost' is only allowed for the default company (webId='$defaultCompanyWebId')!", s"Company:${company.getWebId}", null)
        }
      }

      messages.toList

    } else {
      logger.warn("DefaultCompanyLiferayConfigValidator has been disabled.")
      Nil
    }
  }
}

object DefaultCompanyLiferayConfigValidator {
  private [core] var disabled =  false
  def disable() = disabled = true
}