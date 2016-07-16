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

package at.nonblocking.cliwix.core.liferay62.util

import at.nonblocking.cliwix.core.util.TrashFilter
import com.liferay.portal.model.{TrashedModel, BaseModel}

/**
 * Trash filter impl for Liferay 62.
 */
class TrashFilter62 extends TrashFilter {

  override def isInTrash(entity: BaseModel[_]) = {
    assert(entity.isInstanceOf[TrashedModel], s"${entity.getClass.getName} must be a TrashedModel")
    entity.asInstanceOf[TrashedModel].isInTrash
  }

}
