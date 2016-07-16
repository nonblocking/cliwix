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
public enum SITE_MEMBERSHIP_TYPE {

    OPEN(1),
    RESTRICTED(2),
    PRIVATE(3),
    SYSTEM(4)

    ;

    private int type;

    SITE_MEMBERSHIP_TYPE(int type) {
        this.type = type;
    }

    public static SITE_MEMBERSHIP_TYPE fromType(int type) {
        for (SITE_MEMBERSHIP_TYPE membershipType : SITE_MEMBERSHIP_TYPE.values()) {
            if (membershipType.getType() == type) {
                return membershipType;
            }
        }

        return SYSTEM; //Fallback
    }

    public int getType() {
        return this.type;
    }
}
