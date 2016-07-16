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

import javax.xml.bind.Unmarshaller

import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

import java.{util => jutil}

private[core] class UseMapDbCollectionForPotentialHugeListsUnmarshalListener(potentialHugeLists: List[Class[_]], resourceAwareCollectionFactory: ResourceAwareCollectionFactory)
  extends Unmarshaller.Listener with LazyLogging {

  override def beforeUnmarshal(target: scala.Any, parent: scala.Any): Unit = {
    target match {
      case list: ListType[_] =>
        if (list.getList == null && potentialHugeLists.contains(target.getClass)) {
          val listMethod = target.getClass.getDeclaredMethod("getList")
          val listType = listMethod.getReturnType
          val listField = target.getClass.getDeclaredFields.find(_.getType == listType)
          if (listField.isDefined) {
            listField.get.setAccessible(true)
            val map = resourceAwareCollectionFactory.createMap()
            val wrapper = classOf[MapValuesListWrapper[_, _]].getDeclaredConstructor(classOf[jutil.Map[_, _]]).newInstance(map)
            listField.get.set(target, wrapper)
          } else {
            logger.warn("Couldn't determine list field of type: {}", target.getClass)
          }
        }
      case _ =>
    }
  }

}
