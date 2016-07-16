package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.validation.ArticleTemplateStructureIdRequiredIn61LiferayConfigValidator
import at.nonblocking.cliwix.model._
import org.junit.Test

import org.junit.Assert._

import scala.collection.JavaConversions._

class ArticleTemplateStructureIdRequiredIn61LiferayConfigValidatorTest {

  @Test
  def liferay61Success(): Unit = {
    val config = new LiferayConfig
    val company = new Company()
    company.setCompanyConfiguration(new CompanyConfiguration())
    company.getCompanyConfiguration.setDefaultLocale("de_DE")
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company)))
    company.setSites(new Sites(List(site1)))

    val articleTemplate1 = new ArticleTemplate("TEST", List(new LocalizedTextContent("en_GB", "test"), new LocalizedTextContent("de_DE", "test")), null, null)
    articleTemplate1.setStructureId("STRUCTURE1")
    val webContent = new WebContent
    webContent.setTemplates(new ArticleTemplates(List(articleTemplate1)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val validator = new ArticleTemplateStructureIdRequiredIn61LiferayConfigValidator()
    validator.setLiferayInfo(new LiferayInfo {
      override def getReleaseInfo: String = ???
      override def getVersion: String = ???
      override def getBaseVersion: String = "6.1"
    })

    val messages = validator.validate(config)

    assertTrue(messages.length == 0)
  }

  @Test
  def liferay61Fail(): Unit = {
    val config = new LiferayConfig
    val company = new Company()
    company.setCompanyConfiguration(new CompanyConfiguration())
    company.getCompanyConfiguration.setDefaultLocale("de_DE")
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company)))
    company.setSites(new Sites(List(site1)))

    val articleTemplate1 = new ArticleTemplate("TEST", List(new LocalizedTextContent("en_GB", "test"), new LocalizedTextContent("be_BE", "test")), null, null)
    val webContent = new WebContent
    webContent.setTemplates(new ArticleTemplates(List(articleTemplate1)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val validator = new ArticleTemplateStructureIdRequiredIn61LiferayConfigValidator()
    validator.setLiferayInfo(new LiferayInfo {
      override def getReleaseInfo: String = ???
      override def getVersion: String = ???
      override def getBaseVersion: String = "6.1"
    })

    val messages = validator.validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def liferay62NullAllowed(): Unit = {
    val config = new LiferayConfig
    val company = new Company()
    company.setCompanyConfiguration(new CompanyConfiguration())
    company.getCompanyConfiguration.setDefaultLocale("de_DE")
    val site1 = new Site(Site.GUEST_SITE_NAME, new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company)))
    company.setSites(new Sites(List(site1)))

    val articleTemplate1 = new ArticleTemplate("TEST", List(new LocalizedTextContent("en_GB", "test"), new LocalizedTextContent("be_BE", "test")), null, null)
    val webContent = new WebContent
    webContent.setTemplates(new ArticleTemplates(List(articleTemplate1)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val validator = new ArticleTemplateStructureIdRequiredIn61LiferayConfigValidator()
    validator.setLiferayInfo(new LiferayInfo {
      override def getReleaseInfo: String = ???
      override def getVersion: String = ???
      override def getBaseVersion: String = "6.2"
    })

    val messages = validator.validate(config)

    assertTrue(messages.isEmpty)
  }
}
