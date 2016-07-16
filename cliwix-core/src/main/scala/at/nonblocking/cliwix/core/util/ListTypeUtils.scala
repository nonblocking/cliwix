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

package at.nonblocking.cliwix.core.util

import at.nonblocking.cliwix.model.{IMPORT_POLICY, ListType, LiferayEntity}
import scala.collection.JavaConversions._

trait ListTypeUtils {

  def safeForeach[T <: LiferayEntity](list: ListType[T])(closure: T => Unit) = {
    if (list != null && list.getList != null) list.getList.foreach(closure)
  }

}
