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
import java.util.List;

@XmlRootElement(name = "DocumentLibrary")
@XmlAccessorType(XmlAccessType.FIELD)
public class DocumentLibrary extends TreeType<DocumentLibraryItem> {

    @XmlAttribute(name = "fileDataFolder", required = true)
    private String fileDataFolder;

    @XmlAttribute(name = "mergeFromFileSystem")
    private Boolean mergeFromFileSystem = Boolean.FALSE;

    @XmlElement(name = "Permissions")
    private ResourcePermissions permissions;

    @XmlElementWrapper(name = "Entries")
    @XmlElements({
        @XmlElement(name = "File", type = DocumentLibraryFile.class),
        @XmlElement(name = "Folder", type = DocumentLibraryFolder.class)
    })
    private List<DocumentLibraryItem> rootItems;

    public DocumentLibrary() {
    }

    public DocumentLibrary(String fileDataFolder, List<DocumentLibraryItem> rootItems) {
        this.fileDataFolder = fileDataFolder;
        this.rootItems = rootItems;
    }

    public String getFileDataFolder() {
        return fileDataFolder;
    }

    public void setFileDataFolder(String fileDataFolder) {
        this.fileDataFolder = fileDataFolder;
    }

    public Boolean getMergeFromFileSystem() {
        return mergeFromFileSystem;
    }

    public void setMergeFromFileSystem(Boolean mergeFromFileSystem) {
        this.mergeFromFileSystem = mergeFromFileSystem;
    }

    public void setRootItems(List<DocumentLibraryItem> rootItems) {
        this.rootItems = rootItems;
    }

    @Override
    public List<DocumentLibraryItem> getRootItems() {
        return rootItems;
    }

    @Override
    public List<DocumentLibraryItem> getSubItems(DocumentLibraryItem item) {
        return item.getSubItems();
    }

    public ResourcePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(ResourcePermissions permissions) {
        this.permissions = permissions;
    }
}
