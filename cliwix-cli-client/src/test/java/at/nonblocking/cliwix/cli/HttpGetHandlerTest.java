package at.nonblocking.cliwix.cli;


import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class HttpGetHandlerTest {

    @Test
    public void test() throws Exception {
        TestServer.start();

        Thread.sleep(1000);

        IOUtils.copy(new FileInputStream("src/test/resources/info.json"), new FileOutputStream(new File(TestServer.DOCUMENT_ROOT, "info.json")));

        Response response = new HttpGetHandlerDefaultImpl().get("http://localhost:" + TestServer.PORT, "/info.json", mock(CookieManager.class));

        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getResponseAsString().contains("cliwixVersion"));
    }
}
