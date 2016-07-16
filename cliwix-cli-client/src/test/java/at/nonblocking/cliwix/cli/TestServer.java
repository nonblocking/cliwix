package at.nonblocking.cliwix.cli;

import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.ResourceHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class TestServer {

    final static int PORT = 11223;
    final static File DOCUMENT_ROOT = new File("target/documentroot");

    private static Thread serverThread = null;

    public static void start() {
        if (serverThread == null || !serverThread.isAlive()) {
            DOCUMENT_ROOT.mkdirs();

            serverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                try {
                    Server server = new Server(PORT);
                    ResourceHandler resourceHandler = new ResourceHandler();
                    resourceHandler.setDirectoriesListed(true);
                    resourceHandler.setResourceBase(DOCUMENT_ROOT.getAbsolutePath());

                    HandlerList handlers = new HandlerList();
                    handlers.setHandlers(new Handler[]{resourceHandler, new PostHandler(), new DefaultHandler()});
                    server.setHandler(handlers);

                    server.start();
                    server.join();

                    while (!Thread.interrupted()) {
                        Thread.yield();
                    }

                } catch (Exception e) {
                }
                }
            });
            serverThread.start();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }

    private static class PostHandler extends HandlerWrapper {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (baseRequest.isHandled())
                return;

            if (!HttpMethod.POST.is(request.getMethod())) {
                //try another handler
                super.handle(target, baseRequest, request, response);
                return;
            }

            baseRequest.setHandled(true);

            switch (target) {
                case "/exports":
                    response.setContentType("application/json");
                    IOUtils.copy(new FileInputStream("src/test/resources/exportResult.json"), response.getWriter());
                    break;

                case "/uploads":
                    assertTrue(ServletFileUpload.isMultipartContent(request));
                    assertEquals("multipart/form-data;boundary=*****", request.getContentType());

                    try {
                        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
                        List<FileItem> items = upload.parseRequest(request);

                        assertNotNull(items);
                        assertEquals(1, items.size());
                        assertEquals("test.zip", items.get(0).getName());

                        File zipFile = File.createTempFile("test", "zip");
                        items.get(0).write(zipFile);

                        ZipFile zip = new ZipFile(zipFile);
                        zip.extractFile("info.json", "target/upload");
                        assertEquals(new File("src/test/resources/info.json").length(), new File("target/upload/info.json").length());

                    } catch (Exception e) {
                        throw new IOException(e);
                    }

                    response.setContentType("application/json");
                    IOUtils.copy(new FileInputStream("src/test/resources/uploadResult.json"), response.getWriter());
                    break;

                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);

            }
        }

    }
}