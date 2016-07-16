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

package at.nonblocking.cliwix.core.liferay61.util

import java.{util => jutil}

import com.liferay.portal.kernel.dao.orm.{SessionFactory, Type}
import com.liferay.portal.kernel.util.InfrastructureUtil
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty
import scala.collection.mutable

trait NativeSqlAccessUtil {
  def scalarList(sqlQueryString: String, scalarTypes: Map[String, Type]): jutil.List[_]

  def scalarUniqueResult(sqlQueryString: String, scalarTypes: Map[String, Type]): Option[AnyRef]

  def eraseAllCompanyData(companyId: Long)
}

class NativeSqlAccessUtilImpl extends NativeSqlAccessUtil with LazyLogging {

  @BeanProperty
  var sessionFactory: SessionFactory = _

  override def scalarList(sqlQueryString: String, scalarTypes: Map[String, Type]): jutil.List[_] = {
    logger.debug(s"Executing SQL query: $sqlQueryString")

    val sqlQuery = this.sessionFactory.openSession().createSQLQuery(sqlQueryString)

    scalarTypes.foreach({ case (key: String, value: Type) =>
      sqlQuery.addScalar(key, value)
    })

    sqlQuery.list()
  }

  override def scalarUniqueResult(sqlQueryString: String, scalarTypes: Map[String, Type]): Option[AnyRef] = {
    logger.debug(s"Executing SQL query: $sqlQueryString")

    val sqlQuery = this.sessionFactory.openSession().createSQLQuery(sqlQueryString)

    scalarTypes.foreach({ case (key: String, value: Type) =>
      sqlQuery.addScalar(key, value)
    })

    try {
      Option(sqlQuery.uniqueResult())
    } catch {
      case _: Throwable => None
    }

  }

  /**
    * Remove all table rows which contain companyId=[companyId] in the database.
    * Necessary for Liferay < 6.2, since removeCompany() does not clean up.
    *
    * @param companyId Long
    */
  override def eraseAllCompanyData(companyId: Long) = {
    val connection = InfrastructureUtil.getDataSource.getConnection
    val hibernateSession = this.sessionFactory.openSession

    val metaData = connection.getMetaData
    val tables = metaData.getTables(null, null, "%", null)
    val tableNames = new mutable.MutableList[String]

    while (tables.next()) tableNames += tables.getString(3)

    tableNames.foreach { tableName =>
      val columnNames = new mutable.MutableList[String]
      val columns = metaData.getColumns(null, null, tableName, null)

      while (columns.next()) columnNames += columns.getString(4)

      if (columnNames.exists(_.equalsIgnoreCase("companyId"))) {
        logger.debug(s"Removing all rows from table '$tableName' belonging to company with ID: $companyId")
        try {
          val rowsRemoved = hibernateSession.createSQLQuery(s"DELETE FROM $tableName WHERE companyId=$companyId").executeUpdate()
          logger.info(s"$rowsRemoved rows removed from table '$tableName' belonging to company with ID: $companyId")
        } catch {
          case e: Throwable =>
            logger.error(s"Failed to clean table $tableName", e)
        }
      }
    }

    connection.close()
  }

}
