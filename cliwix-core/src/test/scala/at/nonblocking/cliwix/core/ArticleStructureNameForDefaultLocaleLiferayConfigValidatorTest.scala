package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.validation.ArticleStructureNameForDefaultLocaleLiferayConfigValidator
import at.nonblocking.cliwix.model._
import org.junit.Test

import org.junit.Assert._

import scala.collection.JavaConversions._

class ArticleStructureNameForDefaultLocaleLiferayConfigValidatorTest {

  @Test
  def success(): Unit = {
    val config = new LiferayConfig
    val company = new Company()
    company.setCompanyConfiguration(new CompanyConfiguration())
    company.getCompanyConfiguration.setDefaultLocale("de_DE")
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company)))
    company.setSites(new Sites(List(site1)))

    val articleStructure1 = new ArticleStructure("TEST", List(new LocalizedTextContent("en_GB", "test"), new LocalizedTextContent("de_DE", "test")), null)
    val webContent = new WebContent
    webContent.setStructures(new ArticleStructures(List(articleStructure1)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val messages = new ArticleStructureNameForDefaultLocaleLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def fail(): Unit = {
    val config = new LiferayConfig
    val company = new Company()
    company.setCompanyConfiguration(new CompanyConfiguration())
    company.getCompanyConfiguration.setDefaultLocale("de_DE")
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company)))
    company.setSites(new Sites(List(site1)))

    val articleStructure1 = new ArticleStructure("TEST", List(new LocalizedTextContent("en_GB", "test"), new LocalizedTextContent("be_BE", "test")), null)
    val webContent = new WebContent
    webContent.setStructures(new ArticleStructures(List(articleStructure1)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val messages = new ArticleStructureNameForDefaultLocaleLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

}
