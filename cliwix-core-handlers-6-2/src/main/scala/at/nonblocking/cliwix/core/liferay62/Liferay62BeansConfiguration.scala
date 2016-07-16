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

package at.nonblocking.cliwix.core.liferay62

import com.liferay.counter.service.CounterLocalServiceUtil
import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil
import com.liferay.portal.kernel.dao.orm.{DynamicQueryFactoryUtil, SessionFactory}
import com.liferay.portal.kernel.search.IndexerRegistryUtil
import com.liferay.portal.service._
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil
import com.liferay.portlet.documentlibrary.service.{DLAppLocalServiceUtil, DLFileEntryLocalServiceUtil, DLFileEntryTypeLocalServiceUtil, DLFolderLocalServiceUtil}
import com.liferay.portlet.dynamicdatamapping.service.{DDMStructureLocalServiceUtil, DDMTemplateLocalServiceUtil}
import com.liferay.portlet.journal.service.{JournalArticleLocalServiceUtil, JournalStructureLocalServiceUtil, JournalTemplateLocalServiceUtil}
import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
class Liferay62BeansConfiguration {

  private val LIFERAY_6_TRANSACTION_INTERCEPTOR_BEAN_ID = "transactionAdvice"
  private val LIFERAY_6_SESSION_FACTORY_BEAN_ID = "liferaySessionFactory"

  @Bean
  def companyLocalService = CompanyLocalServiceUtil.getService

  @Bean
  def accountLocalService = AccountLocalServiceUtil.getService

  @Bean
  def virtualHostLocalService = VirtualHostLocalServiceUtil.getService

  @Bean
  def userLocalService = UserLocalServiceUtil.getService

  @Bean
  def userGroupRoleService = UserGroupRoleLocalServiceUtil.getService

  @Bean
  def contactLocalService = ContactLocalServiceUtil.getService

  @Bean
  def roleLocalService = RoleLocalServiceUtil.getService

  @Bean
  def userGroupService = UserGroupLocalServiceUtil.getService

  @Bean
  def userGroupGroupRoleService = UserGroupGroupRoleLocalServiceUtil.getService

  @Bean
  def organizationService = OrganizationLocalServiceUtil.getService

  @Bean
  def groupLocalService = GroupLocalServiceUtil.getService

  @Bean
  def layoutLocalService = LayoutLocalServiceUtil.getService

  @Bean
  def layoutSetLocalService = LayoutSetLocalServiceUtil.getService

  @Bean
  def portalPreferencesLocalService = PortalPreferencesLocalServiceUtil.getService

  @Bean
  def portletLocalService = PortletLocalServiceUtil.getService

  @Bean
  def portletPreferencesLocalService = PortletPreferencesLocalServiceUtil.getService

  @Bean
  def resourcePermissionLocalService = ResourcePermissionLocalServiceUtil.getService

  @Bean
  def resourceActionLocalService = ResourceActionLocalServiceUtil.getService

  @Bean
  def classNameLocalService = ClassNameLocalServiceUtil.getService

  @Bean
  def countryService = CountryServiceUtil.getService

  @Bean
  def regionService = RegionServiceUtil.getService

  @Bean
  def journalArticleLocalService = JournalArticleLocalServiceUtil.getService

  @Bean
  def journalStructureLocalService = JournalStructureLocalServiceUtil.getService

  @Bean
  def journalTemplateLocalService = JournalTemplateLocalServiceUtil.getService

  @Bean
  def ddmStructureLocalService = DDMStructureLocalServiceUtil.getService

  @Bean
  def ddmTemplateLocalService = DDMTemplateLocalServiceUtil.getService

  @Bean
  def dlAppLocalService = DLAppLocalServiceUtil.getService

  @Bean
  def dlFolderLocalService = DLFolderLocalServiceUtil.getService

  @Bean
  def dlFileEntryLocalService = DLFileEntryLocalServiceUtil.getService

  @Bean
  def dlFileEntryTypeLocalService = DLFileEntryTypeLocalServiceUtil.getService

  @Bean
  def assetEntryLocalService = AssetEntryLocalServiceUtil.getService

  @Bean
  def counterService = CounterLocalServiceUtil.getService

  @Bean
  def dynamicQueryFactory = DynamicQueryFactoryUtil.getDynamicQueryFactory

  @Bean
  def indexerRegistry = IndexerRegistryUtil.getIndexerRegistry

  @Bean
  def sessionFactory = PortalBeanLocatorUtil.locate(LIFERAY_6_SESSION_FACTORY_BEAN_ID).asInstanceOf[SessionFactory]

  @Bean
  def liferayTransactionInterceptor = {
    val transactionInterceptor = PortalBeanLocatorUtil.locate(LIFERAY_6_TRANSACTION_INTERCEPTOR_BEAN_ID)
    assert(transactionInterceptor != null, "Bean 'transactionAdvice' in Liferay spring context exists")
    transactionInterceptor
  }
}
