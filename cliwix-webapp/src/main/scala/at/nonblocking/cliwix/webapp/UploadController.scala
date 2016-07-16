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
import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, RequestMethod, RequestMapping}
import org.springframework.web.multipart.MultipartFile

import scala.beans.BeanProperty

@Controller
@RequestMapping(value = Array("/services/uploads"))
class UploadController extends  ControllerDefaults with LazyLogging {

  @BeanProperty
  @Inject
  var taskExecutor: SingleTaskExecutor = _

  @BeanProperty
  @Inject
  var cliwixCoreHolder: CliwixCoreHolder = _

  @RequestMapping(method = Array(RequestMethod.POST))
  def upload(request: HttpServletRequest, @RequestParam("file") file: MultipartFile): UploadResult = {
    checkPermission(request)

    if (this.taskExecutor.isTaskRunning) {
      throw new IllegalStateException(this.webappConfig.getMessage("error.only.one.importexport.at.a.time"))
    }

    logger.info(s"Received file: ${file.getOriginalFilename}")
    logger.info(s"File size: ${file.getSize}")

    val tmpFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename
    val tmpFile = new File(this.webappConfig.getTmpDirectory, tmpFileName)

    logger.info(s"Transferring to: ${tmpFile.getAbsolutePath}")
    file.transferTo(tmpFile)

    val result = new UploadResult
    result.setTmpFileName(tmpFileName)
    result
  }

  class UploadResult extends Serializable {

    @BeanProperty
    var tmpFileName: String = _

  }

}


