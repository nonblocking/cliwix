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
import at.nonblocking.cliwix.model.compare.CompareEqualsIfNotNull;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "Site")
@XmlAccessorType(XmlAccessType.FIELD)
public class Site extends LiferayEntity implements CompanyMember, Group {

    public static final String GUEST_SITE_NAME = "Guest";

    @XmlTransient
    private Long siteId;

    @XmlTransient
    private Long ownerCompanyId;

    @CompareEquals
    @XmlAttribute(name = "name", required = true)
    private String name;

    @CompareEqualsIfNotNull
    @XmlElement(name = "SiteConfiguration")
    private SiteConfiguration siteConfiguration;

    @CompareEqualsIfNotNull
    @XmlElement(name = "SiteMembers")
    private SiteMembers siteMembers;

    @XmlElement(name = "SiteRoleAssignments")
    private SiteRoleAssignments siteRoleAssignments;

    @XmlElement(name = "SiteContent")
    private SiteContent siteContent;

    @XmlElement(name = "PublicPages")
    private PageSet publicPages;

    @XmlElement(name = "PrivatePages")
    private PageSet privatePages;

    public Site() {
    }

    public Site(String name, SiteConfiguration siteConfiguration, SiteMembers siteMembers) {
        this.name = name;
        this.siteConfiguration = siteConfiguration;
        this.siteMembers = siteMembers;
    }

    @Override
    public String identifiedBy() {
        return this.name;
    }

    @Override
    public Long getDbId() {
        return this.siteId;
    }

    @Override
    public Long getGroupId() {
        return this.siteId;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PageSet getPublicPages() {
        return publicPages;
    }

    public void setPublicPages(PageSet publicPages) {
        this.publicPages = publicPages;
    }

    public PageSet getPrivatePages() {
        return privatePages;
    }

    public void setPrivatePages(PageSet privatePages) {
        this.privatePages = privatePages;
    }

    public SiteContent getSiteContent() {
        return siteContent;
    }

    public void setSiteContent(SiteContent siteContent) {
        this.siteContent = siteContent;
    }

    @Override
    public Long getOwnerCompanyId() {
        return ownerCompanyId;
    }

    public void setOwnerCompanyId(Long ownerCompanyId) {
        this.ownerCompanyId = ownerCompanyId;
    }

    public SiteConfiguration getSiteConfiguration() {
        return siteConfiguration;
    }

    public SiteMembers getSiteMembers() {
        return siteMembers;
    }

    public void setSiteMembers(SiteMembers siteMembers) {
        this.siteMembers = siteMembers;
    }

    public void setSiteConfiguration(SiteConfiguration siteConfiguration) {
        this.siteConfiguration = siteConfiguration;
    }

    public SiteRoleAssignments getSiteRoleAssignments() {
        return siteRoleAssignments;
    }

    public void setSiteRoleAssignments(SiteRoleAssignments siteRoleAssignments) {
        this.siteRoleAssignments = siteRoleAssignments;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.siteId = ((Site) other).siteId;
        this.ownerCompanyId = ((Site) other).ownerCompanyId;
    }

    @Override
    public String toString() {
        return "Site{" +
                "siteId=" + siteId +
                ", ownerCompanyId=" + ownerCompanyId +
                ", name='" + name + '\'' +
                ", siteConfiguration=" + siteConfiguration +
                ", siteMembers=" + siteMembers +
                '}';
    }
}
