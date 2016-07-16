package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.expression.{CliwixExpressionException, ExpressionResolverImpl}
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.model._
import com.liferay.portlet.documentlibrary.model.DLFileEntryTypeConstants
import org.junit.Assert._
import org.junit.Test

class ExpressionResolverTest {

  @Test
  def testExpressionToStringValue(): Unit = {

    val resolver = new ExpressionResolverImpl()
    resolver.setHandler(new MockDispatchHandler)

    val value = resolver.expressionToStringValue("{{Article(Site('mySite'),'test').articleDbId}}", 22)

    assertEquals("123456", value)
  }

  @Test
  def testExpressionToStringValueByPath(): Unit = {

    val resolver = new ExpressionResolverImpl()
    resolver.setHandler(new MockDispatchHandler)

    val value = resolver.expressionToStringValue("{{DocumentLibraryFile(Site('mySite'),  '/folder1/folder2/foo.txt').fileId}}", 22)

    assertEquals("8899", value)
  }

  @Test
  def testExpressionToStringValueSimple(): Unit = {

    val resolver = new ExpressionResolverImpl()
    resolver.setHandler(new MockDispatchHandler)

    val value = resolver.expressionToStringValue("{{DocumentLibraryFileType(GROUP_ID_ZERO,'Basic Document').fileEntryTypeId}}", 22)

    assertEquals("0", value)
  }

  @Test
  def testExpressionToStringValueWithGroupIdZero(): Unit = {

    val resolver = new ExpressionResolverImpl()
    resolver.setHandler(new MockDispatchHandler)

    val value = resolver.expressionToStringValue("{{Company('myCompany').webId}}", 22)

    assertEquals("myCompany", value)
  }

  @Test
  def expressionTest1(): Unit = {
    val expression = new ExpressionResolverImpl().parse("{{Article('test2').articleDbId}}")

    assertEquals("Article", expression.entityName)
    assertEquals("test2", expression.identifier)
    assertEquals("articleDbId", expression.propertyName)
  }

  @Test
  def expressionTest2(): Unit = {
    val expression = new ExpressionResolverImpl().parse("{{Article(\"test2\").articleDbId}}")

    assertEquals("Article", expression.entityName)
    assertEquals("test2", expression.identifier)
    assertEquals("articleDbId", expression.propertyName)
  }

  @Test
  def expressionTest3(): Unit = {
    val expression = new ExpressionResolverImpl().parse("{{DocumentLibraryFile(Site('mySite'), '/folder1/folder2/foo.txt').fileId}}")

    assertEquals("DocumentLibraryFile", expression.entityName)
    assertEquals("/folder1/folder2/foo.txt", expression.identifier)
    assertEquals("fileId", expression.propertyName)
  }

  @Test(expected = classOf[CliwixExpressionException])
  def expressionTest4(): Unit = {
    new ExpressionResolverImpl().parse("{{Article(test2).articleDbId}}")
  }

  @Test(expected = classOf[CliwixExpressionException])
  def expressionTest5(): Unit = {
    new ExpressionResolverImpl().parse("{{Article(test2)articleDbId}}")
  }

  class MockDispatchHandler extends DispatchHandler {
    override def execute[T](command: Command[T]): CommandResult[T] = command match {
      case GetByIdentifierOrPathCommand("myCompany", 22, klazz) =>
        val c = new Company("myCompany", new CompanyConfiguration("www.nonblocking.at", null, null, null))
        CommandResult(c).asInstanceOf[CommandResult[T]]
      case GetByIdentifierOrPathWithinGroupCommand("test", 22, 5566, klazz) =>
        val a = new StaticArticle("test", null, null, null)
        a.setArticleDbId(123456)
        CommandResult(a).asInstanceOf[CommandResult[T]]
      case GetByIdentifierOrPathCommand("mySite", 22, klazz) =>
        val s = new Site("mySite", null, null)
        s.setSiteId(5566)
        CommandResult(s).asInstanceOf[CommandResult[T]]
      case GetByIdentifierOrPathWithinGroupCommand("/folder1/folder2/foo.txt", 22, 5566, klazz) =>
        val file = new DocumentLibraryFile("foo", "foo.txt")
        file.setPath("/folder1/folder2/foo.txt")
        file.setFileId(8899)
        CommandResult(file).asInstanceOf[CommandResult[T]]
      case GetByIdentifierOrPathWithinGroupCommand("Basic Document", 22, 0, klazz) =>
        val file = new DocumentLibraryFileType(0, "Basic Document", 0)
        CommandResult(file).asInstanceOf[CommandResult[T]]
    }
  }

}
