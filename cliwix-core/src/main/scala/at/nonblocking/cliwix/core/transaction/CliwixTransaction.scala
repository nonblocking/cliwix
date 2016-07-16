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

import java.lang.reflect.{InvocationHandler, Proxy, InvocationTargetException, Method}

import at.nonblocking.cliwix.core.Reporting
import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty

class CliwixTransaction extends LazyLogging with Reporting {

  @BeanProperty
  var transactionalLocalService: CliwixTransactionalLocalService = _

  @BeanProperty
  var transactionInterceptor: AnyRef = _

  val threadLocalTransactionRunning = new ThreadLocal[Boolean]

  def executeWithinLiferayTransaction[T](readOnly: Boolean = false, rollback: Boolean = false)(closure: => T): T = {
    if (!this.threadLocalTransactionRunning.get()) {
      executeWithinNewTransaction(readOnly, rollback, closure)
    } else {
      executeWithinExistingTransaction(closure)
    }
  }

  private def executeWithinNewTransaction[T](readOnly: Boolean = false, rollback: Boolean = false, closure: => T): T = {
    this.threadLocalTransactionRunning.set(true)

    val task = createTask(closure)

    logger.debug("Starting new transaction")
    if (rollback) report.addWarning("Simulation Mode. No data will actually be written to the database.")

    try {
      //Since the transactionInterceptor was loaded by the Liferay classloader, we have to make sure,
      //that all passed arguments are objects of classes loaded by the Liferay classloader as well
      val methodInvocationClassFromLiferay = PortalBeanLocatorUtil.getBeanLocator.getClassLoader.loadClass("org.aopalliance.intercept.MethodInvocation")
      val handler = new CliwixInvokeWithinTransaction(task, readOnly, rollback)
      val methodInvocationProxyLiferayClassLoader = Proxy.newProxyInstance(
        PortalBeanLocatorUtil.getBeanLocator.getClassLoader,
        Array(methodInvocationClassFromLiferay), handler)

      val invokeMethod = this.transactionInterceptor.getClass.getMethod("invoke", methodInvocationClassFromLiferay)
      val result = invokeMethod.invoke(this.transactionInterceptor, methodInvocationProxyLiferayClassLoader)

      if (readOnly) {
        logger.debug("Readonly transaction completed")
      } else {
        logger.debug("Transaction committed")
      }

      result.asInstanceOf[T]

    } catch {
      case e: InvocationTargetException =>
        e.getCause match {
          case re: CliwixRollbackException =>
            logger.info("Transaction rolled back (wanted)")
            null.asInstanceOf[T]
          case t: Throwable =>
            if (!readOnly) {
              report.addMessage("Transaction rolled back.")
              logger.info("Transaction rolled back")
            }
            throw t
        }
      case t: Throwable =>
        if (!readOnly) {
          report.addMessage("Transaction rolled back.")
          logger.info("Transaction rolled back")
        }
        throw t

    } finally {
      this.threadLocalTransactionRunning.set(false)
    }
  }

  private def executeWithinExistingTransaction[T](closure: => T): T = {
    closure
  }

  private def createTask[T](closure: => T) =
    new CliwixTransactionalTask[T] {
      override def execute(): T = closure
    }

  private class CliwixInvokeWithinTransaction[T](task: CliwixTransactionalTask[T], readOnly: Boolean, rollback: Boolean) extends InvocationHandler {

    override def invoke(proxy: Any, method: Method, args: Array[AnyRef]) = {
      method.getName match {
        case "getMethod" =>
          if (readOnly)
            classOf[CliwixTransactionalLocalServiceImpl].getMethod("executeTransactionalReadonly", classOf[CliwixTransactionalTask[T]])
          else
            classOf[CliwixTransactionalLocalServiceImpl].getMethod("executeTransactional", classOf[Boolean], classOf[CliwixTransactionalTask[T]])
        case "getThis" =>
          transactionalLocalService
        case "proceed" =>
          if (readOnly)
            transactionalLocalService.executeTransactionalReadonly(task).asInstanceOf[AnyRef]
          else
            transactionalLocalService.executeTransactional(rollback, task).asInstanceOf[AnyRef]
        case _ => throw new AssertionError(s"Method org.aopalliance.intercept.MethodInvocation ${method.getName} will not be called")
      }
    }
  }

}
