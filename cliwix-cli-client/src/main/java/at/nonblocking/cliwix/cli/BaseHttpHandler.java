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

abstract class BaseHttpHandler {

    static final int CHUNK_SIZE = 100 * 1024;
    static final int CONNECT_TIMEOUT = 5 * 1000;
    static final int READ_TIMEOUT = 5 * 60 * 1000;

    void setTimeouts(HttpURLConnection httpConn) {
        httpConn.setConnectTimeout(CONNECT_TIMEOUT);
        httpConn.setReadTimeout(READ_TIMEOUT);
    }

    void setDefaultHeaders(HttpURLConnection httpConn) {
        httpConn.setRequestProperty("CliwixClient", "CLI Client");
    }

    void writeRequest(HttpURLConnection httpConn, InputStream inputStream, long contentLength, ProgressCallback progressCallback) throws IOException {
        byte[] buffer = new byte[CHUNK_SIZE];
        int len;
        long uploaded = 0;

        while ((len = inputStream.read(buffer)) > 0) {
            httpConn.getOutputStream().write(buffer, 0, len);
            uploaded += len;
            if (progressCallback != null) progressCallback.bytesTransferred(uploaded, contentLength);
        }
    }
}
