package at.nonblocking.cliwix.integrationtest

import java.util.Calendar

import at.nonblocking.cliwix.core.ExecutionContext
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model._
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

@RunWith(classOf[CliwixIntegrationTestRunner])
class ArticleHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  @Test
  @TransactionalRollback
  def insertTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("22233", "de_DE",
      List(new LocalizedTextContent("de_DE", "First Article")),
      List(new LocalizedXmlContent("en_GB", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artikel</h2><p>test</p>")))
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article)).result

    assertNotNull(insertedArticle)
    assertNotNull(insertedArticle.getArticleDbId)
    assertTrue(insertedArticle.getOwnerGroupId > 0)

    val articleList = this.dispatchHandler.execute(ArticleListCommand(insertedSite.getSiteId)).result

    assertTrue(articleList.contains(article.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(article, articleList.get(article.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def insertWithLocaleOtherThanCompanyLocaleTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val portalPreferences = this.dispatchHandler.execute(PortalPreferencesReadCommand(insertedCompany.getCompanyId)).result
    portalPreferences.setPreferences(List(new Preference("locales", "de_DE,sk_SK,en_US")))
    this.dispatchHandler.execute(UpdateCommand(portalPreferences))

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("22233", "sk_SK",
      List(new LocalizedTextContent("sk_SK", "First Article")),
      List(new LocalizedXmlContent("sk_SK", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artikel</h2><p>test</p>")))
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article)).result

    assertNotNull(insertedArticle)
    assertNotNull(insertedArticle.getArticleDbId)
    assertTrue(insertedArticle.getOwnerGroupId > 0)

    val articleList = this.dispatchHandler.execute(ArticleListCommand(insertedSite.getSiteId)).result

    assertTrue(articleList.contains(article.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(article, articleList.get(article.identifiedBy())))
  }


  @Test
  @TransactionalRollback
  def updateStaticArticleTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("SECOND_123", "de_DE",
      List(new LocalizedTextContent("de_DE", "First Article")),
      List(new LocalizedXmlContent("en_GB", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artikel</h2><p>test</p>")))
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article))
      .result.asInstanceOf[StaticArticle]

    insertedArticle.setTitles(List(new LocalizedTextContent("de_DE", "First Article Updated")))
    insertedArticle.setContents(List(new LocalizedXmlContent("de_DE", "<h2>First Article Updated</h2><p>test</p>")))
    insertedArticle.setAssetTags(List("tag3"))

    val updatedArticle = this.dispatchHandler.execute(UpdateCommand(insertedArticle)).result

    assertNotNull(updatedArticle)
    assertTrue(updatedArticle.getOwnerGroupId > 0)

    val articleList = this.dispatchHandler.execute(ArticleListCommand(insertedSite.getSiteId)).result

    assertTrue(articleList.contains(article.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(insertedArticle, articleList.get(article.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def updateTemplateDrivenArticleTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_GB", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    val insertedArticleStructure = this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val articleTemplate = new ArticleTemplate("MYFIRSTTEMPLATE", List(new LocalizedTextContent("en_GB", "Template 1"), new LocalizedTextContent("de_DE", "Template 1")), "FTL", "<p>Hello ${name}</p>")
    articleTemplate.setStructureId("MYFIRSTSTRUCTURE")
    articleTemplate.setDescriptions(List(new LocalizedTextContent("en_GB", "Description of template 1")))
    var insertedArticleTemplate = this.dispatchHandler.execute(ArticleTemplateInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleTemplate)).result

    val article = new TemplateDrivenArticle("SECOND_123", "de_DE",
      List(new LocalizedTextContent("de_DE", "First Article")),
      "MYFIRSTSTRUCTURE", "MYFIRSTTEMPLATE",
      "\n\t<dynamic-element name=\"sdf\" index=\"0\" type=\"boolean\" index-type=\"keyword\">\n\t\t<dynamic-content language-id=\"en_US\"><![CDATA[true]]></dynamic-content>\n\t</dynamic-element>\n")
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article))
      .result.asInstanceOf[TemplateDrivenArticle]

    //this.dispatchHandler.execute(DeleteCommand(insertedArticleTemplate))
    //this.dispatchHandler.execute(DeleteCommand(insertedArticleStructure))

    val articleStructure2 = new ArticleStructure("MYSECONDSTRUCTURE", List(new LocalizedTextContent("en_GB", "Structure 2"), new LocalizedTextContent("de_DE", "Structure 2")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure2, null))

    val articleTemplate2 = new ArticleTemplate("MYSECONDTEMPLATE", List(new LocalizedTextContent("en_GB", "Template 2"), new LocalizedTextContent("de_DE", "Template 2")), "FTL", "<p>Hello ${name}</p>")
    articleTemplate2.setStructureId("MYSECONDSTRUCTURE")
    this.dispatchHandler.execute(ArticleTemplateInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleTemplate2))

    insertedArticle.setTitles(List(new LocalizedTextContent("de_DE", "First Article Updated")))
    insertedArticle.setDynamicElements("\n\t<dynamic-element name=\"sdf\" index=\"0\" type=\"boolean\" index-type=\"keyword\">\n\t\t<dynamic-content language-id=\"en_US\"><![CDATA[false]]></dynamic-content>\n\t</dynamic-element>\n")
    insertedArticle.setAssetTags(List("tag3"))
    insertedArticle.setStructureId("MYSECONDSTRUCTURE")
    insertedArticle.setTemplateId("MYSECONDTEMPLATE")

    val updatedArticle = this.dispatchHandler.execute(UpdateCommand(insertedArticle)).result

    assertNotNull(updatedArticle)
    assertTrue(updatedArticle.getOwnerGroupId > 0)

    val articleList = this.dispatchHandler.execute(ArticleListCommand(insertedSite.getSiteId)).result

    assertTrue(articleList.contains(article.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(insertedArticle, articleList.get(article.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def updateStaticArticleToTemplateDrivenArticleTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("SECOND_123", "de_DE",
      List(new LocalizedTextContent("de_DE", "First Article")),
      List(new LocalizedXmlContent("en_GB", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artikel</h2><p>test</p>")))
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article)).result

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_GB", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    val insertedArticleStructure = this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val articleTemplate = new ArticleTemplate("MYFIRSTTEMPLATE", List(new LocalizedTextContent("en_GB", "Template 1"), new LocalizedTextContent("de_DE", "Template 1")), "FTL", "<p>Hello ${name}</p>")
    articleTemplate.setStructureId("MYFIRSTSTRUCTURE")
    articleTemplate.setDescriptions(List(new LocalizedTextContent("en_GB", "Description of template 1")))
    var insertedArticleTemplate = this.dispatchHandler.execute(ArticleTemplateInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleTemplate)).result

    val updatedArticle = new TemplateDrivenArticle("SECOND_123", "de_DE",
      List(new LocalizedTextContent("de_DE", "First Article")),
      "MYFIRSTSTRUCTURE", "MYFIRSTTEMPLATE",
      "\n\t<dynamic-element name=\"sdf\" index=\"0\" type=\"boolean\" index-type=\"keyword\">\n\t\t<dynamic-content language-id=\"en_US\"><![CDATA[true]]></dynamic-content>\n\t</dynamic-element>\n")
    updatedArticle.setAssetTags(List("tag3"))
    updatedArticle.setStructureId("MYFIRSTSTRUCTURE")
    updatedArticle.setTemplateId("MYFIRSTTEMPLATE")

    updatedArticle.copyIds(insertedArticle)

    assertFalse(this.liferayEntityComparator.equals(insertedArticle, updatedArticle))

    val updatedArticlePersisted = this.dispatchHandler.execute(UpdateCommand(updatedArticle)).result

    assertNotNull(updatedArticlePersisted)
    assertTrue(updatedArticlePersisted.getOwnerGroupId > 0)

    val articleList = this.dispatchHandler.execute(ArticleListCommand(insertedSite.getSiteId)).result

    assertTrue(articleList.contains(article.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(updatedArticlePersisted, articleList.get(article.identifiedBy())))
  }


  @Test
  @TransactionalRollback
  def encodingTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()
    site.getSiteConfiguration.setDescription("Meine Testsäite")

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("22233", "de_DE",
      List(new LocalizedTextContent("de_DE", "Erster Artükel")),
      List(new LocalizedXmlContent("en_GB", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artükel mit umläute</h2><p>test</p>")))
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article)).result

    assertNotNull(insertedArticle)
    assertNotNull(insertedArticle.getArticleId)

    val articleList = this.dispatchHandler.execute(ArticleListCommand(insertedSite.getSiteId)).result

    assertTrue(articleList.contains(article.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(article, articleList.get(article.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def getByIdTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("SECOND_123", "de_DE",
      List(new LocalizedTextContent("de_DE", "First Article")),
      List(new LocalizedXmlContent("en_GB", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artikel</h2><p>test</p>")))
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article)).result

    val a = this.dispatchHandler.execute(GetByDBIdCommand(insertedArticle.getArticleDbId, classOf[Article])).result

    assertNotNull(a)
    assertTrue(this.liferayEntityComparator.equals(insertedArticle, a))
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("SECOND_123", "de_DE",
      List(new LocalizedTextContent("de_DE", "First Article")),
      List(new LocalizedXmlContent("en_GB", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artikel</h2><p>test</p>")))
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article)).result

    val a = this.dispatchHandler.execute(GetByIdentifierOrPathWithinGroupCommand(insertedArticle.identifiedBy(), insertedCompany.getCompanyId, insertedSite.getSiteId, classOf[Article])).result

    assertNotNull(a)
    assertTrue(this.liferayEntityComparator.equals(insertedArticle, a))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()
    site.getSiteConfiguration.setDescription("Meine Testsäite")

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val article = new StaticArticle("22233", "de_DE",
      List(new LocalizedTextContent("de_DE", "Erster Artükel")),
      List(new LocalizedXmlContent("en_GB", "<h2>First Article</h2><p>test</p>"), new LocalizedXmlContent("de_DE", "<h2>Erster Artükel mit umläute</h2><p>test</p>")))
    val displayDateCal = Calendar.getInstance()
    displayDateCal.set(2014, Calendar.JANUARY, 1, 0, 0, 0)
    displayDateCal.set(Calendar.MILLISECOND, 0)
    article.setDisplayDate(displayDateCal.getTime)
    article.setAssetTags(List("tag1", "tag2"))

    val insertedArticle = this.dispatchHandler.execute(ArticleInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, article)).result

    this.dispatchHandler.execute(DeleteCommand(insertedArticle))

    val articleList = this.dispatchHandler.execute(ArticleListCommand(insertedSite.getSiteId)).result

    assertFalse(articleList.contains(article.identifiedBy()))
  }


}
