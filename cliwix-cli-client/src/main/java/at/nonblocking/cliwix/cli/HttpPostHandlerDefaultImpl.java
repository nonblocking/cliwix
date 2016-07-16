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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpPostHandlerDefaultImpl extends BaseHttpHandler implements HttpPostHandler {

    final static String MULTIPART_BOUNDARY = "*****";
    final static String CRLF = "\r\n";
    final static String TWO_HYPHENS = "--";

    @Override
    public Response post(String serverURL, String path, InputStream request,
                         long contentLength, CONTENT_TYPE contentType, String fileName,
                         ProgressCallback progressCallback, CookieManager cookieManager) {

        HttpURLConnection httpConn = null;
        long responseContentLength = -1L;
        int responseCode = 500;
        String responseMessage;
        String responseContentType = null;

        try  {
            URL url = new URL(serverURL + path);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            cookieManager.setCookies(httpConn);

            setDefaultHeaders(httpConn);
            setTimeouts(httpConn);

            if (contentType == CONTENT_TYPE.UPLOAD) {
                httpConn.setChunkedStreamingMode(CHUNK_SIZE);

                httpConn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + MULTIPART_BOUNDARY);
                httpConn.getOutputStream().write((TWO_HYPHENS + MULTIPART_BOUNDARY + CRLF).getBytes());
                httpConn.getOutputStream().write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + CRLF).getBytes());
                httpConn.getOutputStream().write(CRLF.getBytes());
                writeRequest(httpConn, request, contentLength, progressCallback);
                httpConn.getOutputStream().write(CRLF.getBytes());
                httpConn.getOutputStream().write((TWO_HYPHENS + MULTIPART_BOUNDARY + TWO_HYPHENS + CRLF).getBytes());
                httpConn.getOutputStream().close();

            } else {
                httpConn.setFixedLengthStreamingMode(contentLength);

                httpConn.setRequestProperty("Content-Type", "application/json");
                writeRequest(httpConn, request, contentLength, progressCallback);
                httpConn.getOutputStream().close();
            }

            responseCode = httpConn.getResponseCode();
            responseMessage = httpConn.getResponseMessage();
            responseContentType = httpConn.getContentType();
            responseContentLength = httpConn.getContentLength();
            cookieManager.storeCookies(httpConn);

            return new Response(responseCode, responseMessage, responseContentType, responseContentLength, httpConn.getInputStream(), httpConn);

        } catch (IOException e) {
            if (CliwixCliClient.debug) CliwixCliClient.console.printStacktrace(e);
            responseMessage = e.getMessage();
            return new Response(responseCode, responseMessage, responseContentType, responseContentLength, httpConn != null ? httpConn.getErrorStream() : null, httpConn);
        }
    }
}
