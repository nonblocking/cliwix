package at.nonblocking.cliwix.core.liferay61.handler

import at.nonblocking.cliwix.core.command.CompanyListCommand
import at.nonblocking.cliwix.core.converter.LiferayEntityConverter
import at.nonblocking.cliwix.model.{CompanyConfiguration, Company}
import com.liferay.portal.service.CompanyLocalService
import com.liferay.portal.{model => liferay}
import org.junit.Assert._
import org.junit.Test
import org.mockito.Mockito._

import scala.collection.JavaConversions._

class CompanyHandlerTest {

  @Test
  def listTest() {
    val handler = new CompanyListHandler()
    val mockCompanyService = mock(classOf[CompanyLocalService])
    val mockConverter = mock(classOf[LiferayEntityConverter])

    handler.setCompanyService(mockCompanyService)
    handler.setConverter(mockConverter)

    val company1 = mock(classOf[liferay.Company])
    val company2 = mock(classOf[liferay.Company])

    when(mockCompanyService.getCompanies).thenReturn(List(company1, company2))

    val cliwixCompany1 = new Company("test1", new CompanyConfiguration(null, null, "de_AT", "Europe/Vienna"))
    cliwixCompany1.setCompanyId(1)
    val cliwixCompany2 = new Company("test2", new CompanyConfiguration(null, null, "de_AT", "Europe/Vienna"))
    cliwixCompany2.setCompanyId(2)

    when(mockConverter.convertToCliwixCompany(company1, withConfiguration = true)).thenReturn(cliwixCompany1)
    when(mockConverter.convertToCliwixCompany(company2, withConfiguration = true)).thenReturn(cliwixCompany2)

    val command = CompanyListCommand(true)

    assertTrue(handler.canHandle(command))

    val result = handler.execute(command)

    assertNotNull(result)
    assertEquals(2, result.result.size)

    verify(mockCompanyService, times(1)).getCompanies
  }

}
