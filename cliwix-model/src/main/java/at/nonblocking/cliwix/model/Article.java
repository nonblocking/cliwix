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
import java.util.Date;
import java.util.List;

@XmlTransient
public abstract class Article extends LiferayEntity implements Asset, GroupMember {

    private static final String TYPE_GENERAL = "general";

    @XmlTransient
    private Long articleDbId;

    @XmlTransient
    private Long resourcePrimKey;

    @XmlTransient
    private Long ownerGroupId;

    @CompareEquals
    @XmlAttribute(name = "articleId", required = true)
    private String articleId;

    @CompareEquals
    @XmlElement(name = "DefaultLocale", required = true)
    private String defaultLocale;

    @CompareEquals
    @XmlElement(name = "Type")
    private String type = TYPE_GENERAL;

    @CompareEquals
    @XmlElement(name = "DisplayDate")
    private Date displayDate;

    @CompareEquals
    @XmlElement(name = "ExpirationDate")
    private Date expirationDate;

    @CompareEquals
    @XmlElementWrapper(name = "Summaries")
    @XmlElement(name = "Summary")
    private List<LocalizedTextContent> summaries;

    @CompareEquals
    @XmlElementWrapper(name = "Titles")
    @XmlElement(name = "Title", required = true)
    private List<LocalizedTextContent> titles;

    @CompareEquals
    @XmlElementWrapper(name = "AssetTags")
    @XmlElement(name = "AssetTag")
    private List<String> assetTags;

    @XmlElement(name = "Permissions")
    private ResourcePermissions permissions;

    public Article() {
    }

    public Article(String articleId, String defaultLocale, List<LocalizedTextContent> titles) {
        this.articleId = articleId;
        this.defaultLocale = defaultLocale;
        this.titles = titles;
    }

    @Override
    public String identifiedBy() {
        return this.articleId;
    }

    @Override
    public Long getDbId() {
        return this.articleDbId;
    }

    public Long getArticleDbId() {
        return articleDbId;
    }

    public void setArticleDbId(Long articleDbId) {
        this.articleDbId = articleDbId;
    }

    public Long getResourcePrimKey() {
        return resourcePrimKey;
    }

    public void setResourcePrimKey(Long resourcePrimKey) {
        this.resourcePrimKey = resourcePrimKey;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Date getDisplayDate() {
        return displayDate;
    }

    public void setDisplayDate(Date displayDate) {
        this.displayDate = displayDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    public List<String> getAssetTags() {
        return assetTags;
    }

    public void setAssetTags(List<String> assetTags) {
        this.assetTags = assetTags;
    }

    public List<LocalizedTextContent> getSummaries() {
        return summaries;
    }

    public void setSummaries(List<LocalizedTextContent> summaries) {
        this.summaries = summaries;
    }

    public ResourcePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(ResourcePermissions permissions) {
        this.permissions = permissions;
    }

    public List<LocalizedTextContent> getTitles() {
        return titles;
    }

    public void setTitles(List<LocalizedTextContent> titles) {
        this.titles = titles;
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
        this.articleDbId = ((Article) other).articleDbId;
        this.resourcePrimKey = ((Article) other).resourcePrimKey;
        this.ownerGroupId = ((Article) other).ownerGroupId;
    }

    @Override
    public String toString() {
        return "Article{" +
                "articleDbId='" + articleDbId + '\'' +
                ",resourcePrimKey='" + resourcePrimKey + '\'' +
                ",articleId='" + articleId + '\'' +
                ",ownerGroupId='" + ownerGroupId + '\'' +
                ",defaultLocale'" + defaultLocale + '\'' +
                ",type='" + type + '\'' +
                ",displayDate='" + displayDate + '\'' +
                ",expirationDate='" + expirationDate + '\'' +
                ",summaries=" + summaries +
                ",titles=" + titles +
                ",assetTags=" + assetTags +
                ",permissions=" + permissions +
                '}';
    }
}
