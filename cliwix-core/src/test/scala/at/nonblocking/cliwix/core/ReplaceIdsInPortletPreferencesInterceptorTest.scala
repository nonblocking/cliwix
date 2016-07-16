package at.nonblocking.cliwix.core

import java.io.File

import at.nonblocking.cliwix.core.expression.{ExpressionResolver, ExpressionGenerator}
import at.nonblocking.cliwix.core.interceptor.ReplaceIdsInPortletPreferencesInterceptor
import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.model._
import org.junit.Test
import org.junit.Assert._
import org.mockito.Mockito._

import scala.collection.JavaConversions._

class ReplaceIdsInPortletPreferencesInterceptorTest {

  @Test
  def testReplacementConfiguration() = {
    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    val replacements = interceptor.replacements

    assertTrue(replacements.contains("groupId"))
    assertEquals("groupId", replacements("groupId").key)
    assertEquals(classOf[Group], replacements("groupId").entityClass)
    assertEquals("groupId", replacements("groupId").propertyName)
    assertFalse(replacements("groupId").multiple)
    assertTrue(replacements("groupId").skipOnFailure)
  }

  @Test
  def testReplacementCustomConfiguration() = {
    System.setProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY, new File("src/test/resources").getAbsolutePath)

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    val replacements = interceptor.replacements

