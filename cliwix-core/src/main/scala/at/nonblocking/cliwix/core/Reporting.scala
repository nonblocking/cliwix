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

package at.nonblocking.cliwix.core

import java.io.{ByteArrayOutputStream, File, PrintWriter}

import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.collection.mutable
import scala.util.Random
import scala.xml.dtd.DocType
import scala.xml._

/**
 * Mixin for creating HTML reports during Import and Export
 */
trait Reporting {
  val report = Report
}

private[core] object Report extends LazyLogging {

  var currentReport: Report = new Report(null, "implicit")

  def start(fileName: String, title: String) = {
    failsafe{ this.currentReport = new Report(fileName, title) }
  }

  def print(folder: File) = {
    failsafe{
      val writer = new PrintWriter(new File(folder, this.currentReport.fileName))
      XML.write(writer, this.currentReport.toHTML, "UTF-8", xmlDecl = false, DocType("html"), MinimizeMode.Default)
      writer.close()
    }
  }

  def setSection(name: String) = {
    failsafe{ this.currentReport.setSection(name) }
  }

  def setSubSection(name: String) = {
    failsafe{ this.currentReport.setSubSection(name) }
  }

  def setSubSubSection(name: String) = {
    failsafe{ this.currentReport.setSubSubSection(name) }
  }

  def setSections(sections: Array[String]) = {
    failsafe{ this.currentReport.setSections(sections) }
  }

  def getCurrentSections: Array[String] = {
    failsafe{ this.currentReport.getCurrentSections }
  }

  def setBottomSection() = {
    failsafe{ this.currentReport.setBottomSection() }
  }

  def addMessage(message: String) = {
    failsafe{ this.currentReport.addMessage(message) }
  }

  def addSuccess(message: String) = {
    failsafe{ this.currentReport.addSuccess(message) }
  }

  def addWarning(message: String) = {
    failsafe{ this.currentReport.addWarning(message) }
  }

  def addError(message: String, exception: Throwable) = {
    failsafe{ this.currentReport.addError(message, exception) }
  }

  def addBreak() = {
    failsafe{ this.currentReport.addBreak() }
  }

  private def failsafe[T](closure: => T): T = {
    try {
      closure
    } catch {
      case e: Throwable =>
        logger.error("Error in reporting facility!", e)
        null.asInstanceOf[T]
    }
  }
}

