package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.interceptor.ReplacePortletIdsOnPagesInterceptor
import at.nonblocking.cliwix.core.util.PortletUtil
import at.nonblocking.cliwix.model._
import org.junit.Test
import org.junit.Assert._
import java.{util=>jutil}
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.collection.JavaConversions._

class ReplacePortletIdsOnPagesInterceptorTest {

  @Test
  def testRemoveUnusedConfigurations() {
    val page = new Page()
    page.setPageSettings(List(new PageSetting("column-1", "11,33,"), new PageSetting("column-2", "foo")))
    page.setPortletConfigurations(new PortletConfigurations(new jutil.ArrayList(List(
      new PortletConfiguration("11", null),
      new PortletConfiguration("12", null),
      new PortletConfiguration("33", null),
      new PortletConfiguration("foo", null),
      new PortletConfiguration("bar", null)))))

    new ReplacePortletIdsOnPagesInterceptor().afterEntityExport(page, 0)

    assertEquals(3, page.getPortletConfigurations.getList.size())
  }

  @Test
  def testReplaceInstanceQualifiers() {
    val page = new Page()
    page.setPageSettings(List(new PageSetting("column-1", "11,33_INSTANCE_abc,"), new PageSetting("column-2", "foo,33_INSTANCE_cde,34_INSTANCE_abc")))
    page.setPortletConfigurations(new PortletConfigurations(List(
      new PortletConfiguration("11", null),
      new PortletConfiguration("33_INSTANCE_abc", null),
      new PortletConfiguration("33_INSTANCE_cde", null),
      new PortletConfiguration("foo", null),
      new PortletConfiguration("34_INSTANCE_abc", null))))

    new ReplacePortletIdsOnPagesInterceptor().afterEntityExport(page, 0)

    assertEquals("11,33#1", page.getPageSettings()(0).getValue)
    assertEquals("foo,33#2,34#1", page.getPageSettings()(1).getValue)
    assertEquals("11", page.getPortletConfigurations.getList()(0).getPortletId)
    assertEquals("33#1", page.getPortletConfigurations.getList()(1).getPortletId)
    assertEquals("33#2", page.getPortletConfigurations.getList()(2).getPortletId)
    assertEquals("foo", page.getPortletConfigurations.getList()(3).getPortletId)
    assertEquals("34#1", page.getPortletConfigurations.getList()(4).getPortletId)
  }

  @Test
  def testReplaceNumberQualifiersNewPage() = {
    val page = new Page()
    page.setPageSettings(List(new PageSetting("column-1", "11,33#1,"), new PageSetting("column-2", "foo,33#2,34#1")))
    page.setPortletConfigurations(new PortletConfigurations(new jutil.ArrayList(List(
      new PortletConfiguration("11", null),
      new PortletConfiguration("33#1", null),
      new PortletConfiguration("33#2", null),
      new PortletConfiguration("foo", null),
      new PortletConfiguration("34#1", null),
      new PortletConfiguration("34#2", null)))))

    val mockPortletUtil = mock(classOf[PortletUtil])
    when(mockPortletUtil.getPortletById(anyString())).thenReturn(None)

    val interceptor = new ReplacePortletIdsOnPagesInterceptor()
    interceptor.setPortletUtil(mockPortletUtil)

    interceptor.beforeEntityInsert(page, 0)

    assertEquals(5, page.getPortletConfigurations.getList.size())
    assertFalse(page.getPageSettings()(0).getValue.contains("#"))
    assertFalse(page.getPageSettings()(1).getValue.contains("#"))
    assertEquals("11", page.getPortletConfigurations.getList()(0).getPortletId)
    assertTrue(page.getPortletConfigurations.getList()(1).getPortletId.startsWith("33_INSTANCE_"))
    assertTrue(page.getPortletConfigurations.getList()(2).getPortletId.startsWith("33_INSTANCE_"))
    assertEquals("foo", page.getPortletConfigurations.getList()(3).getPortletId)
    assertTrue(page.getPortletConfigurations.getList()(4).getPortletId.startsWith("34_INSTANCE_"))
  }

  @Test
  def testReplaceNumberQualifiersExistingPage() = {
    val existingPage = new Page()
    existingPage.setPageSettings(List(new PageSetting("column-1", "11,33_INSTANCE_cdef"), new PageSetting("column-2", "foo,33_INSTANCE_abcd")))

    val page = new Page()
    page.setPageSettings(List(new PageSetting("column-1", "11,33#1,33#2"), new PageSetting("column-2", "foo,33#3,34#1")))
    page.setPortletConfigurations(new PortletConfigurations(new jutil.ArrayList(List(
      new PortletConfiguration("11", null),
      new PortletConfiguration("33#1", null),
      new PortletConfiguration("33#2", null),
      new PortletConfiguration("33#3", null),
      new PortletConfiguration("foo", null),
      new PortletConfiguration("34#1", null),
      new PortletConfiguration("34#2", null)))))

    val mockPortletUtil = mock(classOf[PortletUtil])
    when(mockPortletUtil.getPortletById(anyString())).thenReturn(None)

    val interceptor = new ReplacePortletIdsOnPagesInterceptor()
    interceptor.setPortletUtil(mockPortletUtil)

    interceptor.beforeEntityUpdate(page, existingPage, 0)

    assertEquals(6, page.getPortletConfigurations.getList.size())
    assertFalse(page.getPageSettings()(0).getValue.contains("#"))
    assertFalse(page.getPageSettings()(1).getValue.contains("#"))
    assertEquals("11", page.getPortletConfigurations.getList()(0).getPortletId)
    assertEquals("33_INSTANCE_cdef", page.getPortletConfigurations.getList()(1).getPortletId)
    assertTrue(page.getPortletConfigurations.getList()(2).getPortletId.startsWith("33_INSTANCE_"))
    assertEquals("33_INSTANCE_abcd", page.getPortletConfigurations.getList()(3).getPortletId)
    assertEquals("foo", page.getPortletConfigurations.getList()(4).getPortletId)
    assertTrue(page.getPortletConfigurations.getList()(5).getPortletId.startsWith("34_INSTANCE_"))
  }

