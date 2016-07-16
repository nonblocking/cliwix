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

package at.nonblocking.cliwix.core.transaction

import com.liferay.portal.kernel.transaction.{Propagation, Transactional}

class CliwixTransactionalLocalServiceImpl extends CliwixTransactionalLocalService {

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Array(classOf[Throwable]))
  override def executeTransactional[T](rollback: Boolean, task: CliwixTransactionalTask[T]): T = {
    val result = task.execute()

    if (rollback) {
      throw new CliwixRollbackException
    }

    result
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  override def executeTransactionalReadonly[T](task: CliwixTransactionalTask[T]): T = {
    task.execute()
  }

}