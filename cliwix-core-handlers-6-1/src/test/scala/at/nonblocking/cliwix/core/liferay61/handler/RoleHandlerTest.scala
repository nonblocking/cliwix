package at.nonblocking.cliwix.core.liferay61.handler

import at.nonblocking.cliwix.core.command.RoleListCommand
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.core.util.ResourceAwareCollectionFactoryDummyImpl
import at.nonblocking.cliwix.model.Role
import com.liferay.portal.service.RoleLocalService
import com.liferay.portal.{model => liferay}
import junit.framework.Assert._
import org.junit.Test
import org.mockito.Mockito._

import scala.collection.JavaConversions._

class RoleHandlerTest {

  @Test
  def listTest() {

    val handler = new RoleListHandler()
    val mockRoleService = mock(classOf[RoleLocalService])
    val mockConverter = mock(classOf[LiferayEntityConverter])

    handler.setRoleService(mockRoleService)
    handler.setConverter(mockConverter)
    handler.setResourceAwareCollectionFactory(new ResourceAwareCollectionFactoryDummyImpl)

    val role1 = mock(classOf[liferay.Role])
    val role2 = mock(classOf[liferay.Role])

    when(mockRoleService.getRoles(1234L)).thenReturn(List(role1, role2))
    when(mockConverter.convertToCliwixRole(role1)).thenReturn(new Role("R1"))
    when(mockConverter.convertToCliwixRole(role2)).thenReturn(new Role("R2"))

    val command = RoleListCommand(1234L)

    assertTrue(handler.canHandle(command))

    val result = handler.execute(command)

    assertNotNull(result)
    assertEquals(2, result.result.size)
  }

}
