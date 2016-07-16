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

import java.io.{FileInputStream, File}

import at.nonblocking.cliwix.core._
import at.nonblocking.cliwix.core.expression.{ExpressionUtils, ExpressionResolver, ExpressionGenerator}
import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.model._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.beans.BeanProperty

import scala.collection.JavaConversions._
import scala.xml.{XML, Elem}

class ReplaceIdsInPortletPreferencesInterceptor extends ListProcessingInterceptor[PortletConfiguration, PortletConfigurations] with ExpressionUtils with Reporting with LazyLogging {

  @BeanProperty
  var expressionGenerator: ExpressionGenerator = _

  @BeanProperty
  var expressionResolver: ExpressionResolver = _

  @BeanProperty
  var groupUtil: GroupUtil = _

  private val CUSTOM_REPLACEMENTS_FILE_NAME = "portletPreferencesReplacements.xml"

  private val DEFAULT_CONFIG =
    <PortletPreferencesReplacements>
      <PortletPreferencesReplacement>
        <Key>groupId</Key>
        <Type>Group</Type>
        <PropertyName>groupId</PropertyName>
        <Multiple>false</Multiple>
        <Delimiter></Delimiter>
        <SkipOnFailure>true</SkipOnFailure>
      </PortletPreferencesReplacement>
      <PortletPreferencesReplacement>
        <Key>displayStyleGroupId</Key>
        <Type>Group</Type>
        <PropertyName>groupId</PropertyName>
        <Multiple>false</Multiple>
        <Delimiter></Delimiter>
        <SkipOnFailure>true</SkipOnFailure>
      </PortletPreferencesReplacement>
      <PortletPreferencesReplacement>
        <Key>classNameId</Key>
        <Type>ClassName</Type>
        <PropertyName>classNameId</PropertyName>
        <Multiple>false</Multiple>
        <Delimiter></Delimiter>
        <SkipOnFailure>true</SkipOnFailure>
      </PortletPreferencesReplacement>
      <PortletPreferencesReplacement>
        <Key>anyAssetType</Key>
        <Type>ClassName</Type>
        <PropertyName>classNameId</PropertyName>
        <Multiple>false</Multiple>
        <Delimiter></Delimiter>
        <SkipOnFailure>true</SkipOnFailure>
      </PortletPreferencesReplacement>
      <PortletPreferencesReplacement>
        <Key>classNameIds</Key>
        <Type>ClassName</Type>
        <PropertyName>classNameId</PropertyName>
        <Multiple>true</Multiple>
        <Delimiter>splitter:at.nonblocking.cliwix.core.interceptor.PortletConfigurationIdSplitterClassNameIdsImpl</Delimiter>
        <SkipOnFailure>true</SkipOnFailure>
      </PortletPreferencesReplacement>
      <PortletPreferencesReplacement>
        <Key>classTypeIdsDLFileEntryAssetRendererFactory</Key>
        <Type>DocumentLibraryFileType</Type>
        <PropertyName>fileEntryTypeId</PropertyName>
        <Multiple>true</Multiple>
        <Delimiter>,</Delimiter>
        <SkipOnFailure>true</SkipOnFailure>
      </PortletPreferencesReplacement>
    </PortletPreferencesReplacements>

