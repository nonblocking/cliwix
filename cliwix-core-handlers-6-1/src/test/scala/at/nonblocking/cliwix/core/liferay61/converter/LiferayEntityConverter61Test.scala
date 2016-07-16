package at.nonblocking.cliwix.core.liferay61.converter

import java.io.Serializable

import com.liferay.portal.model.{CacheModel, PortletPreferences}
import com.liferay.portal.service.ServiceContext
import com.liferay.portlet.expando.model.ExpandoBridge
import com.liferay.portlet.journal.model.JournalArticle
import org.junit.Assert._
import org.junit.Test
import org.mockito.Mockito._

import scala.collection.JavaConversions._

class LiferayEntityConverter61Test {

  @Test
  def convertToCliwixPortletConfigurationTest() {
    val prefXML =
      <portlet-preferences>
        <preference><name>abstractLength</name><value>200</value></preference>
        <preference><name>classTypeIdsJournalArticleAssetRendererFactory</name><value>15105,20230,15214</value></preference>
        <preference><name>showAvailableLocales</name><value>false</value></preference>
        <preference><name>portletSetupUseCustomTitle</name><value>true</value></preference>
        <preference><name>enableRss</name><value>false</value></preference>
        <preference><name>emailFromAddress</name><value>test@liferay.com</value></preference>
        <preference><name>paginationType</name><value>none</value></preference>
        <preference><name>enableFlags</name><value>false</value></preference>
        <preference><name>queryValues0</name> <value>materials</value><value>laws</value></preference>
        <preference><name>emailAssetEntryAddedEnabled</name><value>false</value></preference>
        <preference><name>classTypeIds</name><value>NULL_VALUE</value></preference>
      </portlet-preferences>

    val converter = new LiferayEntityConverter61

    val liferayPortletPreferences = new PortletPreferences {
      var prefs = prefXML.toString()

      override def isEscapedModel: Boolean = ???
      override def isNew: Boolean = ???
      override def setPortletId(portletId: String): Unit = ???
      override def getPrimaryKey: Long = ???
      override def setPreferences(preferences: String): Unit = prefs = preferences
      override def setCachedModel(cachedModel: Boolean): Unit = ???
      override def getPlid: Long = ???
      override def getPrimaryKeyObj: Serializable = ???
      override def getOwnerId: Long = ???
      override def toCacheModel: CacheModel[PortletPreferences] = ???
      override def getPortletPreferencesId: Long = 1
      override def setOwnerId(ownerId: Long): Unit = ???
      override def toEscapedModel: PortletPreferences = ???
      override def setPrimaryKey(primaryKey: Long): Unit = ???
      override def toXmlString: String = ???
      override def compareTo(portletPreferences: PortletPreferences): Int = ???
      override def getPreferences: String = prefs
      override def getOwnerType: Int = ???
      override def setNew(n: Boolean): Unit = ???
      override def isCachedModel: Boolean = ???
      override def setPlid(plid: Long): Unit = ???
      override def getExpandoBridge: ExpandoBridge = ???
      override def setPortletPreferencesId(portletPreferencesId: Long): Unit = ???
      override def getPortletId: String = "FOO"
      override def setExpandoBridgeAttributes(serviceContext: ServiceContext): Unit = ???
      override def setPrimaryKeyObj(primaryKeyObj: Serializable): Unit = ???
      override def setOwnerType(ownerType: Int): Unit = ???
      override def resetOriginalValues(): Unit = ???
      override def getModelClassName: String = ???
      override def getModelClass: Class[_] = ???
      override def persist(): Unit = ???
    }

    val cliwixPortletPreferences = converter.convertToCliwixPortletConfiguration(liferayPortletPreferences)

    assertNull(cliwixPortletPreferences.getPreferences.find(_.getName == "queryValues0").get.getValue)
    assertNotNull(cliwixPortletPreferences.getPreferences.find(_.getName == "queryValues0").get.getValues)
    assertEquals("materials", cliwixPortletPreferences.getPreferences.find(_.getName == "queryValues0").get.getValues()(0))
    assertEquals("laws", cliwixPortletPreferences.getPreferences.find(_.getName == "queryValues0").get.getValues()(1))

    converter.mergeToLiferayPortletPreferences(cliwixPortletPreferences, liferayPortletPreferences)

    assertTrue(liferayPortletPreferences.getPreferences.contains("<preference><name>queryValues0</name><value>materials</value><value>laws</value></preference>"))
  }

  @Test
  def convertDynamicArticleContentTest() = {
    val liferayArticleContent = "<root available-locales=\"sk_SK\" default-locale=\"sk_SK\">  <dynamic-element name=\"desktopImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[/documents/13203/20075/key+visual+for+tablet]]></dynamic-content>  </dynamic-element>  <dynamic-element name=\"tabletImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[]]></dynamic-content>  </dynamic-element>  <dynamic-element name=\"mobileImage\" index=\"0\" type=\"document_library\" index-type=\"keyword\">   <dynamic-content language-id=\"sk_SK\"><![CDATA[]]></dynamic-content>  </dynamic-element> </root>"

    val journalArticle = mock(classOf[JournalArticle])
    when(journalArticle.getContent).thenReturn(liferayArticleContent)

    val converter = new LiferayEntityConverter61

    val cliwixArticle = converter.convertToCliwixTemplateDrivenArticle(journalArticle)

    val liferayArticleContent2 = converter.toLiferayRootXml(cliwixArticle.getDynamicElements, "sk_SK")

    assertEquals(liferayArticleContent, liferayArticleContent2)
  }

}
