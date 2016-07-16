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

package at.nonblocking.cliwix.core.compare

import java.util.Date
import java.{util => jutil}

import at.nonblocking.cliwix.core._
import at.nonblocking.cliwix.core.CliwixException
import at.nonblocking.cliwix.model.compare._
import com.liferay.portal.kernel.util.DateUtil
import org.apache.commons.beanutils.PropertyUtils

import scala.collection.JavaConversions._

import scala.collection.mutable

sealed trait LiferayEntityComparator {
  def equals[T <: ComparableObject](existingEntity: T, newEntity: T): Boolean
  def diff[T <: ComparableObject](existingEntity: T, newEntity: T): List[PropertyDiff]
}

private[core] class LiferayEntityComparatorImpl extends LiferayEntityComparator {

  private case class PropertyDescriptor(name: String, isCollection: Boolean, compareAnnotationName: String)

  private val compareAnnotations = List(classOf[CompareEquals], classOf[CompareLesserEquals],  classOf[CompareEqualsIfNotNull])

  private val classNameCompareEquals = classOf[CompareEquals].getName
  private val classNameCompareLesserEquals = classOf[CompareLesserEquals].getName
  private val classNameCompareEqualsIfNotNull = classOf[CompareEqualsIfNotNull].getName

  private val compareFieldsCache = new mutable.HashMap[Class[_ <: ComparableObject], List[PropertyDescriptor]]

  override def equals[T <: ComparableObject](existingEntity: T, newEntity: T) = diff(existingEntity, newEntity).isEmpty

  override def diff[T <: ComparableObject](existingEntity: T, newEntity: T) = {
    diff(null, existingEntity, newEntity)
  }

  private def diff[T <: ComparableObject](name: String, existingEntity: T, newEntity: T) = {
    assert(existingEntity != null && newEntity != null, "existingEntity != null and newEntity != null")

    if (existingEntity.getClass != newEntity.getClass) {
      List(PropertyDiff("CHANGE", "tyoe", existingEntity.getClass, newEntity.getClass))
    } else {
      getCompareFields(existingEntity).flatMap{ propertyDescriptor =>
        val fullName =
          if (name == null) propertyDescriptor.name
          else name + "." + propertyDescriptor.name

        val existingObj = PropertyUtils.getProperty(existingEntity, propertyDescriptor.name)
        val newObj = PropertyUtils.getProperty(newEntity, propertyDescriptor.name)

        if (!propertyDescriptor.isCollection) {
          compareObject(fullName, existingObj, newObj, propertyDescriptor.compareAnnotationName)
        } else {
          assert(propertyDescriptor.compareAnnotationName == classNameCompareEquals, "collections cannot be compared using: " + propertyDescriptor.compareAnnotationName)
          compareCollection(fullName, existingObj.asInstanceOf[jutil.Collection[_]], newObj.asInstanceOf[jutil.Collection[_]])
        }
      }
    }
  }

  private def compareObject[T](name: String, existingObj: T, newObj: T, compareAnnotationName: String): List[PropertyDiff] =
    if (existingObj == null && newObj == null) List()
    else if (existingObj == null && newObj != null)
      List(PropertyDiff("ADD", name, null, newObj))
    else if (existingObj != null && newObj == null)
      if (compareAnnotationName != classNameCompareEqualsIfNotNull)
        List(PropertyDiff("REMOVE", name, existingObj, null))
      else
        List()
    else {
      compareAnnotationName match {
        case `classNameCompareEquals` | `classNameCompareEqualsIfNotNull` =>
          existingObj match {
            case date1: Date =>
              val date2 = newObj.asInstanceOf[Date]
              if (!DateUtil.equals(date1, date2, true))
                List(PropertyDiff("CHANGE", name, existingObj, newObj))
              else
                List()
            case comparable: ComparableObject =>
              diff(name, existingObj.asInstanceOf[ComparableObject], newObj.asInstanceOf[ComparableObject])
            case _ =>
              if (!existingObj.equals(newObj))
                List(PropertyDiff("CHANGE", name, existingObj, newObj))
              else
                List()
          }
        case `classNameCompareLesserEquals` =>
          if (!existingObj.isInstanceOf[Number] || !newObj.isInstanceOf[Number])
            throw new CliwixException("CompareLesserEquals is only allowed for numbers")
          if (newObj.asInstanceOf[Number].doubleValue() > existingObj.asInstanceOf[Number].doubleValue())
            List(PropertyDiff("CHANGE", name, existingObj, newObj))
          else
            List()
        case _ =>
          throw new CliwixException("Unsupported comparator: " + compareAnnotationName)
      }
    }

  private def compareCollection(name: String, existingCollection: jutil.Collection[_], newCollection: jutil.Collection[_]): List[PropertyDiff] = {
    if (existingCollection == null && newCollection == null) List()
    else if (existingCollection == null && newCollection.isEmpty) List()
    else if (newCollection == null && existingCollection.isEmpty) List()
    else if (existingCollection == null && newCollection != null)
      newCollection.zipWithIndex.map { case (newObj, i) => PropertyDiff("ADD", name + s"[$i]", null, newObj) }.toList
    else if (existingCollection != null && newCollection == null)
      existingCollection.zipWithIndex.map { case (existingObj, i) => PropertyDiff("REMOVE", name + s"[$i]", null, existingObj) }.toList
    else {
      val list1 = existingCollection.zipWithIndex
        .flatMap { case (existingObj, i) =>
          existingObj match {
            case comparable1: ComparableAndIdentifiableObject =>
              val fullName = name + s"[${comparable1.identifiedBy()}]"
              val newObj = newCollection.find(_.asInstanceOf[ComparableAndIdentifiableObject].identifiedBy() == comparable1.identifiedBy())
              if (newObj.isDefined) {
                diff(fullName, comparable1, newObj.get.asInstanceOf[ComparableObject])
              } else {
                List(PropertyDiff("REMOVE", fullName, existingObj, null))
              }
            case _ =>
              val fullName = name + s"[$i]"
              if (!newCollection.contains(existingObj)) {
                List(PropertyDiff("REMOVE", fullName, existingObj, null))
              } else {
                List()
              }
          }
        }
      .toList

      val list2 = newCollection.zipWithIndex
        .flatMap { case (newObj, i) =>
          newObj match {
            case comparable1: ComparableAndIdentifiableObject =>
              val fullName = name + s"[${comparable1.identifiedBy()}]"
              val existingObj = existingCollection.find(_.asInstanceOf[ComparableAndIdentifiableObject].identifiedBy() == comparable1.identifiedBy())
              if (existingObj.isEmpty) {
                List(PropertyDiff("ADD", fullName, null, newObj))
              } else {
                List()
              }
            case _ =>
              val fullName = name + s"[$i]"
              if (!existingCollection.contains(newObj)) {
                List(PropertyDiff("ADD", fullName, null, newObj))
              } else {
                List[PropertyDiff]()
              }
          }
        }
      .toList

      list1 ++ list2
    }
  }

  private def getCompareFields[T <: ComparableObject](entity: T) = {
    def getCompareFieldsFromClass(klazz: Class[_], fieldList: List[PropertyDescriptor]): List[PropertyDescriptor] = {
      val fields = fieldList ++ klazz.getDeclaredFields
        .filter(field => compareAnnotations.exists(field.getAnnotation(_) != null))
        .map { field =>
          val isCollection = classOf[jutil.Collection[_]].isAssignableFrom(field.getType)
          val annotationName = compareAnnotations.find(field.getAnnotation(_) != null).get.getName
          PropertyDescriptor(field.getName, isCollection, annotationName)
        }

      if (klazz.getSuperclass.getPackage.getName.startsWith(MODEL_PACKAGE_NAME)) getCompareFieldsFromClass(klazz.getSuperclass, fields)
      else fields
    }

    if (this.compareFieldsCache.contains(entity.getClass)) {
      this.compareFieldsCache.get(entity.getClass).get
    } else {
      val fields = getCompareFieldsFromClass(entity.getClass, Nil)
      this.compareFieldsCache += entity.getClass -> fields
      fields
    }
  }

}