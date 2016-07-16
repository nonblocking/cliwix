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

public class HttpDeleteHandlerDefaultImpl extends BaseHttpHandler implements HttpDeleteHandler {

    @Override
    public Response delete(String serverURL, String path, CookieManager cookieManager) {

        HttpURLConnection httpConn = null;
        int responseCode = 500;
        String responseMessage = null;

        try {
            URL url = new URL(serverURL + path);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("DELETE");
            httpConn.setDoInput(true);
            httpConn.setDoOutput(false);
            cookieManager.setCookies(httpConn);

            setDefaultHeaders(httpConn);
            setTimeouts(httpConn);

            responseCode = httpConn.getResponseCode();
            cookieManager.storeCookies(httpConn);

            return new Response(responseCode, null, null, "");

        } catch (IOException e) {
            if (CliwixCliClient.debug) CliwixCliClient.console.printStacktrace(e);
            responseMessage = e.getMessage();
            return new Response(responseCode, responseMessage, null, "");
        }
    }
}
