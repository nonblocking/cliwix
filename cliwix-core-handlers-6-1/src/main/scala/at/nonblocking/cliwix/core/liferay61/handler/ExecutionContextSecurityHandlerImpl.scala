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

package at.nonblocking.cliwix.core.liferay61.handler

import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.core.{CliwixException, ExecutionContextSecurityContext, ExecutionContextSecurityHandler}
import at.nonblocking.cliwix.model.Company
import com.liferay.portal.{model=>liferay}
import com.liferay.portal.security.auth.{PrincipalThreadLocal, CompanyThreadLocal}
import com.liferay.portal.security.permission.{PermissionThreadLocal, PermissionCheckerFactoryUtil}
import com.liferay.portal.service.{RoleLocalService, GroupLocalService, UserLocalService}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.JavaConversions._

import scala.beans.BeanProperty

class ExecutionContextSecurityHandlerImpl extends ExecutionContextSecurityHandler with LazyLogging {

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var roleService: RoleLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  override def updateSecurityContext(company: Company, failWhenNoAdminUserFound: Boolean) = {
    val defaultUser = this.userService.getDefaultUser(company.getCompanyId)
    var adminUser: liferay.User = null

    val adminRole = this.roleService.getRole(company.getCompanyId, liferay.RoleConstants.ADMINISTRATOR)
    if (adminRole != null) {
      val usersWithAdminRole = this.userService.getRoleUsers(adminRole.getRoleId)
      if (usersWithAdminRole.size() > 0) {
        adminUser = usersWithAdminRole.get(0)
      } else {
        //Find users that inherit the Administrator role indirectly - refs #99
        val groups = this.groupService.getRoleGroups(adminRole.getRoleId)
        val usersWithIndirectAdminRole = groups.flatMap { g =>
          if (g.isUserGroup) this.userService.getUserGroupUsers(g.getClassPK)
          else if (g.isOrganization) this.userService.getOrganizationUsers(g.getClassPK)
          else Nil
        }
        if (usersWithIndirectAdminRole.nonEmpty) {
          adminUser = usersWithIndirectAdminRole.get(0)
        } else if (failWhenNoAdminUserFound) {
          throw new CliwixValidationException(s"No user with role ${liferay.RoleConstants.ADMINISTRATOR} found in company '${company.getWebId}'. At least one is required for export/import.")
        } else {
          logger.warn(s"Currently no user with role ${liferay.RoleConstants.ADMINISTRATOR} available. Using defaultUser '${defaultUser.getScreenName}' as authenticated user.")
          adminUser = defaultUser
        }
      }

      logger.info(s"Setting admin user ${adminUser.getScreenName} for company ${company.getWebId}")

      CompanyThreadLocal.setCompanyId(company.getCompanyId)

      PrincipalThreadLocal.setName(adminUser.getUserId)

      val permissionChecker = PermissionCheckerFactoryUtil.create(adminUser, true)
      PermissionThreadLocal.setPermissionChecker(permissionChecker)

      ExecutionContextSecurityContext(defaultUser, adminUser)

    } else {
      throw new CliwixException(s"No role ${liferay.RoleConstants.ADMINISTRATOR} found in company '${company.getWebId}'. Corrupt database?")
    }
  }

}
