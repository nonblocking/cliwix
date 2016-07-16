package at.nonblocking.cliwix.cli;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class HttpPostHandlerTest {

    @Test
    public void postJson() throws Exception {
        TestServer.start();

        File jsonFile = new File("src/test/resources/exportSettings.json");
        assertTrue(jsonFile.exists());

        Response response = new HttpPostHandlerDefaultImpl().post("http://localhost:" + TestServer.PORT, "/exports",
                new FileInputStream(jsonFile), jsonFile.length(), CONTENT_TYPE.JSON, null, null, mock(CookieManager.class));

        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getResponseAsString().contains("exportId"));
    }

    Long uploaded = null;

    @Test
    public void postUpload() throws Exception {
        TestServer.start();

        File zipFile = new File("target/test.zip");
        ZipFile zip = new ZipFile(zipFile);
        zip.addFile(new File("src/test/resources/info.json"), new ZipParameters());

        assertTrue(zipFile.exists());

        Response response = new HttpPostHandlerDefaultImpl().post("http://localhost:" + TestServer.PORT, "/uploads",
                new FileInputStream(zipFile), zipFile.length(), CONTENT_TYPE.UPLOAD, "test.zip" , new ProgressCallback() {
                    @Override
                    public void bytesTransferred(long transferred, long total) {
                        System.out.println("Uploaded: " + Math.round(transferred / total * 100.0) + "%");
                        uploaded = transferred;
                    }
                }, mock(CookieManager.class));

        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getResponseAsString().contains("tmpFileName"));
        assertEquals(zipFile.length(), this.uploaded.longValue());

    }

}
