package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.{ExecutionContext, LiferayInfo}
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
class ArticleStructureHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayInfo: LiferayInfo = _

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

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_GB", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    articleStructure.setDescriptions(List(new LocalizedTextContent("en_GB", "Description of structure 1")))

    val subArticleStructure = new ArticleStructure("MYFIRSTSUBSTRUCTURE", List(new LocalizedTextContent("en_GB", "Substructure 1"), new LocalizedTextContent("de_DE", "Substructure 1")),
      "\n\t<dynamic-element name=\"test2\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n")

    val insertedArticleStructure = this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result
    val insertedArticleSubStructure = this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, subArticleStructure, insertedArticleStructure)).result

    assertNotNull(insertedArticleStructure)
    assertNotNull(insertedArticleStructure.getStructureDbId)
    assertTrue(insertedArticleStructure.getOwnerGroupId > 0)

    assertNotNull(insertedArticleSubStructure)
    assertNotNull(insertedArticleSubStructure.getStructureDbId)
    assertTrue(insertedArticleSubStructure.getOwnerGroupId > 0)

    val articleStructureList = this.dispatchHandler.execute(ArticleStructureListCommand(insertedSite.getSiteId)).result

    val a = articleStructureList.find(_.identifiedBy() == articleStructure.identifiedBy())
    assertTrue(a.isDefined)
    if (this.liferayInfo.getBaseVersion == "6.1") {
      assertTrue(this.liferayEntityComparator.equals(articleStructure, a.get))
    } else {
      assertEquals("\n\t<dynamic-element name=\"surename\" repeatable=\"false\" type=\"text_box\" index-type=\"\">\n\t\t<meta-data>\n\t\t\t<entry name=\"label\"><![CDATA[surename]]></entry>\n\t\t\t<entry name=\"required\"><![CDATA[false]]></entry>\n\t\t</meta-data>\n\t</dynamic-element>\n\t<dynamic-element name=\"forename\" repeatable=\"false\" type=\"text_box\" index-type=\"\">\n\t\t<meta-data>\n\t\t\t<entry name=\"label\"><![CDATA[forename]]></entry>\n\t\t\t<entry name=\"required\"><![CDATA[false]]></entry>\n\t\t</meta-data>\n\t</dynamic-element>\n\t<dynamic-element name=\"test\" repeatable=\"false\" type=\"list\" index-type=\"\">\n\t\t<meta-data>\n\t\t\t<entry name=\"label\"><![CDATA[test]]></entry>\n\t\t\t<entry name=\"required\"><![CDATA[false]]></entry>\n\t\t</meta-data>\n\t</dynamic-element>\n", a.get.getDynamicElements)
    }

    val sa = a.get.getSubStructures.find(_.identifiedBy() == subArticleStructure.identifiedBy())
    assertTrue(sa.isDefined)
    assertTrue(this.liferayEntityComparator.equals(insertedArticleSubStructure, sa.get))
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

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_GB", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    articleStructure.setDescriptions(List(new LocalizedTextContent("en_GB", "Description of structure 1")))

    val insertedArticleStructure = this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    insertedArticleStructure.setNames(List(new LocalizedTextContent("en_GB", "Structure 1 updated"), new LocalizedTextContent("de_DE", "Structure 1 geändert")))

    val updatedArticleStructure = this.dispatchHandler.execute(UpdateCommand(insertedArticleStructure)).result

    assertNotNull(updatedArticleStructure)
    assertTrue(updatedArticleStructure.getOwnerGroupId > 0)

    val articleStructureList = this.dispatchHandler.execute(ArticleStructureListCommand(insertedSite.getSiteId)).result

    val a = articleStructureList.find(_.identifiedBy() == articleStructure.identifiedBy())
    assertTrue(a.isDefined)
    if (this.liferayInfo.getBaseVersion == "6.1") {
      assertTrue(this.liferayEntityComparator.equals(insertedArticleStructure, a.get))
    } else {
      assertEquals("\n\t<dynamic-element name=\"surename\" repeatable=\"false\" type=\"text_box\" index-type=\"\">\n\t\t<meta-data>\n\t\t\t<entry name=\"label\"><![CDATA[surename]]></entry>\n\t\t\t<entry name=\"required\"><![CDATA[false]]></entry>\n\t\t</meta-data>\n\t</dynamic-element>\n\t<dynamic-element name=\"forename\" repeatable=\"false\" type=\"text_box\" index-type=\"\">\n\t\t<meta-data>\n\t\t\t<entry name=\"label\"><![CDATA[forename]]></entry>\n\t\t\t<entry name=\"required\"><![CDATA[false]]></entry>\n\t\t</meta-data>\n\t</dynamic-element>\n\t<dynamic-element name=\"test\" repeatable=\"false\" type=\"list\" index-type=\"\">\n\t\t<meta-data>\n\t\t\t<entry name=\"label\"><![CDATA[test]]></entry>\n\t\t\t<entry name=\"required\"><![CDATA[false]]></entry>\n\t\t</meta-data>\n\t</dynamic-element>\n", a.get.getDynamicElements)
    }
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

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_GB", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    articleStructure.setDescriptions(List(new LocalizedTextContent("en_GB", "Description of structure 1")))

    val insertedArticleStructure = this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val a = this.dispatchHandler.execute(GetByDBIdCommand(insertedArticleStructure.getStructureDbId, classOf[ArticleStructure])).result

    assertNotNull(a)
    assertTrue(this.liferayEntityComparator.equals(insertedArticleStructure, a))
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

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_GB", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    articleStructure.setDescriptions(List(new LocalizedTextContent("en_GB", "Description of structure 1")))

    val insertedArticleStructure = this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    val a = this.dispatchHandler.execute(GetByIdentifierOrPathWithinGroupCommand(insertedArticleStructure.identifiedBy(), insertedCompany.getCompanyId, insertedSite.getSiteId, classOf[ArticleStructure])).result

    assertNotNull(a)
    assertTrue(this.liferayEntityComparator.equals(insertedArticleStructure, a))
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

    val articleStructure = new ArticleStructure("MYFIRSTSTRUCTURE", List(new LocalizedTextContent("en_GB", "Structure 1"), new LocalizedTextContent("de_DE", "Structure 1")),
      "\n\t<dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>\n\t<dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/>\n")
    articleStructure.setDescriptions(List(new LocalizedTextContent("en_GB", "Description of structure 1")))

    val insertedArticleStructure = this.dispatchHandler.execute(ArticleStructureInsertCommand(insertedCompany.getCompanyId, insertedSite.getSiteId, articleStructure, null)).result

    this.dispatchHandler.execute(DeleteCommand(insertedArticleStructure))

    val articleStructureList = this.dispatchHandler.execute(ArticleStructureListCommand(insertedSite.getSiteId)).result

    val a = articleStructureList.find(_.identifiedBy() == articleStructure.identifiedBy())
    assertFalse(a.isDefined)
  }


}
