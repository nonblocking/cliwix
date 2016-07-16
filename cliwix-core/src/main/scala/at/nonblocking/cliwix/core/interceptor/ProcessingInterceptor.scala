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

package at.nonblocking.cliwix.core.interceptor

import at.nonblocking.cliwix.model.{TreeType, ListType, LiferayEntity}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.springframework.beans.factory.InitializingBean

import scala.beans.BeanProperty
import scala.collection.mutable

import java.{util=>jutil}
import scala.collection.JavaConversions._

sealed trait ProcessingInterceptor[E <: LiferayEntity] {
  def beforeEntityInsert(entity: E, companyId: Long): Unit = {}
  def beforeEntityUpdate(entity: E, existingEntity: E, companyId: Long): Unit = {}
  def afterEntityExport(entity: E, companyId: Long): Unit = {}
  def entityClass: Class[E]
}

abstract class ListProcessingInterceptor[E <: LiferayEntity: Manifest, L <: ListType[E]: Manifest] extends ProcessingInterceptor[E] {
  def afterListExport(list: L, companyId: Long): Unit = {}
  def beforeListImport(list: L, companyId: Long): Unit = {}

  override def entityClass = manifest[E].runtimeClass.asInstanceOf[Class[E]]
  def listClass = manifest[L].runtimeClass.asInstanceOf[Class[E]]
}

abstract class TreeProcessingInterceptor[E <: LiferayEntity: Manifest, T <: TreeType[E]: Manifest] extends ProcessingInterceptor[E] {
  def afterTreeExport(tree: T, companyId: Long): Unit = {}
  def beforeTreeImport(tree: T, companyId: Long): Unit = {}

  override def entityClass = manifest[E].runtimeClass.asInstanceOf[Class[E]]
  def treeClass = manifest[T].runtimeClass.asInstanceOf[Class[E]]
}

class ProcessingInterceptorDispatcher extends InitializingBean with LazyLogging {

  type InterceptorList = mutable.MutableList[ProcessingInterceptor[_]]
  type InterceptorMap = mutable.HashMap[Class[_], InterceptorList]

  @BeanProperty
  var interceptors: jutil.List[ProcessingInterceptor[_]] = _

  private val interceptorMap = new InterceptorMap
  private val listInterceptorMap = new InterceptorMap
  private val treeInterceptorMap = new InterceptorMap

  def beforeEntityInsert[E <: LiferayEntity](entity: E, companyId: Long) = {
    if (entity != null) matchingEntityInterceptors(entity).foreach{ interceptor =>
      logger.debug(s"Executing ${interceptor.getClass.getSimpleName}.beforeEntityInsert")
      interceptor.asInstanceOf[ProcessingInterceptor[E]].beforeEntityInsert(entity, companyId)
    }
  }

  def beforeEntityUpdate[E <: LiferayEntity](entity: E, existingEntity: E, companyId: Long) = {
    if (entity != null) matchingEntityInterceptors(entity).foreach{ interceptor =>
      logger.debug(s"Executing ${interceptor.getClass.getSimpleName}.beforeEntityUpdate")
      interceptor.asInstanceOf[ProcessingInterceptor[E]].beforeEntityUpdate(entity, existingEntity, companyId)
    }
  }

  def afterEntityExport[E <: LiferayEntity](entity: E, companyId: Long) = {
    if (entity != null) matchingEntityInterceptors(entity).foreach{ interceptor =>
      logger.debug(s"Executing ${interceptor.getClass.getSimpleName}.afterEntityExport")
      interceptor.asInstanceOf[ProcessingInterceptor[E]].afterEntityExport(entity, companyId)
    }
  }

  def afterListExport[L <: ListType[_]](list: L, companyId: Long) = {
    if (list != null) this.listInterceptorMap.getOrElse(list.getClass, new InterceptorList()).foreach{ interceptor =>
      logger.debug(s"Executing ${interceptor.getClass.getSimpleName}.afterListExport")
      interceptor.asInstanceOf[ListProcessingInterceptor[_,L]].afterListExport(list, companyId)
    }
  }

  def beforeListImport[L <: ListType[_]](list: L, companyId: Long) = {
    if (list != null) this.listInterceptorMap.getOrElse(list.getClass, new InterceptorList()).foreach{ interceptor =>
      logger.debug(s"Executing ${interceptor.getClass.getSimpleName}.beforeListImport")
      interceptor.asInstanceOf[ListProcessingInterceptor[_,L]].beforeListImport(list, companyId)
    }
  }

  def afterTreeExport[T <: TreeType[_]](tree: T, companyId: Long) = {
    if (tree != null) this.treeInterceptorMap.getOrElse(tree.getClass, new InterceptorList()).foreach{ interceptor =>
      logger.debug(s"Executing ${interceptor.getClass.getSimpleName}.afterTreeExport")
      interceptor.asInstanceOf[TreeProcessingInterceptor[_,T]].afterTreeExport(tree, companyId)
    }
  }

  def beforeTreeImport[T <: TreeType[_]](tree: T, companyId: Long) = {
    if (tree != null) this.treeInterceptorMap.getOrElse(tree.getClass, new InterceptorList()).foreach{ interceptor =>
      logger.debug(s"Executing ${interceptor.getClass.getSimpleName}.beforeTreeImport")
      interceptor.asInstanceOf[TreeProcessingInterceptor[_,T]].beforeTreeImport(tree, companyId)
    }
  }

  def afterPropertiesSet() = this.interceptors.foreach{ interceptor =>
    logger.debug(s"Adding interceptor: $interceptor")
    addBinding(this.interceptorMap, interceptor.entityClass, interceptor)
    interceptor match {
      case listInterceptor: ListProcessingInterceptor[_, _] =>
        addBinding(listInterceptorMap, listInterceptor.listClass, interceptor)
      case treeInterceptor: TreeProcessingInterceptor[_, _] =>
        addBinding(treeInterceptorMap, treeInterceptor.treeClass, interceptor)
    }
  }

  private def addBinding(map: InterceptorMap, key: Class[_], value: ProcessingInterceptor[_]) = {
    val v = map.get(key)
    v match {
      case None =>
        val newInterceptorList = new InterceptorList()
        map.put(key, newInterceptorList)
        newInterceptorList += value
      case Some(interceptorList) =>
        interceptorList += value
    }
  }

  private def matchingEntityInterceptors[E <: LiferayEntity](entity: E) =
    this.interceptorMap
      .filter{ case (entityClass, interceptor) => entityClass.isAssignableFrom(entity.getClass) }
      .values
      .flatten
}