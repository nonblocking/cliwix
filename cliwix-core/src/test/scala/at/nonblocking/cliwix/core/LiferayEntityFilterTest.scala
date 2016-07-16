package at.nonblocking.cliwix.core

import at.nonblocking.cliwix.model._
import org.junit.Test
import org.junit.Assert._

class LiferayEntityFilterTest {

  @Test
  def includeFilterTest() = {
    val includeFilter = new LiferayEntityFilterInclude(List(classOf[Company], classOf[User], classOf[Site]), Array("company1", "foo*"), Array("site1", "bar*"))

    assertTrue(includeFilter.exportEntitiesOf(classOf[User]))
    assertTrue(includeFilter.exportEntitiesOf(classOf[Site]))
    assertFalse(includeFilter.exportEntitiesOf(classOf[Role]))

    val company1 = new Company("company1", null)
    val company2 = new Company("fo", null)
    val company3 = new Company("foo", null)
    val company4 = new Company("foobar", null)
    val company5 = new Company("FooBar", null)

    assertTrue(includeFilter.exportEntityInstance(company1))
    assertFalse(includeFilter.exportEntityInstance(company2))
    assertTrue(includeFilter.exportEntityInstance(company3))
    assertTrue(includeFilter.exportEntityInstance(company4))
    assertTrue(includeFilter.exportEntityInstance(company5))

    val site1 = new Site("site1", null, null)
    val site2 = new Site("b", null, null)
    val site3 = new Site(" bar ", null, null)
    val site4 = new Site("BARX", null, null)

    assertTrue(includeFilter.exportEntityInstance(site1))
    assertFalse(includeFilter.exportEntityInstance(site2))
    assertTrue(includeFilter.exportEntityInstance(site3))
    assertTrue(includeFilter.exportEntityInstance(site4))
  }

}
