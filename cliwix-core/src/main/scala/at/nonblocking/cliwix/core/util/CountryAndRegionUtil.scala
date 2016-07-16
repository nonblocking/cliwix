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

package at.nonblocking.cliwix.core.util

import com.liferay.portal.model.{Country, Region}
import com.liferay.portal.service.{CountryService, RegionService}
import com.liferay.portal.{NoSuchCountryException, NoSuchRegionException}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

sealed trait CountryAndRegionUtil {
  def getCountryForA2Code(a2CountryCode: String): Option[Country]
  def getCountryForId(countryId: Long): Option[Country]
  def getRegionForRegionCode(country: Option[Country], regionCode: String): Option[Region]
  def getRegionForId(regionId: Long): Option[Region]
}

class CountryAndRegionUtilImpl extends CountryAndRegionUtil {

  @BeanProperty
  var countryService: CountryService = _

  @BeanProperty
  var regionService: RegionService = _

  override def getCountryForA2Code(a2CountryCode: String) = {
    if (a2CountryCode == null) {
      None
    } else {
      try {
        Some(this.countryService.getCountryByA2(a2CountryCode))
      } catch {
        case e: NoSuchCountryException => None
      }
    }
  }

  override def getCountryForId(countryId: Long) = {
    if (countryId == 0) {
      None
    } else {
      try {
        Some(this.countryService.getCountry(countryId))
      } catch {
        case e: NoSuchCountryException => None
      }
    }
  }

  override def getRegionForRegionCode(country: Option[Country], regionCode: String) = {
    if (!country.isDefined || regionCode == null) {
      None
    } else {
      this.regionService.getRegions(country.get.getCountryId).find(_.getRegionCode == regionCode)
    }
  }

  override def getRegionForId(regionId: Long) = {
    if (regionId == 0) {
      None
    } else {
      try {
        Some(this.regionService.getRegion(regionId))
      } catch {
        case e: NoSuchRegionException => None
      }
    }
  }

}
