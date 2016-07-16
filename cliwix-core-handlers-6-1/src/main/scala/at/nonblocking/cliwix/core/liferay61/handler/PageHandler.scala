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

import java.{util => jutil}

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.Page
import com.liferay.portal.kernel.util.LocaleUtil
import com.liferay.portal.model.LayoutConstants
import com.liferay.portal.service._
import com.liferay.portal.NoSuchLayoutException

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

class PageListHandler extends Handler[PageListCommand, jutil.List[Page]] {

  @BeanProperty
  var layoutService: LayoutLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  private[core] override def handle(command: PageListCommand): CommandResult[jutil.List[Page]] = {
    def addSubPages(page: Page): Unit = {
      val subLayouts = this.layoutService.getLayouts(command.groupId, command.privatePages, page.getPageId)
        .sortBy(_.getPriority)

      subLayouts.foreach{ subLayout =>
        val url: String = subLayout.getFriendlyURL
        logger.debug("Export layout: {}", url)

        val subPage = this.converter.convertToCliwixPage(subLayout)
        if (page.getSubPages == null) page.setSubPages(new jutil.ArrayList())
        page.getSubPages.add(subPage)
        addSubPages(subPage)
      }
    }

    val rootLayouts = this.layoutService.getLayouts(command.groupId, command.privatePages, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID)
      .sortBy(_.getPriority)

    val cliwixRootLayouts = rootLayouts.map { rootLayout =>
      val url: String = rootLayout.getFriendlyURL
      logger.debug("Export root layout: {}", url)

      val page = this.converter.convertToCliwixPage(rootLayout)
      addSubPages(page)
      page
    }

    CommandResult(new jutil.ArrayList(cliwixRootLayouts))
  }
}

class PageInsertHandler extends Handler[PageInsertCommand, Page] {

  @BeanProperty
  var layoutSetService: LayoutSetLocalService = _

  @BeanProperty
  var layoutService: LayoutLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: PageInsertCommand): CommandResult[Page] = {
    val cliwixPage = command.page

    assert(command.pageSet.getPageSetId != null, "pageSetId != null")
    assert(command.parentPage == null || command.parentPage.getPageId != null, "parent pageId != null")
    assert(cliwixPage.getPageOrder != null, "pageOrder != null")

    val layoutSet = this.layoutSetService.getLayoutSet(command.pageSet.getPageSetId)
    val defaultUser = ExecutionContext.securityContext.defaultUser
    val parentLayoutId: Long = if (command.parentPage != null) command.parentPage.getPageId else LayoutConstants.DEFAULT_PARENT_LAYOUT_ID
    val hidden = false
    val serviceContext = ExecutionContext.createServiceContext()

    val namesMap:jutil.Map[jutil.Locale, String] = {
      val nm = this.converter.toLiferayTextMap(cliwixPage.getNames)
      if (nm != null) nm
      else Map[jutil.Locale, String](LocaleUtil.getDefault -> "cliwix_not_set")
    }

    logger.debug("Adding page: {}", cliwixPage)

    val insertedPage =
      try {
        this.layoutService.addLayout(defaultUser.getUserId, layoutSet.getGroupId, layoutSet.isPrivateLayout, parentLayoutId,
          namesMap, null, null, null, null, cliwixPage.getPageType.getType, hidden, cliwixPage.getFriendlyUrl, serviceContext)
      } catch {
        case e: Throwable =>
          if (e.getClass.getName.contains("LayoutFriendlyURL")) {
            val pageWithGeneratedFriendlyUrl = this.layoutService.addLayout(defaultUser.getUserId, layoutSet.getGroupId, layoutSet.isPrivateLayout, parentLayoutId,
              namesMap, null, null, null, null, cliwixPage.getPageType.getType, hidden, null, serviceContext)
            report.addWarning(s"Duplicate friendly URL for page detected: ${cliwixPage.getFriendlyUrl}. " +
              s"Generated new friendly URL: ${pageWithGeneratedFriendlyUrl.getFriendlyURL} - this might lead to duplicate page entries. Please check!")
            cliwixPage.setFriendlyUrl(pageWithGeneratedFriendlyUrl.getFriendlyURL)
            pageWithGeneratedFriendlyUrl
          } else {
            throw e
          }
      }

    this.converter.mergeToLiferayLayout(cliwixPage, insertedPage)
    val updatedLayout = this.layoutService.updateLayout(insertedPage)

    val insertedCliwixPage = this.converter.convertToCliwixPage(updatedLayout)
    CommandResult(insertedCliwixPage)
  }
}

