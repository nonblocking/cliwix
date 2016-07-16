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

@XmlRootElement(name = "Company")
@XmlAccessorType(XmlAccessType.FIELD)
public class Company extends LiferayEntity implements Group, CompanyMember {

    @XmlTransient
    private Long companyId;

    @XmlTransient
    private Long companyGroupId;

    @CompareEquals
    @XmlAttribute(name = "webID", required = true)
    private String webId;

    @CompareEqualsIfNotNull
    @XmlElement(name = "CompanyConfiguration")
    private CompanyConfiguration companyConfiguration;

    @XmlElement(name = "PortalPreferences")
    private PortalPreferences portalPreferences;

    @XmlElement(name = "Users")
    private Users users;

    @XmlElement(name = "UserGroups")
    private UserGroups userGroups;

    @XmlElement(name = "Roles")
    private Roles roles;

    @XmlElement(name = "Organizations")
    private Organizations organizations;

    @XmlElement(name = "RegularRoleAssignments")
    private RegularRoleAssignments regularRoleAssignments;

    @XmlElement(name = "Sites")
    private Sites sites;

    public Company() {
    }

    public Company(String webId, CompanyConfiguration companyConfiguration) {
        this.webId = webId;
        this.companyConfiguration = companyConfiguration;
    }

    @Override
    public String identifiedBy() {
        return this.webId;
    }

    @Override
    public Long getDbId() {
        return this.companyId;
    }

    @Override
    public Long getGroupId() {
        return this.companyGroupId;
    }

    @Override
    public Long getOwnerCompanyId() {
        return this.companyId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getWebId() {
        return webId;
    }

    public void setWebId(String webId) {
        this.webId = webId;
    }

    public CompanyConfiguration getCompanyConfiguration() {
        return companyConfiguration;
    }

    public void setCompanyConfiguration(CompanyConfiguration companyConfiguration) {
        this.companyConfiguration = companyConfiguration;
    }

    public Roles getRoles() {
        return roles;
    }

    public void setRoles(Roles roles) {
        this.roles = roles;
    }

    public UserGroups getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(UserGroups userGroups) {
        this.userGroups = userGroups;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public Organizations getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Organizations organizations) {
        this.organizations = organizations;
    }

    public PortalPreferences getPortalPreferences() {
        return portalPreferences;
    }

    public void setPortalPreferences(PortalPreferences portalPreferences) {
        this.portalPreferences = portalPreferences;
    }

    public Sites getSites() {
        return sites;
    }

    public void setSites(Sites sites) {
        this.sites = sites;
    }

    public RegularRoleAssignments getRegularRoleAssignments() {
        return regularRoleAssignments;
    }

    public void setRegularRoleAssignments(RegularRoleAssignments regularRoleAssignments) {
        this.regularRoleAssignments = regularRoleAssignments;
    }

    public Long getCompanyGroupId() {
        return companyGroupId;
    }

    public void setCompanyGroupId(Long companyGroupId) {
        this.companyGroupId = companyGroupId;
    }
    
    @Override
    public void copyIds(LiferayEntity other) {
        this.companyId = ((Company) other).companyId;
        this.companyGroupId = ((Company) other).companyGroupId;
    }

    @Override
    public String toString() {
        return "Company{" +
                "companyId=" + companyId +
                ", companyGroupId=" + companyGroupId +
                ", webId='" + webId + '\'' +
                ", companyConfiguration=" + companyConfiguration +
                "}";
    }
}
