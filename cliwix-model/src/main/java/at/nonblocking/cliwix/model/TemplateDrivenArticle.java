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
import at.nonblocking.cliwix.model.xml.AdapterCDATA;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;

@XmlRootElement(name = "TemplateDrivenArticle")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType (propOrder={ "articleId", "defaultLocale", "type", "displayDate", "expirationDate",
        "summaries", "titles", "structureId", "templateId", "dynamicElements", "assetTags", "permissions" })
public class TemplateDrivenArticle extends Article {

    @CompareEquals
    @XmlElement(name = "StructureId", required = true)
    private String structureId;

    @CompareEquals
    @XmlElement(name = "TemplateId", required = true)
    private String templateId;

    @CompareEquals
    @XmlJavaTypeAdapter(AdapterCDATA.class)
    @XmlElement(name = "DynamicElements", required = true)
    private String dynamicElements;

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getDynamicElements() {
        return dynamicElements;
    }

    public void setDynamicElements(String dynamicElements) {
        this.dynamicElements = dynamicElements;
    }

    public TemplateDrivenArticle() {}

    public TemplateDrivenArticle(String articleId, String defaultLocale, List<LocalizedTextContent> titles, String structureId, String templateId, String dynamicElements) {
        super(articleId, defaultLocale, titles);
        this.structureId = structureId;
        this.templateId = templateId;
        this.dynamicElements = dynamicElements;
    }

    @Override
    public String toString() {
        return "TemplateDrivenArticle{" +
                "structureId='" + structureId + '\'' +
                ",templateId='" + templateId + '\'' +
                ",dynamicElements='" + dynamicElements + '\'' +
                "} " + super.toString();
    }
}
