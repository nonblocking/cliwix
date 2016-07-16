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

import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.core.{Reporting, ExecutionContext}
import at.nonblocking.cliwix.core.expression.{ExpressionResolver, ExpressionGenerator, ExpressionUtils, CliwixExpressionException}
import at.nonblocking.cliwix.model.DocumentLibraryFolder
import com.typesafe.scalalogging.slf4j.LazyLogging

private[interceptor] trait ReplaceDocumentUrlsUtil extends ExpressionUtils with Reporting with LazyLogging {

  def resolveDocumentUrlExpressions(companyId: Long, content: String, expressionResolver: ExpressionResolver, location: => String) = {
    val matcher = EXPRESSION_PATTERN.matcher(content)
    var newContent = content

    while (matcher.find()) {
      val expression = matcher.group(1)

      try {
        val resolvedValue = expressionResolver.expressionToStringValue(expression, companyId)
        report.addMessage(s"Expression replaced in '$location': $expression -> $resolvedValue'.")
        newContent = newContent.replace(expression, resolvedValue)
      } catch {
        case e: CliwixExpressionException =>
          if (ExecutionContext.flags.ignoreInvalidDocumentReferences)
            report.addWarning(s"Document reference couldn't be resolved: $expression")
          else
            throw e
        case e: Throwable => throw e
      }
    }

    newContent
  }

  def createDocumentUrlExpressions(content: String,  groupUtil: GroupUtil, expressionGenerator: ExpressionGenerator) = {
    val matcher = DOCUMENT_URL_PATTERN.matcher(content)
    var newContent = content

    while (matcher.find()) {
      val groupId = matcher.group(1)
      val folderId = matcher.group(2)
      val fileName = matcher.group(3)
      val rest = matcher.group(4)

      val originalUrl =
        if (rest != null) "/documents/" + groupId + "/" + folderId + "/" + fileName + rest
        else "/documents/" + groupId + "/" + folderId + "/" + fileName

      val groupEntityAndId = groupUtil.getLiferayEntityForGroupId(groupId.toLong)
      val groupIdExpression = if (groupEntityAndId.isDefined) expressionGenerator.createExpression(groupEntityAndId.get._2, "groupId", groupEntityAndId.get._1)
      else None

      val folderIdExpression = expressionGenerator.createExpression(folderId.toLong, "folderId", classOf[DocumentLibraryFolder])

      if (groupIdExpression.isDefined && folderIdExpression.isDefined) {
        val neutralExpression = "/documents/" + groupIdExpression.get + "/" + folderIdExpression.get + "/" + fileName
        logger.debug(s"Replacing $originalUrl by $neutralExpression")
        newContent = newContent.replace(originalUrl, neutralExpression)
      } else {
        report.addWarning(s"Unable to replace document URL '$originalUrl' by an instance neutral expression!")
      }
    }

    newContent
  }


}
