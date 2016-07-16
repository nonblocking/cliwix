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

import com.liferay.portal.kernel.util.{PropsKeys, PropsUtil}
import com.liferay.portal.model.RoleConstants
import com.liferay.portal.security.auth.Authenticator
import com.liferay.portal.service.{RoleLocalService, UserLocalService, CompanyLocalService}
import com.typesafe.scalalogging.slf4j.LazyLogging

import com.liferay.portal.{model=>liferay}
import java.{util => jutil}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

sealed trait LiferayAuthenticator {
  def login(username: String, password: String): Boolean
}

class LiferayAuthenticatorImpl extends LiferayAuthenticator with LazyLogging {

  @BeanProperty
  var companyService: CompanyLocalService = _

  @BeanProperty
  var userService: UserLocalService = _

  @BeanProperty
  var roleService: RoleLocalService = _

  private var _defaultWebId = PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID)

  override def login(username: String, password: String) = {

    val defaultCompany = this.companyService.getCompanies.find(_.getWebId == _defaultWebId)
    if (defaultCompany.isEmpty) {
      logger.error("Login error: No default company found!")
      false
    } else {
      val companyId = defaultCompany.get.getCompanyId

      var user: liferay.User = null

      val byScreenNameResult =  this.userService.authenticateByScreenName(companyId, username, password, emptyMapStringArray, emptyMapStringArray, emptyMapAnyRef)
      if (byScreenNameResult == Authenticator.SUCCESS) {
        user = this.userService.getUserByScreenName(companyId, username)
      } else {
        val byEMailResult =  this.userService.authenticateByEmailAddress(companyId, username, password, emptyMapStringArray, emptyMapStringArray, emptyMapAnyRef)
        if (byEMailResult == Authenticator.SUCCESS) {
          user = this.userService.getUserByEmailAddress(companyId, username)
        }
      }

      if (user == null) {
        logger.error("Login error: No such user or invalid password!")
        false
      } else {
        val isAdmin = this.roleService.hasUserRole(user.getUserId, companyId, RoleConstants.ADMINISTRATOR, true)
        if (!isAdmin) {
          logger.error("Login error: User '{}' exists, but is no admin!", username)
          false
        } else {
          true
        }
      }
    }
  }

  def emptyMapStringArray = new jutil.HashMap[String, Array[String]]()
  def emptyMapAnyRef = new jutil.HashMap[String, AnyRef]()

  //For test purposes only
  def setDefaultWebId(webId: String) = _defaultWebId = webId

}

class LiferayAuthenticatorDummyImpl extends LiferayAuthenticator {

  override def login(username: String, passwordDigest: String) = true

}