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
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpGetHandlerDefaultImpl extends BaseHttpHandler implements HttpGetHandler {

    @Override
    public Response get(String serverURL, String path, CookieManager cookieManager) {

        HttpURLConnection httpConn = null;
        long responseContentLength = -1;
        int responseCode = 500;
        String responseMessage;
        String contentType = null;

        try {
            URL url = new URL(serverURL + path);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setDoInput(true);
            httpConn.setDoOutput(false);
            cookieManager.setCookies(httpConn);

            setDefaultHeaders(httpConn);
            setTimeouts(httpConn);

            responseCode = httpConn.getResponseCode();
            responseMessage = httpConn.getResponseMessage();
            contentType = httpConn.getContentType();
            responseContentLength = httpConn.getContentLength();
            cookieManager.storeCookies(httpConn);

            return new Response(responseCode, responseMessage, contentType, responseContentLength, httpConn.getInputStream(), httpConn);

        } catch (IOException e) {
            if (CliwixCliClient.debug) CliwixCliClient.console.printStacktrace(e);
            responseMessage = e.getMessage();
            return new Response(responseCode, responseMessage, contentType, responseContentLength,  httpConn != null ? httpConn.getErrorStream() : null, httpConn);
        }
    }
}
