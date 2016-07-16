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

import java.io.Serializable
import java.{util => jutil}

import at.nonblocking.cliwix.core.validation.CliwixValidationException
import at.nonblocking.cliwix.model.LiferayEntity

import org.mapdb.HTreeMap

import scala.collection.JavaConversions._

/**
 * Wraps the map values for serialization.
 * Only iterator() is implemented!
 *
 * @param underlyingMap java.util.Map[K, V]
 * @tparam String Key
 * @tparam V Value
 */
private[core] class MapValuesListWrapper[String, V <: LiferayEntity](val underlyingMap: jutil.Map[String, V]) extends jutil.List[V] with Serializable {

  override def iterator(): jutil.Iterator[V] =
    if (underlyingMap.isInstanceOf[HTreeMap[_, _]]) new AutoUpdateIterator(underlyingMap.values.iterator)
    else underlyingMap.values.iterator

  override def isEmpty: Boolean = underlyingMap.isEmpty

  override def size(): Int = underlyingMap.size

  override def remove(elem: scala.Any): Boolean = {
    assert(elem != null, "elem != null")
    val key = elem.asInstanceOf[LiferayEntity].identifiedBy()
    if (key == null) throw new CliwixValidationException(s"Invalid element of type ${elem.getClass.getName} found with no identifier!")
    underlyingMap.remove(key)
    true
  }

  override def removeAll(c: jutil.Collection[_]): Boolean = {
    c.foreach(remove)
    true
  }

  override def add(elem: V): Boolean = {
    assert(elem != null, "elem != null")
    val key = elem.asInstanceOf[LiferayEntity].identifiedBy()
    if (key == null) throw new CliwixValidationException(s"Invalid element of type ${elem.getClass.getName} found with no identifier!")
    underlyingMap.put(key.asInstanceOf[String], elem)
    true
  }

  override def clear(): Unit = underlyingMap.clear()

  //Not implemented
  override def contains(o: scala.Any): Boolean = ???
  override def subList(fromIndex: Int, toIndex: Int): jutil.List[V] = ???
  override def listIterator(index: Int): jutil.ListIterator[V] = ???
  override def listIterator(): jutil.ListIterator[V] = ???
  override def lastIndexOf(o: scala.Any): Int = ???
  override def indexOf(o: scala.Any): Int = ???
  override def remove(index: Int): V = ???
  override def add(index: Int, element: V): Unit = ???
  override def set(index: Int, element: V): V = ???
  override def get(index: Int): V = ???
  override def retainAll(c: jutil.Collection[_]): Boolean = ???
  override def addAll(index: Int, c: jutil.Collection[_ <: V]): Boolean = ???
  override def addAll(c: jutil.Collection[_ <: V]): Boolean = ???
  override def containsAll(c: jutil.Collection[_]): Boolean = ???
  override def toArray[T](a: Array[T with Object]): Array[T with Object] = ???
  override def toArray: Array[AnyRef] = ???

  private class AutoUpdateIterator(val underlyingIterator: jutil.Iterator[V]) extends jutil.Iterator[V] {

    var last: V = _

    override def hasNext: Boolean = underlyingIterator.hasNext

    override def next(): V = {
      if (last != null) {
        underlyingMap.put(last.identifiedBy().asInstanceOf[String], last)
      }

      last = underlyingIterator.next()
      last
    }

    override def remove(): Unit = ???
  }

}

//Companion
private[core] object MapValuesListWrapper {
  def apply[JavaString, V <: LiferayEntity](map: jutil.Map[JavaString, V]): MapValuesListWrapper[JavaString, V] = new MapValuesListWrapper[JavaString, V](map)
}
