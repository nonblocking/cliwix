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

@XmlTransient
public abstract class DocumentLibraryItem extends LiferayEntityWithUniquePathIdentifier implements GroupMember {

    @XmlTransient
    private String path;

    @XmlTransient
    private Long ownerGroupId;

    @CompareEquals
    @XmlAttribute(name = "name", required = true)
    private String name;

    @CompareEquals
    @XmlElement(name = "Description")
    private String description;

    @XmlElement(name = "Permissions")
    private ResourcePermissions permissions;

    protected DocumentLibraryItem() {
    }

    protected DocumentLibraryItem(String name) {
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourcePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(ResourcePermissions permissions) {
        this.permissions = permissions;
    }

    public abstract List<DocumentLibraryItem> getSubItems();

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public Long getOwnerGroupId() {
        return ownerGroupId;
    }

    public void setOwnerGroupId(Long ownerGroupId) {
        this.ownerGroupId = ownerGroupId;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.path = ((DocumentLibraryItem) other).path;
        this.ownerGroupId = ((DocumentLibraryItem) other).ownerGroupId;
    }

    @Override
    public String toString() {
        return "DocumentLibraryItem{" +
                "name='" + name + '\'' +
                ",path='" + path + '\'' +
                ",ownerGroupId='" + ownerGroupId + '\'' +
                ",description='" + description + '\'' +
                '}';
    }
}
