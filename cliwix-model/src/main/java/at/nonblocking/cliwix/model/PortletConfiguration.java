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

@XmlRootElement(name = "PagePortlet")
@XmlAccessorType(XmlAccessType.FIELD)
public class PortletConfiguration extends LiferayEntity {

    @XmlTransient
    private Long portletPreferencesId;

    @XmlTransient
    private String basePortletId;

    @CompareEquals
    @XmlAttribute(name = "portletId", required = true)
    private String portletId;

    @CompareEquals
    @XmlElementWrapper(name = "PortletPreferences")
    @XmlElement(name = "Preference")
    private List<Preference> preferences;

    @XmlElement(name = "Permissions")
    private ResourcePermissions permissions;

    public PortletConfiguration() {
    }

    public PortletConfiguration(String portletId, List<Preference> preferences) {
        this.portletId = portletId;
        this.preferences = preferences;
    }

    @Override
    public String identifiedBy() {
        return this.portletId;
    }

    @Override
    public Long getDbId() {
        return this.portletPreferencesId;
    }

    public Long getPortletPreferencesId() {
        return portletPreferencesId;
    }

    public void setPortletPreferencesId(Long portletPreferencesId) {
        this.portletPreferencesId = portletPreferencesId;
    }

    public String getPortletId() {
        return portletId;
    }

    public void setPortletId(String portletId) {
        this.portletId = portletId;
    }

    public String getBasePortletId() {
        return basePortletId;
    }

    public void setBasePortletId(String basePortletId) {
        this.basePortletId = basePortletId;
    }

    public ResourcePermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(ResourcePermissions permissions) {
        this.permissions = permissions;
    }

    public List<Preference> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<Preference> preferences) {
        this.preferences = preferences;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.portletPreferencesId = ((PortletConfiguration) other).portletPreferencesId;
        this.basePortletId = ((PortletConfiguration) other).basePortletId;
    }

    @Override
    public String toString() {
        return "PortletConfiguration{" +
                "portletPreferencesId=" + portletPreferencesId +
                ",portletId='" + portletId + '\'' +
                ",basePortletId='" + basePortletId + '\'' +
                ",preferences=" + preferences +
                '}';
    }
}
