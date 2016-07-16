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

@XmlRootElement(name = "UserGroup")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserGroup extends LiferayEntity implements CompanyMember, Group {

    @XmlTransient
    private Long userGroupId;

    @XmlTransient
    private Long ownerCompanyId;

    @XmlTransient
    private Long userGroupGroupId;

    @CompareEquals
    @XmlAttribute(name = "name", required = true)
    private String name;

    @CompareEquals
    @XmlElement(name = "Description")
    private String description;

    @CompareEquals
    @XmlElementWrapper(name = "MemberUsers")
    @XmlElement(name = "User")
    private List<MemberUser> memberUsers;

    public UserGroup() {
    }

    public UserGroup(String name) {
        this.name = name;
    }

    public UserGroup(String name, String description, List<MemberUser> memberUsers) {
        this.name = name;
        this.description = description;
        this.memberUsers = memberUsers;
    }

    @Override
    public String identifiedBy() {
        return this.name;
    }


    @Override
    public Long getDbId() {
        return this.userGroupId;
    }

    @Override
    public Long getGroupId() {
        return this.userGroupGroupId;
    }

    public Long getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(Long userGroupId) {
        this.userGroupId = userGroupId;
    }

    public Long getUserGroupGroupId() {
        return userGroupGroupId;
    }

    public void setUserGroupGroupId(Long userGroupGroupId) {
        this.userGroupGroupId = userGroupGroupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public void copyIds(LiferayEntity other) {
        this.userGroupId = ((UserGroup) other).userGroupId;
        this.ownerCompanyId = ((UserGroup) other).ownerCompanyId;
        this.userGroupGroupId = ((UserGroup) other).userGroupGroupId;
    }

    @Override
    public String toString() {
        return "UserGroup{" +
                "userGroupId=" + userGroupId +
                ", ownerCompanyId=" + ownerCompanyId +
                ", userGroupGroupId=" + userGroupGroupId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", memberUsers=" + memberUsers +
                "}";
    }
}
