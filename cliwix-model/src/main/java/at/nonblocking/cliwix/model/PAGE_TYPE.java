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

package at.nonblocking.cliwix.model;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum PAGE_TYPE {

    PORTLET("portlet"),
    PANEL("panel"),
    EMBEDDED("embedded"),
    URL("url"),
    LINK_TO_PAGE("link_to_layout"),
    ARTICLE("article");

    private String type;

    PAGE_TYPE(String type) {
        this.type = type;
    }

    public static PAGE_TYPE fromType(String type) {
        for (PAGE_TYPE pageType : PAGE_TYPE.values()) {
            if (pageType.getType().equals(type)) {
                return pageType;
            }
        }

        return null;
    }

    public String getType() {
        return this.type;
    }



    public static final String TYPE_ARTICLE = "article";

    public static final String TYPE_CONTROL_PANEL = "control_panel";


}