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
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CookieManagerImpl implements CookieManager {

    private Map<String, Map<String, Map<String, String>>> store;

    private static final String SET_COOKIE = "Set-Cookie";
    private static final String COOKIE_VALUE_DELIMITER = ";";
    private static final String PATH = "path";
    private static final String EXPIRES = "expires";
    private static final String DATE_FORMAT = "EEE, dd-MMM-yyyy hh:mm:ss z";
    private static final String SET_COOKIE_SEPARATOR = "; ";
    private static final String COOKIE = "Cookie";

    private static final char NAME_VALUE_SEPARATOR = '=';
    private static final char DOT = '.';

    private DateFormat dateFormat;

    public CookieManagerImpl() {
        store = new HashMap<>();
        dateFormat = new SimpleDateFormat(DATE_FORMAT);
    }

    @Override
    public void clear() {
        this.store.clear();
    }

    @Override
    public void storeCookies(URLConnection conn) throws IOException {

        String domain = getDomainFromHost(conn.getURL().getHost());
        Map<String, Map<String, String>> domainStore;

        if (store.containsKey(domain)) {
            domainStore = store.get(domain);
        } else {
            domainStore = new HashMap<>();
            this.store.put(domain, domainStore);
        }

        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase(SET_COOKIE)) {
                Map<String, String> cookie = new HashMap<>();
                StringTokenizer st = new StringTokenizer(conn.getHeaderField(i), COOKIE_VALUE_DELIMITER);

                if (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    String name = token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR));
                    String value = token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1, token.length());
                    domainStore.put(name, cookie);
                    cookie.put(name, value);
                }

                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (token.indexOf(NAME_VALUE_SEPARATOR) > 0) {
                        cookie.put(token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR)).toLowerCase(),
                                token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1, token.length()));
                    }
                }
            }
        }
    }

    @Override
    public void setCookies(URLConnection conn) throws IOException {

        URL url = conn.getURL();
        String domain = getDomainFromHost(url.getHost());
        String path = url.getPath();

        Map domainStore = (Map) store.get(domain);
        if (domainStore == null) return;
        StringBuilder cookieStringBuffer = new StringBuilder();

        Iterator cookieNames = domainStore.keySet().iterator();
        while (cookieNames.hasNext()) {
            String cookieName = (String) cookieNames.next();
            Map cookie = (Map) domainStore.get(cookieName);

            if (comparePaths((String) cookie.get(PATH), path) && isNotExpired((String) cookie.get(EXPIRES))) {
                cookieStringBuffer.append(cookieName);
                cookieStringBuffer.append("=");
                cookieStringBuffer.append((String) cookie.get(cookieName));
                if (cookieNames.hasNext()) cookieStringBuffer.append(SET_COOKIE_SEPARATOR);
            }
        }
        try {
            conn.setRequestProperty(COOKIE, cookieStringBuffer.toString());
        } catch (java.lang.IllegalStateException ise) {
            throw new IOException("Illegal State! Cookies cannot be set on a URLConnection that is already connected. "
                    + "Only call setCookies(java.net.URLConnection) AFTER calling java.net.URLConnection.connect().");
        }
    }

    private String getDomainFromHost(String host) {
        if (host.indexOf(DOT) != host.lastIndexOf(DOT)) {
            return host.substring(host.indexOf(DOT) + 1);
        } else {
            return host;
        }
    }

    private boolean isNotExpired(String cookieExpires) {
        if (cookieExpires == null) return true;
        Date now = new Date();
        try {
            return (now.compareTo(dateFormat.parse(cookieExpires))) <= 0;
        } catch (java.text.ParseException pe) {
            pe.printStackTrace();
            return false;
        }
    }

    private boolean comparePaths(String cookiePath, String targetPath) {
        return cookiePath == null || cookiePath.equals("/") || targetPath.regionMatches(0, cookiePath, 0, cookiePath.length());
    }

    @Override
    public String toString() {
        return store.toString();
    }

}