  lazy val replacements: Map[String, PortletPreferencesReplacement] = {
    val defaultReplacements = parseReplacements(DEFAULT_CONFIG)

    val customProperties = if (System.getProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY) == null) null
      else new File(System.getProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY) + "/" + CUSTOM_REPLACEMENTS_FILE_NAME)
    if (customProperties != null && customProperties.exists()) {
      logger.info("Loading custom portletPreferences replacements from: {}", customProperties.getAbsolutePath)
      val customReplacements = parseReplacements(XML.load(new FileInputStream(customProperties)))
      defaultReplacements ++ customReplacements
    } else {
      defaultReplacements
    }
  }

  override def beforeEntityInsert(pc: PortletConfiguration, companyId: Long) = {
    if (pc != null && pc.getPreferences != null) pc.getPreferences.foreach { pref =>
      if (this.replacements.contains(pref.getName)) checkForExpressions(pref, this.replacements.get(pref.getName.trim).get, companyId)
    }
  }

  override def beforeEntityUpdate(pc: PortletConfiguration, existingPc: PortletConfiguration, companyId: Long) = {
    if (pc != null && pc.getPreferences != null) pc.getPreferences.foreach { pref =>
      if (this.replacements.contains(pref.getName)) checkForExpressions(pref, this.replacements.get(pref.getName.trim).get, companyId)
    }
  }

  override def afterEntityExport(pc: PortletConfiguration, companyId: Long) = {
    if (pc != null && pc.getPreferences != null) pc.getPreferences.foreach{ pref =>
      if (this.replacements.contains(pref.getName.trim)) tryReplaceByExpression(pref, this.replacements.get(pref.getName.trim).get)
    }
  }

  private def tryReplaceByExpression(pref: Preference, replacement: PortletPreferencesReplacement) = {
    if (pref.getValue != null) {
      pref.setValue(tryReplaceValueByExpression(pref.getName, pref.getValue, replacement))
    } else if (pref.getValues != null) {
      pref.setValues(pref.getValues.map(value => tryReplaceValueByExpression(pref.getName, value, replacement)))
    }
  }

  private def tryReplaceValueByExpression(name: String, value: String, replacement: PortletPreferencesReplacement): String = {
    val values =
      if (replacement.multiple) {
        if (replacement.splitter == null) value.split(replacement.delimiter).filter(v => v != null && v.nonEmpty).map(_.trim).toList
        else replacement.splitter.split(value).filter(v => v != null && v.nonEmpty).map(_.trim)
      } else {
        List(value.trim)
      }

    val valuesWithExpressions = values.map { value =>
      val expression =
        if (!value.isEmpty && isAllDigits(value)) {
          if (replacement.entityClass == classOf[Group]) {
            val actualEntityAndId = this.groupUtil.getLiferayEntityForGroupId(value.toLong)
            if (actualEntityAndId.isDefined) {
              this.expressionGenerator.createExpression(actualEntityAndId.get._2, "groupId", actualEntityAndId.get._1)
            } else {
              None
            }
          } else {
            this.expressionGenerator.createExpression(value.toLong, replacement.propertyName, replacement.entityClass)
          }
        } else {
          None
        }

      if (expression.isEmpty) {
        if (!replacement.skipOnFailure) {
          throw new CliwixException(s"Unable to replace id $value in portletPreference $name by an expression of type ${replacement.entityClass}")
        } else {
          logger.warn(s"Unable to replace id $value in portletPreference $name by an expression of type ${replacement.entityClass}")
        }
        value
      } else {
        expression.get
      }
    }

    if (replacement.multiple && replacement.splitter != null) replacement.splitter.merge(valuesWithExpressions)
    else valuesWithExpressions.mkString(replacement.delimiter)
  }

  private def checkForExpressions(pref: Preference, replacement: PortletPreferencesReplacement, companyId: Long) = {
    def replaceExpression(value: String) = {
      var newVal = value
      val matcher = EXPRESSION_PATTERN.matcher(value)
      while (matcher.find()) {
        val expression = matcher.group(0)
        val resolvedValue = this.expressionResolver.expressionToStringValue(expression, companyId)

        report.addMessage(s"Expression replaced in portlet preference '${pref.getName}': $expression -> $resolvedValue'.")
        newVal = newVal.replace(expression, resolvedValue)
      }
      newVal
    }

    if (pref.getValue != null) {
      pref.setValue(replaceExpression(pref.getValue))
    } else if (pref.getValues != null) {
      pref.setValues(pref.getValues.map(replaceExpression))
    }
  }

  private def parseReplacements(xml: Elem) = {
    (xml \ "PortletPreferencesReplacement").map{ replacementXml =>
      val key = (replacementXml \ "Key").text
      val _type = (replacementXml \ "Type").text
      val propertyName = (replacementXml \ "PropertyName").text
      val multiple = (replacementXml \ "Multiple").text.toLowerCase == "true"
      val delimiter = (replacementXml \ "Delimiter").text
      val skipOnFailure = (replacementXml \ "SkipOnFailure").text.toLowerCase != "false"

      if (key.isEmpty || _type.isEmpty) throw new CliwixException("Invalid portletPreferencesReplacements configuration: Key and Type must not be empty! (" + xml.toString + ")")
      if (multiple && delimiter.isEmpty) throw new CliwixException("Invalid portletPreferencesReplacements configuration: Delimiter must not be empty if Multiple is true! (" + xml.toString + ")")

      val entityClassName = MODEL_PACKAGE_NAME + "." + _type
      val entityClass =
      try {
        Class.forName(entityClassName).asInstanceOf[Class[_ <: LiferayEntity]]
      } catch {
        case e: ClassNotFoundException =>
          throw new CliwixException("Invalid portletPreferencesReplacements configuration: " + xml.toString, e)
      }

      val splitter: PortletConfigurationIdSplitter = if (delimiter.startsWith("splitter:")) {
        try {
          val splitterClass = Class.forName(delimiter.substring("splitter:".length)).asInstanceOf[Class[_ <: PortletConfigurationIdSplitter]]
          splitterClass.newInstance()
        } catch {
          case e: ClassNotFoundException =>
            throw new CliwixException("Invalid portletPreferencesReplacements configuration: " + xml.toString, e)
        }
      } else {
        null
      }

      key -> new PortletPreferencesReplacement(key, entityClass, propertyName, multiple, delimiter, splitter, skipOnFailure)

    }.toMap
  }

  private def isAllDigits(x: String) = x forall Character.isDigit

  case class PortletPreferencesReplacement(key: String, entityClass: Class[_ <: LiferayEntity], propertyName: String, multiple: Boolean = false, delimiter: String = null, splitter: PortletConfigurationIdSplitter, skipOnFailure: Boolean = true)

}
