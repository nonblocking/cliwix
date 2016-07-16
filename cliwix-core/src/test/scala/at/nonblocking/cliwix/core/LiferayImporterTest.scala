package at.nonblocking.cliwix.core

import java.{util => jutil}

import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparatorImpl
import at.nonblocking.cliwix.core.handler.DispatchHandler
import at.nonblocking.cliwix.core.interceptor.ProcessingInterceptorDispatcher
import at.nonblocking.cliwix.model._
import org.junit.Assert._
import org.junit.Test
import org.mockito.Mockito._

import scala.collection.JavaConversions._
import scala.collection.mutable

class LiferayImporterTest {

  @Test
  def processTreeNoExistingTest(): Unit = {

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)

    val targetPageSet = new PageSet
    val existingPageSet = new PageSet

    val page1 = new Page(PAGE_TYPE.PORTLET, "/a", null)
    val page1_1 = new Page(PAGE_TYPE.PORTLET, "/aa", null)
    val page1_2 = new Page(PAGE_TYPE.PORTLET, "/ab", null)
    page1.setSubPages(List(page1_1, page1_2))
    val page2 = new Page(PAGE_TYPE.PORTLET, "/b", null)

    targetPageSet.setPages(new Pages(List(page1, page2)))
    targetPageSet.getPages.setImportPolicy(IMPORT_POLICY.UPDATE_INSERT)
    existingPageSet.setPages(null)

    when(mockHandler.execute(PageInsertCommand(page1, null, null))).thenReturn(CommandResult(page1))
    when(mockHandler.execute(PageInsertCommand(page1_1, page1, null))).thenReturn(CommandResult(page1_1))
    when(mockHandler.execute(PageInsertCommand(page1_2, page1, null))).thenReturn(CommandResult(page1_2))
    when(mockHandler.execute(PageInsertCommand(page2, null, null))).thenReturn(CommandResult(page2))

