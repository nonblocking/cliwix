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

import at.nonblocking.cliwix.model.compare.CompareEquals;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "StaticArticle")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType (propOrder={ "articleId", "defaultLocale", "type", "displayDate", "expirationDate",
        "summaries", "titles", "contents", "assetTags", "permissions" })
public class StaticArticle extends Article {

    @CompareEquals
    @XmlElementWrapper(name = "Contents")
    @XmlElement(name = "Content", required = true)
    private List<LocalizedXmlContent> contents;

    public List<LocalizedXmlContent> getContents() {
        return contents;
    }

    public void setContents(List<LocalizedXmlContent> contents) {
        this.contents = contents;
    }

    public StaticArticle() {}

    public StaticArticle(String articleId, String defaultLocale, List<LocalizedTextContent> titles, List<LocalizedXmlContent> contents) {
        super(articleId, defaultLocale, titles);
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "StaticArticle{" +
                "contents=" + contents +
                "} " + super.toString();
    }
}
