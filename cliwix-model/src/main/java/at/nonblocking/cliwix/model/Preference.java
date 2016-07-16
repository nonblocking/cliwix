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
import at.nonblocking.cliwix.model.compare.CompareEquals;
import at.nonblocking.cliwix.model.xml.AdapterCDATA;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "Preference")
@XmlAccessorType(XmlAccessType.FIELD)
public class Preference extends ComparableAndIdentifiableObject implements Serializable {

    @CompareEquals
    @XmlElement(name = "Name", required = true)
    private String name;

    @CompareEquals
    @XmlJavaTypeAdapter(AdapterCDATA.class)
    @XmlElement(name = "Value")
    private String value;

    @CompareEquals
    @XmlJavaTypeAdapter(AdapterCDATA.class)
    @XmlElementWrapper(name = "Values")
    @XmlElement(name = "Value")
    private List<String> values;

    public Preference() {}

    public Preference(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Preference(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public String identifiedBy() {
        return this.name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "Preference{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", values=" + values +
                '}';
    }
}
