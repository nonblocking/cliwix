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

package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.model._
import java.{util => jutil}

import scala.collection.JavaConversions._

object TestEntityFactory {

  def createTestCompany(webID: String = "company" + System.currentTimeMillis()) = {
    val company = new Company(webID,
      new CompanyConfiguration(webID + ".nonblocking.at", webID + ".mail.nonblocking.at", "de_DE", "Europe/Vienna"))
    company.getCompanyConfiguration.setAccountName(webID.toUpperCase)
    company
  }

  def createTestSite(siteName: String = "Test Site") = {
    val siteFriendlyUrl = "/" + siteName.replaceAll(" ", "").toLowerCase
    val site = new Site(siteName, new SiteConfiguration(siteFriendlyUrl, SITE_MEMBERSHIP_TYPE.PRIVATE), null)
    site.getSiteConfiguration.setActive(true)
    site.getSiteConfiguration.setDescription("My test site")
    site
  }

  def createTestSiteWithMembers(memberUserGroups: List[String], memberUsers: List[String], memberOrganizations: List[String], siteName: String = "Test Site") = {
    val site = new Site("Test Site", new SiteConfiguration("/testsite", SITE_MEMBERSHIP_TYPE.PRIVATE), new SiteMembers)
    site.getSiteConfiguration.setActive(true)
    site.getSiteConfiguration.setDescription("My test site")
    site.getSiteMembers.setMemberUserGroups(memberUserGroups.map(ug => new MemberUserGroup(ug)))
    site.getSiteMembers.setMemberUsers(memberUsers.map(u => new MemberUser(u)))
    site.getSiteMembers.setMemberOrganizations(memberOrganizations.map(o => new MemberOrganization(o)))
    site
  }

  def createTestUser(name: String = "testuser1") = {
    new User(name, name + "@nonblocking.at", new Password(false, "test"),
      null, name, null, "Mustermann", null, GENDER.M, jutil.Locale.GERMAN.getLanguage, jutil.TimeZone.getDefault.getID, "Hi " + name)
  }

  def toMemberUsers(users: User*): jutil.List[MemberUser] = users.map(u => new MemberUser(u.getScreenName))
  def toMemberUserGroups(userGroups: UserGroup*): jutil.List[MemberUserGroup] = userGroups.map(ug => new MemberUserGroup(ug.getName))
  def toMemberOrganizations(organizations: Organization*): jutil.List[MemberOrganization] = organizations.map(o => new MemberOrganization(o.getName))

}
