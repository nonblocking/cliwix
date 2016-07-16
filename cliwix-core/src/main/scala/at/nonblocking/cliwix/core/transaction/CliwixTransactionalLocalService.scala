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

/**
 * A custom service for Liferay to be able to use the transaction mechanism.
 * Just executes the given closure within a transaction.
 * <br/>
 * Must go into ext-spring.xml, see: https://www.liferay.com/de/community/wiki/-/wiki/Main/Adding+Spring+Capabilitites+to+Hook
 */
trait CliwixTransactionalLocalService {

  @throws(classOf[Exception])
  def executeTransactional[T](rollback: Boolean, task: CliwixTransactionalTask[T]): T

  @throws(classOf[Exception])
  def executeTransactionalReadonly[T](task: CliwixTransactionalTask[T]): T

}
