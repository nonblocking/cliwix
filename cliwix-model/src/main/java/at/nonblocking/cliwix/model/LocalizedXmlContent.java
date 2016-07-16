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

@XmlRootElement(name = "LocalizedXmlContent")
@XmlAccessorType(XmlAccessType.FIELD)
public class LocalizedXmlContent extends ComparableAndIdentifiableObject implements Serializable {

    @CompareEquals
    @XmlAttribute(name = "locale", required = true)
    private String locale;

    @CompareEquals
    @XmlJavaTypeAdapter(AdapterCDATA.class)
    @XmlValue
    private String xml;

    public LocalizedXmlContent() {
    }

    public LocalizedXmlContent(String locale, String xml) {
        this.locale = locale;
        this.xml = xml;
    }

    @Override
    public String identifiedBy() {
        return this.locale;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    @Override
    public String toString() {
        return "LocalizedXmlContent{" +
                "locale='" + locale + '\'' +
                ",xml='" + xml + '\'' +
                '}';
    }
}
