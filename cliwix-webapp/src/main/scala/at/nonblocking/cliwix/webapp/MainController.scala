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

import javax.inject.Inject
import javax.servlet.http.HttpServletRequest

import at.nonblocking.cliwix.core._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.{RequestMethod, RequestMapping}

import scala.beans.BeanProperty

@Controller
class MainController extends ControllerDefaults with LazyLogging {

  @BeanProperty
  @Inject
  var cliwixCoreHolder: CliwixCoreHolder = _

  @RequestMapping(value = Array("/", "/index.html", "index.htm"), method = Array(RequestMethod.GET))
  def index(request: HttpServletRequest, modelMap: ModelMap) = {
    var liferayFound = false
    var liferaySupported = false
    var liferayReady = false

    try {
      this.cliwixCoreHolder.getCliwix
      liferayFound = true
      liferaySupported = true
      liferayReady = true

    } catch {
      case e: CliwixLiferayNotFoundException =>
      case e: CliwixLiferayNotSupportedException =>
        liferayFound = true
      case e: CliwixLiferayNotReadyException =>
        liferayFound = true
        liferaySupported = true
    }

    modelMap.addAttribute("liferayFound", liferayFound)
    modelMap.addAttribute("liferaySupported", liferaySupported)
    modelMap.addAttribute("liferayReady", liferayReady)

    modelMap.addAttribute("user", getUsername(request))
    modelMap.addAttribute("version", Cliwix.getVersion)
    modelMap.addAttribute("standalone", false)

    "index"
  }

  @RequestMapping(value = Array("/unsupported_browser", "/unsupported_browser.html"), method = Array(RequestMethod.GET))
  def unsupportedBrowser() = {
    "unsupported_browser"
  }

}
