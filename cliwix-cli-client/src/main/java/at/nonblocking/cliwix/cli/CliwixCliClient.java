/*
 * Copyright (c) 2014-2016
 * nonblocking.at gmbh [http://www.nonblocking.at]
 *
 * This file is part of Cliwix.
 *
 * Cliwix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package at.nonblocking.cliwix.cli;


import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class CliwixCliClient {

    private static final int EXIT_STATUS_FAIL = 1;

    static boolean debug = false;
    static Console console = new ConsoleDefaultImpl();
    static HttpGetHandler getHandler = new HttpGetHandlerDefaultImpl();
    static HttpPostHandler postHandler = new HttpPostHandlerDefaultImpl();
    static HttpDeleteHandler deleteHandler = new HttpDeleteHandlerDefaultImpl();

    static CookieManager cookieManager = new CookieManagerImpl();

    public static void main(String[] args) throws Exception {
        Options options = null;

        try {
            options = Options.parseArgs(args);
        } catch (CliwixCliClientArgumentException e) {
            if (debug) console.printStacktrace(e);
            console.printlnError("Error: Invalid arguments: " + e.getMessage());
            Options.printUsage();
            console.exit(EXIT_STATUS_FAIL);
            return;
        }

        if (options == null || Options.COMMAND_HELP.equals(options.getCommand())) {
            Options.printUsage();
            console.exit(0);
            return;
        }

        if (Options.COMMAND_CREATE_CONFIG.equals(options.getCommand())) {
            if (args.length < 2) {
                console.printlnError("Error: No config file location given!");
                console.exit(EXIT_STATUS_FAIL);
                return;
            }
            File configFile = new File(args[1]);
            options.copyDefaultPropertiesTo(configFile);
            console.println("Configuration saved under: " + configFile.getAbsolutePath());
            console.exit(0);
            return;
        }

        debug = options.isDebug();

        String cliwixServerUrl = options.getServerCliwixUrl();
        String username = options.getServerUsername();
        String password = options.getServerPassword();

        if (cliwixServerUrl == null) {
            console.printlnError("Error: Property server.cliwix.url is required!");
            console.exit(EXIT_STATUS_FAIL);
            return;
        }

        cookieManager.clear();

        //Login
        if (username != null && password != null) {
            JSONObject loginData = new JSONObject();
            loginData.put("username", username);
            loginData.put("password", password);
            JSONObject result = postJson(cliwixServerUrl, "/services/login", loginData);
            JSONObject loginResult = (JSONObject) result.get("loginResult");

            if (loginResult.get("succeeded") == Boolean.TRUE) {
                console.println("Login successful");
            } else {
                console.printlnError("Error: Login failed! Reason: " + loginData.get("errorMessage"));
                console.exit(EXIT_STATUS_FAIL);
                return;
            }

        } else if (username != null) {
            console.printlnError("Error: If -user is set -pass is required!");
            console.exit(EXIT_STATUS_FAIL);
            return;
        } else if (password != null) {
            console.printlnError("Error: If -pass is set -user is required!");
            console.exit(EXIT_STATUS_FAIL);
            return;
        }

        switch (options.getCommand()) {
            case Options.COMMAND_INFO:
                doInfo(cliwixServerUrl, options);
                break;
            case Options.COMMAND_EXPORT:
                doExport(cliwixServerUrl, options);
                break;
            case Options.COMMAND_IMPORT:
                doImport(cliwixServerUrl, options);
                break;
            default:
            case Options.COMMAND_HELP:
                Options.printUsage();
        }
    }

    private static boolean serverTest(String serverUrl) {
        JSONObject json = getJson(serverUrl, "/services/info/status");
        String state = (String) ((JSONObject) json.get("infoStatus")).get("status");

        switch (state) {
            case "LIFERAY_NOT_FOUND":
                console.printlnError("Error: No Liferay server found!");
                console.exit(EXIT_STATUS_FAIL);
            case "LIFERAY_VERSION_NOT_SUPPORTED":
                console.printlnError("Error: Liferay version not supported!");
                console.exit(EXIT_STATUS_FAIL);
            case "NOT_READY":
                return false;
            default:
            case "READY":
                console.println("Server is ready.");
                return true;
        }
    }

    private static void waitForServerReady(String serverUrl, Options options) {
        long startTime = System.currentTimeMillis();
        boolean ready = false;

        while (!ready && (System.currentTimeMillis() - startTime) < options.getTimeoutServerReadySec() * 1000) {
            ready = serverTest(serverUrl);
            if (!ready) {
                console.println("Server not ready. Waiting...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static void doInfo(String cliwixServerUrl, Options options) {
        waitForServerReady(cliwixServerUrl, options);

        JSONObject json = getJson(cliwixServerUrl, "/services/info");
        JSONObject info = (JSONObject) json.get("info");

        console.println("Server info:");
        console.println("  Cliwix Version: " + info.get("cliwixVersion"));
        console.println("  Cliwix Workspace Directory: " + info.get("cliwixWorkspaceDirectory"));
        console.println("  Liferay Release: " + info.get("liferayRelease"));
    }

    private static void doExport(String cliwixServerUrl, Options options) {
        waitForServerReady(cliwixServerUrl, options);

        String exportFolder = options.getExportFolder();
        if (exportFolder == null) {
            console.printlnError("Error: Property export.folder is required.");
            console.exit(EXIT_STATUS_FAIL);
            return;
        }

        File outputDir = null;
        try {
            outputDir = new File(exportFolder);
            outputDir.mkdirs();
        } catch (Exception e) {
            console.printlnError("Error: Invalid export folder: " + exportFolder);
            console.exit(EXIT_STATUS_FAIL);
            return;
        }

        JSONObject exportSettings = options.getExportSettings();

        JSONObject json = postJson(cliwixServerUrl, "/services/exports", exportSettings);
        JSONObject exportResult = (JSONObject) json.get("exportResult");
        String exportId = (String) exportResult.get("exportId");

        console.println("Export started. Id: " + exportId);

        boolean success = waitForImportExport(true, exportId, cliwixServerUrl, options);

        if (success) {
            console.println("Export succeeded.");

            String path = "/services/exports/" + exportId + "/zip";
            if (debug) console.println("Sending GET request: " + path);
            Response zipResponse = getHandler.get(cliwixServerUrl, path, cookieManager);
            handleError(zipResponse);

            File tempFile = null;
            try {
                tempFile = File.createTempFile("export", ".zip");
            } catch (IOException e) {
                if (debug) console.printStacktrace(e);
                console.printlnError("Error: Unable to create temporary zip file");
                console.exit(EXIT_STATUS_FAIL);
                return;
            }
            long fileLength = zipResponse.getContentLength();
            ProgressCallback progressCallback = new ProgressCallbackConsoleImpl(console);

            try (InputStream inputStream = zipResponse.getResponseStream();
                OutputStream outputStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[BaseHttpHandler.CHUNK_SIZE];
                long transferred = 0;
                int len = -1;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                    transferred = transferred + len;
                    progressCallback.bytesTransferred(transferred, fileLength);
                }

                progressCallback.bytesTransferred(fileLength, fileLength);

            } catch (IOException e) {
                if (debug) console.printStacktrace(e);
                console.printlnError("Error: " + e.getMessage());
                console.exit(EXIT_STATUS_FAIL);
            } finally {
                zipResponse.disconnect();
            }

            if (options.isExtractZip()) {
                try {
                    ZipFile zip = new ZipFile(tempFile);
                    zip.extractAll(outputDir.getAbsolutePath());
                } catch (ZipException e) {
                    if (debug) console.printStacktrace(e);
                    console.printlnError("Error: Corrupt ZIP file");
                    console.exit(EXIT_STATUS_FAIL);
                } finally {
                    tempFile.delete();
                }
            } else {
                File targetFile = new File(outputDir, "export_" + exportId + ".zip");
                try (FileInputStream fis = new FileInputStream(tempFile)) {
                    Files.copy(fis, targetFile.toPath());
                } catch (IOException e) {
                    if (debug) console.printStacktrace(e);
                    console.printlnError("Error: " + e.getMessage());
                    console.exit(EXIT_STATUS_FAIL);
                } finally {
                    tempFile.delete();
                }
            }

            console.println("Export data written to: " + outputDir.getAbsolutePath());

        } else {
            console.printlnError("Export failed!");

            String path = "/services/exports/" + exportId + "/report";
            if (debug) console.println("Sending GET request: " + path);
            Response reportResponse = getHandler.get(cliwixServerUrl, path, cookieManager);
            handleError(reportResponse);

            File reportFile = new File(outputDir, "export-report.html");

            try (OutputStream outputStream = new FileOutputStream(reportFile)) {
                outputStream.write(reportResponse.getResponseAsString().getBytes("UTF-8"));
                console.println("Error report written to: " + reportFile.getAbsolutePath());

            } catch (IOException e) {
                if (debug) console.printStacktrace(e);
                console.printlnError("Error: No report found");
            } finally {
                reportResponse.disconnect();
            }
        }

        if (options.isExportDeleteOnServerAfterTransfer()) {
            Response deleteResponse = deleteHandler.delete(cliwixServerUrl, "/services/exports/" + exportId, cookieManager);
            if (deleteResponse.getStatusCode() == 200) {
                console.println("Successfully deleted export folder on server.");
            } else {
                console.printlnError("Failed to delete export folder on server!");
            }
        }

        if (!success) {
            console.exit(EXIT_STATUS_FAIL);
        }
    }

    private static void doImport(String cliwixServerUrl, Options options) {
        waitForServerReady(cliwixServerUrl, options);

        String importReportFolder = options.getImportReportFolder();
        if (importReportFolder == null) {
            importReportFolder = ".";
        }
        File outputDir = null;
        try {
            outputDir = new File(importReportFolder);
            outputDir.mkdirs();
        } catch (Exception e) {
            console.printlnError("Error: Invalid import report folder: " + importReportFolder);
            console.exit(EXIT_STATUS_FAIL);
            return;
        }

        String input = options.getImportInput();
        if (input == null) {
            console.printlnError("Error: Property import.input is required.");
            console.exit(EXIT_STATUS_FAIL);
            return;
        }

        File inputFile = new File(input);
        if (!inputFile.exists()) {
            console.printlnError("Error: Input does not exist: " + inputFile.getAbsolutePath());
            console.exit(EXIT_STATUS_FAIL);
        }

        if (inputFile.isDirectory()) {
            try {
                File tempFile = File.createTempFile("import", ".zip");
                tempFile.delete(); //Must not exist
                ZipFile zipFile = new ZipFile(tempFile);
                ZipParameters zipParams = new ZipParameters();
                zipParams.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                zipParams.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST);
                zipParams.setIncludeRootFolder(false);
                zipFile.addFolder(inputFile, zipParams);
                inputFile = tempFile;
            } catch (IOException | ZipException e) {
                if (debug) console.printStacktrace(e);
                console.printlnError("Error: Unable to create zip file");
                console.exit(EXIT_STATUS_FAIL);
                return;
            }

        } else if (!inputFile.getName().endsWith(".xml") && !inputFile.getName().endsWith(".zip")) {
            console.printlnError("Error: Invalid input (must be a folder or a XML file or a ZIP archive): " + inputFile.getAbsolutePath());
            console.exit(EXIT_STATUS_FAIL);
            return;
        }

        JSONObject uploadResult = uploadFile(cliwixServerUrl, "/services/uploads", inputFile);
        String tmpFileName = (String) ((JSONObject) uploadResult.get("uploadResult")).get("tmpFileName");

        JSONObject importSettings = options.getImportSettings(tmpFileName);

        JSONObject importResult = postJson(cliwixServerUrl, "/services/imports", importSettings);

        String importId = (String) ((JSONObject) importResult.get("importResult")).get("importId");
        console.println("Import started. Id: " + importId);

        boolean success = waitForImportExport(false, importId, cliwixServerUrl, options);

        if (success) {
            console.println("Import succeeded.");
        } else {
            console.printlnError("Error: Import failed!");
        }

        String path = "/services/imports/" + importId + "/report";
        if (debug) console.println("Sending GET request: " + path);
        Response reportResponse = getHandler.get(cliwixServerUrl, path, cookieManager);
        handleError(reportResponse);

        File reportFile = new File(outputDir, "import-report.html");

        try (OutputStream outputStream = new FileOutputStream(reportFile)) {
            outputStream.write(reportResponse.getResponseAsString().getBytes("UTF-8"));
            console.println("Import report written to: " + reportFile.getAbsolutePath());

        } catch (IOException e) {
            if (debug) console.printStacktrace(e);
            console.printlnError("Error: No import report found");
            success = false;
        } finally {
            reportResponse.disconnect();
        }

        if (options.isImportDeleteOnServerAfterTransfer()) {
            Response deleteResponse = deleteHandler.delete(cliwixServerUrl, "/services/imports/" + importId, cookieManager);
            if (deleteResponse.getStatusCode() == 200) {
                console.println("Successfully deleted import folder on server.");
            } else {
                console.printlnError("Failed to delete import folder on server!");
            }
        }

        if (!success) {
            console.exit(EXIT_STATUS_FAIL);
        }
    }

    private static boolean waitForImportExport(boolean export, String id, String serverUrl, Options options) {
        String path;
        if (export) path = "/services/exports/" + id + "/status";
        else path = "/services/imports/" + id + "/status";

        long startTime = System.currentTimeMillis();
        long timeout = export ? options.getTimeoutExportSec() * 1000 : options.getTimeoutImportSec() * 1000;

        while (System.currentTimeMillis() - startTime < timeout) {
            JSONObject json = getJson(serverUrl, path);
            String state = (String) ((JSONObject) json.get(export ? "exportStatus" : "importStatus")).get("status");

            switch (state) {
                case "success":
                    return true;
                case "processing":
                    console.println("Waiting for " + (export ? "export" : "import") + " to complete...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    break;
                default:
                case "fail":
                    return false;
            }
        }

        return false;
    }

    private static void handleError(Response response) {
        if (response.getStatusCode() != 200) {
            String message = response.getStatusCode() + ": " + response.getStatusMessage();

            try {
                if (response.getContentType().contains("json")) {
                    String jsonString = response.getResponseAsString();
                    JSONObject json = (JSONObject) JSONValue.parse(jsonString);
                    message = (String) ((JSONObject) json.get("error")).get("message");
                }
            } catch (Exception e) {
                if (debug) console.printStacktrace(e);
            }

            console.printlnError("Error: " + message);
            console.exit(EXIT_STATUS_FAIL);
        }
    }

    private static JSONObject getJson(String serverUrl, String path) {
        if (debug) console.println("Sending GET request: " + path);
        Response response = getHandler.get(serverUrl, path, cookieManager);
        handleError(response);

        JSONObject obj = null;
        try {
            String infoResponseJson = response.getResponseAsString();
            if (debug) console.println("Received: " + infoResponseJson);
            return (JSONObject) JSONValue.parse(infoResponseJson);
        } catch (IOException e) {
            if (debug) console.printStacktrace(e);
            console.printlnError("Error: " + e.getMessage());
            console.exit(EXIT_STATUS_FAIL);
        } finally {
            response.disconnect();
        }

        return null;
    }

    private static JSONObject postJson(String serverUrl, String path, JSONObject request) {
        String requestString = JSONValue.toJSONString(request);

        if (debug) console.println("Sending: " + requestString);
        Response response = postHandler.post(serverUrl, path, new ByteArrayInputStream(requestString.getBytes()),
                requestString.length(), CONTENT_TYPE.JSON, null, null, cookieManager);
        handleError(response);

        JSONObject obj = null;
        try {
            String exportResult = response.getResponseAsString();
            if (debug) console.println("Received: " + exportResult);
            return (JSONObject) JSONValue.parse(exportResult);
        } catch (IOException e) {
            if (debug) console.printStacktrace(e);
            console.printlnError("Error: " + e.getMessage());
            console.exit(EXIT_STATUS_FAIL);
        } finally {
            response.disconnect();
        }

        return null;
    }

    private static JSONObject uploadFile(String serverUrl, String path, File file) {
        if (debug) console.println("Uploading file to: " + path);

        Response response = null;
        try {
            response = postHandler.post(serverUrl, path,
                    new FileInputStream(file), file.length(), CONTENT_TYPE.UPLOAD, file.getName(),
                    new ProgressCallbackConsoleImpl(console), cookieManager);
        } catch (IOException e) {
            if (debug) console.printStacktrace(e);
            console.printlnError("Error: File not found. " + file.getAbsolutePath());
            console.exit(EXIT_STATUS_FAIL);
            return null;
        }

        handleError(response);

        JSONObject obj = null;
        try {
            String exportResult = response.getResponseAsString();
            if (debug) console.println("Received: " + exportResult);
            return (JSONObject) JSONValue.parse(exportResult);
        } catch (IOException e) {
            if (debug) console.printStacktrace(e);
            console.printlnError("Error: " + e.getMessage());
            console.exit(EXIT_STATUS_FAIL);
        } finally {
            response.disconnect();
        }

        return null;
    }

}

