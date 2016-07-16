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

package at.nonblocking.cliwix.model.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.regex.Pattern;

public class AdapterCDATA extends XmlAdapter<String, String> {

    public static final String CDATA_BEGIN = "<![CDATA[";
    public static final String CDATA_END = "]]>";

    private static final Pattern PATTERN_ILLEGAL_XML_CHARACTERS = Pattern.compile("[<>&]");

    @Override
    public String marshal(String text) throws Exception {
        if (PATTERN_ILLEGAL_XML_CHARACTERS.matcher(text).find()) {
            String escapedText = escapeNestedCDATA(text);
            return CDATA_BEGIN + escapedText + CDATA_END;
        } else {
            return text;
        }
    }
    @Override
    public String unmarshal(String xml) throws Exception {
        return removeNestedCDATA(xml);
    }

    private String escapeNestedCDATA(String xml) {
        return xml.replace("]]>", "]]]]><![CDATA[>");
    }

    private String removeNestedCDATA(String xml) {
        return xml.replace("]]]]><![CDATA[>", "]]> ");
    }

}
