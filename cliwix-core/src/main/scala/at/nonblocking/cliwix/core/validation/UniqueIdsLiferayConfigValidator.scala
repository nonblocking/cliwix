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

package at.nonblocking.cliwix.core.validation

import java.util.UUID

import at.nonblocking.cliwix.core.util.{TreeTypeUtils, ListTypeUtils}
import at.nonblocking.cliwix.model.{DocumentLibraryFolder, LiferayConfig}

import scala.collection.mutable
import scala.collection.JavaConversions._

private[core] class UniqueIdsLiferayConfigValidator extends LiferayConfigValidator with ListTypeUtils with TreeTypeUtils {

  val UNIQUE_VALUE_WEB_ID = "Company.webId"
  val UNIQUE_VALUE_VIRTUAL_HOST = "virtualHost"
  val UNIQUE_VALUE_USERGROUP_NAME = "UserGroup.name"
  val UNIQUE_VALUE_ROLE_NAME = "Role.name"
  val UNIQUE_VALUE_ORGANIZATION_NAME = "Organization.name"
  val UNIQUE_VALUE_USER_SCREENNAME = "User.screenName"
  val UNIQUE_VALUE_USER_EMAILADDRESS = "User.emailAddress"
  val UNIQUE_VALUE_SITE_NAME = "Site.name"
  val UNIQUE_VALUE_SITE_FRIENDLY_URL = "Site.friendlyURL"
  val UNIQUE_VALUE_PAGE_URL = "Page.url"
  val UNIQUE_VALUE_FILE_FOLDER_NAME = "Folder.name and File.name"
  val UNIQUE_VALUE_ARTICLE_ID = "Article.articleId"
  val UNIQUE_VALUE_ARTICLE_STRUCTURE_ID = "ArticleStructure.structureId"
  val UNIQUE_VALUE_ARTICLE_TEMPLATE_ID = "ArticleTemplate.templateId"

  var keyMap: mutable.HashMap[String, mutable.MutableList[String]] = _

  override def validate(liferayConfig: LiferayConfig): List[ValidationError] = {
    assert(liferayConfig != null, "liferayConfig != null")
    keyMap = new mutable.HashMap[String, mutable.MutableList[String]]()

    val messages = new mutable.MutableList[ValidationError]()

    safeForeach(liferayConfig.getCompanies){ company =>
      checkUnique(UNIQUE_VALUE_WEB_ID, null, company.getWebId, s"Company: ${company.identifiedBy()}", messages)

      if (company.getCompanyConfiguration != null) {
        checkUnique(UNIQUE_VALUE_VIRTUAL_HOST, null, company.getCompanyConfiguration.getVirtualHost, s"Company: ${company.identifiedBy()}", messages)
      }

      safeForeach(company.getRoles) { role =>
        checkUnique(UNIQUE_VALUE_ROLE_NAME, company.identifiedBy(), role.getName, s"Company: ${company.identifiedBy()}, Role: ${role.identifiedBy()}", messages, ignoreCase = true)
      }

      safeForeach(company.getUserGroups) { userGroup =>
        checkUnique(UNIQUE_VALUE_USERGROUP_NAME, company.identifiedBy(), userGroup.getName, s"Company: ${company.identifiedBy()}, UserGroup: ${userGroup.identifiedBy()}", messages, ignoreCase = true)
      }

      safeProcessRecursively(company.getOrganizations) { org =>
        checkUnique(UNIQUE_VALUE_ORGANIZATION_NAME, company.identifiedBy(), org.getName, s"Company: ${company.identifiedBy()}, Organization: ${org.identifiedBy()}", messages, ignoreCase = true)
      }

      safeForeach(company.getUsers) { user =>
        checkUnique(UNIQUE_VALUE_USER_SCREENNAME, company.identifiedBy(), user.getScreenName, s"Company: ${company.identifiedBy()}, User: ${user.identifiedBy()}", messages, ignoreCase = true)
        checkUnique(UNIQUE_VALUE_USER_EMAILADDRESS, company.identifiedBy(), user.getEmailAddress, s"Company: ${company.identifiedBy()}, User: ${user.identifiedBy()}", messages, ignoreCase = true)
      }

      safeForeach(company.getSites) { site =>
        checkUnique(UNIQUE_VALUE_SITE_NAME, company.identifiedBy(), site.getName, s"Company: ${company.identifiedBy()}, Site:${site.identifiedBy()}", messages, ignoreCase = true)

        if (site.getSiteConfiguration != null) {
          checkUnique(UNIQUE_VALUE_SITE_FRIENDLY_URL, company.identifiedBy(), site.getSiteConfiguration.getFriendlyURL, s"Site:${site.identifiedBy()}", messages)
          checkUnique(UNIQUE_VALUE_VIRTUAL_HOST, null, site.getSiteConfiguration.getVirtualHostPublicPages, s"Company: ${company.identifiedBy()}, Site: ${site.identifiedBy()}, PublicPages", messages)
          checkUnique(UNIQUE_VALUE_VIRTUAL_HOST, null, site.getSiteConfiguration.getVirtualHostPrivatePages, s"Company: ${company.identifiedBy()}, Site: ${site.identifiedBy()}, PrivatePages", messages)
        }

        val subKeyBaseSite = company.identifiedBy() + "_" + site.identifiedBy()

        if (site.getPublicPages != null) safeProcessRecursivelyWithParent(site.getPublicPages.getPages) { (parent, page) =>
          val parentPageName = if (parent == null) "/" else parent.identifiedBy()
          val subKey = subKeyBaseSite + "_" + "public"
          checkUnique(UNIQUE_VALUE_PAGE_URL, subKey, page.getFriendlyUrl, s"Company: ${company.identifiedBy()}, Site: ${site.identifiedBy()}, Public pages, Parent page:$parentPageName", messages)
        }
        if (site.getPrivatePages != null) safeProcessRecursivelyWithParent(site.getPrivatePages.getPages) { (parent, page) =>
          val parentPageName = if (parent == null) "/" else parent.identifiedBy()
          val subKey = subKeyBaseSite + "_" + "private" + parentPageName
          checkUnique(UNIQUE_VALUE_PAGE_URL, subKey, page.getFriendlyUrl, s"Company: ${company.identifiedBy()}, Site: ${site.identifiedBy()}, Private pages, Parent page:$parentPageName", messages)
        }

        val siteContent = site.getSiteContent

        if (siteContent != null) {
          safeProcessRecursivelyWithParent(siteContent.getDocumentLibrary) { (parent, item) =>
            item match {
              case folder: DocumentLibraryFolder =>
                val folderName = if (folder.getName == null) "/" else folder.getName
                val subKey = UUID.randomUUID().toString
                if (folder.getSubItems != null) folder.getSubItems.foreach { subItem =>
                  checkUnique(UNIQUE_VALUE_FILE_FOLDER_NAME, subKey, subItem.getName, s"Company: ${company.identifiedBy()}, Site: ${site.identifiedBy()}, Parent folder: $folderName}", messages, ignoreCase = false)
                }
              case _ =>
            }
          }

          if (siteContent.getWebContent != null) {
            safeProcessRecursively(siteContent.getWebContent.getStructures) { articleStructure =>
              checkUnique(UNIQUE_VALUE_ARTICLE_STRUCTURE_ID, subKeyBaseSite, articleStructure.getStructureId, s"Company: ${company.identifiedBy()}, Site: ${site.identifiedBy()}, ArticleStructure: ${articleStructure.identifiedBy()}", messages)
            }
            safeForeach(siteContent.getWebContent.getTemplates) { articleTemplates =>
              checkUnique(UNIQUE_VALUE_ARTICLE_TEMPLATE_ID, subKeyBaseSite, articleTemplates.getTemplateId, s"Company: ${company.identifiedBy()}, Site: ${site.identifiedBy()}, ArticleTemplate: ${articleTemplates.identifiedBy()}", messages)
            }
            safeForeach(siteContent.getWebContent.getArticles) { article =>
              checkUnique(UNIQUE_VALUE_ARTICLE_ID, subKeyBaseSite, article.getArticleId, s"Company: ${company.identifiedBy()}, Site: ${site.identifiedBy()}, Article: ${article.identifiedBy()}", messages)
            }
          }
        }
      }
    }

    messages.toList
  }

  private def checkUnique(key: String, subKey: String, value: String, location: String, messages: mutable.MutableList[ValidationError], ignoreCase: Boolean = false) = {
    if (value != null) {
      val compareValue =
        if (ignoreCase) value.toLowerCase
        else value
      val fullKey = if (subKey == null) key else key + "_" + subKey
      if (!keyMap.contains(fullKey)) keyMap.put(fullKey, new mutable.MutableList[String])
      val list = keyMap.get(fullKey).get
      if (list.contains(compareValue)) {
        messages += new ValidationError(s"$key must be unique! Duplicate value: '$value'.", location, null)
      } else {
        list += compareValue
      }
    }
  }

}
