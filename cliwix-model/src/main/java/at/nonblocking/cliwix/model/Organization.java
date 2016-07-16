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

@XmlRootElement(name = "Organization")
@XmlAccessorType(XmlAccessType.FIELD)
public class Organization extends LiferayEntity implements CompanyMember, Group {

    public static final String TYPE_LOCATION = "location";
    public static final String TYPE_REGULAR_ORGANIZATION = "regular-organization";

    @XmlTransient
    private Long organizationId;

    @XmlTransient
    private Long ownerCompanyId;

    @XmlTransient
    private Long organizationGroupId;

    @CompareEquals
    @XmlAttribute(name = "name", required = true)
    private String name;

    @CompareEquals
    @XmlElement(name = "Type")
    private String type = TYPE_REGULAR_ORGANIZATION;

    @CompareEquals
    @XmlElement(name = "CountryCode")
    private String countryCode;

    @CompareEquals
    @XmlElement(name = "RegionCode")
    private String regionCode;

    @CompareEquals
    @XmlElement(name = "OrganizationMembers")
    private OrganizationMembers organizationMembers;

    @XmlElement(name = "OrganizationRoleAssignments")
    private OrganizationRoleAssignments organizationRoleAssignments;

    @XmlElementWrapper(name = "SubOrganizations")
    @XmlElement(name = "Organization")
    private List<Organization> subOrganizations;

    public Organization() {
    }

    public Organization(String name) {
        this.name = name;
    }

    public Organization(String name, OrganizationMembers organizationMembers) {
        this.name = name;
        this.organizationMembers = organizationMembers;
    }

    @Override
    public String identifiedBy() {
        return this.name;
    }

    @Override
    public Long getDbId() {
        return this.organizationId;
    }

    @Override
    public Long getGroupId() {
        return this.organizationGroupId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public OrganizationMembers getOrganizationMembers() {
        return organizationMembers;
    }

    public void setOrganizationMembers(OrganizationMembers organizationMembers) {
        this.organizationMembers = organizationMembers;
    }

    public List<Organization> getSubOrganizations() {
        return subOrganizations;
    }

    public void setSubOrganizations(List<Organization> subOrganizations) {
        this.subOrganizations = subOrganizations;
    }

    public Long getOrganizationGroupId() {
        return organizationGroupId;
    }

    public void setOrganizationGroupId(Long organizationGroupId) {
        this.organizationGroupId = organizationGroupId;
    }

    @Override
    public Long getOwnerCompanyId() {
        return ownerCompanyId;
    }

    public void setOwnerCompanyId(Long ownerCompanyId) {
        this.ownerCompanyId = ownerCompanyId;
    }

    public OrganizationRoleAssignments getOrganizationRoleAssignments() {
        return organizationRoleAssignments;
    }

    public void setOrganizationRoleAssignments(OrganizationRoleAssignments organizationRoleAssignments) {
        this.organizationRoleAssignments = organizationRoleAssignments;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.organizationId = ((Organization) other).organizationId;
        this.organizationGroupId = ((Organization) other).organizationGroupId;
        this.ownerCompanyId = ((Organization) other).ownerCompanyId;
    }

    @Override
    public String toString() {
        return "Organization{" +
                "organizationId=" + organizationId +
                ", ownerCompanyId=" + ownerCompanyId +
                ", organizationGroupId=" + organizationGroupId +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", organizationMembers=" + organizationMembers +
                "}";
    }
}
