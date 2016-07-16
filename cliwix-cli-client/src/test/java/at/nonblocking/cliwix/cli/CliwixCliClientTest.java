package at.nonblocking.cliwix.cli;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class CliwixCliClientTest {

    @Test
    public void testInfoWithWaitForServer() throws Exception {

        final long startTime = System.currentTimeMillis();

        HttpGetHandler getHandler = new HttpGetHandler() {
            @Override
            public Response get(String serverURL, String path, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);
                String responseBody = null;

                switch (path) {
                    case "/services/info/status":
                        if (System.currentTimeMillis() - startTime < 3000) {
                            responseBody = "{\"infoStatus\":{\"status\":\"NOT_READY\"}}";
                        } else {
                            responseBody = "{\"infoStatus\":{\"status\":\"READY\"}}";
                        }
                        break;

                    case "/services/info":
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            IOUtils.copy(new FileInputStream("src/test/resources/info.json"), baos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        responseBody = baos.toString();
                        break;

                    default:
                        fail("Unexpected path: " + path);
                }

                return new Response(200, "OK", "application/json", responseBody);
            }
        };

        StringBufferConsole console = new StringBufferConsole();

        CliwixCliClient.getHandler = getHandler;
        CliwixCliClient.console = console;
        CliwixCliClient.main(new String[]{"info", "src/test/resources/cliwix-cli-test.properties"});

        String consoleDump = console.getBufferAsString();
        System.out.println(consoleDump);

        assertTrue(consoleDump.contains("Server not ready. Waiting..."));
        assertTrue(consoleDump.contains("Cliwix Version: 1.0.0-20140718-1129"));
    }

    @Test
    public void testCreateConfig() throws Exception {
        File newConfig = new File("target/cliwix-cli-new.properties");
        if (newConfig.exists()) newConfig.delete();

        StringBufferConsole console = new StringBufferConsole();
        CliwixCliClient.console = console;
        CliwixCliClient.main(new String[]{"create-config", newConfig.getAbsolutePath()});

        assertTrue(newConfig.exists());
    }

    @Test
    public void testExportExcludeFilterSuccess() throws Exception {
        File outputDir = new File("target/export/" + System.currentTimeMillis());
        final long startTime = System.currentTimeMillis();

        final File zipFile = new File("target/export.zip");
        ZipFile zip = new ZipFile(zipFile);
        zip.addFile(new File("src/test/resources/liferay-config.xml"), new ZipParameters());
        final InputStream zipFileStream = new FileInputStream(zipFile);

        HttpGetHandler getHandler = new HttpGetHandler() {
            @Override
            public Response get(String serverURL, String path, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/info/status":
                        return new Response(200, "OK", "application/json", "{\"infoStatus\":{\"status\":\"READY\"}}");

                    case "/services/exports/123456789/status":
                        String responseBody;
                        if (System.currentTimeMillis() - startTime < 3000) {
                            responseBody = "{\"exportStatus\":{\"status\":\"processing\"}}";
                        } else {
                            responseBody = "{\"exportStatus\":{\"status\":\"success\"}}";
                        }

                        return new Response(200, "OK", "application/json", responseBody);

                    case "/services/exports/123456789/zip":
                        return new Response(200, "OK", "application/zip", zipFile.length(), zipFileStream, null);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        HttpPostHandler postHandler = new HttpPostHandler() {
            @Override
            public Response post(String serverURL, String path, InputStream request,
                                 long contentLength, CONTENT_TYPE contentType, String fileName,
                                 ProgressCallback progressCallback, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/exports":
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            IOUtils.copy(request, baos);
                        } catch (IOException e) {
                        }

                        String requestBody = baos.toString();

                        assertTrue(requestBody.contains("\"exportOrganizations\":\"true\""));
                        assertTrue(requestBody.contains("\"exportRoles\":\"true\""));
                        assertTrue(requestBody.contains("\"siteFilter\":null"));
                        assertTrue(requestBody.contains("\"exportDocumentLibrary\":\"false\""));
                        assertTrue(requestBody.contains("\"exportUserGroups\":\"true\""));
                        assertTrue(requestBody.contains("\"exportSiteConfiguration\":\"true\""));
                        assertTrue(requestBody.contains("\"exportPages\":\"false\""));
                        assertTrue(requestBody.contains("\"skipCorruptDocuments\":\"false\""));
                        assertTrue(requestBody.contains("\"exportWebContent\":\"true\""));
                        assertTrue(requestBody.contains("\"exportPortalInstanceConfiguration\":\"true\""));
                        assertTrue(requestBody.contains("\"exportOnlyFileDataLastModifiedWithinDays\":null"));
                        assertTrue(requestBody.contains("\"companyFilter\":null"));
                        assertTrue(requestBody.contains("\"exportUsers\":\"true\""));

                        String responseBody = "{\"exportResult\":{\"exportId\":\"123456789\"}}";
                        return new Response(200, "OK", "application/json", responseBody);

                    default:
                        fail("Unexpected path: " + path);

                }

                return null;
            }
        };


        StringBufferConsole console = new StringBufferConsole();

        CliwixCliClient.getHandler = getHandler;
        CliwixCliClient.postHandler = postHandler;
        CliwixCliClient.console = console;

        CliwixCliClient.main(new String[]{"export", "src/test/resources/cliwix-cli-test.properties", "--export.folder=" + outputDir.getAbsolutePath()});

        String consoleDump = console.getBufferAsString();
        System.out.println(consoleDump);

        assertTrue(outputDir.exists());
        assertTrue(new File(outputDir, "liferay-config.xml").exists());

        assertTrue(consoleDump.contains("Server is ready"));
        assertTrue(consoleDump.contains("Export started. Id: 123456789"));
        assertTrue(consoleDump.contains("Export succeeded."));
        assertTrue(consoleDump.contains("Transferred:"));
        assertTrue(consoleDump.contains("Export data written to:"));
    }

    @Test
    public void testExportIncludeFilterSuccess() throws Exception {
        File outputDir = new File("target/export/" + System.currentTimeMillis());
        final long startTime = System.currentTimeMillis();

        final File zipFile = new File("target/export.zip");
        ZipFile zip = new ZipFile(zipFile);
        zip.addFile(new File("src/test/resources/liferay-config.xml"), new ZipParameters());
        final InputStream zipFileStream = new FileInputStream(zipFile);

        HttpGetHandler getHandler = new HttpGetHandler() {
            @Override
            public Response get(String serverURL, String path, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/info/status":
                        return new Response(200, "OK", "application/json", "{\"infoStatus\":{\"status\":\"READY\"}}");

                    case "/services/exports/123456789/status":
                        String responseBody;
                        if (System.currentTimeMillis() - startTime < 3000) {
                            responseBody = "{\"exportStatus\":{\"status\":\"processing\"}}";
                        } else {
                            responseBody = "{\"exportStatus\":{\"status\":\"success\"}}";
                        }

                        return new Response(200, "OK", "application/json", responseBody);

                    case "/services/exports/123456789/zip":
                        return new Response(200, "OK", "application/zip", zipFile.length(), zipFileStream, null);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        HttpPostHandler postHandler = new HttpPostHandler() {
            @Override
            public Response post(String serverURL, String path, InputStream request,
                                 long contentLength, CONTENT_TYPE contentType, String fileName,
                                 ProgressCallback progressCallback, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/exports":

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            IOUtils.copy(request, baos);
                        } catch (IOException e) {
                        }

                        String requestBody = baos.toString();

                        assertTrue(requestBody.contains("\"exportOrganizations\":\"true\""));
                        assertTrue(requestBody.contains("\"exportRoles\":\"true\""));
                        assertTrue(requestBody.contains("\"siteFilter\":null"));
                        assertTrue(requestBody.contains("\"exportDocumentLibrary\":\"false\""));
                        assertTrue(requestBody.contains("\"exportUserGroups\":\"true\""));
                        assertTrue(requestBody.contains("\"exportSiteConfiguration\":\"true\""));
                        assertTrue(requestBody.contains("\"exportPages\":\"false\""));
                        assertTrue(requestBody.contains("\"skipCorruptDocuments\":\"true\""));
                        assertTrue(requestBody.contains("\"exportWebContent\":\"true\""));
                        assertTrue(requestBody.contains("\"exportPortalInstanceConfiguration\":\"true\""));
                        assertTrue(requestBody.contains("\"exportOnlyFileDataLastModifiedWithinDays\":null"));
                        assertTrue(requestBody.contains("\"companyFilter\":null"));
                        assertTrue(requestBody.contains("\"exportUsers\":\"true\""));

                        String responseBody = "{\"exportResult\":{\"exportId\":\"123456789\"}}";
                        return new Response(200, "OK", "application/json", responseBody);

                    default:
                        fail("Unexpected path: " + path);

                }

                return null;
            }
        };


        StringBufferConsole console = new StringBufferConsole();

        CliwixCliClient.getHandler = getHandler;
        CliwixCliClient.postHandler = postHandler;
        CliwixCliClient.console = console;

        CliwixCliClient.main(new String[]{"export", "src/test/resources/cliwix-cli-test.properties", "--export.folder=" + outputDir.getAbsolutePath(), "--export.settings.skipCorruptDocuments=true"});

        String consoleDump = console.getBufferAsString();
        System.out.println(consoleDump);

        assertTrue(outputDir.exists());
        assertTrue(new File(outputDir, "liferay-config.xml").exists());

        assertTrue(consoleDump.contains("Server is ready"));
        assertTrue(consoleDump.contains("Export started. Id: 123456789"));
        assertTrue(consoleDump.contains("Export succeeded."));
        assertTrue(consoleDump.contains("Transferred:"));
        assertTrue(consoleDump.contains("Export data written to:"));
    }

    @Test
    public void testExportFailure() throws Exception {
        File outputDir = new File("target/export/" + System.currentTimeMillis());
        final long startTime = System.currentTimeMillis();

        HttpGetHandler getHandler = new HttpGetHandler() {
            @Override
            public Response get(String serverURL, String path, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/info/status":
                        return new Response(200, "OK", "application/json", "{\"infoStatus\":{\"status\":\"READY\"}}");

                    case "/services/exports/123456789/status":
                        String responseBody;
                        if (System.currentTimeMillis() - startTime < 3000) {
                            responseBody = "{\"exportStatus\":{\"status\":\"processing\"}}";
                        } else {
                            responseBody = "{\"exportStatus\":{\"status\":\"failed\"}}";
                        }

                        return new Response(200, "OK", "application/json", responseBody);

                    case "/services/exports/123456789/report":
                        String report = "<html><body><h1>test</h1></body></html>";
                        return new Response(200, "OK", "application/zip", report.length(), new ByteArrayInputStream(report.getBytes()), null);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        HttpPostHandler postHandler = new HttpPostHandler() {
            @Override
            public Response post(String serverURL, String path, InputStream request,
                                 long contentLength, CONTENT_TYPE contentType, String fileName,
                                 ProgressCallback progressCallback, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/exports":
                        String responseBody = "{\"exportResult\":{\"exportId\":\"123456789\"}}";
                        return new Response(200, "OK", "application/json", responseBody);

                    default:
                        fail("Unexpected path: " + path);

                }

                return null;
            }
        };


        StringBufferConsole console = new StringBufferConsole();

        CliwixCliClient.getHandler = getHandler;
        CliwixCliClient.postHandler = postHandler;
        CliwixCliClient.console = console;

        try {
            CliwixCliClient.main(new String[]{"export",  "src/test/resources/cliwix-cli-test.properties", "--export.folder=" + outputDir.getAbsolutePath()});
            fail("Exit status must be failure");
        } catch (ExitStatusFailureException e) {
        }

        String consoleDump = console.getBufferAsString();
        System.out.println(consoleDump);

        assertTrue(outputDir.exists());
        assertTrue(new File(outputDir, "export-report.html").exists());

        assertTrue(consoleDump.contains("Server is ready"));
        assertTrue(consoleDump.contains("Export started. Id: 123456789"));
        assertTrue(consoleDump.contains("Export failed!"));
        assertTrue(consoleDump.contains("Error report written to:"));
    }

    @Test
    public void testImportZIPSuccess() throws Exception {
        File outputDir = new File("target/import/" + System.currentTimeMillis());
        final long startTime = System.currentTimeMillis();

        HttpGetHandler getHandler = new HttpGetHandler() {
            @Override
            public Response get(String serverURL, String path, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/info/status":
                        return new Response(200, "OK", "application/json", "{\"infoStatus\":{\"status\":\"READY\"}}");

                    case "/services/imports/123456789/status":
                        String responseBody;
                        if (System.currentTimeMillis() - startTime < 3000) {
                            responseBody = "{\"importStatus\":{\"status\":\"processing\"}}";
                        } else {
                            responseBody = "{\"importStatus\":{\"status\":\"success\"}}";
                        }

                        return new Response(200, "OK", "application/json", responseBody);

                    case "/services/imports/123456789/report":
                        String report = "<html><body><h1>test</h1></body></html>";
                        return new Response(200, "OK", "application/zip", report.length(), new ByteArrayInputStream(report.getBytes()), null);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        HttpPostHandler postHandler = new HttpPostHandler() {
            @Override
            public Response post(String serverURL, String path, InputStream request,
                                 long contentLength, CONTENT_TYPE contentType, String fileName,
                                 ProgressCallback progressCallback, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/uploads":
                        assertEquals("import2.zip", fileName);
                        assertEquals(CONTENT_TYPE.UPLOAD, contentType);
                        progressCallback.bytesTransferred(contentLength, contentLength);
                        return new Response(200, "OK", "application/json", "{\"uploadResult\":{\"tmpFileName\":\"import.zip\"}}");

                    case "/services/imports":
                        String responseBody = "{\"importResult\":{\"importId\":\"123456789\"}}";
                        return new Response(200, "OK", "application/json", responseBody);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        File zipFile = new File("target/import2.zip");
        ZipFile zip = new ZipFile(zipFile);
        zip.addFile(new File("src/test/resources/liferay-config.xml"), new ZipParameters());

        StringBufferConsole console = new StringBufferConsole();

        CliwixCliClient.getHandler = getHandler;
        CliwixCliClient.postHandler = postHandler;
        CliwixCliClient.console = console;

        CliwixCliClient.main(new String[]{"import", "src/test/resources/cliwix-cli-test.properties", "--import.input=" + zipFile.getAbsolutePath(), "--import.report.folder=" + outputDir.getAbsolutePath()});

        String consoleDump = console.getBufferAsString();
        System.out.println(consoleDump);

        assertTrue(outputDir.exists());
        assertTrue(new File(outputDir, "import-report.html").exists());

        assertTrue(consoleDump.contains("Server is ready"));
        assertTrue(consoleDump.contains("Transferred: "));
        assertTrue(consoleDump.contains("Import started. Id: 123456789"));
        assertTrue(consoleDump.contains("Import succeeded."));
        assertTrue(consoleDump.contains("Import report written to:"));
    }

    @Test
    public void testImportFolderSuccess() throws Exception {
        File outputDir = new File("target/import/" + System.currentTimeMillis());
        final long startTime = System.currentTimeMillis();

        HttpGetHandler getHandler = new HttpGetHandler() {
            @Override
            public Response get(String serverURL, String path, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/info/status":
                        return new Response(200, "OK", "application/json", "{\"infoStatus\":{\"status\":\"READY\"}}");

                    case "/services/imports/123456789/status":
                        String responseBody;
                        if (System.currentTimeMillis() - startTime < 3000) {
                            responseBody = "{\"importStatus\":{\"status\":\"processing\"}}";
                        } else {
                            responseBody = "{\"importStatus\":{\"status\":\"success\"}}";
                        }

                        return new Response(200, "OK", "application/json", responseBody);

                    case "/services/imports/123456789/report":
                        String report = "<html><body><h1>test</h1></body></html>";
                        return new Response(200, "OK", "application/zip", report.length(), new ByteArrayInputStream(report.getBytes()), null);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        HttpPostHandler postHandler = new HttpPostHandler() {
            @Override
            public Response post(String serverURL, String path, InputStream request,
                                 long contentLength, CONTENT_TYPE contentType, String fileName,
                                 ProgressCallback progressCallback, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/uploads":
                        assertTrue(fileName.startsWith("import") && fileName.endsWith(".zip"));
                        assertEquals(CONTENT_TYPE.UPLOAD, contentType);
                        progressCallback.bytesTransferred(contentLength, contentLength);
                        return new Response(200, "OK", "application/json", "{\"uploadResult\":{\"tmpFileName\":\"import.zip\"}}");

                    case "/services/imports":
                        String responseBody = "{\"importResult\":{\"importId\":\"123456789\"}}";
                        return new Response(200, "OK", "application/json", responseBody);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        File importFolder  = new File("target/import");
        importFolder.mkdirs();
        IOUtils.copy(new FileInputStream("src/test/resources/liferay-config.xml"), new FileOutputStream(new File(importFolder, "liferay-config.xml")));

        StringBufferConsole console = new StringBufferConsole();

        CliwixCliClient.getHandler = getHandler;
        CliwixCliClient.postHandler = postHandler;
        CliwixCliClient.console = console;

        CliwixCliClient.main(new String[]{"import", "src/test/resources/cliwix-cli-test.properties", "--import.input=" + importFolder.getAbsolutePath(), "--import.report.folder=" + outputDir.getAbsolutePath()});

        String consoleDump = console.getBufferAsString();
        System.out.println(consoleDump);

        assertTrue(outputDir.exists());
        assertTrue(new File(outputDir, "import-report.html").exists());

        assertTrue(consoleDump.contains("Server is ready"));
        assertTrue(consoleDump.contains("Transferred: "));
        assertTrue(consoleDump.contains("Import started. Id: 123456789"));
        assertTrue(consoleDump.contains("Import succeeded."));
        assertTrue(consoleDump.contains("Import report written to:"));
    }

    @Test
    public void testImportXMLSuccess() throws Exception {
        File outputDir = new File("target/import/" + System.currentTimeMillis());
        final long startTime = System.currentTimeMillis();

        HttpGetHandler getHandler = new HttpGetHandler() {
            @Override
            public Response get(String serverURL, String path, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/info/status":
                        return new Response(200, "OK", "application/json", "{\"infoStatus\":{\"status\":\"READY\"}}");

                    case "/services/imports/123456789/status":
                        String responseBody;
                        if (System.currentTimeMillis() - startTime < 3000) {
                            responseBody = "{\"importStatus\":{\"status\":\"processing\"}}";
                        } else {
                            responseBody = "{\"importStatus\":{\"status\":\"success\"}}";
                        }

                        return new Response(200, "OK", "application/json", responseBody);

                    case "/services/imports/123456789/report":
                        String report = "<html><body><h1>test</h1></body></html>";
                        return new Response(200, "OK", "application/zip", report.length(), new ByteArrayInputStream(report.getBytes()), null);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        HttpPostHandler postHandler = new HttpPostHandler() {
            @Override
            public Response post(String serverURL, String path, InputStream request,
                                 long contentLength, CONTENT_TYPE contentType, String fileName,
                                 ProgressCallback progressCallback, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/uploads":
                        assertEquals("liferay-config.xml", fileName);
                        assertEquals(CONTENT_TYPE.UPLOAD, contentType);
                        progressCallback.bytesTransferred(contentLength, contentLength);
                        return new Response(200, "OK", "application/json", "{\"uploadResult\":{\"tmpFileName\":\"import.zip\"}}");

                    case "/services/imports":
                        String responseBody = "{\"importResult\":{\"importId\":\"123456789\"}}";
                        return new Response(200, "OK", "application/json", responseBody);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        StringBufferConsole console = new StringBufferConsole();

        CliwixCliClient.getHandler = getHandler;
        CliwixCliClient.postHandler = postHandler;
        CliwixCliClient.console = console;

        CliwixCliClient.main(new String[]{"import", "src/test/resources/cliwix-cli-test.properties", "--import.input=" + "src/test/resources/liferay-config.xml", "--import.report.folder=" + outputDir.getAbsolutePath()});

        String consoleDump = console.getBufferAsString();
        System.out.println(consoleDump);

        assertTrue(outputDir.exists());
        assertTrue(new File(outputDir, "import-report.html").exists());

        assertTrue(consoleDump.contains("Server is ready"));
        assertTrue(consoleDump.contains("Transferred: "));
        assertTrue(consoleDump.contains("Import started. Id: 123456789"));
        assertTrue(consoleDump.contains("Import succeeded."));
        assertTrue(consoleDump.contains("Import report written to:"));
    }

    @Test
    public void testImportFailure() throws Exception {
        File outputDir = new File("target/import/" + System.currentTimeMillis());
        final long startTime = System.currentTimeMillis();

        HttpGetHandler getHandler = new HttpGetHandler() {
            @Override
            public Response get(String serverURL, String path, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/info/status":
                        return new Response(200, "OK", "application/json", "{\"infoStatus\":{\"status\":\"READY\"}}");

                    case "/services/imports/123456789/status":
                        String responseBody;
                        if (System.currentTimeMillis() - startTime < 3000) {
                            responseBody = "{\"importStatus\":{\"status\":\"processing\"}}";
                        } else {
                            responseBody = "{\"importStatus\":{\"status\":\"failed\"}}";
                        }

                        return new Response(200, "OK", "application/json", responseBody);

                    case "/services/imports/123456789/report":
                        String report = "<html><body><h1>test</h1></body></html>";
                        return new Response(200, "OK", "application/zip", report.length(), new ByteArrayInputStream(report.getBytes()), null);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        HttpPostHandler postHandler = new HttpPostHandler() {
            @Override
            public Response post(String serverURL , String path, InputStream request,
                                 long contentLength, CONTENT_TYPE contentType, String fileName,
                                 ProgressCallback progressCallback, CookieManager cookieManager) {
                assertEquals("http://localhost:8080/cliwix", serverURL);

                switch (path) {
                    case "/services/uploads":
                        assertEquals("import3.zip", fileName);
                        assertEquals(CONTENT_TYPE.UPLOAD, contentType);
                        progressCallback.bytesTransferred(contentLength, contentLength);
                        return new Response(200, "OK", "application/json", "{\"uploadResult\":{\"tmpFileName\":\"import.zip\"}}");

                    case "/services/imports":
                        String responseBody = "{\"importResult\":{\"importId\":\"123456789\"}}";
                        return new Response(200, "OK", "application/json", responseBody);

                    default:
                        fail("Unexpected path: " + path);
                }

                return null;
            }
        };

        final File zipFile = new File("target/import3.zip");
        final ZipFile zip = new ZipFile(zipFile);
        zip.addFile(new File("src/test/resources/liferay-config.xml"), new ZipParameters());

        StringBufferConsole console = new StringBufferConsole();

        CliwixCliClient.getHandler = getHandler;
        CliwixCliClient.postHandler = postHandler;
        CliwixCliClient.console = console;

        try {
            CliwixCliClient.main(new String[]{"import", "src/test/resources/cliwix-cli-test.properties", "--import.input=" + zipFile.getAbsolutePath(), "--import.report.folder=" + outputDir.getAbsolutePath()});
            fail("Exit status must be failure");
        } catch (ExitStatusFailureException e) {
        }

        String consoleDump = console.getBufferAsString();
        System.out.println(consoleDump);

        assertTrue(outputDir.exists());
        assertTrue(new File(outputDir, "import-report.html").exists());

        assertTrue(consoleDump.contains("Server is ready"));
        assertTrue(consoleDump.contains("Transferred: "));
        assertTrue(consoleDump.contains("Import started. Id: 123456789"));
        assertTrue(consoleDump.contains("Import failed!"));
        assertTrue(consoleDump.contains("Import report written to:"));
    }

}
