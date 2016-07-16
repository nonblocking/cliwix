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

@XmlRootElement(name = "Structure")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArticleStructure extends LiferayEntity implements GroupMember {

    @XmlTransient
    private Long structureDbId;

    @XmlTransient
    private Long ownerGroupId;

    @CompareEquals
    @XmlAttribute(name = "structureId", required = true)
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
    @XmlElement(name = "DynamicElements", required = true)
    @XmlJavaTypeAdapter(AdapterCDATA.class)
    private String dynamicElements;

    @XmlElement(name = "Permissions")
    private ResourcePermissions permissions;

    @XmlElementWrapper(name = "SubStructures")
    @XmlElement(name = "Structure")
    private List<ArticleStructure> subStructures;

    public ArticleStructure() {}

    public ArticleStructure(String structureId, List<LocalizedTextContent> names, String dynamicElements) {
        this.structureId = structureId;
        this.names = names;
        this.dynamicElements = dynamicElements;
    }

    @Override
    public Long getOwnerGroupId() {
        return this.ownerGroupId;
    }

    @Override
    public String identifiedBy() {
        return this.structureId;
    }

    @Override
    public Long getDbId() {
        return this.structureDbId;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.structureDbId = ((ArticleStructure) other).structureDbId;
        this.ownerGroupId = ((ArticleStructure) other).ownerGroupId;
    }

    public List<ArticleStructure> getSubStructures() {
        return subStructures;
    }

    public void setSubStructures(List<ArticleStructure> subStructures) {
        this.subStructures = subStructures;
    }

    public Long getStructureDbId() {
        return structureDbId;
    }

    public void setStructureDbId(Long structureDbId) {
        this.structureDbId = structureDbId;
    }

    public void setOwnerGroupId(Long ownerGroupId) {
        this.ownerGroupId = ownerGroupId;
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

    public ResourcePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(ResourcePermissions permissions) {
        this.permissions = permissions;
    }

    public String getDynamicElements() {
        return dynamicElements;
    }

    public void setDynamicElements(String dynamicElements) {
        this.dynamicElements = dynamicElements;
    }

    @Override
    public String toString() {
        return "ArticleStructure{" +
                "structureDbId=" + structureDbId +
                ",ownerGroupId=" + ownerGroupId +
                ",structureId='" + structureId + '\'' +
                ",names=" + names +
                ",descriptions=" + descriptions +
                ",dynamicElements='" + dynamicElements + '\'' +
                ",permissions=" + permissions +
                ",subStructures=" + subStructures +
                '}';
    }
}
