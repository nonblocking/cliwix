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

@XmlAccessorType(XmlAccessType.FIELD)
public class PageSet extends LiferayEntity implements GroupMember {

    @XmlTransient
    private Long pageSetId;

    @XmlTransient
    private Long ownerGroupId;

    @XmlTransient
    private Boolean privatePageSet;

    @CompareEquals
    @XmlElement(name = "DefaultThemeId")
    private String defaultThemeId;

    @CompareEquals
    @XmlElement(name = "DefaultColorSchemeId")
    private String defaultColorSchemeId;

    @CompareEquals
    @XmlElement(name = "CSS")
    private String css;

    @XmlElement(name = "Pages")
    private Pages pages;

    public PageSet() {
    }

    @Deprecated
    public PageSet(Pages pages) {
        this.pages = pages;
    }

    @Override
    public String identifiedBy() {
        return null;
    }

    @Override
    public Long getDbId() {
        return this.pageSetId;
    }

    public Long getPageSetId() {
        return pageSetId;
    }

    public void setPageSetId(Long pageSetId) {
        this.pageSetId = pageSetId;
    }

    public Boolean getPrivatePageSet() {
        return privatePageSet;
    }

    public void setPrivatePageSet(Boolean privatePageSet) {
        this.privatePageSet = privatePageSet;
    }

    public String getDefaultThemeId() {
        return defaultThemeId;
    }

    public void setDefaultThemeId(String defaultThemeId) {
        this.defaultThemeId = defaultThemeId;
    }

    public String getDefaultColorSchemeId() {
        return defaultColorSchemeId;
    }

    public void setDefaultColorSchemeId(String defaultColorSchemeId) {
        this.defaultColorSchemeId = defaultColorSchemeId;
    }

    public String getCss() {

        return css;
    }

    public void setCss(String css) {
        this.css = css;
    }

    public void setOwnerGroupId(Long ownerGroupId) {
        this.ownerGroupId = ownerGroupId;
    }

    public Pages getPages() {
        return pages;
    }

    public void setPages(Pages pages) {
        this.pages = pages;
    }

    @Override
    public Long getOwnerGroupId() {
        return ownerGroupId;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.pageSetId = ((PageSet) other).pageSetId;
        this.privatePageSet = ((PageSet) other).privatePageSet;
        this.ownerGroupId = ((PageSet) other).ownerGroupId;
    }

    @Override
    public String toString() {
        return "PageSet{" +
                "pageSetId=" + pageSetId +
                ", ownerGroupId=" + ownerGroupId +
                ", privatePageSet=" + privatePageSet +
                ", defaultThemeId='" + defaultThemeId + '\'' +
                ", defaultColorSchemeId='" + defaultColorSchemeId + '\'' +
                ", css='" + css + '\'' +
                "} " + super.toString();
    }
}
