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

import at.nonblocking.cliwix.model.{IMPORT_POLICY, Site, LiferayConfig}

import scala.collection.JavaConversions._
import scala.collection.mutable

private[core] class GuestSiteLiferayConfigValidator extends LiferayConfigValidator {

  override def validate(liferayConfig: LiferayConfig): List[ValidationError] = {
    assert(liferayConfig != null, "liferayConfig != null")

    val messages = new mutable.MutableList[ValidationError]()

    if (liferayConfig.getCompanies != null && liferayConfig.getCompanies.getList != null) {

      liferayConfig.getCompanies.getList.foreach { company =>
        if (company.getSites != null && company.getSites.getList != null) {
          val guestSiteCount = company.getSites.getList.count(_.getName == Site.GUEST_SITE_NAME)
          if (guestSiteCount > 1) messages += new ValidationError("Only one Guest site allowed per Company", s"Company: ${company.getWebId}", null)

          val policyCompany = liferayConfig.getCompanies.getImportPolicy
          val policySite = company.getSites.getImportPolicy
          if (policySite == IMPORT_POLICY.ENFORCE || (policySite == null && policyCompany == IMPORT_POLICY.ENFORCE)) {
            if (!company.getSites.getList.exists(_.getName == Site.GUEST_SITE_NAME)) {
              messages += new ValidationError("If policy is ENFORCE at least one Guest site is required", s"Company: ${company.getWebId}", null)
            }
          }
        }
      }
    } else {
      Nil
    }

    messages.toList
  }

}
