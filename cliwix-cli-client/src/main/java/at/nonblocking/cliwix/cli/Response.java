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

import java.io.*;
import java.net.HttpURLConnection;

public class Response {

    private int statusCode;
    private String statusMessage;
    private long contentLength;
    private String contentType;
    private InputStream responseStream;
    private HttpURLConnection connection;

    public Response(int statusCode, String statusMessage, String contentType, String responseString) {
        this(statusCode, statusMessage, contentType, responseString.length(), new ByteArrayInputStream(responseString.getBytes()), null);
    }

    public Response(int statusCode, String statusMessage, String contentType, long contentLength, InputStream responseStream, HttpURLConnection connection) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.responseStream = responseStream;
        this.connection = connection;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public InputStream getResponseStream() {
        return responseStream;
    }

    public String getResponseAsString() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = this.responseStream.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }

        return baos.toString("UTF-8");
    }

    public void disconnect() {
        if (this.responseStream != null) {
            try {
                this.responseStream.close();
            } catch (IOException e) {
                if (CliwixCliClient.debug) CliwixCliClient.console.printStacktrace(e);
            }
        }
        if (this.connection != null) {
            this.connection.disconnect();
        }
    }
}
