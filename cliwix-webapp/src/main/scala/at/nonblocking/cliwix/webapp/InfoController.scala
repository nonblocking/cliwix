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
import javax.inject.Inject
import javax.servlet.http.HttpServletRequest

import at.nonblocking.cliwix.core.{CliwixLiferayNotReadyException, CliwixLiferayNotSupportedException, CliwixLiferayNotFoundException, Cliwix}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import scala.beans.BeanProperty

@Controller
@RequestMapping(value = Array("/services"))
class InfoController extends ControllerDefaults {

  @BeanProperty
  @Inject
  var cliwixCoreHolder: CliwixCoreHolder = _

  @RequestMapping(value = Array("/info"), method = Array(RequestMethod.GET))
  def info(request: HttpServletRequest) = {
    checkPermission(request)
    new Info
  }

  @RequestMapping(value = Array("/info/status"), method = Array(RequestMethod.GET))
  def liferayInfo(request: HttpServletRequest) = {
    checkPermission(request)

    try {
      cliwixCoreHolder.getCliwix
      new InfoStatus("READY")
    } catch {
      case e: CliwixLiferayNotFoundException =>
        new InfoStatus("LIFERAY_NOT_FOUND")
      case e: CliwixLiferayNotSupportedException =>
        new InfoStatus("LIFERAY_VERSION_NOT_SUPPORTED")
      case e: CliwixLiferayNotReadyException =>
        new InfoStatus("NOT_READY")
    }
  }

  class InfoStatus(s: String) extends Serializable {

    @BeanProperty
    val status: String = s

  }

  class Info extends Serializable {

    @BeanProperty
    val cliwixVersion = Cliwix.getVersion

    @BeanProperty
    val cliwixWorkspaceDirectory = webappConfig.getWorkspaceDirectory.getAbsolutePath

    @BeanProperty
    val liferayRelease = cliwixCoreHolder.getCliwix.getLiferayInfo.getReleaseInfo

  }

}