    assertTrue(replacements.contains("foo"))
    assertEquals("foo", replacements("foo").key)
    assertEquals(classOf[Company], replacements("foo").entityClass)
    assertEquals("webId", replacements("foo").propertyName)
    assertTrue(replacements("foo").multiple)
    assertEquals(",", replacements("foo").delimiter)
    assertFalse(replacements("foo").skipOnFailure)
  }

  @Test
  def testReplaceGroupId() = {
    val mockGroupUtil = mock(classOf[GroupUtil])
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])

    when(mockGroupUtil.getLiferayEntityForGroupId(123456)).thenReturn(Some((classOf[Site], 445566L)))
    when(mockExpressionGenerator.createExpression(445566, "groupId", classOf[Site])).thenReturn(Some("{{Site('mySite').groupId}}"))

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setGroupUtil(mockGroupUtil)
    interceptor.setExpressionGenerator(mockExpressionGenerator)

    val portletConfiguration = new PortletConfiguration("portlet1", List(new Preference("x", "y"), new Preference("groupId", "123456 ")))

    interceptor.afterEntityExport(portletConfiguration, 22)

    assertEquals("y", portletConfiguration.getPreferences()(0).getValue)
    assertEquals("{{Site('mySite').groupId}}", portletConfiguration.getPreferences()(1).getValue)
  }

  @Test
  def testReplaceGroupIdValueList() = {
    val mockGroupUtil = mock(classOf[GroupUtil])
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])

    when(mockGroupUtil.getLiferayEntityForGroupId(123456)).thenReturn(Some((classOf[Site], 445566L)))
    when(mockGroupUtil.getLiferayEntityForGroupId(22222)).thenReturn(None)
    when(mockExpressionGenerator.createExpression(445566, "groupId", classOf[Site])).thenReturn(Some("{{Site('mySite').groupId}}"))

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setGroupUtil(mockGroupUtil)
    interceptor.setExpressionGenerator(mockExpressionGenerator)

    val portletConfiguration = new PortletConfiguration("portlet1", List(new Preference("x", "y"), new Preference("groupId", List("123456", "22222"))))

    interceptor.afterEntityExport(portletConfiguration, 22)

    assertEquals("y", portletConfiguration.getPreferences()(0).getValue)
    assertEquals("{{Site('mySite').groupId}}", portletConfiguration.getPreferences()(1).getValues()(0))
    assertEquals("22222", portletConfiguration.getPreferences()(1).getValues()(1))
  }

  @Test
  def testReplaceClassNameId() = {
    System.setProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY, new File("src/test/resources").getAbsolutePath)

    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])

    when(mockExpressionGenerator.createExpression(123456, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('foo').classNameId}}"))

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)

    val portletConfiguration = new PortletConfiguration("portlet1", List(new Preference("x", "y"), new Preference("classNameId", "123456")))

    interceptor.afterEntityExport(portletConfiguration, 22)

    assertEquals("y", portletConfiguration.getPreferences()(0).getValue)
    assertEquals("{{ClassName('foo').classNameId}}", portletConfiguration.getPreferences()(1).getValue)
  }

  @Test
  def testReplaceAnyAssetType() = {
    System.setProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY, new File("src/test/resources").getAbsolutePath)

    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])

    when(mockExpressionGenerator.createExpression(123456, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('foo').classNameId}}"))

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)

    val portletConfiguration = new PortletConfiguration("portlet1", List(new Preference("x", "y"), new Preference("anyAssetType", "123456"), new Preference("anyAssetType", "true")))

    interceptor.afterEntityExport(portletConfiguration, 22)

    assertEquals("y", portletConfiguration.getPreferences()(0).getValue)
    assertEquals("{{ClassName('foo').classNameId}}", portletConfiguration.getPreferences()(1).getValue)
    assertEquals("true", portletConfiguration.getPreferences()(2).getValue)
  }

  @Test
  def testReplaceMultipleClassNameIds() = {
    System.setProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY, new File("src/test/resources").getAbsolutePath)

    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])

    when(mockExpressionGenerator.createExpression(123456, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('foo').classNameId}}"))
    when(mockExpressionGenerator.createExpression(234567, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('bar').classNameId}}"))

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)

    val portletConfiguration = new PortletConfiguration("portlet1", List(new Preference("x", "y"), new Preference("classNameIds", List("123456", "234567"))))

    interceptor.afterEntityExport(portletConfiguration, 22)

    assertEquals("y", portletConfiguration.getPreferences()(0).getValue)
    assertEquals("{{ClassName('foo').classNameId}}", portletConfiguration.getPreferences()(1).getValues()(0))
    assertEquals("{{ClassName('bar').classNameId}}", portletConfiguration.getPreferences()(1).getValues()(1))
  }


  @Test(expected = classOf[CliwixException])
  def testFailOnReplace() = {
    System.setProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY, new File("src/test/resources").getAbsolutePath)

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()

    val portletConfiguration = new PortletConfiguration("portlet1", List(new Preference("x", "y"), new Preference("foo", "bar")))

    interceptor.afterEntityExport(portletConfiguration, 22)
  }

  @Test
  def testResolveGroupId() = {
    val mockExpressionResolver = mock(classOf[ExpressionResolver])

    when(mockExpressionResolver.expressionToStringValue("{{Site('mySite').groupId}}", 22)).thenReturn("123456")

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setExpressionResolver(mockExpressionResolver)

    val portletConfiguration = new PortletConfiguration("portlet1", List(new Preference("foo", "bar"), new Preference("groupId", "  {{Site('mySite').groupId}}")))

    interceptor.beforeEntityUpdate(portletConfiguration, null, 22)

    assertEquals("bar", portletConfiguration.getPreferences()(0).getValue)
    assertEquals("  123456", portletConfiguration.getPreferences()(1).getValue)
  }

  @Test
  def testResolveMultipleExpressions() = {
    System.setProperty(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY, new File("src/test/resources").getAbsolutePath)

    val mockExpressionResolver = mock(classOf[ExpressionResolver])

    when(mockExpressionResolver.expressionToStringValue("{{ClassName('foo').classNameId}}", 22)).thenReturn("123456")
    when(mockExpressionResolver.expressionToStringValue("{{ClassName('bar').classNameId}}", 22)).thenReturn("234567")

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setExpressionResolver(mockExpressionResolver)

    val portletConfiguration = new PortletConfiguration("portlet1", List(new Preference("foo", "bar"), new Preference("classNameIds", List("  {{ClassName('foo').classNameId}}", "  {{ClassName('bar').classNameId}}", "test"))))

    interceptor.beforeEntityUpdate(portletConfiguration, null, 22)

    assertEquals("bar", portletConfiguration.getPreferences()(0).getValue)
    assertEquals("  123456",  portletConfiguration.getPreferences()(1).getValues()(0))
    assertEquals("  234567", portletConfiguration.getPreferences()(1).getValues()(1))
  }

  @Test
  def testAssetPublisherConfigurationReplaceExpressions() {
    System.getProperties.remove(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY)

    val mockGroupUtil = mock(classOf[GroupUtil])
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])

    when(mockExpressionGenerator.createExpression(10007, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('com.liferay.portlet.blogs.model.BlogsEntry').classNameId}}"))
    when(mockExpressionGenerator.createExpression(10011, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('com.liferay.portlet.messageboards.model.MBMessage').classNameId}}"))
    when(mockExpressionGenerator.createExpression(10108, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('com.liferay.portlet.journal.model.JournalArticle').classNameId}}"))
    when(mockExpressionGenerator.createExpression(10009, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('com.liferay.portlet.calendar.model.CalEvent').classNameId}}"))
    when(mockExpressionGenerator.createExpression(10008, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('com.liferay.portlet.bookmarks.model.BookmarksEntry').classNameId}}"))
    when(mockExpressionGenerator.createExpression(10010, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('com.liferay.portlet.documentlibrary.model.DLFileEntry').classNameId}}"))
    when(mockExpressionGenerator.createExpression(10013, "classNameId", classOf[ClassName])).thenReturn(Some("{{ClassName('com.liferay.portlet.wiki.model.WikiPage').classNameId}}"))

    when(mockExpressionGenerator.createExpression(10300, "fileEntryTypeId", classOf[DocumentLibraryFileType])).thenReturn(Some("{{DocumentLibraryFileType(Site('mySite'),'Legal Contracts').fileEntryTypeId}}"))
    when(mockExpressionGenerator.createExpression(10302, "fileEntryTypeId", classOf[DocumentLibraryFileType])).thenReturn(Some("{{DocumentLibraryFileType(Site('mySite'),'Marketing Banner').fileEntryTypeId}}"))
    when(mockExpressionGenerator.createExpression(10304, "fileEntryTypeId", classOf[DocumentLibraryFileType])).thenReturn(Some("{{DocumentLibraryFileType(Site('mySite'),'Online Training').fileEntryTypeId}}"))
    when(mockExpressionGenerator.createExpression(10306, "fileEntryTypeId", classOf[DocumentLibraryFileType])).thenReturn(Some("{{DocumentLibraryFileType(Site('mySite'),'Sales Presentation').fileEntryTypeId}}"))

    val portletConfiguration = new PortletConfiguration("portlet1", List(
      new Preference("abstractLength", "200"),
      new Preference("classTypeIdsDLFileEntryAssetRendererFactory", "10306,10304,10300,10302"),
      new Preference("classNameIds", "10007100111010810009100081001010013"),
      new Preference("orderByColumn2", "title")
    ))

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)

    interceptor.afterEntityExport(portletConfiguration, 22)

    assertEquals("{{DocumentLibraryFileType(Site('mySite'),'Sales Presentation').fileEntryTypeId}},{{DocumentLibraryFileType(Site('mySite'),'Online Training').fileEntryTypeId}},{{DocumentLibraryFileType(Site('mySite'),'Legal Contracts').fileEntryTypeId}},"
      +"{{DocumentLibraryFileType(Site('mySite'),'Marketing Banner').fileEntryTypeId}}", portletConfiguration.getPreferences()(1).getValue)
    assertEquals("{{ClassName('com.liferay.portlet.blogs.model.BlogsEntry').classNameId}}{{ClassName('com.liferay.portlet.messageboards.model.MBMessage').classNameId}}{{ClassName('com.liferay.portlet.journal.model.JournalArticle').classNameId}}"
      + "{{ClassName('com.liferay.portlet.calendar.model.CalEvent').classNameId}}{{ClassName('com.liferay.portlet.bookmarks.model.BookmarksEntry').classNameId}}{{ClassName('com.liferay.portlet.documentlibrary.model.DLFileEntry').classNameId}}{{ClassName('com.liferay.portlet.wiki.model.WikiPage').classNameId}}", portletConfiguration.getPreferences()(2).getValue)
  }

  @Test
  def testAssetPublisherConfigurationResolveExpressions() {
    System.getProperties.remove(Cliwix.CLIWIX_CUSTOM_CONFIGURATION_FOLDER_SYSTEM_PROPERTY)

    val mockExpressionResolver = mock(classOf[ExpressionResolver])

    when(mockExpressionResolver.expressionToStringValue("{{DocumentLibraryFileType(Site('mySite'),'Sales Presentation').fileEntryTypeId}}", 22)).thenReturn("10306")
    when(mockExpressionResolver.expressionToStringValue("{{DocumentLibraryFileType(Site('mySite'),'Online Training').fileEntryTypeId}}", 22)).thenReturn("10304")
    when(mockExpressionResolver.expressionToStringValue("{{ClassName('com.liferay.portlet.blogs.model.BlogsEntry').classNameId}}", 22)).thenReturn("10007")
    when(mockExpressionResolver.expressionToStringValue("{{ClassName('com.liferay.portlet.messageboards.model.MBMessage').classNameId}}", 22)).thenReturn("10011")

    val portletConfiguration = new PortletConfiguration("portlet1", List(
      new Preference("abstractLength", "200"),
      new Preference("classTypeIdsDLFileEntryAssetRendererFactory", "{{DocumentLibraryFileType(Site('mySite'),'Sales Presentation').fileEntryTypeId}},{{DocumentLibraryFileType(Site('mySite'),'Online Training').fileEntryTypeId}}"),
      new Preference("classNameIds", "{{ClassName('com.liferay.portlet.blogs.model.BlogsEntry').classNameId}}{{ClassName('com.liferay.portlet.messageboards.model.MBMessage').classNameId}}"),
      new Preference("orderByColumn2", "title")
    ))

    val interceptor = new ReplaceIdsInPortletPreferencesInterceptor()
    interceptor.setExpressionResolver(mockExpressionResolver)

    interceptor.beforeEntityUpdate(portletConfiguration, null, 22)

    assertEquals("10306,10304", portletConfiguration.getPreferences()(1).getValue)
    assertEquals("1000710011", portletConfiguration.getPreferences()(2).getValue)
  }

}
