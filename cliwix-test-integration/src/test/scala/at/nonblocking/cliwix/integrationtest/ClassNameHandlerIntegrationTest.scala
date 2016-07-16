package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.command.{GetByDBIdCommand, GetByIdentifierOrPathCommand}
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.model.ClassName
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty

@RunWith(classOf[CliwixIntegrationTestRunner])
class ClassNameHandlerIntegrationTest {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @Test
  @TransactionalRollback
  def getByIdTest() {
    val className = this.dispatchHandler.execute(GetByDBIdCommand(10004, classOf[ClassName])).result

    assertNotNull(className)
    assertEquals(10004L, className.getClassNameId)
    assertEquals("com.liferay.portal.model.Role", className.getClassName)
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val className = this.dispatchHandler.execute(GetByIdentifierOrPathCommand("com.liferay.portal.model.Role", -1, classOf[ClassName])).result

    assertNotNull(className)
    assertEquals(10004L, className.getClassNameId)
    assertEquals("com.liferay.portal.model.Role", className.getClassName)
  }


}
