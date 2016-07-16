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

/**
 * Internal, not part of the XML
 */
public class Portlet extends LiferayEntity {

    private long portletDbId;

    @CompareEquals
    private String portletId;

    @CompareEquals
    private String displayName;

    @CompareEquals
    private boolean instanceable;

    public Portlet() {
    }

    public Portlet(String portletId, String displayName, boolean instanceable) {
        this.portletId = portletId;
        this.displayName = displayName;
        this.instanceable = instanceable;
    }

    @Override
    public Long getDbId() {
        return this.portletDbId;
    }

    @Override
    public String identifiedBy() {
        return this.getPortletId();
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.portletDbId = ((Portlet) other).portletDbId;
    }

    public String getPortletId() {
        return portletId;
    }

    public void setPortletId(String portletId) {
        this.portletId = portletId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isInstanceable() {
        return instanceable;
    }

    public void setInstanceable(boolean instanceable) {
        this.instanceable = instanceable;
    }

    public long getPortletDbId() {
        return portletDbId;
    }

    public void setPortletDbId(long portletDbId) {
        this.portletDbId = portletDbId;
    }
}
