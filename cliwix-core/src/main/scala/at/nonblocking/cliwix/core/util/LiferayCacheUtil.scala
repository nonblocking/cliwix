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

import com.liferay.portal.kernel.cache.CacheRegistryUtil
import com.liferay.portal.kernel.dao.db.DBFactoryUtil
import com.liferay.portal.kernel.dao.orm.{EntityCacheUtil, FinderCacheUtil}
import com.typesafe.scalalogging.slf4j.LazyLogging

trait LiferayCacheUtil extends LazyLogging {

  def clearLiferayCaches() = {
    //Clear only if no embedded hypersonic is used, to avoid locking problems
    if (!isHSQLDB) {
      logger.info("Clearing Liferay cache")
      FinderCacheUtil.clearCache()
      EntityCacheUtil.clearCache()
      CacheRegistryUtil.clear()
    }
  }

  def disableLiferayCaching() = {
    if (isCachingEnabled && !isHSQLDB) CacheRegistryUtil.setActive(false)
  }

  def enableLiferayCaching() = {
    if (isCachingEnabled && !isHSQLDB) CacheRegistryUtil.setActive(true)
  }

  lazy val isCachingEnabled = CacheRegistryUtil.isActive

  lazy val isHSQLDB = DBFactoryUtil.getDB.getType.equals(com.liferay.portal.kernel.dao.db.DB.TYPE_HYPERSONIC)

}
