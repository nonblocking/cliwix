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

import at.nonblocking.cliwix.model.compare.ComparableAndIdentifiableObject;
import at.nonblocking.cliwix.model.compare.ComparableObject;
import at.nonblocking.cliwix.model.compare.CompareEquals;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "MemberUserGroup")
@XmlAccessorType(XmlAccessType.FIELD)
public class MemberUserGroup extends ComparableAndIdentifiableObject {

    @CompareEquals
    @XmlAttribute(name = "name", required = true)
    private String name;

    public MemberUserGroup() {
    }

    public MemberUserGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String identifiedBy() {
        return this.name;
    }

    @Override
    public String toString() {
        return "MemberUserGroup{" +
                "name='" + name + '\'' +
                '}';
    }
}
