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

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "Permissions")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourcePermissions extends ListType<ResourcePermission> {

    @XmlElement(name = "Permission")
    private List<ResourcePermission> permissions;

    public ResourcePermissions() {
    }

    public ResourcePermissions(List<ResourcePermission> permissions) {
        this.permissions = permissions;
    }

    public List<ResourcePermission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(List<ResourcePermission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public List<ResourcePermission> getList() {
        return getPermissions();
    }
}
