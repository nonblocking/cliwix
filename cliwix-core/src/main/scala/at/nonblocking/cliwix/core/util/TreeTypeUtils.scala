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

import java.{util=>jutil}
import scala.collection.JavaConversions._

import at.nonblocking.cliwix.model.{IMPORT_POLICY, TreeType, LiferayEntity}

trait TreeTypeUtils {

  def safeProcessRecursively[T <: LiferayEntity](tree: TreeType[T])(closure: T => Unit): Unit =
    if (tree != null) processRecursively(tree.getRootItems, tree)(closure)

  private def processRecursively[T <: LiferayEntity](items: jutil.List[T], tree: TreeType[T])(closure: T => Unit): Unit = {
    if (items != null) items.foreach { it =>
      closure(it)
      processRecursively(tree.getSubItems(it), tree)(closure)
    }
  }

  def safeProcessRecursivelyWithParent[T <: LiferayEntity](tree: TreeType[T])(closure: (T, T) => Unit): Unit =
    if (tree != null) processRecursivelyWithParent(tree.getRootItems, null.asInstanceOf[T], tree)(closure)

  private def processRecursivelyWithParent[T <: LiferayEntity](items: jutil.List[T], parent: T, tree: TreeType[T])(closure: (T, T) => Unit): Unit = {
    if (items != null) items.foreach { it =>
      closure(parent, it)
      processRecursivelyWithParent(tree.getSubItems(it), it, tree)(closure)
    }
  }

}
