package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.core.validation.DefaultCompanyLiferayConfigValidator
import at.nonblocking.cliwix.model._
import com.liferay.portal.kernel.util.{PropsKeys, PropsUtil}
import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConversions._

class DefaultCompanyLiferayConfigValidatorTest {

  @Test
  def testSuccess() {
    val defaultWebId = PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID)

    val company1 = new Company(defaultWebId, new CompanyConfiguration("localhost", null, null, null))
    val company2 = new Company("test", new CompanyConfiguration("nonblocking.at", null, null, null))
    val config = new LiferayConfig
    config.setCompanies(new Companies(List(company1, company2)))

    val messages = new DefaultCompanyLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def testSuccessIfPolicyEnforceAndDefaultCompanyPresent() {
    val defaultWebId = PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID)

    val company1 = new Company(defaultWebId, new CompanyConfiguration("localhost", null, null, null))
    val company2 = new Company("test", new CompanyConfiguration("nonblocking.at", null, null, null))
    val companies = new Companies(List(company1, company2))
    companies.setImportPolicy(IMPORT_POLICY.ENFORCE)
    val config = new LiferayConfig
    config.setCompanies(companies)

    val messages = new DefaultCompanyLiferayConfigValidator().validate(config)

    assertTrue(messages.isEmpty)
  }

  @Test
  def testFailIfPolicyEnforceAndNoDefaultCompany() {

    val company1 = new Company("test", new CompanyConfiguration("test.com", null, null, null))
    val company2 = new Company("test2", new CompanyConfiguration("nonblocking.at", null, null, null))
    val companies = new Companies(List(company1, company2))
    companies.setImportPolicy(IMPORT_POLICY.ENFORCE)
    val config = new LiferayConfig
    config.setCompanies(companies)

    val messages = new DefaultCompanyLiferayConfigValidator().validate(config)

    assertFalse(messages.isEmpty)
  }

  @Test
  def testFailDefaultCompanyNotLocalhost() {
    val defaultWebId = PropsUtil.get(PropsKeys.COMPANY_DEFAULT_WEB_ID)

    val company = new Company(defaultWebId, new CompanyConfiguration("nonblocking.at", null, null, null))
    val config = new LiferayConfig
    config.setCompanies(new Companies(List(company)))

    val messages = new DefaultCompanyLiferayConfigValidator().validate(config)

    assertFalse(messages.isEmpty)
  }

  @Test
  def testFailNonDefaultCompanyLocalhost() {

    val company = new Company("test", new CompanyConfiguration("localhost", null, null, null))
    val config = new LiferayConfig
    config.setCompanies(new Companies(List(company)))

    val messages = new DefaultCompanyLiferayConfigValidator().validate(config)

    assertFalse(messages.isEmpty)
  }

}
