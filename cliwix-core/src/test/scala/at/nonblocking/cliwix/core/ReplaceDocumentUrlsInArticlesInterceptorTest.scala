package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.expression.{ExpressionResolver, ExpressionGenerator}
import at.nonblocking.cliwix.core.interceptor.ReplaceDocumentUrlsInArticlesInterceptor
import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.model._
import org.junit.Test

import org.junit.Assert._
import org.mockito.Mockito._

import scala.collection.JavaConversions._

class ReplaceDocumentUrlsInArticlesInterceptorTest {

  @Test
  def testReplaceStaticArticle() = {
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockExpressionGenerator.createExpression(5566, "groupId", classOf[Site])).thenReturn(Some("{{Site('mySite').groupId}}"))
    when(mockExpressionGenerator.createExpression(1122, "folderId", classOf[DocumentLibraryFolder])).thenReturn(Some("{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}"))
    when(mockGroupUtil.getLiferayEntityForGroupId(123456)).thenReturn(Some((classOf[Site], 5566L)))

    val article = new StaticArticle()
    article.setContents(List(
      new LocalizedXmlContent("de_DE", "<p>test </p><img src=\"/documents/123456/1122/cliwix_logo.png/0b0de96c-06de-4901-a337-ceb8be1b5080?t=1404561936672 \" /><p> test</p>"),
      new LocalizedXmlContent("en_GB", "<p>test </p><img src=\"/documents/123456/1122/cliwix_logo.png/0b0de96c-06de-4901-a337-ceb8be1b5080?t=1404561936672 \" /><p> test</p>")
    ))

