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

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import at.nonblocking.cliwix.core.Cliwix
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod}

@Controller
@RequestMapping(value = Array("/services/downloads"))
class DownloadController extends ControllerDefaults with LazyLogging {

  @RequestMapping(value = Array("/cliClient"), method = Array(RequestMethod.GET))
  def downloadCliClient(request: HttpServletRequest, response: HttpServletResponse): Unit = {

    val cliClientFileName = "cliwix-cli-client-" + Cliwix.getVersion + ".zip"
    val cliJarPath = "/WEB-INF/cli-client/" + cliClientFileName

    val inputStream = request.getServletContext.getResourceAsStream(cliJarPath)
    if (inputStream == null) {
      logger.error("Not found: {}", cliJarPath)
      throw new CliwixNotFoundException("CLI Client zip not found")
    }

    response.setContentType("application/zip")
    response.setHeader("Content-Disposition", "attachment; filename=" + cliClientFileName)

    IOUtils.copy(inputStream, response.getOutputStream)
  }

  @RequestMapping(value = Array("/cliwixAPI"), method = Array(RequestMethod.GET))
  def downloadCliwixAPI(request: HttpServletRequest, response: HttpServletResponse): Unit = {

    val plainVersion = if (Cliwix.getVersion.contains("SNAPSHOT")) {
      Cliwix.getVersion.substring(0, Cliwix.getVersion.indexOf("SNAPSHOT") + "SNAPSHOT-".length - 1)
    } else {
      Cliwix.getVersion.substring(0, Cliwix.getVersion.indexOf("-"))
    }

    val cliwixAPIFileName = "cliwix-model-" + plainVersion + ".jar"
    val cliwixAPIFileNameDownload = "cliwix-api-" + Cliwix.getVersion + ".jar"
    val cliwixAPIJarPath = "/WEB-INF/lib/" + cliwixAPIFileName

    val inputStream = request.getServletContext.getResourceAsStream(cliwixAPIJarPath)
    if (inputStream == null) {
      logger.error("Not found: {}", cliwixAPIJarPath)
      throw new CliwixNotFoundException("Cliwix API JAR not found")
    }

    response.setContentType("application/zip")
    response.setHeader("Content-Disposition", "attachment; filename=" + cliwixAPIFileNameDownload)

    IOUtils.copy(inputStream, response.getOutputStream)
  }

}


