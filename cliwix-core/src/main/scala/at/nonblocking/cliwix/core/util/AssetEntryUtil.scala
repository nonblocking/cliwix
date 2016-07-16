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

import java.{util => jutil}

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.model.Asset
import com.liferay.portlet.asset.NoSuchEntryException
import com.liferay.portlet.asset.service.AssetEntryLocalService

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

sealed trait AssetEntryUtil {
  def getTagListForAssetEntry(className: String, classPk: Long): jutil.List[String]

  @deprecated("Use serviceContext.setAssetTagNames() to update tags")
  def updateAssetEntry(className: String, classPk: Long, cliwixAsset: Asset)
}

private[core] class AssetEntryUtilImpl extends AssetEntryUtil {

  @BeanProperty
  var assetEntryLocalService: AssetEntryLocalService = _

  override def getTagListForAssetEntry(className: String, classPk: Long): jutil.List[String] = {
    try {
      val assetEntry = this.assetEntryLocalService.getEntry(className, classPk)
      val tags = assetEntry.getTagNames.toList
      if (tags.nonEmpty) tags else null
    } catch  {
      case e: NoSuchEntryException =>
        null
    }
  }

  override def updateAssetEntry(className: String, classPk: Long, cliwixAsset: Asset) = {
    val assetEntry = this.assetEntryLocalService.getEntry(className, classPk)

    val defaultUser = ExecutionContext.securityContext.defaultUser
    val tags = if (cliwixAsset.getAssetTags != null) cliwixAsset.getAssetTags.toArray(Array[String]()) else Array[String]()

    this.assetEntryLocalService.updateEntry(defaultUser.getUserId, assetEntry.getGroupId, className, classPk,
      assetEntry.getCategoryIds, tags)
  }

}
