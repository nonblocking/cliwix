package at.nonblocking.cliwix.integrationtest

import at.nonblocking.cliwix.core.LiferayInfo
import at.nonblocking.cliwix.core.command._
import at.nonblocking.cliwix.core.compare.LiferayEntityComparator
import at.nonblocking.cliwix.core.handler._
import at.nonblocking.cliwix.integrationtest.TestEntityFactory._
import at.nonblocking.cliwix.model.Company
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith

import scala.beans.BeanProperty

@RunWith(classOf[CliwixIntegrationTestRunner])
class CompanyHandlerIntegrationTest extends LazyLogging {

  @BeanProperty
  var dispatchHandler: DispatchHandler = _

  @BeanProperty
  var liferayEntityComparator: LiferayEntityComparator = _

  @BeanProperty
  var liferayInfo: LiferayInfo = _

  @Test
  @TransactionalRollback
  def insertTest() {
    val company = createTestCompany()
    company.getCompanyConfiguration.setDefaultGreeting("Grüße dich!")
    company.getCompanyConfiguration.setHomeUrl("/web/test")
    logger.info("Original company: " + company.toString)

    val insertCompanyCommand = CompanyInsertCommand(company)
    val insertedCompany = this.dispatchHandler.execute(insertCompanyCommand).result
    logger.info("Inserted company: " + insertCompanyCommand.toString)

    assertNotNull(insertedCompany)
    assertNotNull(insertedCompany.getCompanyId)
    assertTrue(this.liferayEntityComparator.equals(insertedCompany, company))

    val list = this.dispatchHandler.execute(CompanyListCommand(withConfiguration = true)).result
    val retrievedCompany = list.get(company.getWebId)
    logger.info("Retrieved company: " + retrievedCompany.toString)

    assertNotNull(retrievedCompany)
    assertNotNull(retrievedCompany.getCompanyConfiguration)
    assertTrue(this.liferayEntityComparator.equals(retrievedCompany, company))
  }

  @Test
  @TransactionalRollback
  def updateTest() {
    val company = createTestCompany()
    company.getCompanyConfiguration.setHomeUrl("/web/test")
    company.getCompanyConfiguration.setDefaultGreeting("Hallo!")
    logger.info("Original company: " + company.toString)

    val insertCompanyCommand = CompanyInsertCommand(company)
    val insertedCompany = this.dispatchHandler.execute(insertCompanyCommand).result
    logger.info("Inserted company: " + insertCompanyCommand.toString)

    assertNotNull(insertedCompany)
    assertNotNull(insertedCompany.getCompanyId)
    assertTrue(this.liferayEntityComparator.equals(insertedCompany, company))

    insertedCompany.getCompanyConfiguration.setActive(false)
    insertedCompany.getCompanyConfiguration.setDefaultGreeting("Hallo 2!")

    val updatedCompany = this.dispatchHandler.execute(UpdateCommand(insertedCompany)).result
    logger.info("Updated company: " + updatedCompany.toString)

    assertTrue(this.liferayEntityComparator.equals(updatedCompany, insertedCompany))

    val list = this.dispatchHandler.execute(CompanyListCommand(withConfiguration = true)).result
    val retrievedCompany = list.get(company.getWebId)
    logger.info("Retrieved company: " + retrievedCompany.toString)

    assertNotNull(retrievedCompany)
    assertTrue(this.liferayEntityComparator.equals(retrievedCompany, updatedCompany))
  }

  @Test
  @TransactionalRollback
  def updateNoConfigurationTest() {
    val company = createTestCompany()
    company.getCompanyConfiguration.setHomeUrl("/web/test")
    company.getCompanyConfiguration.setDefaultGreeting("Hallo!")
    logger.info("Original company: " + company.toString)

    val insertCompanyCommand = CompanyInsertCommand(company)
    val insertedCompany = this.dispatchHandler.execute(insertCompanyCommand).result
    logger.info("Inserted company: " + insertCompanyCommand.toString)

    assertNotNull(insertedCompany)
    assertNotNull(insertedCompany.getCompanyId)
    assertTrue(this.liferayEntityComparator.equals(insertedCompany, company))

    insertedCompany.setCompanyConfiguration(null)

    val updatedCompany = this.dispatchHandler.execute(UpdateCommand(insertedCompany)).result
    logger.info("Updated company: " + updatedCompany.toString)

    assertTrue(this.liferayEntityComparator.equals(company, updatedCompany))
  }

  @Test
  @TransactionalRollback
  def getByIdTest() {
    val company = createTestCompany()
    company.getCompanyConfiguration.setHomeUrl("/web/test")
    company.getCompanyConfiguration.setDefaultGreeting("Hallo!")
    logger.info("Original company: " + company.toString)

    val insertCompanyCommand = CompanyInsertCommand(company)
    val insertedCompany = this.dispatchHandler.execute(insertCompanyCommand).result
    logger.info("Inserted company: " + insertCompanyCommand.toString)

    assertTrue(this.liferayEntityComparator.equals(insertedCompany, company))

    val c = this.dispatchHandler.execute(GetByDBIdCommand(insertedCompany.getCompanyId, classOf[Company])).result

    assertNotNull(c)
    assertTrue(this.liferayEntityComparator.equals(c, insertedCompany))
  }

  @Test
  @TransactionalRollback
  def getByNaturalIdentifierTest() {
    val company = createTestCompany()
    company.getCompanyConfiguration.setHomeUrl("/web/test")
    company.getCompanyConfiguration.setDefaultGreeting("Hallo!")
    logger.info("Original company: " + company.toString)

    val insertCompanyCommand = CompanyInsertCommand(company)
    val insertedCompany = this.dispatchHandler.execute(insertCompanyCommand).result
    logger.info("Inserted company: " + insertCompanyCommand.toString)

    assertTrue(this.liferayEntityComparator.equals(insertedCompany, company))

    val c = this.dispatchHandler.execute(GetByIdentifierOrPathCommand(insertedCompany.identifiedBy(), 0, classOf[Company])).result

    assertNotNull(c)
    assertTrue(this.liferayEntityComparator.equals(c, insertedCompany))
  }

  @Test
  @TransactionalRollback
  def deleteTest() {
    if (this.liferayInfo.getBaseVersion == "6.2") return

    val company1 = createTestCompany("one")
    company1.getCompanyConfiguration.setHomeUrl("/web/test")
    company1.getCompanyConfiguration.setDefaultGreeting("Hallo!")

    val company2 = createTestCompany("two")
    company2.getCompanyConfiguration.setHomeUrl("/web/test2")
    company2.getCompanyConfiguration.setDefaultGreeting("Hallo!")

    this.dispatchHandler.execute(CompanyInsertCommand(company1)).result
    val insertedCompany2 = this.dispatchHandler.execute(CompanyInsertCommand(company2)).result

    val map1 = this.dispatchHandler.execute(new CompanyListCommand(withConfiguration = true)).result

    assertTrue(map1.containsKey(company1.getWebId))
    assertTrue(map1.containsKey(company2.getWebId))

    this.dispatchHandler.execute(DeleteCommand(insertedCompany2))

    val map2 = this.dispatchHandler.execute(new CompanyListCommand(withConfiguration = true)).result

    assertFalse(map2.containsKey(company2.getWebId))
  }

}
