package at.nonblocking.cliwix.core

import java.{util=>jutil}
import at.nonblocking.cliwix.model._
import java.io.{FileOutputStream, File}
import org.junit.{After, Ignore, Test}

import at.nonblocking.cliwix.core.util._

import scala.collection.JavaConversions._

class ResourceAwareXmlSerializerTest {

  val oneMillion = 1000000

  val file = File.createTempFile("test", ".xml")
  println(s"Writing to ${file.getAbsolutePath}")

  @After
  def after(): Unit = file.delete()

  //This test takes 20min and more
  //Run it with -Xmx1024m
  @Ignore
  @Test
  def dumpAndReadOneMillionUsers() {

    val liferayConfig = new LiferayConfig
    val company = new Company("test", new CompanyConfiguration("test.nonblocking.at", "nonblocking.at", "de_DE", "Europe/Vienna"))

    val resourceAwareCollectionFactory = new ResourceAwareCollectionFactoryImpl
    val map = resourceAwareCollectionFactory.createMap[String, User](oneMillion)
    for (i <- 1 to oneMillion) {
      map.put(s"user$i", new User(s"user$i", s"user$i@test.at", null, "developer", "Max", null, "Mustermann",
        new jutil.Date(), GENDER.F, "de_DE", jutil.TimeZone.getDefault.getDisplayName, s"Hello user$i!"))
    }

    liferayConfig.setCompanies(new Companies(List(company)))
    company.setUsers(new Users(MapValuesListWrapper(map)))

    val serializer = new ResourceAwareXmlSerializerImpl

    val fos = new FileOutputStream(file)
    serializer.writeXML(liferayConfig, fos)
    fos.close()

    println(s"Size of written XML file: ${file.length() / 1024 / 1024}MB")

    val loadedConfig = serializer.fromXML(file)

    val totalMemory = Runtime.getRuntime.totalMemory() / 1024 / 1024
    println(s"Memory usage after unmarshalling: $totalMemory MB")

    println(loadedConfig.getCompanies.getList()(0).getUsers.getList.size())
    loadedConfig.getCompanies.getList()(0).getUsers.getList.foreach( user  => user.setFirstName("franz"))

    val totalMemoryAfterIteration = Runtime.getRuntime.totalMemory() / 1024 / 1024
    println(s"Memory usage after iteration: $totalMemoryAfterIteration MB")
  }

}
