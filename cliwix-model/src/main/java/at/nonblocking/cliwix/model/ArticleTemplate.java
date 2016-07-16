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

@XmlRootElement(name = "Template")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArticleTemplate extends LiferayEntity implements GroupMember {

    @XmlTransient
    private Long templateDbId;

    @XmlTransient
    private Long ownerGroupId;

    @CompareEquals
    @XmlAttribute(name = "templateId", required = true)
    private String templateId;

    @CompareEquals
    @XmlElement(name = "StructureId")
    private String structureId;

    @CompareEquals
    @XmlElementWrapper(name = "Names")
    @XmlElement(name = "Name", required = true)
    private List<LocalizedTextContent> names;

    @CompareEquals
    @XmlElementWrapper(name = "Descriptions")
    @XmlElement(name = "Description")
    private List<LocalizedTextContent> descriptions;

    @CompareEquals
    @XmlElement(name = "Language", required = true)
    private String language;

    @CompareEquals
    @XmlElement(name = "Script", required = true)
    @XmlJavaTypeAdapter(AdapterCDATA.class)
    private String script;

    @XmlElement(name = "Permissions")
    private ResourcePermissions permissions;

    public ArticleTemplate() {
    }

    public ArticleTemplate(String templateId, List<LocalizedTextContent> names, String language, String script) {
        this.templateId = templateId;
        this.names = names;
        this.language = language;
        this.script = script;
    }

    @Override
    public Long getOwnerGroupId() {
        return this.ownerGroupId;
    }

    @Override
    public String identifiedBy() {
        return this.templateId;
    }

    @Override
    public Long getDbId() {
        return this.templateDbId;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.templateDbId = ((ArticleTemplate) other).templateDbId;
        this.ownerGroupId = ((ArticleTemplate) other).ownerGroupId;
    }

    public Long getTemplateDbId() {
        return templateDbId;
    }

    public void setTemplateDbId(Long templateDbId) {
        this.templateDbId = templateDbId;
    }

    public void setOwnerGroupId(Long ownerGroupId) {
        this.ownerGroupId = ownerGroupId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getStructureId() {
        return structureId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    public List<LocalizedTextContent> getNames() {
        return names;
    }

    public void setNames(List<LocalizedTextContent> names) {
        this.names = names;
    }

    public List<LocalizedTextContent> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<LocalizedTextContent> descriptions) {
        this.descriptions = descriptions;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public ResourcePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(ResourcePermissions permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "ArticleTemplate{" +
                "templateDbId=" + templateDbId +
                ",ownerGroupId=" + ownerGroupId +
                ",templateId='" + templateId + '\'' +
                ",structureId='" + structureId + '\'' +
                ",names=" + names +
                ",descriptions=" + descriptions +
                ",language='" + language + '\'' +
                ",script='" + script + '\'' +
                ",permissions=" + permissions +
                '}';
    }
}
