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
import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._

/*
 * Ignore some implicit roles such as User and Guest if configured
 */
class IgnoreRegularRoleAssignmentsInterceptor extends ListProcessingInterceptor[RegularRoleAssignment, RegularRoleAssignments] with Reporting with LazyLogging {

  override def afterListExport(assignments: RegularRoleAssignments, companyId: Long) = {
    removeRolesInIgnoreList(assignments)
  }

  override def beforeListImport(assignments: RegularRoleAssignments, companyId: Long) = {
    removeRolesInIgnoreList(assignments)
  }

  private def removeRolesInIgnoreList(assignments: RegularRoleAssignments) = {
    val ignoreRoles = Cliwix.getProperty(Cliwix.PROPERTY_IGNORE_REGULAR_ROLE_ASSIGNMENTS).trim.split("[,;]")
    if (ignoreRoles.nonEmpty) {
      assignments.getList.removeAll(
        assignments.getList.filter(findAssignmentsToIgnore(ignoreRoles))
      )
    }
  }

  private def findAssignmentsToIgnore(rolesToIngore: Array[String])(regularRoleAssignment: RegularRoleAssignment) = rolesToIngore.contains(regularRoleAssignment.getRoleName)

}
