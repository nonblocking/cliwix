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

@XmlRootElement(name = "Permission")
@XmlAccessorType(XmlAccessType.FIELD)
public class RolePermission extends LiferayEntity {

    @XmlTransient
    private Long resourcePermissionId;

    @XmlTransient
    private Long roleId;

    @CompareEquals
    @XmlAttribute(name = "resourceName", required = true)
    private String resourceName;

    @CompareEquals
    @XmlElementWrapper(name = "Actions", required = true)
    @XmlElement(name = "Action", required = true)
    private List<String> actions;

    public RolePermission() {
    }

    public RolePermission(String resourceName, List<String> actions) {
        this.resourceName = resourceName;
        this.actions = actions;
    }

    @Override
    public String identifiedBy() {
        return this.resourceName;
    }

    @Override
    public Long getDbId() {
        return this.resourcePermissionId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public Long getResourcePermissionId() {
        return resourcePermissionId;
    }

    public void setResourcePermissionId(Long resourcePermissionId) {
        this.resourcePermissionId = resourcePermissionId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.resourcePermissionId = ((RolePermission) other).resourcePermissionId;
        this.roleId = ((RolePermission) other).roleId;
    }

    @Override
    public String toString() {
        return "RolePermission{" +
                "resourcePermissionId=" + resourcePermissionId +
                ", resourceName='" + resourceName + '\'' +
                ", roleId='" + roleId + '\'' +
                ", actions=" + actions +
                "} ";
    }
}
