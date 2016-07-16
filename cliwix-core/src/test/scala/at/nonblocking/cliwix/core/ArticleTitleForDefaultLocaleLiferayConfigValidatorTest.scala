package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.validation.ArticleTitleForDefaultLocaleLiferayConfigValidator
import at.nonblocking.cliwix.model._
import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConversions._

class ArticleTitleForDefaultLocaleLiferayConfigValidatorTest {

  @Test
  def success(): Unit = {
    val config = new LiferayConfig
    val company = new Company()
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company)))
    company.setSites(new Sites(List(site1)))

    val article1 = new StaticArticle("TEST", "de_DE", List(new LocalizedTextContent("en_GB", "test"), new LocalizedTextContent("de_DE", "test")), Nil)
    val webContent = new WebContent
    webContent.setArticles(new Articles(List(article1)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val messages = new ArticleTitleForDefaultLocaleLiferayConfigValidator().validate(config)

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

    val article1 = new StaticArticle("TEST", "de_DE", List(new LocalizedTextContent("en_GB", "test"), new LocalizedTextContent("be_BE", "test")), Nil)
    val webContent = new WebContent
    webContent.setArticles(new Articles(List(article1)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val messages = new ArticleTitleForDefaultLocaleLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

}
