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

import at.nonblocking.cliwix.core.Cliwix
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.mapdb.DBMaker

import scala.annotation.tailrec
import scala.util.Random

/**
 * Collections created by this factory have a limited RAM usage
 */
sealed trait ResourceAwareCollectionFactory {

  val listSizeThreshold = 100000

  def createMap[K, V](sizeHint: Long = listSizeThreshold * 2): jutil.Map[K, V];

}

private[core] class ResourceAwareCollectionFactoryImpl extends ResourceAwareCollectionFactory with LazyLogging {

  val availableRamMB = Runtime.getRuntime.maxMemory() / 1024 / 1024

  logger.info(s"Available RAM: $availableRamMB MB")

  def createMap[K, V](sizeHint: Long) = {
    if (sizeHint < listSizeThreshold) {
      new jutil.HashMap
    } else {
      val db = if (availableRamMB < 2048) {
        DBMaker
          .newTempFileDB()
          .transactionDisable()
          .deleteFilesAfterClose()
          .closeOnJvmShutdown()
          .make()
      } else {
        DBMaker
          .newTempFileDB()
          .transactionDisable()
          .mmapFileEnablePartial()
          .deleteFilesAfterClose()
          .closeOnJvmShutdown()
          .make()
      }

      db.getHashMap(randomString)
    }
  }

  def randomString: String = {
    @tailrec
    def randomStringTailRecursive(n: Int, list: List[Char]): List[Char] = {
      if (n == 1) Random.nextPrintableChar :: list
      else randomStringTailRecursive(n - 1, Random.nextPrintableChar :: list)
    }

    randomStringTailRecursive(10, Nil).mkString
  }

}

class ResourceAwareCollectionFactoryDummyImpl extends ResourceAwareCollectionFactory {
  def createMap[K, V](sizeHint: Long) = new jutil.HashMap
}