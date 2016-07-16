package at.nonblocking.cliwix.core.liferay61.handler

import at.nonblocking.cliwix.core.command.UserListCommand
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.util.ResourceAwareCollectionFactoryDummyImpl
import at.nonblocking.cliwix.model.User
import com.liferay.portal.service.UserLocalService
import com.liferay.portal.{model => liferay}
import org.junit.Assert._
import org.junit.Test
import org.mockito.Mockito._

import scala.collection.JavaConversions._

class UserHandlerTest {

  @Test
  def listTest() {

    val handler = new UserListHandler()
    val mockUserService = mock(classOf[UserLocalService])
    val mockConverter = mock(classOf[LiferayEntityConverter])

    handler.setUserService(mockUserService)
    handler.setConverter(mockConverter)
    handler.setResourceAwareCollectionFactory(new ResourceAwareCollectionFactoryDummyImpl)

    val user1 = mock(classOf[liferay.User])
    val user2 = mock(classOf[liferay.User])
    val cliwixUser1 = new User()
    cliwixUser1.setScreenName("user1")
    val cliwixUser2 = new User()
    cliwixUser2.setScreenName("user2")

    when(mockUserService.getCompanyUsersCount(1234L)).thenReturn(2)
    when(mockUserService.getCompanyUsers(1234L, 0, 100)).thenReturn(List(user1, user2))
    when(mockConverter.convertToCliwixUser(user1)).thenReturn(cliwixUser1)
    when(mockConverter.convertToCliwixUser(user2)).thenReturn(cliwixUser2)

    val command = UserListCommand(1234)

    assertTrue(handler.canHandle(command))

    val result = handler.execute(command)

    assertNotNull(result)
    assertEquals(2, result.result.size)

    verify(mockUserService, times(1)).getCompanyUsers(1234L, 0, 100)
  }

}
