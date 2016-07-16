package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.compare.LiferayEntityComparatorImpl
import at.nonblocking.cliwix.model._
import org.junit.Test
import org.junit.Assert._

import java.{util=>jutil}
import scala.collection.JavaConversions._

class LiferayEntityComparatorTest {

  @Test
  def equalsTest1() {

    val article1 = new StaticArticle("one", "de_DE", List(new LocalizedTextContent("de_DE", "test")), List(new LocalizedXmlContent("de_DE", "the content")))
    val article2 = new StaticArticle("one", "de_DE", List(new LocalizedTextContent("de_DE", "test")), List(new LocalizedXmlContent("de_DE", "the content")))

    val comparator = new LiferayEntityComparatorImpl

    assertTrue(comparator.equals(article1, article2))
  }

  @Test
  def equalsTest2() {
    val expirationDate = new jutil.Date
    val expirationDate2 = new jutil.Date(expirationDate.getTime + 100) //Only the milliseconds differ
    val displayDate = new jutil.Date
    val displayDate2 = new jutil.Date(displayDate.getTime + 100) //Only the milliseconds differ

    val article1 = new StaticArticle("one", "de_DE", List(new LocalizedTextContent("de_DE", "test")), List(new LocalizedXmlContent("de_DE", "the content")))
    article1.setExpirationDate(expirationDate)
    article1.setDisplayDate(displayDate)
    article1.setAssetTags(List("a", "c", "b"))
    article1.setSummaries(List(new LocalizedTextContent("de_DE", "foo"), new LocalizedTextContent("en_GB", "bar")))

    val article2 = new StaticArticle("one", "de_DE", List(new LocalizedTextContent("de_DE", "test")), List(new LocalizedXmlContent("de_DE", "the content")))
    article2.setExpirationDate(expirationDate2)
    article2.setDisplayDate(displayDate2)
    article2.setAssetTags(List("a", "b", "c"))
    article2.setSummaries(List(new LocalizedTextContent("en_GB", "bar"), new LocalizedTextContent("de_DE", "foo")))

    val comparator = new LiferayEntityComparatorImpl

    assertTrue(comparator.equals(article1, article2))
  }

  @Test
  def equalsTest3() {

    val portletConfiguration1 = new PortletConfiguration("test", List(new Preference("a", "a1"), new Preference("b", "b2")))
    val portletConfiguration2 = new PortletConfiguration("test", List(new Preference("b", "b2"), new Preference("a", "a1")))

    val comparator = new LiferayEntityComparatorImpl

    assertTrue(comparator.equals(portletConfiguration1, portletConfiguration2))
  }

  @Test
  def notEqualsTest1() {

    val article1 = new StaticArticle("one", "de_DE", List(new LocalizedTextContent("de_DE", "test")), List(new LocalizedXmlContent("de_DE", "the content")))
    val article2 = new StaticArticle("two", "de_DE", List(new LocalizedTextContent("de_DE", "test2")), List(new LocalizedXmlContent("de_DE", "the content 2")))

    val comparator = new LiferayEntityComparatorImpl

    assertFalse(comparator.equals(article1, article2))
    assertEquals(3, comparator.diff(article1, article2).size)

    println(comparator.diff(article1, article2).mkString(","))
  }

  @Test
  def notEqualsTest2() {
    val expirationDate = new jutil.Date
    val expirationDate2 = new jutil.Date(expirationDate.getTime + 60 * 1000)
    val displayDate = new jutil.Date
    val displayDate2 = new jutil.Date(displayDate.getTime + 60 * 1000)

    val article1 = new StaticArticle("one", "de_DE", List(new LocalizedTextContent("de_DE", "test")), List(new LocalizedXmlContent("de_DE", "the content")))
    article1.setExpirationDate(expirationDate)
    article1.setDisplayDate(displayDate)
    article1.setAssetTags(List("a", "c", "b"))
    article1.setSummaries(List(new LocalizedTextContent("de_DE", "foo"), new LocalizedTextContent("en_GB", "bar")))

    val article2 = new StaticArticle("two", "de_DE", List(new LocalizedTextContent("de_DE", "test")), List(new LocalizedXmlContent("de_DE", "the content")))
    article2.setExpirationDate(expirationDate2)
    article2.setDisplayDate(displayDate2)
    article2.setAssetTags(List("a", "b", "d"))
    article2.setSummaries(List(new LocalizedTextContent("en_GB", "bar2"), new LocalizedTextContent("de_DE", "foo")))

    val comparator = new LiferayEntityComparatorImpl

    println(comparator.diff(article1, article2).mkString(","))
    assertFalse(comparator.equals(article1, article2))
    assertEquals(6, comparator.diff(article1, article2).size)
  }

