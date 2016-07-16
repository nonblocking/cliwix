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

package at.nonblocking.cliwix.integrationtest

import java.io.{File, FilenameFilter}
import java.net.{URL, URLClassLoader}
import java.util.Properties

import at.nonblocking.cliwix.core.{ExecutionContextFlags, ExecutionContext, CliwixTestContext}
import at.nonblocking.cliwix.core.transaction.CliwixTransaction
import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil
import com.liferay.portal.kernel.messaging.sender.{MessageSender, SynchronousMessageSender}
import com.liferay.portal.kernel.messaging.{MessageBus, MessageBusUtil}
import com.liferay.portal.kernel.workflow.WorkflowHandlerRegistryUtil
import com.liferay.portal.service.CompanyLocalServiceUtil
import com.liferay.portal.tools.DBUpgrader
import com.liferay.portal.util.InitUtil
import com.liferay.portlet.directory.workflow.UserWorkflowHandler
import com.liferay.portlet.documentlibrary.workflow.DLFileEntryWorkflowHandler
import com.liferay.portlet.journal.workflow.JournalArticleWorkflowHandler
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.junit.internal.runners.statements.InvokeMethod
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.{Statement, FrameworkMethod}
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

import scala.reflect.runtime.universe._

case class TransactionalRollback() extends scala.annotation.StaticAnnotation

case class TransactionalCommit() extends scala.annotation.StaticAnnotation

case class ExplicitExecutionContext() extends scala.annotation.StaticAnnotation

private[integrationtest] class CliwixIntegrationTestRunner(klass: Class[_]) extends BlockJUnit4ClassRunner(klass) {

  override def createTest = {
    LiferayTestContext.instantiateAndAutowire(getTestClass.getJavaClass)
  }

  override def methodInvoker(method: FrameworkMethod, target: AnyRef) = {
    val scalaMethod = determineScalaMethod(method.getMethod, target)

    if (scalaMethod.annotations.exists(_.tree.tpe == typeOf[TransactionalRollback]))
      new MethodInvokerWithTransaction(method, target, LiferayTestContext.txManager, true)
    else if (scalaMethod.annotations.exists(_.tree.tpe == typeOf[TransactionalCommit]))
      new MethodInvokerWithTransaction(method, target, LiferayTestContext.txManager, false)
    else
      new InvokeMethod(method, target)
  }

  override def withBefores(method: FrameworkMethod, target: scala.Any, statement: Statement) = {
    val befores = super.withBefores(method, target, statement)

    new Statement {
      override def evaluate() = {
        if (!explicitExecutionContext) {
          ExecutionContext.init()
        }
        befores.evaluate()
      }
    }
  }

  override def withAfters(method: FrameworkMethod, target: scala.Any, statement: Statement) = {
    val afters = super.withAfters(method, target, statement)

    new Statement {
      override def evaluate() = {
        try {
          afters.evaluate()
        } finally {
          if (!explicitExecutionContext) {
            ExecutionContext.destroy()
          }
        }
      }
    }
  }

  private def explicitExecutionContext = {
    val scalaClass = determineScalaClass(getTestClass.getJavaClass)
    scalaClass.annotations.exists(_.tree.tpe == typeOf[ExplicitExecutionContext])
  }

  private def determineScalaClass(testClass: Class[_]) = {
    val mirror = runtimeMirror(testClass.getClassLoader)
    mirror.classSymbol(testClass)
  }

  private def determineScalaMethod(method: java.lang.reflect.Method, target: AnyRef) = {
    val scalaClassSymbol = determineScalaClass(target.getClass)
    scalaClassSymbol.toType.decls.find(d => d.name.toString == method.getName).get
  }
}

private object LiferayTestContext extends LazyLogging {

  val context = createContext()
  val txManager = context.getBean(classOf[CliwixTransaction])

  def instantiateAndAutowire(klazz: Class[_]) = {
    context.getAutowireCapableBeanFactory.autowire(klazz, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true)
  }

  def isLiferayAvailable = CompanyLocalServiceUtil.getService != null

  private def createContext() = {
    val testProperties = new Properties()
    testProperties.load(getClass.getResourceAsStream("/test.properties"))

    val liferayTomcatFolder = testProperties.getProperty("cliwix.test.liferay.tomcat.folder")
    assert(liferayTomcatFolder != null, "Property cliwix.test.liferay.tomcat.folder exists in test.properties")

    val liferayLibFolder = new File(liferayTomcatFolder + "/webapps/ROOT/WEB-INF/lib")

    setClasspath(liferayLibFolder)

    System.setProperty("external-properties", "test.properties")

    InitUtil.initWithSpring()

    DBUpgrader.upgrade()

    fixTestEnvironment()

    DBUpgrader.verify()

    CliwixTestContext.getContext
  }

  private def fixTestEnvironment() = {
    //Workflows
    WorkflowHandlerRegistryUtil.register(new UserWorkflowHandler)
    WorkflowHandlerRegistryUtil.register(new DLFileEntryWorkflowHandler)
    WorkflowHandlerRegistryUtil.register(new JournalArticleWorkflowHandler)

    //Message Bus
    val messageBus = PortalBeanLocatorUtil.locate(classOf[MessageBus].getName).asInstanceOf[MessageBus]
    val messageSender = PortalBeanLocatorUtil.locate(classOf[MessageSender].getName).asInstanceOf[MessageSender]
    val synchronousMessageSender = PortalBeanLocatorUtil.locate(classOf[SynchronousMessageSender].getName).asInstanceOf[SynchronousMessageSender]

    MessageBusUtil.init(messageBus, messageSender, synchronousMessageSender)
  }

  private def setClasspath(liferayLibFolder: File) = {
    val libraryDirs = List(liferayLibFolder)

    libraryDirs.foreach { dir =>
      assert(dir.exists(), s"Folder ${dir.getAbsolutePath} exists")
    }

    val classLoader = Thread.currentThread().getContextClassLoader.asInstanceOf[URLClassLoader]
    val addURLMethod = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
    addURLMethod.setAccessible(true)

    val jars = libraryDirs.flatMap(_.listFiles(new FilenameFilter {
      def accept(dir: File, name: String): Boolean = name.endsWith(".jar")
    }))

    jars.foreach { jar =>
      println(s"Add to classpath: ${jar.getAbsolutePath}")
      addURLMethod.invoke(classLoader, jar.toURI.toURL)
    }
  }

}

private class MethodInvokerWithTransaction(testMethod: FrameworkMethod, target: AnyRef, tx: CliwixTransaction, rollback: Boolean)
  extends InvokeMethod(testMethod, target) with LazyLogging {

  override def evaluate(): Unit = {
    tx.executeWithinLiferayTransaction(readOnly = false, rollback = rollback)(super.evaluate())
  }

}