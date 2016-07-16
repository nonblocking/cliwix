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

package at.nonblocking.cliwix.core.expression

import at.nonblocking.cliwix.core.Reporting
import at.nonblocking.cliwix.core.command.GetByDBIdCommand
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.model.{GroupMember, LiferayEntityWithUniquePathIdentifier, LiferayEntity}
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty

trait ExpressionGenerator {
  def createExpression(dbId: Long, propertyName: String, entityClass: Class[_ <: LiferayEntity]): Option[String]
}

class ExpressionGeneratorImpl extends ExpressionGenerator with ExpressionUtils with LazyLogging with Reporting {

  @BeanProperty
  var handler: DispatchHandler = _

  @BeanProperty
  var groupUtil: GroupUtil = _

  override def createExpression(dbId: Long, propertyName: String, entityClass: Class[_ <: LiferayEntity]) = {
    val entity = this.handler.execute(GetByDBIdCommand(dbId, entityClass)).result
    if (entity == null) {
      report.addWarning(s"Unable to replace '$dbId' by an expression, because an ${entityClass.getSimpleName} with this DB ID doesn't exist.")
      None
    } else {
      val naturalIdentifier = entity match {
        case pathItem: LiferayEntityWithUniquePathIdentifier => pathItem.getPath
        case e => e.identifiedBy()
      }

      entity match {
        case groupMember: GroupMember =>
          assert(groupMember.getOwnerGroupId != null, s"ownerGroupId != null for entity: $entity")

          val ownerGroupId = groupMember.getOwnerGroupId
          if (ownerGroupId == 0) {

            Some(EXPRESSION_START_DELIMITER + entityClass.getSimpleName + "(" + EXPRESSION_GROUP_ID_ZERO + ",'" + naturalIdentifier + "')." + propertyName + EXPRESSION_END_DELIMITER)

          } else {
            val ownerEntityClassAndId = this.getGroupUtil.getLiferayEntityForGroupId(ownerGroupId)
            if (ownerEntityClassAndId.isDefined) {
              val ownerEntityClass = ownerEntityClassAndId.get._1.asInstanceOf[Class[LiferayEntity]]
              val ownerEntity = this.handler.execute(new GetByDBIdCommand[LiferayEntity](ownerEntityClassAndId.get._2, ownerEntityClass)).result
              val ownerEntityExpression = ownerEntity.getClass.getSimpleName + "('" + ownerEntity.identifiedBy() + "')"

              Some(EXPRESSION_START_DELIMITER + entityClass.getSimpleName + "(" + ownerEntityExpression + ",'" + naturalIdentifier + "')." + propertyName + EXPRESSION_END_DELIMITER)

            } else {
              report.addWarning(s"Unable to replace DB ID '$dbId' by an expression, since an owner group with id $ownerGroupId doesn't exist.")
              None
            }
          }
        case _ =>
          Some(EXPRESSION_START_DELIMITER + entityClass.getSimpleName + "('" + naturalIdentifier + "')." + propertyName + EXPRESSION_END_DELIMITER)
      }
    }
  }
}

