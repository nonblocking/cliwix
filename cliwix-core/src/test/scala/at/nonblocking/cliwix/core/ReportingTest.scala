package at.nonblocking.cliwix.core

import org.junit.Test
import org.junit.Assert._

class ReportingTest {

  @Test
  def test1() = {

    val report = Report

    report.start("test", "test")

    report.setSection("one")
    report.setSubSection("two")

    assertEquals(Array("one", "two").deep, report.getCurrentSections.deep)

    report.setSubSection("three")
    report.setSubSubSection("four")

    assertEquals(Array("one", "three", "four").deep, report.getCurrentSections.deep)

    report.setSections(Array())

    assertEquals(Array().deep, report.getCurrentSections.deep)

    report.setSections(Array("one", "two"))

    assertEquals(Array("one", "two").deep, report.getCurrentSections.deep)
  }

}