  @Test
  def testReplaceNumberQualifiersExistingPage2() = {
    val existingPage = new Page()
    existingPage.setPageSettings(List(new PageSetting("column-1", "101_INSTANCE_aZ3HPhP4488w,101_INSTANCE_oW6ZpmQAmfX1,101_INSTANCE_63MSbikP6IAq,101_INSTANCE_0GIAVSVfQ7xY,101_INSTANCE_57bU08dImPJX,101_INSTANCE_3ROoEqx9Gnvn,101_INSTANCE_D4m2PD7ZrWJt,101_INSTANCE_8zfaYL18qMMJ,101_INSTANCE_AHXRJcN5NMbv,101_INSTANCE_K03UK8xYWz6b,101_INSTANCE_3nzulpVzdn4c,101_INSTANCE_8slZtLCloqIY,101_INSTANCE_LEiJOuc1uSsr")))

    val page = new Page()
    page.setPageSettings(List(new PageSetting("column-1", "101#1,101#2,101#3,101#4,101#5,101#6,101#7,101#8,101#9,101#10,101#11,101#12,101#13,101#14,101#15,101#16,101#17,101#18,101#19,101#20,101#21,101#22,101#23,101#24,101#25,101#26,101#27,101#28,101#29,101#30")))
    page.setPortletConfigurations(new PortletConfigurations(new jutil.ArrayList(List(
      new PortletConfiguration("101#1", null),
      new PortletConfiguration("101#2", null),
      new PortletConfiguration("101#3", null),
      new PortletConfiguration("101#4", null),
      new PortletConfiguration("101#5", null),
      new PortletConfiguration("101#6", null),
      new PortletConfiguration("101#7", null),
      new PortletConfiguration("101#8", null),
      new PortletConfiguration("101#9", null),
      new PortletConfiguration("101#10", null),
      new PortletConfiguration("101#11", null),
      new PortletConfiguration("101#12", null),
      new PortletConfiguration("101#13", null),
      new PortletConfiguration("101#14", null),
      new PortletConfiguration("101#15", null),
      new PortletConfiguration("101#16", null),
      new PortletConfiguration("101#17", null),
      new PortletConfiguration("101#18", null),
      new PortletConfiguration("101#19", null),
      new PortletConfiguration("101#20", null),
      new PortletConfiguration("101#21", null),
      new PortletConfiguration("101#22", null),
      new PortletConfiguration("101#23", null),
      new PortletConfiguration("101#24", null),
      new PortletConfiguration("101#25", null),
      new PortletConfiguration("101#26", null),
      new PortletConfiguration("101#27", null),
      new PortletConfiguration("101#28", null),
      new PortletConfiguration("101#29", null),
      new PortletConfiguration("101#30", null)
    ))))

    val mockPortletUtil = mock(classOf[PortletUtil])
    when(mockPortletUtil.getPortletById(anyString())).thenReturn(None)

    val interceptor = new ReplacePortletIdsOnPagesInterceptor()
    interceptor.setPortletUtil(mockPortletUtil)

    interceptor.beforeEntityUpdate(page, existingPage, 0)

    println(page.getPageSettings.get(0).getValue)

    page.getPortletConfigurations.getList.foreach { pc =>
      assertTrue(pc.getPortletId.startsWith("101_INSTANCE_"))
      assertTrue("Instance should be in page setting: " + pc.getPortletId, page.getPageSettings.get(0).getValue.contains(pc.getPortletId))
    }
  }

  @Test
  def testAddQualifiersForInstanciablePortlets() = {
    val page = new Page()
    page.setPageSettings(List(new PageSetting("column-1", "11,33#1"), new PageSetting("column-2", "foo,myInstanciablePortlet")))
    page.setPortletConfigurations(new PortletConfigurations(new jutil.ArrayList(List(
      new PortletConfiguration("11", null),
      new PortletConfiguration("33#1", null),
      new PortletConfiguration("foo", null),
      new PortletConfiguration("myInstanciablePortlet", null),
      new PortletConfiguration("34", null)))))

    val mockPortletUtil = mock(classOf[PortletUtil])
    when(mockPortletUtil.getPortletById("11")).thenReturn(None)
    when(mockPortletUtil.getPortletById("foo")).thenReturn(None)
    when(mockPortletUtil.getPortletById("myInstanciablePortlet")).thenReturn(Some(new Portlet("myInstanciablePortlet", "", true)))

    val interceptor = new ReplacePortletIdsOnPagesInterceptor()
    interceptor.setPortletUtil(mockPortletUtil)

    interceptor.beforeEntityInsert(page, 0)

    assertEquals(4, page.getPortletConfigurations.getList.size())
    assertEquals("11", page.getPortletConfigurations.getList()(0).getPortletId)
    assertTrue(page.getPortletConfigurations.getList()(1).getPortletId.startsWith("33_INSTANCE_"))
    assertEquals("foo", page.getPortletConfigurations.getList()(2).getPortletId)
    assertTrue(page.getPortletConfigurations.getList()(3).getPortletId.startsWith("myInstanciablePortlet_INSTANCE_"))
  }

}
