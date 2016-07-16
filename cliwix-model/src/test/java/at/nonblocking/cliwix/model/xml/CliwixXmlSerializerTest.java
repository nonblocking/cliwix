package at.nonblocking.cliwix.model.xml;

import at.nonblocking.cliwix.model.*;
import org.junit.Test;

import javax.xml.bind.JAXB;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class CliwixXmlSerializerTest {

    @Test
    public void serializationTest() throws Exception {

        LiferayConfig liferayConfig = new LiferayConfig();

        Companies companies = new Companies(null);
        liferayConfig.setCompanies(companies);

        CompanyConfiguration company1Config = new CompanyConfiguration("nonblocking.at", "nonblocking.at", "de_AT", "Europe/Vienna");
        Company company1 = new Company("nonblocking.at", company1Config);
        companies.setCompanies(Arrays.asList(company1));

        Role role1 = new Role("ROLE1");
        role1.setPermissions(new RolePermissions(Collections.singletonList(new RolePermission("com.liferay.portlet.documentlibrary.model.DLFolder", Collections.singletonList("VIEW")))));

        List<Role> roles = Arrays.asList(role1, new Role("ROLE2"), new Role("SITE_ROLE1", ROLE_TYPE.SITE));
        List<UserGroup> userGroups = Arrays.asList(new UserGroup("GROUP1"), new UserGroup("GROUP2"));
        List<User> users = Arrays.asList(
                new User("user1", "user1@nonblocking.at", null, "Developer", "John", null, "Do",
                        null, GENDER.M, "en_GB", "Europe/Lisbon", "Tuedelue"),
                new User("user2", "user2@nonblocking.at", new Password(false, "test"), "Developer", "Mary", null, "Do",
                        new Date(), GENDER.M, "de_DE", null, "Welcome"));
        company1.setRoles(new Roles(roles));
        company1.setUserGroups(new UserGroups(userGroups));
        company1.setUsers(new Users(users));

        Organization org = new Organization("Accounting");
        org.setCountryCode("AT");
        org.setRegionCode("W");
        company1.setOrganizations(new Organizations(Arrays.asList(org)));
        Organization subOrganization = new Organization("Foo");
        org.setSubOrganizations(Arrays.asList(subOrganization));

        SiteConfiguration siteConfiguration1 = new SiteConfiguration("/my", SITE_MEMBERSHIP_TYPE.OPEN);
        SiteMembers members1 = new SiteMembers();
        Site site1 = new Site("My Sites", siteConfiguration1, members1);
        members1.setMemberUsers(Arrays.asList(new MemberUser("testuser1")));
        members1.setMemberUserGroups(Arrays.asList(new MemberUserGroup("GROUP1")));

        PageSet site1PublicPages = new PageSet(new Pages(new ArrayList<Page>()));
        site1PublicPages.setDefaultThemeId("my-custom-theme");
        site1PublicPages.setDefaultColorSchemeId("02");
        site1.setPublicPages(site1PublicPages);

        Page rootPage = new Page(PAGE_TYPE.PORTLET, "/home", Arrays.asList(new LocalizedTextContent("en_GB", "Page1")));
        Page page1 = new Page(PAGE_TYPE.PORTLET, "/info", Arrays.asList(new LocalizedTextContent("en_GB", "Page2")));
        page1.setThemeId("test");
        page1.setCss("fooo");
        rootPage.setSubPages(Arrays.asList(page1));
        Page page2 = new Page(PAGE_TYPE.PORTLET, "/a", Arrays.asList(new LocalizedTextContent("en_GB", "Page3")));
        Page page3 = new Page(PAGE_TYPE.PORTLET, "/b", Arrays.asList(new LocalizedTextContent("en_GB", "Page4")));
        page1.setSubPages(Arrays.asList(page2, page3));
        site1PublicPages.getPages().setRootPages(Arrays.asList(rootPage));

        ResourcePermissions permissionsPage1 = new ResourcePermissions();
        permissionsPage1.setPermissions(Arrays.asList(new ResourcePermission("ROLE1", Arrays.asList("VIEW")), new ResourcePermission("ROLE1", Arrays.asList("EDIT"))));
        page1.setPermissions(permissionsPage1);

        PortletConfigurations portletConfigurations = new PortletConfigurations();
        List<Preference> portletPrefs = Arrays.asList(new Preference("testname", "testvalue"), new Preference("testname2", "testvalue2"));
        PortletConfiguration portletConfiguration = new PortletConfiguration("foo", portletPrefs);
        portletConfigurations.setPortletConfigurations(Arrays.asList(portletConfiguration));
        page1.setPortletConfigurations(portletConfigurations);

        company1.setSites(new Sites(Arrays.asList(site1)));

        site1.setSiteContent(new SiteContent());

        ArticleStructure structure = new ArticleStructure("myFirstStructure", Arrays.asList(new LocalizedTextContent("en_GB", "Structure 1")),
                "  <dynamic-element name=\"surename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>  \n" +
                        "    <dynamic-element name=\"forename\" type=\"text_box\" index-type=\"\" repeatable=\"false\"/>  \n" +
                        "    <dynamic-element name=\"test\" type=\"list\" index-type=\"\" repeatable=\"false\"/> ");

        ArticleStructure subStructure = new ArticleStructure("mySubStructure", Arrays.asList(new LocalizedTextContent("en_GB", "Sub Structure 1")),
                "<dynamic-element name=\"test\" type=\"boolean\" index-type=\"\" repeatable=\"false\"/>");

        structure.setSubStructures(Collections.singletonList(subStructure));

        ArticleTemplate template = new ArticleTemplate("myFirstTemplate", Arrays.asList(new LocalizedTextContent("en_GB", "Template 1")),
                "FTL", "<p>Hello ${name}</p>");

        List<LocalizedTextContent> title1 = Arrays.asList(new LocalizedTextContent("en_GB", "My first article"));
        List<LocalizedXmlContent> content1 = Arrays.asList(new LocalizedXmlContent("en_GB", "<xml><content>Hi döre</content>"));
        Article article1 = new StaticArticle("first", "en_GB", title1, content1);

        List<LocalizedTextContent> title2 = Arrays.asList(new LocalizedTextContent("en_GB", "My second article"));
        List<LocalizedXmlContent> content2 = Arrays.asList(new LocalizedXmlContent("en_GB", "<xml><content>Hi there!</content>"));
        Article article2 = new StaticArticle("second", "en_GB", title2, content2);

        List<LocalizedTextContent> title3 = Arrays.asList(new LocalizedTextContent("en_GB", "My third article"));
        Article article3 = new TemplateDrivenArticle("second", "en_GB", title3, "myFirstStructure", "myFirstTemplate",
                "  <dynamic-element name=\"sdf\" index=\"0\" type=\"boolean\" index-type=\"keyword\">\n" +
                        "        <dynamic-content language-id=\"en_US\"><![CDATA[true]]></dynamic-content>\n" +
                        "    </dynamic-element>");

        WebContent webContent = new WebContent();
        webContent.setStructures(new ArticleStructures(Collections.singletonList(structure)));
        webContent.setTemplates(new ArticleTemplates(Collections.singletonList(template)));
        webContent.setArticles(new Articles(Arrays.asList(article1, article3, article2)));
        site1.getSiteContent().setWebContent(webContent);

        DocumentLibrary dl = new DocumentLibrary("/data", null);
        DocumentLibraryItem folder1 = new DocumentLibraryFolder("doc1");
        folder1.setDescription("My folder 1");
        DocumentLibraryItem folder2 = new DocumentLibraryFolder("doc2");
        DocumentLibraryItem file1 = new DocumentLibraryFile("test", "test.txt");
        file1.setDescription("My file 1");
        DocumentLibraryItem file2 = new DocumentLibraryFile("test2", "test2.txt");

        ((DocumentLibraryFile) file1).setAssetTags(Arrays.asList("foo", "bar"));

        dl.setRootItems(Arrays.asList(folder1));
        ((DocumentLibraryFolder) folder1).setSubItems(Arrays.asList(folder2, file1));
        ((DocumentLibraryFolder) folder2).setSubItems(Arrays.asList(file2));

        site1.getSiteContent().setDocumentLibrary(dl);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        CliwixXmlSerializer.toXML(liferayConfig, baos);
        String xml = baos.toString();
        System.out.println(xml);

        assertTrue(xml.length() > 1000);
        assertTrue(xml.contains("xmlns=\"http://nonblocking.at/cliwix\""));
        assertTrue(xml.contains("xsi:schemaLocation=\"http://nonblocking.at/cliwix cliwix_1_1.xsd\""));
        assertTrue(xml.contains("xmlns:xi=\"http://www.w3.org/2001/XInclude\""));
    }

    @Test
    public void deserializeSiteMembersTest() throws Exception {
        SiteMembers siteMembers = JAXB.unmarshal(new FileInputStream("src/test/resources/site-members-test.xml"), SiteMembers.class);

        assertNotNull(siteMembers.getMemberUserGroups());
        assertEquals(2, siteMembers.getMemberUserGroups().size());
    }

    @Test
    public void deserializationTest() throws Exception {

        LiferayConfig config = CliwixXmlSerializer.fromXML(new File("src/test/resources/liferay-config-test.xml"));

        assertNotNull(config);
        assertEquals("CliwixDemo", config.getCompanies().getList().get(0).getWebId());
        assertEquals("Guest", config.getCompanies().getList().get(0).getSites().getList().get(0).getName());
    }

    @Test(expected = CliwixSchemaValidationException.class)
    public void deserializationOfInvalidXMLTest() throws Exception {
        CliwixXmlSerializer.fromXML(new File("src/test/resources/liferay-config-invalid.xml"));
    }

    @Test
    public void deserializationOfInvalidXMLWithCustomListenerTest() throws Exception {
        final List<String> messages = new ArrayList<>();

        CliwixXmlSerializer.fromXML(new File("src/test/resources/liferay-config-invalid.xml"), null, new ValidationEventHandler() {
                    @Override
                    public boolean handleEvent(ValidationEvent event) {
                        System.out.println("Severity: " + event.getSeverity() + ", message: " + event.getMessage() + ", location: " + event.getLocator().toString());
                        messages.add(event.getMessage());
                        return true;
                    }
                }
        );

        assertEquals(8, messages.size());
    }

    @Test
    public void deserializationWithIncludesTest() throws Exception {
        LiferayConfig config = CliwixXmlSerializer.fromXML(new File("src/test/resources/liferay-config-with-includes.xml"));

        assertNotNull(config);
        assertEquals("CliwixDemo", config.getCompanies().getList().get(0).getWebId());
        assertNotNull(config.getCompanies().getList().get(0).getUsers());
        assertEquals(3, config.getCompanies().getList().get(0).getUsers().getList().size());
        assertEquals("Guest", config.getCompanies().getList().get(0).getSites().getList().get(0).getName());
    }

}
