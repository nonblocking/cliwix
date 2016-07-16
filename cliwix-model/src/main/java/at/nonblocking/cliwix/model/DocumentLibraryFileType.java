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

/**
 * Internal, not part of the XML
 */
public class DocumentLibraryFileType extends LiferayEntity implements GroupMember {

    private Long fileEntryTypeId;

    @CompareEquals
    private String fileEntryTypeKey;

    private Long ownerGroupId;

    public DocumentLibraryFileType(Long fileEntryTypeId, String fileEntryTypeKey, Long ownerGroupId) {
        this.fileEntryTypeId = fileEntryTypeId;
        this.fileEntryTypeKey = fileEntryTypeKey;
        this.ownerGroupId = ownerGroupId;
    }

    public Long getFileEntryTypeId() {
        return fileEntryTypeId;
    }

    public void setFileEntryTypeId(Long fileEntryTypeId) {
        this.fileEntryTypeId = fileEntryTypeId;
    }

    public String getFileEntryTypeKey() {
        return fileEntryTypeKey;
    }

    public void setFileEntryTypeKey(String fileEntryTypeKey) {
        this.fileEntryTypeKey = fileEntryTypeKey;
    }

    @Override
    public String identifiedBy() {
        return this.fileEntryTypeKey;
    }

    @Override
    public Long getOwnerGroupId() {
        return this.ownerGroupId;
    }

    @Override
    public Long getDbId() {
        return this.fileEntryTypeId;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.fileEntryTypeId = ((DocumentLibraryFileType) other).fileEntryTypeId;
    }

    @Override
    public String toString() {
        return "DocumentLibraryFileType{" +
                "fileEntryTypeId=" + fileEntryTypeId +
                ",fileEntryTypeKey='" + fileEntryTypeKey + '\'' +
                ",ownerGroupId=" + ownerGroupId +
                '}';
    }
}