    val order = inOrder(mockHandler)

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importer.processTree[Page](targetPageSet.getPages, existingPageSet.getPages, 0, (p, parent) => PageInsertCommand(p, parent, null), deferredDeletes)

    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page1, null, null))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page1_1, page1, null))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page1_2, page1, null))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page2, null, null))
  }

  @Test
  def processTreePolicyInsertTest(): Unit = {

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)

    val targetPageSet = new PageSet
    val existingPageSet = new PageSet

    val existingPage1 = new Page(PAGE_TYPE.PORTLET, "/a", null)
    existingPage1.setPortletLayoutId(1L)
    val existingPage1_1 = new Page(PAGE_TYPE.PORTLET, "/aa", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage1_1.setPortletLayoutId(11L)
    existingPage1.setSubPages(List(existingPage1_1))
    val existingPage2 = new Page(PAGE_TYPE.PORTLET, "/b", null)
    existingPage2.setPortletLayoutId(2L)
    val existingPage2_1 = new Page(PAGE_TYPE.PORTLET, "/ba", null)
    existingPage2_1.setPortletLayoutId(21L)
    existingPage2.setSubPages(List(existingPage2_1))
    val existingPage3 = new Page(PAGE_TYPE.PORTLET, "/x", null)
    existingPage3.setPortletLayoutId(3L)

    existingPageSet.setPages(new Pages(List(existingPage1, existingPage2, existingPage3)))

    val page1 = new Page(PAGE_TYPE.PORTLET, "/a", null)
    val page1_1 = new Page(PAGE_TYPE.PORTLET, "/aa", List(new LocalizedTextContent("de_DE", "test")))
    val page1_2 = new Page(PAGE_TYPE.PORTLET, "/ab", null)
    page1.setSubPages(List(page1_1, page1_2))
    val page2 = new Page(PAGE_TYPE.PORTLET, "/b", null)
    val page2_1 = new Page(PAGE_TYPE.PORTLET, "/ba", null)
    page2.setSubPages(List(page2_1))
    val page2_1_1 = new Page(PAGE_TYPE.PORTLET, "/baa", null)
    page2_1.setSubPages(List(page2_1_1))
    val page3 = new Page(PAGE_TYPE.PORTLET, "/c", null)

    targetPageSet.setPages(new Pages(List(page1, page2, page3)))
    targetPageSet.getPages.setImportPolicy(IMPORT_POLICY.INSERT)

    when(mockHandler.execute(PageInsertCommand(page1_2, page1, null))).thenReturn(CommandResult(page1_2))
    when(mockHandler.execute(PageInsertCommand(page2_1_1, page2_1, null))).thenReturn(CommandResult(page2_1_1))
    when(mockHandler.execute(PageInsertCommand(page3, null, null))).thenReturn(CommandResult(page3))

    val order = inOrder(mockHandler)

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importer.processTree[Page](targetPageSet.getPages, existingPageSet.getPages, 0, (p, parent) => PageInsertCommand(p, parent, null), deferredDeletes)

    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page1_2, page1, null))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page2_1_1, page2_1, null))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page3, null, null))
  }

  @Test
  def processTreePolicyUpdateInsertTest(): Unit = {

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)
    val comparator = new LiferayEntityComparatorImpl
    importer.setLiferayEntityComparator(comparator)

    val targetPageSet = new PageSet
    val existingPageSet = new PageSet

    val existingPage1 = new Page(PAGE_TYPE.PORTLET, "/a", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage1.setPortletLayoutId(1L)
    val existingPage1_1 = new Page(PAGE_TYPE.PORTLET, "/aa", List(new LocalizedTextContent("de_DE", "test")))
    existingPage1_1.setPortletLayoutId(11L)
    existingPage1.setSubPages(List(existingPage1_1))
    val existingPage2 = new Page(PAGE_TYPE.PORTLET, "/b", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage2.setPortletLayoutId(2L)
    val existingPage2_1 = new Page(PAGE_TYPE.PORTLET, "/ba", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage2_1.setPortletLayoutId(21L)
    existingPage2.setSubPages(List(existingPage2_1))
    val existingPage3 = new Page(PAGE_TYPE.PORTLET, "/x", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage3.setPortletLayoutId(3L)

    existingPageSet.setPages(new Pages(List(existingPage1, existingPage2, existingPage3)))

    val page1 = new Page(PAGE_TYPE.PORTLET, "/a", List(new LocalizedTextContent("de_DE", "test")))
    val page1_1 = new Page(PAGE_TYPE.PORTLET, "/aa", List(new LocalizedTextContent("de_DE", "test")))
    val page1_2 = new Page(PAGE_TYPE.PORTLET, "/ab", List(new LocalizedTextContent("de_DE", "test")))
    page1.setSubPages(List(page1_1, page1_2))
    val page2 = new Page(PAGE_TYPE.PORTLET, "/b", List(new LocalizedTextContent("de_DE", "test")))
    val page2_1 = new Page(PAGE_TYPE.PORTLET, "/ba", List(new LocalizedTextContent("de_DE", "test")))
    page2.setSubPages(List(page2_1))
    val page2_1_1 = new Page(PAGE_TYPE.PORTLET, "/baa", List(new LocalizedTextContent("de_DE", "test")))
    page2_1.setSubPages(List(page2_1_1))
    val page3 = new Page(PAGE_TYPE.PORTLET, "/c", List(new LocalizedTextContent("de_DE", "test")))

    targetPageSet.setPages(new Pages(List(page1, page2, page3)))
    targetPageSet.getPages.setImportPolicy(IMPORT_POLICY.UPDATE_INSERT)

    when(mockHandler.execute(PageInsertCommand(page1_2, page1, null))).thenReturn(CommandResult(page1_2))
    when(mockHandler.execute(PageInsertCommand(page2_1_1, page2_1, null))).thenReturn(CommandResult(page2_1_1))
    when(mockHandler.execute(PageInsertCommand(page3, null, null))).thenReturn(CommandResult(page3))

    when(mockHandler.execute(UpdateCommand(page1))).thenReturn(CommandResult(page1))
    when(mockHandler.execute(UpdateCommand(page2))).thenReturn(CommandResult(page2))
    when(mockHandler.execute(UpdateCommand(page2_1))).thenReturn(CommandResult(page2_1))

    val order = inOrder(mockHandler)

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importer.processTree[Page](targetPageSet.getPages, existingPageSet.getPages, 0, (p, parent) => PageInsertCommand(p, parent, null), deferredDeletes)

    assertEquals(0, deferredDeletes.size())

    order.verify(mockHandler, times(1)).execute(UpdateCommand(page1))

    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page1_2, page1, null))
    order.verify(mockHandler, times(1)).execute(UpdateCommand(page2))
    order.verify(mockHandler, times(1)).execute(UpdateCommand(page2_1))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page2_1_1, page2_1, null))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page3, null, null))
  }

  @Test
  def processTreePolicyEnforceTest(): Unit = {

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)
    val comparator = new LiferayEntityComparatorImpl
    importer.setLiferayEntityComparator(comparator)

    val targetPageSet = new PageSet
    val existingPageSet = new PageSet

    val existingPage1 = new Page(PAGE_TYPE.PORTLET, "/a", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage1.setPath("publicPages:/a")
    existingPage1.setPortletLayoutId(1L)
    val existingPage1_1 = new Page(PAGE_TYPE.PORTLET, "/aa", List(new LocalizedTextContent("de_DE", "test")))
    existingPage1_1.setPortletLayoutId(11L)
    existingPage1_1.setPath("publicPages:/aa")
    existingPage1.setSubPages(List(existingPage1_1))
    val existingPage2 = new Page(PAGE_TYPE.PORTLET, "/b", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage2.setPortletLayoutId(2L)
    existingPage2.setPath("publicPages:/b")
    val existingPage2_1 = new Page(PAGE_TYPE.PORTLET, "/ba", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage2_1.setPortletLayoutId(21L)
    existingPage2_1.setPath("publicPages:/ba")
    existingPage2.setSubPages(List(existingPage2_1))
    val existingPage3 = new Page(PAGE_TYPE.PORTLET, "/x", List(new LocalizedTextContent("en_GB", "test2")))
    existingPage3.setPortletLayoutId(3L)
    existingPage3.setPath("publicPages:/x")

    existingPageSet.setPages(new Pages(new jutil.ArrayList))
    existingPageSet.getPages.getRootPages.add(existingPage1)
    existingPageSet.getPages.getRootPages.add(existingPage2)
    existingPageSet.getPages.getRootPages.add(existingPage3)

    val page1 = new Page(PAGE_TYPE.PORTLET, "/a", List(new LocalizedTextContent("de_DE", "test")))
    val page1_1 = new Page(PAGE_TYPE.PORTLET, "/aa", List(new LocalizedTextContent("de_DE", "test")))
    val page1_2 = new Page(PAGE_TYPE.PORTLET, "/ab", List(new LocalizedTextContent("de_DE", "test")))
    page1.setSubPages(List(page1_1, page1_2))
    val page2 = new Page(PAGE_TYPE.PORTLET, "/b", List(new LocalizedTextContent("de_DE", "test")))
    val page2_1 = new Page(PAGE_TYPE.PORTLET, "/ba", List(new LocalizedTextContent("de_DE", "test")))
    page2.setSubPages(List(page2_1))
    val page2_1_1 = new Page(PAGE_TYPE.PORTLET, "/baa", List(new LocalizedTextContent("de_DE", "test")))
    page2_1.setSubPages(List(page2_1_1))
    val page3 = new Page(PAGE_TYPE.PORTLET, "/c", List(new LocalizedTextContent("de_DE", "test")))

    targetPageSet.setPages(new Pages(List(page1, page2, page3)))
    targetPageSet.getPages.setImportPolicy(IMPORT_POLICY.ENFORCE)

    val createdPage1_2 = new Page(PAGE_TYPE.PORTLET, "/ab", List(new LocalizedTextContent("de_DE", "test")))
    createdPage1_2.setPath("publicPages:/ab")

    when(mockHandler.execute(PageInsertCommand(page1_2, page1, null))).thenReturn(CommandResult(createdPage1_2))
    when(mockHandler.execute(PageInsertCommand(page2_1_1, page2_1, null))).thenReturn(CommandResult(page2_1_1))
    when(mockHandler.execute(PageInsertCommand(page3, null, null))).thenReturn(CommandResult(page3))

    when(mockHandler.execute(UpdateCommand(page1))).thenReturn(CommandResult(page1))
    when(mockHandler.execute(UpdateCommand(page2))).thenReturn(CommandResult(page2))
    when(mockHandler.execute(UpdateCommand(page2_1))).thenReturn(CommandResult(page2_1))

    when(mockHandler.execute(DeleteCommand(existingPage3))).thenReturn(CommandResult(null.asInstanceOf[Page]))

    val order = inOrder(mockHandler)

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importer.processTree[Page](targetPageSet.getPages, existingPageSet.getPages, 0, (p, parent) => PageInsertCommand(p, parent, null), deferredDeletes)

    order.verify(mockHandler, times(1)).execute(DeleteCommand(existingPage3))

    order.verify(mockHandler, times(1)).execute(UpdateCommand(page1))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page1_2, page1, null))
    order.verify(mockHandler, times(1)).execute(UpdateCommand(page2))
    order.verify(mockHandler, times(1)).execute(UpdateCommand(page2_1))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page2_1_1, page2_1, null))
    order.verify(mockHandler, times(1)).execute(PageInsertCommand(page3, null, null))
  }

  @Test
  def processListNoExistingTest(): Unit = {

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)

    val targetRoles = new Roles(new jutil.ArrayList())
    targetRoles.setImportPolicy(IMPORT_POLICY.INSERT)
    val existingRoles = new mutable.HashMap[String, Role]()

    val role1 = new Role("ROLE1")
    val role2 = new Role("ROLE2")
    val role5 = new Role("ROLE5")

    targetRoles.getList.add(role1)
    targetRoles.getList.add(role2)
    targetRoles.getList.add(role5)

    when(mockHandler.execute(RoleInsertCommand(0, role1))).thenReturn(CommandResult(role1))
    when(mockHandler.execute(RoleInsertCommand(0, role2))).thenReturn(CommandResult(role2))
    when(mockHandler.execute(RoleInsertCommand(0, role5))).thenReturn(CommandResult(role5))

    val order = inOrder(mockHandler)

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importer.processList[Role](targetRoles, existingRoles, 0, (r) => RoleInsertCommand(0, r), deferredDeletes)

    order.verify(mockHandler, times(1)).execute(RoleInsertCommand(0, role1))
    order.verify(mockHandler, times(1)).execute(RoleInsertCommand(0, role2))
    order.verify(mockHandler, times(1)).execute(RoleInsertCommand(0, role5))
  }


  @Test
  def processListPolicyInsertTest(): Unit = {

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)

    val targetRoles = new Roles(new jutil.ArrayList())
    targetRoles.setImportPolicy(IMPORT_POLICY.INSERT)
    val existingRoles = new mutable.HashMap[String, Role]()

    val existingRole1 = new Role("ROLE1")
    existingRole1.setRoleId(1L)
    val existingRole2 = new Role("ROLE2")
    existingRole2.setRoleId(2L)
    val existingRole3 = new Role("ROLE3")
    existingRole3.setRoleId(3L)
    val existingRole4 = new Role("ROLE4")
    existingRole4.setRoleId(4L)

    existingRoles += "ROLE1" -> existingRole1
    existingRoles += "ROLE2" -> existingRole2
    existingRoles += "ROLE3" -> existingRole3
    existingRoles += "ROLE4" -> existingRole4

    val updatedRole1 = new Role("ROLE1")
    updatedRole1.setTitles(List(new LocalizedTextContent("de_DE", "Foo")))
    val role2 = new Role("ROLE2")
    val role5 = new Role("ROLE5")

    targetRoles.getList.add(updatedRole1)
    targetRoles.getList.add(role2)
    targetRoles.getList.add(role5)

    when(mockHandler.execute(RoleInsertCommand(0, role5))).thenReturn(CommandResult(role5))

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importer.processList[Role](targetRoles, existingRoles, 0, (r) => RoleInsertCommand(0, r), deferredDeletes)

    verify(mockHandler, times(1)).execute(RoleInsertCommand(0, role5))
  }


  @Test
  def processListPolicyEnforceTest(): Unit = {

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)
    val comparator = new LiferayEntityComparatorImpl
    importer.setLiferayEntityComparator(comparator)

    val targetRoles = new Roles(new jutil.ArrayList())
    targetRoles.setImportPolicy(IMPORT_POLICY.ENFORCE)
    val existingRoles = new jutil.HashMap[String, Role]()

    val existingRole1 = new Role("ROLE1")
    existingRole1.setRoleId(1L)
    val existingRole2 = new Role("ROLE2")
    existingRole2.setRoleId(2L)
    val existingRole3 = new Role("ROLE3")
    existingRole3.setRoleId(3L)
    val existingRole4 = new Role("ROLE4")
    existingRole4.setRoleId(4L)

    existingRoles += "ROLE1" -> existingRole1
    existingRoles += "ROLE2" -> existingRole2
    existingRoles += "ROLE3" -> existingRole3
    existingRoles += "ROLE4" -> existingRole4

    val updatedRole1 = new Role("ROLE1")
    updatedRole1.setTitles(List(new LocalizedTextContent("de_DE", "Foo")))
    val role2 = new Role("ROLE2")
    val role5 = new Role("ROLE5")

    targetRoles.getList.add(updatedRole1)
    targetRoles.getList.add(role2)
    targetRoles.getList.add(role5)

    when(mockHandler.execute(DeleteCommand(existingRole3))).thenReturn(CommandResult(null.asInstanceOf[Role]))
    when(mockHandler.execute(DeleteCommand(existingRole4))).thenReturn(CommandResult(null.asInstanceOf[Role]))
    when(mockHandler.execute(UpdateCommand(updatedRole1))).thenReturn(CommandResult(updatedRole1))
    when(mockHandler.execute(RoleInsertCommand(0, role5))).thenReturn(CommandResult(role5))

    val order = inOrder(mockHandler)

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importer.processList[Role](targetRoles, existingRoles, 0, (r) => RoleInsertCommand(0, r), deferredDeletes)

    order.verify(mockHandler, times(1)).execute(DeleteCommand(existingRole3))
    order.verify(mockHandler, times(1)).execute(DeleteCommand(existingRole4))
    order.verify(mockHandler, times(1)).execute(UpdateCommand(updatedRole1))
    order.verify(mockHandler, times(1)).execute(RoleInsertCommand(0, role5))
  }

  @Test
  def deferDeletesTest(): Unit = {

    Report.start("test", "test")

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)
    val comparator = new LiferayEntityComparatorImpl
    importer.setLiferayEntityComparator(comparator)

    val targetRoles = new Roles(new jutil.ArrayList())
    targetRoles.setImportPolicy(IMPORT_POLICY.ENFORCE)
    val existingRoles = new jutil.HashMap[String, Role]()

    val existingRole1 = new Role("ROLE1")
    existingRole1.setRoleId(1L)
    val existingRole2 = new Role("ROLE2")
    existingRole2.setRoleId(2L)
    val existingRole3 = new Role("ROLE3")
    existingRole3.setRoleId(3L)
    val existingRole4 = new Role("ROLE4")
    existingRole4.setRoleId(4L)

    existingRoles += "ROLE1" -> existingRole1
    existingRoles += "ROLE2" -> existingRole2
    existingRoles += "ROLE3" -> existingRole3
    existingRoles += "ROLE4" -> existingRole4

    val role2 = new Role("ROLE2")

    targetRoles.getList.add(role2)

    when(mockHandler.execute(DeleteCommand(existingRole1))).thenReturn(CommandResult(null.asInstanceOf[Role]))
    when(mockHandler.execute(DeleteCommand(existingRole3))).thenThrow(new CliwixCommandExecutionException(null, "test", new RuntimeException("Test")))
    when(mockHandler.execute(DeleteCommand(existingRole4))).thenReturn(CommandResult(null.asInstanceOf[Role]))

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    importer.processList[Role](targetRoles, existingRoles, 0, (r) => RoleInsertCommand(0, r), deferredDeletes)

    assertEquals(1, deferredDeletes.size)
  }

  @Test
  def executeDeferredDeletesTest(): Unit = {

    val importer = new LiferayImporterImpl
    val mockHandler = mock(classOf[DispatchHandler])
    importer.setHandler(mockHandler)
    val mockProcessingInterceptor = mock(classOf[ProcessingInterceptorDispatcher])
    importer.setProcessingInterceptor(mockProcessingInterceptor)
    val comparator = new LiferayEntityComparatorImpl
    importer.setLiferayEntityComparator(comparator)

    val existingRole1 = new Role("ROLE1")
    existingRole1.setRoleId(1L)

    val existingPage1 = new Page(PAGE_TYPE.PORTLET, "/a", null)
    existingPage1.setPortletLayoutId(1L)

    val deferredDeletes = new jutil.ArrayList[DeferredDeleteCommand[_]]
    deferredDeletes += DeferredDeleteCommand(null, -1, Array("Company1"), "", new DeleteCommand(existingRole1))
    deferredDeletes += DeferredDeleteCommand(null, -1, Array("Company1", "Site1"), "", new DeleteCommand(existingPage1))

    when(mockHandler.execute(DeleteCommand(existingPage1))).thenReturn(CommandResult(null.asInstanceOf[Page]))
    when(mockHandler.execute(DeleteCommand(existingRole1))).thenReturn(CommandResult(null.asInstanceOf[Role]))

    val order = inOrder(mockHandler)

    importer.executeDeferredDeletes(deferredDeletes)

    order.verify(mockHandler, times(1)).execute(DeleteCommand(existingPage1))
    order.verify(mockHandler, times(1)).execute(DeleteCommand(existingRole1))
  }

}