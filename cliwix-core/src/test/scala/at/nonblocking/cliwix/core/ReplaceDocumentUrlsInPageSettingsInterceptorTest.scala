package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.expression.{ExpressionResolver, ExpressionGenerator}
import at.nonblocking.cliwix.core.interceptor.ReplaceDocumentUrlsInPageSettingsInterceptor
import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.model._
import org.junit.Test
import org.mockito.Mockito._
import org.junit.Assert._
import scala.collection.JavaConversions._

class ReplaceDocumentUrlsInPageSettingsInterceptorTest {

  @Test
  def testReplaceDocumentUrl() = {
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockExpressionGenerator.createExpression(5566, "groupId", classOf[Site])).thenReturn(Some("{{Site('mySite').groupId}}"))
    when(mockExpressionGenerator.createExpression(1122, "folderId", classOf[DocumentLibraryFolder])).thenReturn(Some("{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}"))
    when(mockGroupUtil.getLiferayEntityForGroupId(123456)).thenReturn(Some((classOf[Site], 5566L)))

    val page = new Page(PAGE_TYPE.URL, null, null)
    page.setPageSettings(List(new PageSetting("url", "/documents/123456/1122/cliwix_logo.png/0b0de96c-06de-4901-a337-ceb8be1b5080?t=1404561936672")))

    val interceptor = new ReplaceDocumentUrlsInPageSettingsInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)
    interceptor.setGroupUtil(mockGroupUtil)

    interceptor.afterEntityExport(page, 22)

    assertEquals("/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/cliwix_logo.png", page.getPageSettings.head.getValue)
  }

  @Test
  def testResolveDocumentUrl() = {
    val mockExpressionResolver = mock(classOf[ExpressionResolver])

    when(mockExpressionResolver.expressionToStringValue("{{Site('mySite').groupId}}", 22)).thenReturn("123456")
    when(mockExpressionResolver.expressionToStringValue("{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}", 22)).thenReturn("1122")

    val page = new Page(PAGE_TYPE.URL, null, null)
    page.setPageSettings(List(new PageSetting("url", "/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/cliwix_logo.png")))

    val interceptor = new ReplaceDocumentUrlsInPageSettingsInterceptor()
    interceptor.setExpressionResolver(mockExpressionResolver)

    interceptor.beforeEntityInsert(page, 22)

    assertEquals("/documents/123456/1122/cliwix_logo.png", page.getPageSettings.head.getValue)
  }

}
