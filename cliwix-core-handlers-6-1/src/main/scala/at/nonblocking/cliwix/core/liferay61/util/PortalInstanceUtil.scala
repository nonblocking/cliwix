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
package at.nonblocking.cliwix.core.liferay61.util

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil

trait PortalInstanceUtil {
  def addPortalInstance(companyId: Long)
  def removePortalInstance(companyId: Long)
}

class PortalInstanceUtilImpl extends PortalInstanceUtil {

  val PORTAL_INSTANCES_CLASS = "com.liferay.portal.util.PortalInstances"

  override def addPortalInstance(companyId: Long) = {
    val portalInstancesClass = PortalBeanLocatorUtil.getBeanLocator.getClassLoader.loadClass(PORTAL_INSTANCES_CLASS)
    val addCompanyIdMethod = portalInstancesClass.getMethod("addCompanyId", classOf[Long])
    addCompanyIdMethod.invoke(portalInstancesClass, companyId.asInstanceOf[AnyRef])
  }

  override def removePortalInstance(companyId: Long) = {
    val portalInstancesClass = PortalBeanLocatorUtil.getBeanLocator.getClassLoader.loadClass(PORTAL_INSTANCES_CLASS)
    try {
      val removeCompanyIdMethod = portalInstancesClass.getMethod("removeCompany", classOf[Long])
      removeCompanyIdMethod.invoke(portalInstancesClass, companyId.asInstanceOf[AnyRef])
    } catch {
      case e: NoSuchMethodException =>
        //Ignore. Not implemented prior to 6.2
    }
  }

}
