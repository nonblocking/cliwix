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

package at.nonblocking.cliwix.core.liferay62.util

import com.liferay.portal.kernel.xml.{Element, SAXReaderUtil}

import scala.collection.JavaConversions._
import scala.collection.mutable

sealed trait Liferay62StructureXsdUtil {

  /**
   * Fixes the problem that 6.1 exports the structure like this:
   * <pre>
   *  dynamic-element
   *    meta-data
   *    dynamic-element
   *      meta-data
   *    dynamic-element
   *      meta-data
   * </pre>
   *
   * But 6.2 require the meta-data element to be after the child elements:
   *
   * <pre>
   *  dynamic-element
   *    dynamic-element
   *      meta-data
   *    dynamic-element
   *      meta-data
   *    meta-data
   * </pre>
   *
   * @param xsd String
   * @return String
   */
  def moveMetaDataAfterChildDynamicElements(xsd: String): String

}

class Liferay62StructureXsdUtilImpl extends Liferay62StructureXsdUtil {

  def moveMetaDataAfterChildDynamicElements(xsd: String) = {
    def processDynamicElement(dynamicElement: Element): Unit = {
      var metaDataList = new mutable.MutableList[Element]
      dynamicElement.elements().foreach{ elem =>
        if (elem.getName == "meta-data") {
          elem.detach()
          metaDataList += elem
        } else if (elem.getName == "dynamic-element") {
          processDynamicElement(elem)
        }
      }
      metaDataList.foreach{ metaDataElem =>
        dynamicElement.elements().add(metaDataElem)
      }
    }

    val document = SAXReaderUtil.read(xsd)
    document.getRootElement.elements().foreach{ elem =>
      if (elem.getName == "dynamic-element") processDynamicElement(elem)
    }

    document.asXML()
  }

}


