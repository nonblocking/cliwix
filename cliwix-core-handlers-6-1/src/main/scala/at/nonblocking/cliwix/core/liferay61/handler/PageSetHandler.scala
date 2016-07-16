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

package at.nonblocking.cliwix.core.liferay61.handler

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.PageSet
import com.liferay.portal.NoSuchLayoutSetException
import com.liferay.portal.service._

import scala.beans.BeanProperty

class PageSetReadHandler extends Handler[PageSetReadCommand, PageSet] {

  @BeanProperty
  var layoutSetService: LayoutSetLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: PageSetReadCommand): CommandResult[PageSet] = {
    var cliwixPageSet: PageSet = null

    try {
      val layoutSet = this.layoutSetService.getLayoutSet(command.groupId, command.privatePages)
      cliwixPageSet = this.converter.convertToCliwixPageSet(layoutSet)
      logger.debug("Export layoutSet with id={}", layoutSet.getLayoutSetId.toString)
    } catch {
      case e: NoSuchLayoutSetException => cliwixPageSet = null
      case e: Throwable => throw e
    }

    CommandResult(cliwixPageSet)
  }
}

class PageSetInsertHandler extends Handler[PageSetInsertCommand, PageSet] {

  @BeanProperty
  var layoutSetService: LayoutSetLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: PageSetInsertCommand): CommandResult[PageSet] = {
    val cliwixPageSet = command.pageSet

    logger.debug("Adding pageSet: {}", cliwixPageSet)

    val insertedLayoutSet = this.layoutSetService.addLayoutSet(command.groupId, command.privatePageSet)
    this.converter.mergeToLiferayLayoutSet(cliwixPageSet, insertedLayoutSet)
    val insertedAndUpdatedLayoutSet = this.layoutSetService.updateLayoutSet(insertedLayoutSet)

    val insertedCliwixPageSet = this.converter.convertToCliwixPageSet(insertedAndUpdatedLayoutSet)
    CommandResult(insertedCliwixPageSet)
  }

}

class PageSetUpdateHandler extends Handler[UpdateCommand[PageSet], PageSet] {

  @BeanProperty
  var layoutSetService: LayoutSetLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[PageSet]): CommandResult[PageSet] = {
    val cliwixPageSet = command.entity
    assert(cliwixPageSet.getPageSetId != null, "pageSetId != null")

    logger.debug("Updating pageSet: {}", cliwixPageSet)

    val layoutSet = this.layoutSetService.getLayoutSet(cliwixPageSet.getPageSetId)
    this.converter.mergeToLiferayLayoutSet(cliwixPageSet, layoutSet)
    val updatedLayoutSet = this.layoutSetService.updateLayoutSet(layoutSet)

    val updateCliwixPageSet = this.converter.convertToCliwixPageSet(updatedLayoutSet)
    CommandResult(updateCliwixPageSet)
  }

}

class PageSetDeleteHandler extends Handler[DeleteCommand[PageSet], PageSet] {

  @BeanProperty
  var layoutSetService: LayoutSetLocalService = _

  override private[core] def handle(command: DeleteCommand[PageSet]): CommandResult[PageSet] = {
    logger.debug("Deleting pageSet: {}", command.entity)

    //In some Liferay versions deleteLayoutSet() returns a LayoutSet in some not,
    //so we must use reflection here
    val deleteLayoutSetMethod = this.layoutSetService.getClass.getMethod("deleteLayoutSet", classOf[Long])
    deleteLayoutSetMethod.invoke(this.layoutSetService, command.entity.getPageSetId)

    CommandResult(null)
  }

}