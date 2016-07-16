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

package at.nonblocking.cliwix.webapp

import javax.inject.{Inject, Named}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.springframework.core.task

import scala.beans.BeanProperty

sealed trait SingleTaskExecutor {
  def execute(taskInfo: TaskInfo, task: () => Unit): Unit

  def getCurrentTask: TaskInfo

  def isTaskRunning: Boolean
}

case class TaskInfo(id: Long, user: String, client: String, clientIP: String, startTime: Long = System.currentTimeMillis())

@Named
class SingleTaskExecutorImpl extends SingleTaskExecutor with LazyLogging {

  @BeanProperty
  @Inject
  var webappConfig: WebappConfig = _

  private var currentTask: TaskInfo = _

  @Inject
  @BeanProperty
  var executor: task.AsyncTaskExecutor = _

  override def execute(taskInfo: TaskInfo, task: () => Unit) = {
    if (this.currentTask != null) {
      throw new IllegalStateException(this.webappConfig.getMessage("error.only.one.importexport.at.a.time"))
    }

    this.currentTask = taskInfo

    this.executor.submit(new Runnable {
      override def run(): Unit = {
        try {
          logger.info(s"Executing task $currentTask")
          task()
        } finally {
          logger.info(s"Task finished: $currentTask")
          currentTask = null
        }
      }
    })
  }

  override def getCurrentTask = this.currentTask

  override def isTaskRunning = this.currentTask != null

}