    val interceptor = new ReplaceDocumentUrlsInArticlesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)
    interceptor.setGroupUtil(mockGroupUtil)

    interceptor.afterEntityExport(article, 22)

    assertEquals("<p>test </p><img src=\"/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/cliwix_logo.png\" /><p> test</p>", article.getContents()(0).getXml)
    assertEquals("<p>test </p><img src=\"/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/cliwix_logo.png\" /><p> test</p>", article.getContents()(1).getXml)
  }

  @Test
  def testReplaceStaticArticle2() = {
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockExpressionGenerator.createExpression(5566, "groupId", classOf[Site])).thenReturn(Some("{{Site('mySite').groupId}}"))
    when(mockExpressionGenerator.createExpression(370704, "folderId", classOf[DocumentLibraryFolder])).thenReturn(Some("{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}"))
    when(mockGroupUtil.getLiferayEntityForGroupId(10180)).thenReturn(Some((classOf[Site], 5566L)))

    val article = new StaticArticle()
    article.setContents(List(
      new LocalizedXmlContent("de_DE", "<img src=\"/documents/10180/370704/Willkommen/6c387e4a-ea21-4e20-ae6c-6dfbd7c86ab4?t=1386828806150\" style=\"width:512px;height:180px;display:inline-block;float:left;border-radius:8px;margin-bottom:15px;-webkit-border-radius:8px;-moz-border-radius:8px;\" /></p>")
    ))

    val interceptor = new ReplaceDocumentUrlsInArticlesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)
    interceptor.setGroupUtil(mockGroupUtil)

    interceptor.afterEntityExport(article, 22)

    assertEquals("<img src=\"/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/Willkommen\" style=\"width:512px;height:180px;display:inline-block;float:left;border-radius:8px;margin-bottom:15px;-webkit-border-radius:8px;-moz-border-radius:8px;\" /></p>", article.getContents()(0).getXml)
  }

  @Test
  def testReplaceTemplateDrivenArticle() = {
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockExpressionGenerator.createExpression(5566, "groupId", classOf[Site])).thenReturn(Some("{{Site('mySite').groupId}}"))
    when(mockExpressionGenerator.createExpression(1122, "folderId", classOf[DocumentLibraryFolder])).thenReturn(Some("{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}"))
    when(mockGroupUtil.getLiferayEntityForGroupId(123456)).thenReturn(Some((classOf[Site], 5566L)))

    val article = new TemplateDrivenArticle()
    article.setDynamicElements("<![CDATA[ <dynamic-element instance-id=\"XbU4Tt8d\" name=\"page-title\" type=\"text\" index-type=\"\"> <dynamic-content language-id=\"de_DE\"><![CDATA[jojo]]]]>\n\n<![CDATA[></dynamic-content> <dynamic-content language-id=\"en_US\"><![CDATA[See how Liferay can change the way you do business.]]]]>\n\n<![CDATA[></dynamic-content> </dynamic-element> <dynamic-element instance-id=\"zLvpsWs9\" name=\"links\" type=\"text\" index-type=\"\"> <dynamic-element instance-id=\"XbnjZ8Kf\" name=\"bg-image\" type=\"document_library\" index-type=\"\"> <dynamic-content><![CDATA[/documents/123456/1122/cliwix_logo.png/0b0de96c-06de-4901-a337-ceb8be1b5080?t=1404561936672]]]]>\n\n<![CDATA[></dynamic-content> </dynamic-element> ")

    val interceptor = new ReplaceDocumentUrlsInArticlesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)
    interceptor.setGroupUtil(mockGroupUtil)

    interceptor.afterEntityExport(article, 22)

    assertEquals("<![CDATA[ <dynamic-element instance-id=\"XbU4Tt8d\" name=\"page-title\" type=\"text\" index-type=\"\"> <dynamic-content language-id=\"de_DE\"><![CDATA[jojo]]]]>\n\n<![CDATA[></dynamic-content> <dynamic-content language-id=\"en_US\"><![CDATA[See how Liferay can change the way you do business.]]]]>\n\n<![CDATA[></dynamic-content> </dynamic-element> <dynamic-element instance-id=\"zLvpsWs9\" name=\"links\" type=\"text\" index-type=\"\"> <dynamic-element instance-id=\"XbnjZ8Kf\" name=\"bg-image\" type=\"document_library\" index-type=\"\"> <dynamic-content><![CDATA[/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/cliwix_logo.png]]]]>\n\n<![CDATA[></dynamic-content> </dynamic-element> ", article.getDynamicElements)
  }

  @Test
  def testReplaceTemplateDrivenArticle2() = {
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockExpressionGenerator.createExpression(5566, "groupId", classOf[Site])).thenReturn(Some("{{Site('mySite').groupId}}"))
    when(mockExpressionGenerator.createExpression(20075, "folderId", classOf[DocumentLibraryFolder])).thenReturn(Some("{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}"))
    when(mockGroupUtil.getLiferayEntityForGroupId(13203)).thenReturn(Some((classOf[Site], 5566L)))

    val article = new TemplateDrivenArticle()
    article.setDynamicElements("<dynamic-element name=\"desktopImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[/documents/13203/20075/Startpage_2_SK.png]]></dynamic-content>  </dynamic-element>  <dynamic-element name=\"tabletImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[/documents/13203/20075/key+visual+for+tablet]]></dynamic-content>  </dynamic-element>  <dynamic-element name=\"mobileImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[/documents/13203/20075/news]]></dynamic-content>  </dynamic-element>")

    val interceptor = new ReplaceDocumentUrlsInArticlesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)
    interceptor.setGroupUtil(mockGroupUtil)

    interceptor.afterEntityExport(article, 22)

    assertEquals("<dynamic-element name=\"desktopImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/Startpage_2_SK.png]]></dynamic-content>  </dynamic-element>  <dynamic-element name=\"tabletImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/key+visual+for+tablet]]></dynamic-content>  </dynamic-element>  <dynamic-element name=\"mobileImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/news]]></dynamic-content>  </dynamic-element>", article.getDynamicElements)
  }

  @Test
  def testReplaceMultiline() = {
    val mockExpressionGenerator = mock(classOf[ExpressionGenerator])
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockExpressionGenerator.createExpression(5566, "groupId", classOf[Site])).thenReturn(Some("{{Site('mySite').groupId}}"))
    when(mockExpressionGenerator.createExpression(0, "folderId", classOf[DocumentLibraryFolder])).thenReturn(Some("{{DocumentLibraryFolder(Site('mySite').groupId,'/').folderId}}"))
    when(mockGroupUtil.getLiferayEntityForGroupId(19)).thenReturn(Some((classOf[Site], 5566L)))

    val article = new StaticArticle()
    article.setContents(List(
      new LocalizedXmlContent("de_DE",
        """
        <![CDATA[<style type="text/css">
         	.content-area.selected {
         		background: url(/documents/19/0/welcome_bg_8/334d5fbd-a014-41dc-a5bf-a6256d79ffb0?version=1.0&t=1405671204954) 100% 0 no-repeat;
         	}
         </style>

         <div class="navigation-wrapper">
         	<header class="content-head content-head-liferay-portal">
         		<hgroup>
         			<h1>
         				Liferay helps you build feature-rich, easy-to-use web applications quickly.
         			</h1>

         			<hr />
         		</hgroup>

         		<p>
         			Here are some of our customers from around the globe:
         		</p>

         		<ul class="left">
         			<li><span>Rolex</span></li>
         			<li><span>Bugaboo</span></li>
         			<li><span>Deluxe Corporation</span></li>
         			<li><span>Domino's Pizza</span></li>
         			<li><span>BASF</span></li>
         		</ul>

         		<ul class="right">
         			<li><span>Honda</span></li>
         			<li><span>GE Capital</span></li>
         			<li><span>Sesame Street</span></li>
         			<li><span>China Mobile</span></li>
         			<li><span>York University</span></li>
         		</ul>
         	</header>

         	<div class="content-area selected">
         		<a href="//www.liferay.com/users?wh=8" id="marketplace">&nbsp;</a>
         	</div>
        </div>]]>
        """
    )))

    val interceptor = new ReplaceDocumentUrlsInArticlesInterceptor()
    interceptor.setExpressionGenerator(mockExpressionGenerator)
    interceptor.setGroupUtil(mockGroupUtil)

    interceptor.afterEntityExport(article, 22)

    assertEquals(
      """
        <![CDATA[<style type="text/css">
         	.content-area.selected {
         		background: url(/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/').folderId}}/welcome_bg_8) 100% 0 no-repeat;
         	}
         </style>

         <div class="navigation-wrapper">
         	<header class="content-head content-head-liferay-portal">
         		<hgroup>
         			<h1>
         				Liferay helps you build feature-rich, easy-to-use web applications quickly.
         			</h1>

         			<hr />
         		</hgroup>

         		<p>
         			Here are some of our customers from around the globe:
         		</p>

         		<ul class="left">
         			<li><span>Rolex</span></li>
         			<li><span>Bugaboo</span></li>
         			<li><span>Deluxe Corporation</span></li>
         			<li><span>Domino's Pizza</span></li>
         			<li><span>BASF</span></li>
         		</ul>

         		<ul class="right">
         			<li><span>Honda</span></li>
         			<li><span>GE Capital</span></li>
         			<li><span>Sesame Street</span></li>
         			<li><span>China Mobile</span></li>
         			<li><span>York University</span></li>
         		</ul>
         	</header>

         	<div class="content-area selected">
         		<a href="//www.liferay.com/users?wh=8" id="marketplace">&nbsp;</a>
         	</div>
        </div>]]>
        """, article.getContents()(0).getXml)
  }

  @Test
  def testResolve() = {
    val mockExpressionResolver = mock(classOf[ExpressionResolver])

    when(mockExpressionResolver.expressionToStringValue("{{Site('mySite').groupId}}", 22)).thenReturn("123456")
    when(mockExpressionResolver.expressionToStringValue("{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}", 22)).thenReturn("1122")

    val article = new StaticArticle()
    article.setContents(List(
      new LocalizedXmlContent("de_DE", "<p>test </p><img src=\"/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/cliwix_logo.png\" /><p> test</p>"),
      new LocalizedXmlContent("en_GB", "<p>test </p><img src=\"/documents/{{Site('mySite').groupId}}/{{DocumentLibraryFolder(Site('mySite').groupId,'/folder1').folderId}}/cliwix_logo.png\" /><p> test</p>")
    ))

    val interceptor = new ReplaceDocumentUrlsInArticlesInterceptor()
    interceptor.setExpressionResolver(mockExpressionResolver)

    interceptor.beforeEntityInsert(article, 22)

    assertEquals("<p>test </p><img src=\"/documents/123456/1122/cliwix_logo.png\" /><p> test</p>", article.getContents()(0).getXml)
    assertEquals("<p>test </p><img src=\"/documents/123456/1122/cliwix_logo.png\" /><p> test</p>", article.getContents()(1).getXml)
  }

}
