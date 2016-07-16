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
public class ResourcePermission extends LiferayEntity {

    @XmlTransient
    private Long resourcePermissionId;

    @CompareEquals
    @XmlAttribute(name = "role", required = true)
    private String role;

    @CompareEquals
    @XmlElementWrapper(name = "Actions", required = true)
    @XmlElement(name = "Action", required = true)
    private List<String> actions;

    public ResourcePermission() {
    }

    public ResourcePermission(String role, List<String> actions) {
        this.role = role;
        this.actions = actions;
    }

    @Override
    public String identifiedBy() {
        return this.role;
    }

    @Override
    public Long getDbId() {
        return this.resourcePermissionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    @Override
    public void copyIds(LiferayEntity other) {
        this.resourcePermissionId = ((ResourcePermission) other).resourcePermissionId;
    }

    @Override
    public String toString() {
        return "ResourcePermission{" +
                "resourcePermissionId='" + resourcePermissionId + '\'' +
                ",role='" + role + '\'' +
                ",actions=" + actions +
                '}';
    }
}
