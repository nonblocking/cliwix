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

@XmlRootElement(name = "PortalPreferences")
@XmlAccessorType(XmlAccessType.FIELD)
public class PortalPreferences extends LiferayEntity {

    @XmlTransient
    private Long portalPreferencesId;

    @CompareEquals
    @XmlElement(name = "Preference")
    private List<Preference> preferences;

    public PortalPreferences() {
    }

    @Override
    public String identifiedBy() {
        //This element exists only once per company
        return null;
    }

    @Override
    public Long getDbId() {
        return this.portalPreferencesId;
    }

    public PortalPreferences(List<Preference> preferences) {
        this.preferences = preferences;
    }

    public Long getPortalPreferencesId() {
        return portalPreferencesId;
    }

    public void setPortalPreferencesId(Long portalPreferencesId) {
        this.portalPreferencesId = portalPreferencesId;
    }

    public List<Preference> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<Preference> preferences) {
        this.preferences = preferences;
    }

    @Override
    public void copyIds(LiferayEntity other) {
        this.portalPreferencesId = ((PortalPreferences) other).portalPreferencesId;
    }

    @Override
    public String toString() {
        return "PortalPreferences{" +
                "portalPreferencesId=" + portalPreferencesId +
                ",preferences=" + preferences +
                '}';
    }
}
