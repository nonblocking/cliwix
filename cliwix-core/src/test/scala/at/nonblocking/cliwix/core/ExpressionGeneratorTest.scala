package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.command.{GetByDBIdCommand, Command, CommandResult}
import at.nonblocking.cliwix.core.expression.{ExpressionGeneratorImpl}
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.core.util.GroupUtil
import at.nonblocking.cliwix.model._
import com.liferay.portlet.documentlibrary.model.DLFileEntryTypeConstants
import org.junit.Assert._
import org.mockito.Mockito._
import org.junit.Test

class ExpressionGeneratorTest  {

  @Test
  def testCreateExpressionSimple() = {
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockGroupUtil.getLiferayEntityForGroupId(333)).thenReturn(Some((classOf[Site].asInstanceOf[Class[_ <: LiferayEntity]], 5566L)))

    val generator = new ExpressionGeneratorImpl()
    generator.setHandler(new MockDispatchHandler)
    generator.setGroupUtil(mockGroupUtil)

    val expression = generator.createExpression(3, "name", classOf[Company])

    assertTrue(expression.isDefined)
    assertEquals("{{Company('myCompany').name}}", expression.get)
  }

  @Test
  def testCreateExpression() = {
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockGroupUtil.getLiferayEntityForGroupId(333)).thenReturn(Some((classOf[Site].asInstanceOf[Class[_ <: LiferayEntity]], 5566L)))

    val generator = new ExpressionGeneratorImpl()
    generator.setHandler(new MockDispatchHandler)
    generator.setGroupUtil(mockGroupUtil)

    val expression = generator.createExpression(1234L, "articleDbId", classOf[Article])

    assertTrue(expression.isDefined)
    assertEquals("{{Article(Site('mySite'),'test').articleDbId}}", expression.get)
  }


  @Test
  def testCreateExpressionWithPath() = {
    val mockGroupUtil = mock(classOf[GroupUtil])

    when(mockGroupUtil.getLiferayEntityForGroupId(333)).thenReturn(Some((classOf[Site].asInstanceOf[Class[_ <: LiferayEntity]], 5566L)))

    val generator = new ExpressionGeneratorImpl()
    generator.setHandler(new MockDispatchHandler)
    generator.setGroupUtil(mockGroupUtil)

    val expression = generator.createExpression(2233L, "fileId", classOf[DocumentLibraryFile])

    assertTrue(expression.isDefined)
    assertEquals("{{DocumentLibraryFile(Site('mySite'),'/folder1/folder2/foo.txt').fileId}}", expression.get)
  }

  @Test
  def testExpressionWithGroupIdZero() = {
    val mockGroupUtil = mock(classOf[GroupUtil])

    val generator = new ExpressionGeneratorImpl()
    generator.setHandler(new MockDispatchHandler)
    generator.setGroupUtil(mockGroupUtil)

    val expression = generator.createExpression(0, "fileEntryTypeId", classOf[DocumentLibraryFileType])

    assertTrue(expression.isDefined)
    assertEquals("{{DocumentLibraryFileType(GROUP_ID_ZERO,'Basic Document').fileEntryTypeId}}", expression.get)
  }

  class MockDispatchHandler extends DispatchHandler {
    override def execute[T](command: Command[T]): CommandResult[T] = command match {
      case GetByDBIdCommand(3, klazz) => klazz.getSimpleName match {
        case "Company" =>
          val c = new Company("myCompany", null)
          CommandResult(c).asInstanceOf[CommandResult[T]]
      }
      case GetByDBIdCommand(1234, klazz) => klazz.getSimpleName match {
        case "Article" =>
          val a = new StaticArticle("test", null, null, null)
          a.setOwnerGroupId(333)
          CommandResult(a).asInstanceOf[CommandResult[T]]
      }
      case GetByDBIdCommand(5566, klazz) => klazz.getSimpleName match {
        case "Site" =>
          val s = new Site("mySite", null, null)
          s.setSiteId(5566)
          CommandResult(s).asInstanceOf[CommandResult[T]]
      }
      case GetByDBIdCommand(2233, klazz) => klazz.getSimpleName match {
        case "DocumentLibraryFile" =>
          val file = new DocumentLibraryFile("foo", "foo.txt")
          file.setPath("/folder1/folder2/foo.txt")
          file.setOwnerGroupId(333)
          CommandResult(file).asInstanceOf[CommandResult[T]]
      }
      case GetByDBIdCommand(0, klazz) => klazz.getSimpleName match {
        case "DocumentLibraryFileType" =>
          val file = new DocumentLibraryFileType(0, DLFileEntryTypeConstants.NAME_BASIC_DOCUMENT, 0)
          CommandResult(file).asInstanceOf[CommandResult[T]]
      }
    }
  }

}
