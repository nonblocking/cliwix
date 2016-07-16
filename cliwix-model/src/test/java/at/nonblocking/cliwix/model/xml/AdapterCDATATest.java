package at.nonblocking.cliwix.model.xml;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdapterCDATATest {

    @Test
    public void testPlainText() throws Exception {
        assertEquals("foo", new AdapterCDATA().marshal("foo"));
        assertEquals("mäder", new AdapterCDATA().marshal("mäder"));
    }

    @Test
    public void testIllegalXMLCharacter() throws Exception {
        assertEquals("<![CDATA[foo <strong>test</strong>]]>", new AdapterCDATA().marshal("foo <strong>test</strong>"));
        assertEquals("<![CDATA[foo & bar]]>", new AdapterCDATA().marshal("foo & bar"));
    }
}