  @Test
  def notEqualsTest3() {

    val portletConfiguration1 = new PortletConfiguration("test", List(new Preference("a", "a1"), new Preference("b", "b2")))
    val portletConfiguration2 = new PortletConfiguration("test", List(new Preference("c", "c3"), new Preference("a", "a1")))

    val comparator = new LiferayEntityComparatorImpl

    assertFalse(comparator.equals(portletConfiguration1, portletConfiguration2))
    assertEquals(2, comparator.diff(portletConfiguration1, portletConfiguration2).size)

    println(comparator.diff(portletConfiguration1, portletConfiguration2).mkString(","))
  }

  @Test
  def equalsInSuperClassTest() {

    val file1 = new DocumentLibraryFile("test.txt", "foo")
    val file2 = new DocumentLibraryFile("test.txt", "foo")

    val comparator = new LiferayEntityComparatorImpl

    assertTrue(comparator.equals(file1, file2))
  }

  @Test
  def notEqualsInSuperClassTest() {

    val file1 = new DocumentLibraryFile("test.txt", "foo")
    val file2 = new DocumentLibraryFile("test2.txt", "bar")

    val comparator = new LiferayEntityComparatorImpl

    assertFalse(comparator.equals(file1, file2))
    assertEquals("List(CHANGE: name: test.txt -> test2.txt)", comparator.diff(file1, file2).toString())

    println(comparator.diff(file1, file2).mkString(","))
  }

  @Test
  def lesserEqualsTest() {
    val file1 = new DocumentLibraryFile("test.txt", "foo")
    file1.setFileDataUpdateTimestamp(12345678)
    val file2 = new DocumentLibraryFile("test.txt", "foo")
    file2.setFileDataUpdateTimestamp(12345677)

    val comparator = new LiferayEntityComparatorImpl

    assertTrue(comparator.equals(file1, file2))
  }

  @Test
  def notLesserEqualsTest() {
    val file1 = new DocumentLibraryFile("test.txt", "foo")
    file1.setFileDataUpdateTimestamp(12345678)
    val file2 = new DocumentLibraryFile("test.txt", "foo")
    file2.setFileDataUpdateTimestamp(12345679)

    val comparator = new LiferayEntityComparatorImpl

    assertFalse(comparator.equals(file1, file2))
    assertEquals(1, comparator.diff(file1, file2).size)

    println(comparator.diff(file1, file2).mkString(","))
  }

  @Test
  def equalsIfNotNullTest() {
    val company1 = new Company("test1", new CompanyConfiguration("nonblocking.at", "nonblocking.at", "a", "b"))
    val company2 = new Company("test1", new CompanyConfiguration("nonblocking.at", "nonblocking.at", "a", "b"))
    val company2Changed = new Company("test1", new CompanyConfiguration("nonblocking.at", "nonblocking.at", "c", "b"))
    val company2WithoutConfiguration = new Company("test1", null)

    val comparator = new LiferayEntityComparatorImpl

    assertTrue(comparator.equals(company1, company2))
    assertFalse(comparator.equals(company1, company2Changed))
    assertEquals("CHANGE: companyConfiguration.defaultLocale: a -> c",
      comparator.diff(company1, company2Changed).mkString(","))
    assertTrue(comparator.equals(company1, company2WithoutConfiguration))
  }

  @Test
  def equalsIfNotNullTest2() {
    val site1 = new Site("Foo", new SiteConfiguration("/foo", SITE_MEMBERSHIP_TYPE.OPEN),
      new SiteMembers())
    site1.getSiteMembers.setMemberUserGroups(List(new MemberUserGroup("one"), new MemberUserGroup("two")))

    val site2 = new Site("Foo", new SiteConfiguration("/foo", SITE_MEMBERSHIP_TYPE.OPEN),
      new SiteMembers())
    site2.getSiteMembers.setMemberUserGroups(List(new MemberUserGroup("one"), new MemberUserGroup("two")))

    val site2Changed = new Site("Foo", new SiteConfiguration("/foo2", SITE_MEMBERSHIP_TYPE.OPEN),
      new SiteMembers())
    site2Changed.getSiteMembers.setMemberUserGroups(List(new MemberUserGroup("three"), new MemberUserGroup("two")))

    val site2WithoutConfiguration = new Site("Foo", null, null)

    val comparator = new LiferayEntityComparatorImpl

    assertTrue(comparator.equals(site1, site2))
    assertFalse(comparator.equals(site1, site2Changed))
    assertEquals("CHANGE: siteConfiguration.friendlyURL: /foo -> /foo2,REMOVE: siteMembers.memberUserGroups[one]: MemberUserGroup{name='one'} -> null,ADD: siteMembers.memberUserGroups[three]: null -> MemberUserGroup{name='three'}",
      comparator.diff(site1, site2Changed).mkString(","))
    assertTrue(comparator.equals(site1, site2WithoutConfiguration))
  }

  @Test
  def articleClassChangeTest() {
    val article1 = new StaticArticle("foo", "de_DE", null, null)
    val article2 = new TemplateDrivenArticle("foo", "de_DE", null, null, "a", "a")

    val comparator = new LiferayEntityComparatorImpl

    assertFalse(comparator.equals(article2, article1))
  }
  
}
