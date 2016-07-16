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

package at.nonblocking.cliwix.core

import java.util.regex.Pattern

import at.nonblocking.cliwix.model.{Site, Company, LiferayEntity}

trait LiferayEntityFilter {
  def exportEntitiesOf[T](entity: Class[T]): Boolean
  def exportEntityInstance[T <: LiferayEntity](entity: T): Boolean
}

case class LiferayEntityFilterExclude(excludeEntities: List[Class[_]], excludeCompanies: Array[String] = null, excludeSites: Array[String] = null) extends LiferayEntityFilter {
  val includeFilter = LiferayEntityFilterInclude(excludeEntities, excludeCompanies, excludeSites)

  override def exportEntitiesOf[T](entity: Class[T]) = !includeFilter.exportEntitiesOf(entity)
  override def exportEntityInstance[T <: LiferayEntity](entity: T) = !includeFilter.exportEntityInstance(entity)
}

case class LiferayEntityFilterInclude(includeEntities: List[Class[_]], includeCompanies: Array[String] = null, includeSites: Array[String] = null) extends LiferayEntityFilter {

  val includeCompaniesRegex =
    if (includeCompanies != null && includeCompanies.nonEmpty) includeCompanies.map(c => Pattern.compile(c.trim.replace("*", ".*"), Pattern.CASE_INSENSITIVE | Pattern.DOTALL))
    else null
  val includeSitesRegex =
    if (includeSites != null && includeSites.nonEmpty) includeSites.map(s => Pattern.compile(s.trim.replace("*", ".*"), Pattern.CASE_INSENSITIVE | Pattern.DOTALL))
    else null


  override def exportEntitiesOf[T](entity: Class[T]) = includeEntities.contains(entity)
  override def exportEntityInstance[T <: LiferayEntity](entity: T) =
    if (includeCompanies != null && entity.isInstanceOf[Company]) {
      includeCompaniesRegex.exists(_.matcher(entity.identifiedBy().trim).matches())
    } else if (includeSites != null && entity.isInstanceOf[Site]) {
      includeSitesRegex.exists(_.matcher(entity.identifiedBy().trim).matches())
    } else {
      true
    }
}

object LiferayEntityFilter {
  def ALL = new LiferayEntityFilter {
    override def exportEntitiesOf[T](entity: Class[T]) = true
    override def exportEntityInstance[T <: LiferayEntity](entity: T) = true
  }
}

case class LiferayExporterConfig(filter: LiferayEntityFilter = LiferayEntityFilter.ALL,
                                 skipCorruptDocuments: Boolean = false,
                                 exportOnlyFileDataLastModifiedWithinDays: Option[Int]  = None);