class PageUpdateHandler extends Handler[UpdateCommand[Page], Page] {

  @BeanProperty
  var layoutService: LayoutLocalService = _

  @BeanProperty
  var groupService: GroupLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: UpdateCommand[Page]): CommandResult[Page] = {
    val cliwixPage = command.entity
    assert(cliwixPage.getPortletLayoutId != null && cliwixPage.getPageId != null, "pageId != null")
    assert(cliwixPage.getPageOrder != null, "pageOrder != null")

    logger.debug("Updating page: {}", cliwixPage)

    val layout = this.layoutService.getLayout(cliwixPage.getPortletLayoutId)
    this.converter.mergeToLiferayLayout(cliwixPage, layout)
    val updatedLayout = this.layoutService.updateLayout(layout)

    val updatedCliwixPage = this.converter.convertToCliwixPage(updatedLayout)
    CommandResult(updatedCliwixPage)
  }

}


class PageGetByIdHandler extends Handler[GetByDBIdCommand[Page], Page] {

  @BeanProperty
  var layoutService: LayoutLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByDBIdCommand[Page]): CommandResult[Page] = {
    try {
      val layout = this.layoutService.getLayout(command.dbId)
      val cliwixPage = this.converter.convertToCliwixPage(layout)
      CommandResult(cliwixPage)
    } catch {
      case e: NoSuchLayoutException =>
        logger.warn(s"No page with plid ${command.dbId} found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class PageGetByPathHandler extends Handler[GetByIdentifierOrPathWithinGroupCommand[Page], Page] {

  @BeanProperty
  var layoutService: LayoutLocalService = _

  @BeanProperty
  var converter: LiferayEntityConverter = _

  override private[core] def handle(command: GetByIdentifierOrPathWithinGroupCommand[Page]): CommandResult[Page] = {
    assert(command.identifierOrPath.contains(":"), "Page path contains a prefix")

    try {
      val naturalIdentifierParts = command.identifierOrPath.split(":")
      val prefix = naturalIdentifierParts(0)
      val friendlyURL = naturalIdentifierParts(1)
      val privatePages = prefix == "privatePages"

      val layout = this.layoutService.getFriendlyURLLayout(command.groupId, privatePages, friendlyURL)
      val cliwixPage = this.converter.convertToCliwixPage(layout)
      CommandResult(cliwixPage)
    } catch {
      case e: NoSuchLayoutException =>
        logger.warn(s"No page with path ${command.identifierOrPath} found", e)
        CommandResult(null)
      case e: Throwable => throw e
    }
  }

}

class PageDeleteHandler extends Handler[DeleteCommand[Page], Page] {

  @BeanProperty
  var layoutService: LayoutLocalService = _

  override private[core] def handle(command: DeleteCommand[Page]): CommandResult[Page] = {
    logger.debug("Removing page: {}", command.entity)

    val serviceContext = ExecutionContext.createServiceContext()

    //In Liferay 6.1.2 the LayoutLocalServiceStagingAdvice rejects all deleteLayout calls except ones with 3 or 4 arguments
    val layout = this.layoutService.getLayout(command.entity.getPortletLayoutId)
    this.layoutService.deleteLayout(layout.getGroupId, layout.isPrivateLayout, layout.getLayoutId, serviceContext)

    CommandResult(null)
  }

}
