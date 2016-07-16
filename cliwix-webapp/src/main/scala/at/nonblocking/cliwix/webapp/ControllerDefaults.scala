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

import java.io.{Serializable, File}
import javax.inject.Inject
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import at.nonblocking.cliwix.core.CliwixInfoProperties
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.springframework.web.bind.annotation.ExceptionHandler

import scala.beans.BeanProperty

trait ControllerDefaults extends LazyLogging {

  val DEFAULT_LIST_LIMIT = 100

  val REQUEST_HEADER_CLIWIX_CLIENT = "CliwixClient"
  val REQUEST_HEADER_CLIWIX_SECURITY_TOKEN = "CliwixToken"
  val REQUEST_QUERY_PARAM_SECURITY_TOKEN = "token"

  val INFO_PROPERTY_USER = "user"
  val INFO_PROPERTY_CLIENT = "client"
  val INFO_PROPERTY_CLIENT_IP = "client.ip"

  private val SESSION_ATTRIBUTE_USER = "cliwix.user"

  @BeanProperty
  @Inject
  var webappConfig: WebappConfig = _

  def storeUser(request: HttpServletRequest, user: User): Unit = {
    request.getSession(true).setAttribute(SESSION_ATTRIBUTE_USER, user)
  }

  def checkPermission(request: HttpServletRequest): Unit = {
    if (this.webappConfig.getProperty(WebappConfig.PROPERTY_ENABLE_SECURITY) != "false") {
      val user = request.getSession.getAttribute(SESSION_ATTRIBUTE_USER).asInstanceOf[User]
      if (user == null) {
        throw new CliwixLoginRequiredException("Access without login from: " + getRemoteIPAddress(request))
      }

      logger.debug("Access granted to user: {}", user.username)
    }
  }

  def getUsername(request: HttpServletRequest): String = {
    val user = request.getSession.getAttribute(SESSION_ATTRIBUTE_USER)
    if (user != null) {
      user.asInstanceOf[User].username
    } else {
      "anonymous"
    }
  }

  def getClient(request: HttpServletRequest): String = {
    val clientName = request.getHeader(REQUEST_HEADER_CLIWIX_CLIENT)
    if (clientName == null) "Unknown"
    else clientName
  }

  def getRemoteIPAddress(request: HttpServletRequest): String = {
    if (request.getHeader("x-forwarded-for") != null) {
      request.getHeader("x-forwarded-for")
    } else {
      request.getRemoteAddr
    }
  }

  def isAllDigits(x: String) = x forall Character.isDigit

  @ExceptionHandler
  def handleError(throwable: Throwable, response: HttpServletResponse): Error = {

    throwable match {
      case e: CliwixNotFoundException =>
        logger.error(e.getMessage)
        response.setStatus(HttpServletResponse.SC_NOT_FOUND)
      case e: CliwixLoginRequiredException =>
        logger.error(e.getMessage)
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
      case _ =>
        logger.error(throwable.getMessage, throwable)
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    }

    val error = new Error
    error.setMessage(throwable.getMessage)
    error
  }

  def enrichInfoProperties(taskInfo: TaskInfo, baseFolder: File) = {
    val info = new CliwixInfoProperties(baseFolder)

    info.setProperty(INFO_PROPERTY_USER, taskInfo.user)
    info.setProperty(INFO_PROPERTY_CLIENT, taskInfo.client)
    info.setProperty(INFO_PROPERTY_CLIENT_IP, taskInfo.clientIP)

    info.save()
  }

  class Error extends Serializable {

    @BeanProperty
    var message: String = _

  }

}
