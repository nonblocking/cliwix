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

package at.nonblocking.cliwix.apidoc

import java.io.{PrintWriter, File}
import java.lang.reflect.{ParameterizedType, Field, Modifier}
import javax.xml.bind.annotation.{XmlAttribute, XmlElement, XmlRootElement}

import at.nonblocking.cliwix.model.{LiferayEntity, TreeType, ListType, LiferayConfig}
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.`type`.filter.AssignableTypeFilter

object ApiDocGenerator {

  val basePackage = classOf[LiferayConfig].getPackage.getName

  def main(args: Array[String]) = {
    val targetFile = args(0)
    val generatedApiDoc = new File(targetFile)

    generatedApiDoc.delete()
    println(s"Generating: ${generatedApiDoc.getAbsolutePath}")

    val writer = new PrintWriter(generatedApiDoc)

    writer.write("List Elements\n")
    writer.write("~~~~~~~~~~~~~\n")

    writer.write("\n")
    findClassesThatInheritFrom(classOf[ListType[_]]).foreach{ klazz =>
      writer.write(s" * ${klazz.getSimpleName}\n")
    }
    writer.write("\n")

    writer.write("Tree Elements\n")
    writer.write("~~~~~~~~~~~~~\n")

    writer.write("\n")
    findClassesThatInheritFrom(classOf[TreeType[_]]).foreach{ klazz =>
      writer.write(s" * ${klazz.getSimpleName}\n")
    }
    writer.write("\n")

    writer.write("Entities\n")
    writer.write("~~~~~~~~\n")

    writer.write("\n")
    writer.write("[options=\"header\"]\n")
    writer.write("|=======================\n")
    writer.write("|Entity Name| Property Name | XML | Type\n")
    findClassesThatInheritFrom(classOf[LiferayEntity]).foreach{ klazz =>
      val xmlRootElementAnnotation = klazz.getAnnotation(classOf[XmlRootElement])
      val xmlName =
        if (xmlRootElementAnnotation != null) "<" + xmlRootElementAnnotation.name() + ">"
        else "-"

      writer.write(s"| ${klazz.getSimpleName}|  | $xmlName | \n")

      getAllFields(klazz)
        .filter(field => !Modifier.isStatic(field.getModifiers))
        .sortBy(_.getName)
        .foreach{ field =>
          val xmlElementAnnotation = field.getAnnotation(classOf[XmlElement])
          val xmlAttributeAnnotation = field.getAnnotation(classOf[XmlAttribute])
          val xmlName =
            if (xmlElementAnnotation != null) "<" + xmlElementAnnotation.name() + ">"
            else if (xmlAttributeAnnotation != null) "@" + xmlAttributeAnnotation.name()
            else "-"

          writer.write(s"| | ${field.getName}  | $xmlName | ${getType(field)} \n")
        }
    }
    writer.write("\n")
    writer.write("|=======================\n")

    writer.write("\n")

    writer.close()
  }

  def findClassesThatInheritFrom(baseClass: Class[_]) = {
    val bdr = new SimpleBeanDefinitionRegistry()
    val scanner = new ClassPathBeanDefinitionScanner(bdr)

    scanner.addIncludeFilter(new AssignableTypeFilter(baseClass))
    scanner.scan(basePackage)

    val beanNames = bdr.getBeanDefinitionNames
    beanNames
      .map(bdr.getBeanDefinition(_).getBeanClassName)
      .filter(_.startsWith(basePackage))
      .sorted
      .map(Class.forName)
  }

  def getAllFields(klazz: Class[_]) = {
    def getAllDeclaredFields(klazz: Class[_], fields: List[Field]): List[Field] =
      if (klazz.getSuperclass.getPackage.getName.startsWith(basePackage)) getAllDeclaredFields(klazz.getSuperclass, fields ++ klazz.getDeclaredFields)
      else fields ++ klazz.getDeclaredFields

    getAllDeclaredFields(klazz, List())
  }

  def getType(field: Field) =
      field.getGenericType match {
        case klazz: Class[_] =>       field.getType.getSimpleName
        case parameterizedType: ParameterizedType =>
          field.getType.getSimpleName +
            "<" +
            parameterizedType.getActualTypeArguments.map(_.asInstanceOf[Class[_]].getSimpleName).mkString(",") +
           ">"
      }

}