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

@XmlRootElement(name = "Role")
@XmlAccessorType(XmlAccessType.FIELD)
public class Role extends LiferayEntity implements CompanyMember {

    @XmlTransient
    private Long roleId;

    @XmlTransient
    private Long ownerCompanyId;

    @CompareEquals
    @XmlAttribute(name = "name", required = true)
    private String name;

    @CompareEquals
    @XmlElement(name = "Type", required = false)
    private ROLE_TYPE type = ROLE_TYPE.DEFAULT;

    @CompareEquals
    @XmlElementWrapper(name = "Titles")
    @XmlElement(name = "Title")
    private List<LocalizedTextContent> titles;

    @CompareEquals
    @XmlElementWrapper(name = "Descriptions")
    @XmlElement(name = "Description")
    private List<LocalizedTextContent> descriptions;

    @XmlElement(name = "Permissions")
    private RolePermissions permissions;

    public Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    public Role(String name, ROLE_TYPE type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String identifiedBy() {
        return this.name;
    }

    @Override
    public Long getDbId() {
        return this.roleId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ROLE_TYPE getType() {
        return type;
    }

    public void setType(ROLE_TYPE type) {
        this.type = type;
    }

    public List<LocalizedTextContent> getTitles() {
        return titles;
    }

    public void setTitles(List<LocalizedTextContent> titles) {
        this.titles = titles;
    }

    public List<LocalizedTextContent> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<LocalizedTextContent> descriptions) {
        this.descriptions = descriptions;
    }

    public RolePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(RolePermissions permissions) {
        this.permissions = permissions;
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
        this.roleId = ((Role) other).roleId;
        this.ownerCompanyId = ((Role) other).ownerCompanyId;
    }

    @Override
    public String toString() {
        return "Role{" +
                "ownerCompanyId=" + ownerCompanyId +
                ",roleId=" + roleId +
                ",name='" + name + '\'' +
                ",type='" + type + '\'' +
                ",titles=" + titles +
                ",descriptions=" + descriptions +
                '}';
    }
}
