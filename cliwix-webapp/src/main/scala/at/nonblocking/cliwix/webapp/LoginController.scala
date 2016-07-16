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

package at.nonblocking.cliwix.webapp

import java.io.Serializable
import java.security.SecureRandom
import javax.inject.Inject
import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestBody, RequestMethod, RequestMapping}

import scala.beans.BeanProperty

@Controller
class LoginController extends ControllerDefaults with LazyLogging {

  @BeanProperty
  @Inject
  var cliwixCoreHolder: CliwixCoreHolder = _

  var random = new SecureRandom()

  @RequestMapping(value = Array("/services/login"), method = Array(RequestMethod.POST))
  def login(request: HttpServletRequest, @RequestBody loginData: LoginData): LoginResult = {
    val succeeded =
      if (loginData.getUsername != null && loginData.getPassword != null) {
        this.cliwixCoreHolder.getCliwix.getLiferayAuthenticator.login(loginData.getUsername, loginData.getPassword)
      } else {
        false
      }

    val result = new LoginResult
    result.setSucceeded(succeeded)

    if (!succeeded) {
      result.setErrorMessage("Invalid username or password!")
    } else {
      val user = new User(loginData.username)
      logger.info("User login successful: {}", user)

      storeUser(request, user)
    }

    result
  }

  @RequestMapping(value = Array("/services/logout"), method = Array(RequestMethod.GET))
  def logout(request: HttpServletRequest): Unit = {
    storeUser(request, null)
    request.getSession.invalidate()
  }

}

class LoginData extends Serializable {

  @BeanProperty
  var username: String = _

  @BeanProperty
  var password: String = _

}


class LoginResult extends Serializable {

  @BeanProperty
  var succeeded: Boolean = _

  @BeanProperty
  var errorMessage: String = _

}