private[core] class Report(val fileName: String, val title: String) extends LazyLogging {

  trait XMLSerializable {
    def toXml: Node
  }

  class Message(text: String) extends XMLSerializable {
    override def toXml = <p>{text}</p>
  }

  class Break() extends Message(null) {
    override def toXml = <br/>
  }

  class Success(text: String) extends Message(text) {
    override def toXml = <p class="success-msg">{text}</p>
  }

  class Warning(text: String) extends Message(text) {
    override def toXml = <p class="warn-msg">{text}</p>
  }

  class Error(text: String, cause: Throwable) extends Message(text) {
    override def toXml = {
      def stacktrace = {
        val baos = new ByteArrayOutputStream()
        val writer = new PrintWriter(baos)
        cause.printStackTrace(writer)
        writer.flush()

        Utility.escape(baos.toString)
          .replaceAll("[\r]", "")
          .replaceAll("[\t]", "&#160;&#160;")
          .replaceAll("[\n]", "<br/>")
      }

      val id = "ST_" + Random.nextInt(10000)
      val onclick = s"var el = document.getElementById('$id'); if (el.style.display == 'none') { el.style.display = 'block'; this.innerHTML = 'Hide Stacktrace'; } else { el.style.display = 'none'; this.innerHTML = 'Show Stacktrace'; };"

      val link = if (cause != null) {
        <span class="show-stacktrace" onclick={onclick}>Show stacktrace</span>
      } else {
        <span></span>
      }

      val stacktraceDiv = if (cause != null) {
        <div id={id} class="stacktrace" style="display:none">{Unparsed(stacktrace)}</div>
      } else {
        <span></span>
      }

      <div>
        <p class="error-msg">
          {text} {link}
        </p>
        {stacktraceDiv}
      </div>
    }
  }

  class Section(val parent: Section, val level: Int, val name: String, val subsections: mutable.MutableList[Section], val messages: mutable.MutableList[Message]) extends XMLSerializable {
    override def toXml = {
      val header = level match {
        case 1 => <h2>{name}</h2>
        case 2 => <h3>{name}</h3>
        case 3 => <h4>{name}</h4>
        case 4 => <h5>{name}</h5>
        case _ =>
      }

      if (subsections != null) <section>{header}{messages.map(_.toXml)}{subsections.map(_.toXml)}</section>
      else <section>{header}{messages.map(_.toXml)}</section>
    }
  }

  val topSection = new Section(null, 0, null, new mutable.MutableList[Section], new mutable.MutableList[Message])
  val bottomSection = new Section(null, 0, null, null, new mutable.MutableList[Message])
  var currentSection = topSection

  def setSections(sections: Array[String]) = {
    assert(currentSection != bottomSection, "when bottom section once activated setSection() must no longer be used")

    def getSection(sections: Array[String], currentSection: Section): Section = {
      if (sections.isEmpty) {
        currentSection
      } else {
        val name = sections.head
        val sectionOption = currentSection.subsections.find(_.name == name)
        val section =
          if (sectionOption.isDefined) sectionOption.get
          else {
            val newSection = new Section(currentSection, currentSection.level + 1, name, new mutable.MutableList[Section], new mutable.MutableList[Message])
            currentSection.subsections += newSection
            newSection
          }

        val pad = "".padTo((currentSection.level + 1) * 2, '-')
        logger.info(s"$pad $name $pad")

        getSection(sections.tail, section)
      }
    }

    val section = getSection(sections, topSection)
    currentSection = section
  }

  def setSection(name: String) = {
    setSections(Array(name))
  }

  def setSubSection(name: String) = {
    assert(currentSection.level >= 1)

    while (currentSection.level > 1) currentSection = currentSection.parent

    setSections(Array(currentSection.name, name))
  }

  def setSubSubSection(name: String) = {
    assert(currentSection.level >= 2)

    while (currentSection.level > 2) currentSection = currentSection.parent

    setSections(Array(currentSection.parent.name, currentSection.name, name))
  }

  def getCurrentSections: Array[String] = {
    def generateList(section: Section, list: List[String]): List[String] = {
      if (section.parent == null) {
        list
      } else {
        list ++ generateList(section.parent, list) ++ List(section.name)
      }
    }

    generateList(currentSection, Nil).toArray
  }

  def setBottomSection() = {
    currentSection = bottomSection
  }

  def addMessage(message: String) = {
    logger.info(message)
    internalAddMessage(new Message(message))
  }

  def addSuccess(message: String) = {
    logger.info(message)
    internalAddMessage(new Success(message))
  }

  def addWarning(message: String) = {
    logger.warn(message)
    internalAddMessage(new Warning("WARN: " + message))
  }

  def addError(message: String, exception: Throwable) = {
    val command = exception match {
      case cee: CliwixCommandExecutionException => cee.command
      case _ => null
    }

    val rootCause = exception match {
      case cee: CliwixCommandExecutionException => cee.getCause
      case e => e
    }

    val rootCauseMessage =
      if (rootCause == null) ""
      else if (rootCause.getMessage == null) rootCause.getClass.getName
      else rootCause.getClass.getName + ": " + rootCause.getMessage

    val errorMessage =
      if (command != null) s"$message Failed to execute: ${command.desc}.  Reason: $rootCauseMessage"
      else if (rootCause != null) s"$message Reason: $rootCauseMessage"
      else message

    if (command != null) logger.error("Command failed: {}", command)
    logger.error(errorMessage, rootCause)
    internalAddMessage(new Error("ERROR: " + errorMessage, rootCause))
  }

  def addBreak() = internalAddMessage(new Break)

  private def internalAddMessage(message: Message) = currentSection.messages += message

  def toHTML =
    <html>
      <head>
        <meta charset="utf-8"/>
        <title>{title}</title>
        <style type="text/css">
          <![CDATA[
            body {
              font-family: Arial, sans-serif;
            }
            h1 {
              font-size: 24px;
              margin: 5px 0 15px 0;
            }
            h2 {
              font-size: 20px;
              margin: 10px 0;
            }
            h3 {
              font-size: 18px;
              margin: 10px 0;
            }
            p {
              font-size: 16px;
              margin: 0 0 5px 0;
            }
            section section {
              margin-left: 20px;
            }
            section section section {
              margin-left: 40px;
            }
            .logo {
              margin-top: 5px;
              width: 100px;
              float: left;
              font-size: 26px;
              font-weight: bold;
            }
            #subtitle {
              float: left;
              padding: 5px 0 0 10px;
              font-size: 14px;
              line-height: 100%;
            }
            #report {
              clear: both;
              padding-top: 5px;
              padding-bottom: 10px;
            }
            .success-msg {
              font-weight: bold;
              color: #3c763d;
            }
            .warn-msg {
              color: #000088;
            }
            .error-msg {
              font-weight: bold;
              color: #923132;
            }
            .show-stacktrace {
                font-weight: 400;
                text-decoration: underline;
                cursor: pointer;
            }
            .stacktrace {
                border: 1px solid #999;
                background-color: #eee;
                padding: 5px;
                margin: 10px;
            }
          ]]>
        </style>
      </head>
      <body>
        <div id="header">
          <a href="http://www.cliwix.com"><img class="logo" src="http://www.nonblocking.at/img/cliwix_logo.svg" alt="Cliwix" /></a>
          <div id="subtitle">Configure Liferay<br/>With XML</div>
        </div>
        <div id="report">
          <h1>{title}</h1>
          {topSection.toXml}
          <br/>
          {bottomSection.toXml}
        </div>
      </body>
    </html>

}
