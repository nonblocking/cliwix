package at.nonblocking.cliwix.integrationtest

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
class ArticleTemplateHandlerIntegrationTest {

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

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_US", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val articleTemplate = new ArticleTemplate("MYFIRSTTEMPLATE", List(new LocalizedTextContent("en_US", "Template 1"), new LocalizedTextContent("de_DE", "Template 1")), "ftl", "<p>Hello ${name}</p>")
    articleTemplate.setStructureId("MYFIRSTSTRUCTURE")
    articleTemplate.setDescriptions(List(new LocalizedTextContent("en_US", "Description of template 1")))

    val insertedArticleTemplate = this.dispatchHandler.execute(ArticleTemplateInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleTemplate)).result

    assertNotNull(insertedArticleTemplate)
    assertNotNull(insertedArticleTemplate.getTemplateDbId)
    assertTrue(insertedArticleTemplate.getOwnerGroupId > 0)

    val articleTemplateList = this.dispatchHandler.execute(ArticleTemplateListCommand(insertedSite.getSiteId)).result

    assertTrue(this.liferayEntityComparator.equals(articleTemplate, articleTemplateList.get(articleTemplate.identifiedBy())))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_US", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val articleTemplate = new ArticleTemplate("MYFIRSTTEMPLATE", List(new LocalizedTextContent("en_US", "Template 1"), new LocalizedTextContent("de_DE", "Template 1")), "FTL", "<p>Hello ${name}</p>")
    articleTemplate.setStructureId("MYFIRSTSTRUCTURE")
    articleTemplate.setDescriptions(List(new LocalizedTextContent("en_US", "Description of template 1")))

    val insertedArticleTemplate = this.dispatchHandler.execute(ArticleTemplateInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleTemplate)).result

    insertedArticleTemplate.setNames(List(new LocalizedTextContent("en_US", "Template 1 updated"), new LocalizedTextContent("de_DE", "Template 1 geändert")))

    val updatedArticleTemplate = this.dispatchHandler.execute(UpdateCommand(insertedArticleTemplate)).result

    assertNotNull(updatedArticleTemplate)
    assertTrue(updatedArticleTemplate.getOwnerGroupId > 0)

    val articleTemplateList = this.dispatchHandler.execute(ArticleTemplateListCommand(insertedSite.getSiteId)).result

    assertTrue(articleTemplateList.contains(insertedArticleTemplate.identifiedBy()))
    assertTrue(this.liferayEntityComparator.equals(insertedArticleTemplate, articleTemplateList.get(insertedArticleTemplate.identifiedBy())))
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

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_US", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val articleTemplate = new ArticleTemplate("MYFIRSTTEMPLATE", List(new LocalizedTextContent("en_US", "Template 1"), new LocalizedTextContent("de_DE", "Template 1")), "FTL", "<p>Hello ${name}</p>")
    articleTemplate.setStructureId("MYFIRSTSTRUCTURE")
    articleTemplate.setDescriptions(List(new LocalizedTextContent("en_US", "Description of template 1")))

    val insertedArticleTemplate = this.dispatchHandler.execute(ArticleTemplateInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleTemplate)).result

    val a = this.dispatchHandler.execute(GetByDBIdCommand(insertedArticleTemplate.getTemplateDbId, classOf[ArticleTemplate])).result

    assertNotNull(a)
    assertTrue(this.liferayEntityComparator.equals(insertedArticleTemplate, a))
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

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_US", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val articleTemplate = new ArticleTemplate("MYFIRSTTEMPLATE", List(new LocalizedTextContent("en_US", "Template 1"), new LocalizedTextContent("de_DE", "Template 1")), "FTL", "<p>Hello ${name}</p>")
    articleTemplate.setStructureId("MYFIRSTSTRUCTURE")
    articleTemplate.setDescriptions(List(new LocalizedTextContent("en_US", "Description of template 1")))

    val insertedArticleTemplate = this.dispatchHandler.execute(ArticleTemplateInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleTemplate)).result

    val a = this.dispatchHandler.execute(GetByIdentifierOrPathWithinGroupCommand(insertedArticleTemplate.identifiedBy(), insertedCompany.getCompanyId, insertedSite.getSiteId, classOf[ArticleTemplate])).result

    assertNotNull(a)
    assertTrue(this.liferayEntityComparator.equals(insertedArticleTemplate, a))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    val company = createTestCompany()

    val insertedCompany = this.dispatchHandler.execute(CompanyInsertCommand(company)).result
    ExecutionContext.updateCompanyContext(insertedCompany)

    val site = createTestSite()

    val insertedSite = this.dispatchHandler.execute(SiteInsertCommand(insertedCompany.getCompanyId, site)).result
    ExecutionContext.updateGroupContext(insertedSite.getSiteId)

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_US", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val articleTemplate = new ArticleTemplate("MYFIRSTTEMPLATE", List(new LocalizedTextContent("en_US", "Template 1"), new LocalizedTextContent("de_DE", "Template 1")), "FTL", "<p>Hello ${name}</p>")
    articleTemplate.setStructureId("MYFIRSTSTRUCTURE")
    articleTemplate.setDescriptions(List(new LocalizedTextContent("en_US", "Description of template 1")))

    val insertedArticleTemplate = this.dispatchHandler.execute(ArticleTemplateInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleTemplate)).result

    this.dispatchHandler.execute(DeleteCommand(insertedArticleTemplate))

    val articleTemplateList = this.dispatchHandler.execute(ArticleTemplateListCommand(insertedSite.getSiteId)).result

    assertFalse(articleTemplateList.contains(articleTemplate.identifiedBy()))
  }


}
