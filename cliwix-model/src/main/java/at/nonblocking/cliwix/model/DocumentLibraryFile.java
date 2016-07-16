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
import at.nonblocking.cliwix.model.compare.CompareLesserEquals;

import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.List;

@XmlRootElement(name = "DocumentLibraryFile")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType (propOrder={ "fileDataName", "description", "permissions", "assetTags" })
public class DocumentLibraryFile extends DocumentLibraryItem implements Asset {

    @XmlTransient
    private Long fileId;

    @XmlTransient
    private URI fileDataUri;

    @CompareLesserEquals
    @XmlTransient
    private long fileDataUpdateTimestamp;

    @XmlElement(name = "FileDataName")
    private String fileDataName;

    @CompareEquals
    @XmlElementWrapper(name = "AssetTags")
    @XmlElement(name = "AssetTag")
    private List<String> assetTags;

    public DocumentLibraryFile() {
    }

    public DocumentLibraryFile(String name, String fileDataName) {
        super(name);
        this.fileDataName = fileDataName;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileDataName() {
        return fileDataName;
    }

    public void setFileDataName(String fileDataName) {
        this.fileDataName = fileDataName;
    }

    public URI getFileDataUri() {
        return fileDataUri;
    }

    public void setFileDataUri(URI fileDataUri) {
        this.fileDataUri = fileDataUri;
    }

    public long getFileDataUpdateTimestamp() {
        return fileDataUpdateTimestamp;
    }

    public void setFileDataUpdateTimestamp(long fileDataUpdateTimestamp) {
        this.fileDataUpdateTimestamp = fileDataUpdateTimestamp;
    }

    @Override
    public List<String> getAssetTags() {
        return assetTags;
    }

    public void setAssetTags(List<String> assetTags) {
        this.assetTags = assetTags;
    }

    @Override
    public List<DocumentLibraryItem> getSubItems() {
        return null;
    }

    @Override
    public Long getDbId() {
        return this.fileId;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        super.copyIds(other);
        this.fileId = ((DocumentLibraryFile) other).fileId;
    }

    @Override
    public String toString() {
        return "DocumentLibraryFile{" +
                "fileId=" + fileId +
                ",fileDataName=" + fileDataName +
                ",fileDataUri=" + fileDataUri +
                ",fileDataUpdateTimestamp=" + fileDataUpdateTimestamp +
                ",assetTags=" + assetTags +
                "} " + super.toString();
    }

}
