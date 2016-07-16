package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.validation.GuestSiteLiferayConfigValidator
import at.nonblocking.cliwix.model._
import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConversions._

class GuestSiteLiferayValidatorConfigValidatorTest {

  @Test
  def success(): Unit = {
    val config = new LiferayConfig
    val company = new Company()
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)
    val site2 = new Site("Site2", new SiteConfiguration("/site2", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company)))
    company.setSites(new Sites(List(site1, site2)))

    val messages = new GuestSiteLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def failOnMultipleGuestSites(): Unit = {
    val config = new LiferayConfig
    val company = new Company()
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)
    val site2 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site2", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company)))
    company.setSites(new Sites(List(site1, site2)))

    val messages = new GuestSiteLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def successOnMultipleGuestSitesInDifferentCompanies(): Unit = {
    val config = new LiferayConfig
    val company1 = new Company()
    val company2 = new Company()
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)
    val site2 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site2", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company1, company2)))
    company1.setSites(new Sites(List(site1)))
    company2.setSites(new Sites(List(site2)))

    val messages = new GuestSiteLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def failOnNoGuestSiteWhenPolicyIsEnforce() {
    val config = new LiferayConfig
    val company1 = new Company()
    val site1 = new Site("Site1", new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)
    val site2 = new Site("Site2", new SiteConfiguration("/site2", SITE_MEMBERSHIP_TYPE.OPEN), null)


    val companies = new Companies(List(company1))
    company1.setSites(new Sites(List(site1)))
    company1.setSites(new Sites(List(site2)))
    companies.setImportPolicy(IMPORT_POLICY.ENFORCE)
    config.setCompanies(companies)

    val messages = new GuestSiteLiferayConfigValidator().validate(config)

    assertEquals(1, messages.length)
  }

}
