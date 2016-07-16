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

@XmlRootElement(name = "Page")
@XmlAccessorType(XmlAccessType.FIELD)
public class Page extends LiferayEntityWithUniquePathIdentifier implements GroupMember {

    @XmlTransient
    private Long pageId;

    @XmlTransient
    private Long ownerGroupId;

    @XmlTransient
    private Long portletLayoutId;

    @XmlTransient
    private String path;

    @CompareEquals
    @XmlTransient
    private Integer pageOrder;

    @CompareEquals
    @XmlAttribute(name = "url", required = true)
    private String friendlyUrl;

    @CompareEquals
    @XmlElement(name = "PageType", required = true)
    private PAGE_TYPE pageType;

    @CompareEquals
    @XmlElementWrapper(name = "Names")
    @XmlElement(name = "Name", required = true)
    private List<LocalizedTextContent> names;

    @CompareEquals
    @XmlElementWrapper(name = "Titles")
    @XmlElement(name = "Title")
    private List<LocalizedTextContent> htmlTitles;

    @CompareEquals
    @XmlElementWrapper(name = "Descriptions")
    @XmlElement(name = "Description")
    private List<LocalizedTextContent> descriptions;

    @CompareEquals
    @XmlElementWrapper(name = "KeywordsList")
    @XmlElement(name = "Keywords")
    private List<LocalizedTextContent> keywordsList;

    @CompareEquals
    @XmlElementWrapper(name = "RobotsList")
    @XmlElement(name = "Robots")
    private List<LocalizedTextContent> robotsList;

    @CompareEquals
    @XmlElement(name = "Hidden")
    private Boolean hidden = Boolean.FALSE;

    @CompareEquals
    @XmlElement(name = "ThemeId")
    private String themeId;

    @CompareEquals
    @XmlElement(name = "ColorSchemeId")
    private String colorSchemeId;

    @CompareEquals
    @XmlElement(name = "CSS")
    private String css;

    @CompareEquals
    @XmlElementWrapper(name = "PageSettings")
    @XmlElement(name = "PageSetting")
    private List<PageSetting> pageSettings;

    @XmlElement(name = "Permissions")
    private ResourcePermissions permissions;

    @XmlElement(name = "PortletConfigurations")
    private PortletConfigurations portletConfigurations;

    @XmlElementWrapper(name = "SubPages")
    @XmlElement(name = "Page")
    private List<Page> subPages;

    public Page() {
    }

    public Page(PAGE_TYPE pageType, String friendlyUrl, List<LocalizedTextContent> names) {
        this.pageType = pageType;
        this.friendlyUrl = friendlyUrl;
        this.names = names;
    }

    @Override
    public String identifiedBy() {
        return this.friendlyUrl;
    }

    @Override
    public Long getDbId() {
        return this.portletLayoutId;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public Integer getPageOrder() {
        return pageOrder;
    }

    public void setPageOrder(Integer pageOrder) {
        this.pageOrder = pageOrder;
    }

    public Long getPortletLayoutId() {
        return portletLayoutId;
    }

    public void setPortletLayoutId(Long portletLayoutId) {
        this.portletLayoutId = portletLayoutId;
    }

    public String getFriendlyUrl() {
        return friendlyUrl;
    }

    public void setFriendlyUrl(String friendlyUrl) {
        this.friendlyUrl = friendlyUrl;
    }

    public PAGE_TYPE getPageType() {
        return pageType;
    }

    public void setPageType(PAGE_TYPE pageType) {
        this.pageType = pageType;
    }

    public List<LocalizedTextContent> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<LocalizedTextContent> descriptions) {
        this.descriptions = descriptions;
    }

    public List<LocalizedTextContent> getKeywordsList() {
        return keywordsList;
    }

    public void setKeywordsList(List<LocalizedTextContent> keywordsList) {
        this.keywordsList = keywordsList;
    }

    public List<LocalizedTextContent> getRobotsList() {
        return robotsList;
    }

    public void setRobotsList(List<LocalizedTextContent> robotsList) {
        this.robotsList = robotsList;
    }

    public List<LocalizedTextContent> getNames() {
        return names;
    }

    public void setNames(List<LocalizedTextContent> names) {
        this.names = names;
    }

    public List<PageSetting> getPageSettings() {
        return pageSettings;
    }

    public void setPageSettings(List<PageSetting> pageSettings) {
        this.pageSettings = pageSettings;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public ResourcePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(ResourcePermissions permissions) {
        this.permissions = permissions;
    }

    public void setSubPages(List<Page> subPages) {
        this.subPages = subPages;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getColorSchemeId() {
        return colorSchemeId;
    }

    public void setColorSchemeId(String colorSchemeId) {
        this.colorSchemeId = colorSchemeId;
    }

    public String getCss() {
        return css;
    }

    public void setCss(String css) {
        this.css = css;
    }

    public List<LocalizedTextContent> getHtmlTitles() {
        return htmlTitles;
    }

    public void setHtmlTitles(List<LocalizedTextContent> htmlTitles) {
        this.htmlTitles = htmlTitles;
    }

    public PortletConfigurations getPortletConfigurations() {
        return portletConfigurations;
    }

    public void setPortletConfigurations(PortletConfigurations portletConfigurations) {
        this.portletConfigurations = portletConfigurations;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    public List<Page> getSubPages() {
        return this.subPages;
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
        this.pageId = ((Page) other).pageId;
        this.portletLayoutId = ((Page) other).portletLayoutId;
        this.path = ((Page) other).path;
        this.ownerGroupId = ((Page) other).ownerGroupId;
    }

    @Override
    public String toString() {
        return "Page{" +
                "pageId=" + pageId +
                ", portletLayoutId=" + portletLayoutId +
                ", path='" + path + '\'' +
                ", pageOrder=" + pageOrder +
                ", friendlyUrl='" + friendlyUrl + '\'' +
                ", pageType=" + pageType +
                ", names=" + names +
                ", htmlTitles=" + htmlTitles +
                ", descriptions=" + descriptions +
                ", keywordsList=" + keywordsList +
                ", robotsList=" + robotsList +
                ", hidden=" + hidden +
                ", themeId='" + themeId + '\'' +
                ", colorSchemeId='" + colorSchemeId + '\'' +
                ", css='" + css + '\'' +
                ", pageSettings=" + pageSettings +
                "} " + super.toString();
    }
}
