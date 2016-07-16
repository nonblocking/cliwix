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

import java.util.regex.Pattern

import at.nonblocking.cliwix.core._
import at.nonblocking.cliwix.core.command.{GetByIdentifierOrPathWithinGroupCommand, GetByIdentifierOrPathCommand}
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.model._
import org.apache.commons.beanutils.PropertyUtils

import scala.beans.BeanProperty

trait ExpressionResolver {

  @throws(classOf[CliwixExpressionException])
  def expressionToStringValue(expression: String, companyId: Long): String

}

class ExpressionResolverImpl extends ExpressionResolver with ExpressionUtils with Reporting {

  val EXPRESSION_PATTERN_WITHOUT_OWNER_GROUP = Pattern.compile("\\{\\{(\\w+)\\(['\"]([^\\)'\"]+)['\"]\\)\\.(\\w+)\\}\\}")
  val EXPRESSION_PATTERN_WITH_OWNER_GROUP = Pattern.compile("\\{\\{(\\w+)\\((\\w+)\\(['\"]([^\\)'\"]+)['\"]\\),\\s*['\"]([^\\)'\"]+)['\"]\\)\\.(\\w+)\\}\\}")
  val EXPRESSION_PATTERN_WITH_OWNER_GROUP_ZERO = Pattern.compile("\\{\\{(\\w+)\\(" + EXPRESSION_GROUP_ID_ZERO + ",\\s*['\"]([^\\)'\"]+)['\"]\\)\\.(\\w+)\\}\\}")

  @BeanProperty
  var handler: DispatchHandler = _

  override def expressionToStringValue(expression: String, companyId: Long) = {
    val expr = parse(expression)

    val entityClassName = classOf[Company].getPackage.getName + "." + expr.entityName
    val entityClass =
      try {
        Class.forName(entityClassName)
      } catch {
        case e: ClassNotFoundException => throw new CliwixExpressionException(s"Invalid Expression '$expression")
      }

    val groupId = determineGroupId(expression, expr, companyId)
    val entity =
      if (groupId.isDefined) {
        this.handler.execute(GetByIdentifierOrPathWithinGroupCommand(expr.identifier, companyId, groupId.get, entityClass.asInstanceOf[Class[_ <: GroupMember]])).result
      } else {
        this.handler.execute(GetByIdentifierOrPathCommand(expr.identifier, companyId, entityClass.asInstanceOf[Class[_ <: CompanyMember]])).result
      }

    if (entity == null) throw new CliwixExpressionException(s"Invalid Expression '$expression (no entity with identifier '${expr.identifier}' has been found)")

    try {
      String.valueOf(PropertyUtils.getProperty(entity, expr.propertyName))
    } catch {
      case e: Exception => throw new CliwixExpressionException(s"Invalid Expression '$expression (no property '${expr.propertyName}' has been found)", e)
    }
  }

  def determineGroupId(expression: String, expr: Expression, companyId: Long): Option[Long] = {
    if (expr.ownerGroupEntityName != null) {
      if (expr.ownerGroupEntityName == EXPRESSION_GROUP_ID_ZERO) {
        Some(0)
      } else {
        val ownerEntityClassName = MODEL_PACKAGE_NAME + "." + expr.ownerGroupEntityName
        val ownerEntityClass =
          try {
            Class.forName(ownerEntityClassName)
          } catch {
            case e: ClassNotFoundException => throw new CliwixExpressionException(s"Invalid Expression '$expression")
          }

        val ownerEntity = this.handler.execute(GetByIdentifierOrPathCommand(expr.ownerGroupIdentifier, companyId, ownerEntityClass.asInstanceOf[Class[_ <: CompanyMember]])).result
        if (ownerEntity == null) {
          throw new CliwixExpressionException(s"Invalid Expression '$expression (no group '${expr.ownerGroupEntityName}/${expr.ownerGroupIdentifier}' has been found)")
        } else if (!ownerEntity.isInstanceOf[Group]) {
          throw new CliwixExpressionException(s"Invalid Expression '$expression ('${expr.ownerGroupEntityName}' is no group type)")
        } else {
          Some(ownerEntity.asInstanceOf[Group].getGroupId)
        }
      }

    } else {
      None
    }
  }

  def parse(expression: String): Expression = {
    if (expression.startsWith(EXPRESSION_START_DELIMITER) && expression.contains("(")) {
      val matcher = EXPRESSION_PATTERN_WITHOUT_OWNER_GROUP.matcher(expression)
      if (matcher.matches()) {
        new Expression(matcher.group(1), matcher.group(2), matcher.group(3), null, null)
      } else {
        val matcher = EXPRESSION_PATTERN_WITH_OWNER_GROUP.matcher(expression)
        if (matcher.matches()) {
          new Expression(matcher.group(1), matcher.group(4), matcher.group(5), matcher.group(2), matcher.group(3))
        } else {
          val matcher = EXPRESSION_PATTERN_WITH_OWNER_GROUP_ZERO.matcher(expression)
          if (matcher.matches()) {
            new Expression(matcher.group(1), matcher.group(2), matcher.group(3), EXPRESSION_GROUP_ID_ZERO, EXPRESSION_GROUP_ID_ZERO)
          } else {
            throw new CliwixExpressionException(s"Invalid Expression '$expression'")
          }
      }
      }
    } else {
      throw new CliwixExpressionException(s"Invalid Expression '$expression'")
    }
  }

  case class Expression(entityName: String, identifier: String, propertyName: String, ownerGroupEntityName: String, ownerGroupIdentifier: String)

}
