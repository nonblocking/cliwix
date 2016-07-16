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

import javax.inject.{Inject, Named}

import at.nonblocking.cliwix.core.Cliwix

import scala.beans.BeanProperty

sealed trait CliwixCoreHolder {
  def getCliwix: Cliwix
}

@Named
class CliwixCoreHolderImpl extends CliwixCoreHolder {

  @BeanProperty
  @Inject
  var webappConfig: WebappConfig = _

  private var cliwix: Cliwix = _

  override def getCliwix = {
    if (this.cliwix == null) {
      val createDummyCoreIfLiferayNotFound = this.webappConfig.getProperty(WebappConfig.PROPERTY_CREATE_DUMMY_CORE_IF_NO_LIFERAY_FOUND) == "true"
      this.cliwix = Cliwix(createDummyCoreIfLiferayNotFound)
    }
    this.cliwix
  }
}
