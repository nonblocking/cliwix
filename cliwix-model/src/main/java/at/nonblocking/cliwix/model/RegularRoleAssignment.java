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

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RegularRoleAssignment extends LiferayEntity implements CompanyMember {

    @XmlTransient
    private Long ownerCompanyId;

    @CompareEquals
    @XmlAttribute(name = "name", required = true)
    private String roleName;

    @CompareEquals
    @XmlElementWrapper(name = "MemberUserGroups")
    @XmlElement(name = "UserGroup")
    private List<MemberUserGroup> memberUserGroups;

    @CompareEquals
    @XmlElementWrapper(name = "MemberOrganizations")
    @XmlElement(name = "Organization")
    private List<MemberOrganization> memberOrganizations;

    @CompareEquals
    @XmlElementWrapper(name = "MemberUsers")
    @XmlElement(name = "User")
    private List<MemberUser> memberUsers;

    public RegularRoleAssignment() {
    }

    public RegularRoleAssignment(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<MemberUserGroup> getMemberUserGroups() {
        return memberUserGroups;
    }

    public void setMemberUserGroups(List<MemberUserGroup> memberUserGroups) {
        this.memberUserGroups = memberUserGroups;
    }

    public List<MemberOrganization> getMemberOrganizations() {
        return memberOrganizations;
    }

    public void setMemberOrganizations(List<MemberOrganization> memberOrganizations) {
        this.memberOrganizations = memberOrganizations;
    }

    public List<MemberUser> getMemberUsers() {
        return memberUsers;
    }

    public void setMemberUsers(List<MemberUser> memberUsers) {
        this.memberUsers = memberUsers;
    }

    @Override
    public Long getOwnerCompanyId() {
        return ownerCompanyId;
    }

    public void setOwnerCompanyId(Long ownerCompanyId) {
        this.ownerCompanyId = ownerCompanyId;
    }

    @Override
    public Long getDbId() {
        return null;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.ownerCompanyId = ((RegularRoleAssignment) other).ownerCompanyId;
    }

    @Override
    public String identifiedBy() {
        return this.roleName;
    }

    @Override
    public String toString() {
        return "RegularRoleAssignment{" +
                "ownerCompanyId=" + ownerCompanyId +
                ", roleName='" + roleName + '\'' +
                ", memberUserGroups=" + memberUserGroups +
                ", memberOrganizations=" + memberOrganizations +
                ", memberUsers=" + memberUsers +
                "}";
    }
}
