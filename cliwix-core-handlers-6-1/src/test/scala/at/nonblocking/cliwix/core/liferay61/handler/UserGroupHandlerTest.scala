package at.nonblocking.cliwix.core.liferay61.handler

import at.nonblocking.cliwix.core.command.UserGroupListCommand
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.util.ResourceAwareCollectionFactoryDummyImpl
import at.nonblocking.cliwix.model.UserGroup
import com.liferay.portal.service.UserGroupLocalService
import com.liferay.portal.{model => liferay}
import org.junit.Assert._
import org.junit.Test
import org.mockito.Mockito._

import scala.collection.JavaConversions._

class UserGroupHandlerTest {

  @Test
  def listTest() {

    val handler = new UserGroupListHandler()
    val mockUserGroupService = mock(classOf[UserGroupLocalService])
    val mockConverter = mock(classOf[LiferayEntityConverter])

    handler.setUserGroupService(mockUserGroupService)
    handler.setConverter(mockConverter)
    handler.setResourceAwareCollectionFactory(new ResourceAwareCollectionFactoryDummyImpl)

    val userGroup1 = mock(classOf[liferay.UserGroup])
    val userGroup2 = mock(classOf[liferay.UserGroup])
    val userGroup3 = mock(classOf[liferay.UserGroup])

    when(userGroup1.getUserGroupId).thenReturn(1)
    when(userGroup2.getUserGroupId).thenReturn(2)
    when(userGroup3.getUserGroupId).thenReturn(3)
    when(userGroup1.getName).thenReturn("G1")
    when(userGroup2.getName).thenReturn("G2")
    when(userGroup3.getName).thenReturn("G3")
    when(userGroup3.getParentUserGroupId).thenReturn(2)

    when(mockUserGroupService.getUserGroups(1234)).thenReturn(List(userGroup1, userGroup2, userGroup3))

    when(mockConverter.convertToCliwixUserGroup(userGroup1)).thenReturn(new UserGroup("G1", null, null) { setOwnerCompanyId(1234L); setUserGroupId(1L) })
    when(mockConverter.convertToCliwixUserGroup(userGroup2)).thenReturn(new UserGroup("G2", null, null) { setOwnerCompanyId(1234L); setUserGroupId(2L) })
    when(mockConverter.convertToCliwixUserGroup(userGroup3)).thenReturn(new UserGroup("G3", null, null) { setOwnerCompanyId(1234L); setUserGroupId(3L) })

    val command = UserGroupListCommand(1234)

    assertTrue(handler.canHandle(command))

    val result = handler.execute(command)

    assertNotNull(result)
    assertEquals(3, result.result.size)
  }

}

