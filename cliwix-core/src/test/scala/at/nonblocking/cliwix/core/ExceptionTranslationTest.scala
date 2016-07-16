package at.nonblocking.cliwix.core

import org.junit.Test
import at.nonblocking.cliwix.core.command.{CommandResult, CompanyListCommand}
import at.nonblocking.cliwix.core.handler.Handler
import at.nonblocking.cliwix.model.Company
import java.{util=>jutil}

class ExceptionTranslationTest {

  @Test(expected = classOf[CliwixCommandExecutionException])
  def test(): Unit = {
    class ListHandlerTest extends Handler[CompanyListCommand, jutil.Map[String, Company]] {
      override def handle(command: CompanyListCommand): CommandResult[jutil.Map[String, Company]] = {
        throw new IllegalArgumentException
      }
    }

    new ListHandlerTest().execute(new CompanyListCommand(true))
  }

}
