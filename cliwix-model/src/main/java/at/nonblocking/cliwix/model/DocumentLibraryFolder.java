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

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "DocumentLibraryFolder")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType (propOrder={ "description", "permissions", "subItems" })
public class DocumentLibraryFolder extends DocumentLibraryItem {

    @XmlTransient
    private Long folderId;

    @XmlElementWrapper(name = "SubEntries")
    @XmlElements({
            @XmlElement(name = "File", type = DocumentLibraryFile.class),
            @XmlElement(name = "Folder", type = DocumentLibraryFolder.class)
    })
    private List<DocumentLibraryItem> subItems;

    public DocumentLibraryFolder() {
    }

    public DocumentLibraryFolder(String name) {
        super(name);
    }

    @Override
    public List<DocumentLibraryItem> getSubItems() {
        return subItems;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public void setSubItems(List<DocumentLibraryItem> subItems) {
        this.subItems = subItems;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        super.copyIds(other);
        this.folderId = ((DocumentLibraryFolder) other).folderId;
    }

    @Override
    public Long getDbId() {
        return this.folderId;
    }

    @Override
    public String toString() {
        return "DocumentLibraryFolder{" +
                "folderId=" + folderId +
                "} " + super.toString();
    }

}

