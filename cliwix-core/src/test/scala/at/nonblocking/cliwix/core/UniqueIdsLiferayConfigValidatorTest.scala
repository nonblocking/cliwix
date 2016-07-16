package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.validation.UniqueIdsLiferayConfigValidator
import at.nonblocking.cliwix.model._
import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConversions._

class UniqueIdsLiferayConfigValidatorTest {

  @Test
  def success() {
    val config = new LiferayConfig
    val company1 = new Company("test1", new CompanyConfiguration("test1.nonblocking.at", null, null, null))
    val company2 = new Company("test2", new CompanyConfiguration("test2.nonblocking.at", null, null, null))

    config.setCompanies(new Companies(List(company1, company2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def failOnDuplicateCompanyWebIds() {
    val config = new LiferayConfig
    val company1 = new Company("test", new CompanyConfiguration("test1.nonblocking.at", null, null, null))
    val company2 = new Company("test", new CompanyConfiguration("test2.nonblocking.at", null, null, null))

    config.setCompanies(new Companies(List(company1, company2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnDuplicateVirtualHosts() {
    val config = new LiferayConfig
    val company1 = new Company("test1", new CompanyConfiguration("test.nonblocking.at", null, null, null))
    val company2 = new Company("test2", new CompanyConfiguration("test.nonblocking.at", null, null, null))

    config.setCompanies(new Companies(List(company1, company2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnMultipleDuplicateVirtualHosts() {
    val config = new LiferayConfig
    val company1 = new Company("test1", new CompanyConfiguration("test.nonblocking.at", null, null, null))
    val company2 = new Company("test2", new CompanyConfiguration("test.nonblocking.at", null, null, null))
    val company3 = new Company("test3", new CompanyConfiguration("test.nonblocking.at", null, null, null))
    val company4 = new Company("test4", new CompanyConfiguration("test.nonblocking.at", null, null, null))

    config.setCompanies(new Companies(List(company1, company2, company3, company4)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)
    messages.foreach(m => println(m.message + " (" + m.location + ")"))

    assertTrue(messages.length == 3)
  }

  @Test
  def failOnDuplicateUserGroupName() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val group1 = new UserGroup("GROUP", "description1", null)
    val group2 = new UserGroup("GROUP", "description2", null)

    config.setCompanies(new Companies(List(company1)))
    company1.setUserGroups(new UserGroups(List(group1, group2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnDuplicateRoleName() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val role1 = new Role("ROLE")
    val role2 = new Role("ROLE")

    config.setCompanies(new Companies(List(company1)))
    company1.setRoles(new Roles(List(role1, role2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnRoleNameOnlyDiffersInCase() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val role1 = new Role("ROLE")
    val role2 = new Role("role")

    config.setCompanies(new Companies(List(company1)))
    company1.setRoles(new Roles(List(role1, role2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnDuplicateOrganizationName() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val org1 = new Organization("ORG1")
    val org2 = new Organization("ORG2")
    val subOrg1 = new Organization("ORG1")
    org1.setSubOrganizations(List(subOrg1))

    config.setCompanies(new Companies(List(company1)))
    company1.setOrganizations(new Organizations(List(org1, org2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnDuplicateUserScreenName() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val user1 = new User()
    user1.setScreenName("foo")
    val user2 = new User()
    user2.setScreenName("foo")

    config.setCompanies(new Companies(List(company1)))
    company1.setUsers(new Users(List(user1, user2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnDuplicateUserMailAddresses() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val user1 = new User()
    user1.setScreenName("foo1")
    user1.setEmailAddress("test@nonblocking.at")
    val user2 = new User()
    user2.setScreenName("foo2")
    user2.setEmailAddress("test@nonblocking.at")

    config.setCompanies(new Companies(List(company1)))
    company1.setUsers(new Users(List(user1, user2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def successOnDuplicateUserScreenNameOnDifferentCompanies() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val company2 = new Company("test2", null)
    val user1 = new User()
    user1.setScreenName("foo")
    val user2 = new User()
    user2.setScreenName("foo")

    config.setCompanies(new Companies(List(company1, company2)))
    company1.setUsers(new Users(List(user1)))
    company2.setUsers(new Users(List(user2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def failOnDuplicateSiteNames() {
    val config = new LiferayConfig

    val company1 = new Company("test1", null)
    val site1 = new Site("Site1", new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)
    val site2 = new Site("Site1", new SiteConfiguration("/site2", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1, site2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertEquals(1, messages.length)
  }

  @Test
  def failOnDuplicateSiteFriendlyUrls() {
    val config = new LiferayConfig

    val company1 = new Company("test1", null)
    val site1 = new Site("Site1", new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)
    val site2 = new Site("Site2", new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1, site2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertEquals(1, messages.length)
  }

  @Test
  def failOnDuplicateSiteVirtualHosts() {
    val config = new LiferayConfig

    val company1 = new Company("test1", new CompanyConfiguration("nonblocking.at", null, null, null))

    val site1 = new Site("Site1", new SiteConfiguration("/site1", SITE_MEMBERSHIP_TYPE.OPEN), null)
    val site2 = new Site("Site2", new SiteConfiguration("/site2", SITE_MEMBERSHIP_TYPE.OPEN), null)
    site2.getSiteConfiguration.setVirtualHostPublicPages("nonblocking.at")

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1, site2)))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertEquals(1, messages.length)
    assertEquals("virtualHost must be unique! Duplicate value: 'nonblocking.at'.", messages.head.message)
  }

  @Test
  def failOnDuplicatePageUrlWithinFolder() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))

    site1.setPublicPages(new PageSet(new Pages(List(
      page("/a", List(
        page("/aa"),
        page("/aa")
      )),
      page("/b", List(
        page("/ba", List(
          page("/baa")
        ))
      )),
      page("/c")
    ))))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnDuplicatePageUrlWithinDifferentFolders() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))

    site1.setPublicPages(new PageSet(new Pages(List(
      page("/a", List(
        page("/aa"),
        page("/ab")
      )),
      page("/b", List(
        page("/ab", List(
          page("/baa")
        ))
      )),
      page("/c")
    ))))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertFalse(messages.isEmpty)
  }

  @Test
  def successOnDuplicatePageUrlWithinDifferentCompanies() {
    val config = new LiferayConfig

    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)
    val company2 = new Company("test2", null)
    val site2 = new Site(Site.GUEST_SITE_NAME, null, null)

    config.setCompanies(new Companies(List(company1, company2)))
    company1.setSites(new Sites(List(site1)))
    company2.setSites(new Sites(List(site2)))

    site1.setPublicPages(new PageSet(new Pages(List(
      page("/a", List(
        page("/aa"),
        page("/ab")
      )),
      page("/b", List(
        page("/abb", List(
          page("/baa")
        ))
      )),
      page("/c")
    ))))

    site2.setPublicPages(new PageSet(new Pages(List(
      page("/a", List(
        page("/aa"),
        page("/ab")
      ))
    ))))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def failOnDuplicatePageUrlWithinDifferentSubFolders() {
    val config = new LiferayConfig

    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))

    site1.setPublicPages(new PageSet(new Pages(List(
      page("/a", List(
        page("/aa"),
        page("/ab")
      )),
      page("/b", List(
        page("/ab", List(
          page("/a", List(
            page("/aa"),
            page("/ab")
          ))
        ))
      )),
      page("/c")
    ))))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertFalse(messages.isEmpty)
  }

  @Test
  def successOnDuplicatePageUrlWithinPublicAndPrivatePages() {
    val config = new LiferayConfig

    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))

    site1.setPublicPages(new PageSet(new Pages(List(
      page("/a", List(
        page("/aa"),
        page("/ab")
      )),
      page("/b", List(
        page("/abc", List(
        ))
      )),
      page("/c")
    ))))

    site1.setPrivatePages(new PageSet(new Pages(List(
      page("/a", List(
        page("/aa"),
        page("/ab")
      )),
      page("/b", List(
        page("/abc", List(
        ))
      )),
      page("/c")
    ))))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def failOnDuplicateFileNameWithinFolder() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)
    site1.setSiteContent(new SiteContent)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))

    site1.getSiteContent.setDocumentLibrary(new DocumentLibrary(null, List(
      folder("test", List(
        folder("a", List(
          file("b"),
          file("b"),
          file("b")
        ))
      )),
      folder("test2", List(

      ))
    )))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)
    messages.foreach(m => println(m.message + " (" + m.location + ")"))

    assertTrue(messages.length == 2)
  }

  @Test
  def successOnDuplicateFileNameWithinDifferentFolders() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)
    site1.setSiteContent(new SiteContent)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))

    site1.getSiteContent.setDocumentLibrary(new DocumentLibrary(null, List(
      folder("test", List(
        folder("a", List(
          file("b"),
          file("c")
        ))
      )),
      folder("test2", List(
        folder("a", List(
          file("b")
        ))
      ))
    )))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def successOnDuplicateFileNameWithinDifferentCompanies() {
    val config = new LiferayConfig

    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)
    site1.setSiteContent(new SiteContent)

    val company2 = new Company("test2", null)
    val site2 = new Site(Site.GUEST_SITE_NAME, null, null)
    site2.setSiteContent(new SiteContent)

    config.setCompanies(new Companies(List(company1, company2)))
    company1.setSites(new Sites(List(site1)))
    company2.setSites(new Sites(List(site2)))

    site1.getSiteContent.setDocumentLibrary(new DocumentLibrary(null, List(
      folder("test", List(
        folder("a", List(
          file("b"),
          file("c")
        ))
      )),
      folder("test2", List(
        file("b")
      ))
    )))

    site2.getSiteContent.setDocumentLibrary(new DocumentLibrary(null, List(
      folder("test", List(
        folder("a", List(
          file("b"),
          file("c")
        ))
      ))
    )))

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def failOnDuplicateArticleStructuresIds() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)

    val articleStructure1 = new ArticleStructure("STRUCTURE1", null, null)
    val articleStructure2 = new ArticleStructure("STRUCTURE2", null, null)
    val articleStructure3 = new ArticleStructure("STRUCTURE1", null, null)
    articleStructure2.setSubStructures(List(articleStructure3))

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))
    val webContent = new WebContent
    webContent.setStructures(new ArticleStructures(List(articleStructure1, articleStructure2)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnDuplicateArticleTemplateIds() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)

    val articleTemplate1 = new ArticleTemplate("TEMPLATE1", null, null, null)
    val articleTemplate2 = new ArticleTemplate("TEMPLATE1", null, null, null)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))
    val webContent = new WebContent
    webContent.setTemplates(new ArticleTemplates(List(articleTemplate1, articleTemplate2)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  @Test
  def failOnDuplicateArticleIds() {
    val config = new LiferayConfig
    val company1 = new Company("test1", null)
    val site1 = new Site(Site.GUEST_SITE_NAME, null, null)

    val article1 = new StaticArticle("ARTICLE1", null, null, null)
    val article2 = new StaticArticle("ARTICLE1", null, null, null)

    config.setCompanies(new Companies(List(company1)))
    company1.setSites(new Sites(List(site1)))
    val webContent = new WebContent
    webContent.setArticles(new Articles(List(article1, article2)))
    site1.setSiteContent(new SiteContent)
    site1.getSiteContent.setWebContent(webContent)

    val messages = new UniqueIdsLiferayConfigValidator().validate(config)

    assertTrue(messages.length == 1)
  }

  private def page(url: String, subpages: List[Page] = null): Page = {
    val page = new Page(PAGE_TYPE.PORTLET, url, null)
    if (subpages != null) page.setSubPages(subpages)
    page
  }

  private def folder(name: String, subItems: List[DocumentLibraryItem]): DocumentLibraryFolder = {
    val folder = new DocumentLibraryFolder(name)
    folder.setSubItems(subItems)
    folder
  }

  private def file(name: String): DocumentLibraryFile = {
    new DocumentLibraryFile(name, name)
  }

}
