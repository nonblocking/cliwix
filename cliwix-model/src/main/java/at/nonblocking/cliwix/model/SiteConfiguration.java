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

import at.nonblocking.cliwix.model.compare.ComparableObject;
import at.nonblocking.cliwix.model.compare.CompareEquals;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlRootElement(name = "SiteConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class SiteConfiguration extends ComparableObject implements  Serializable {

    @CompareEquals
    @XmlElement(name = "FriendlyURL", required = true)
    private String friendlyURL;

    @CompareEquals
    @XmlElement(name = "Active")
    private boolean active = true;

    @CompareEquals
    @XmlElement(name = "MembershipType", required = true)
    private SITE_MEMBERSHIP_TYPE membershipType;

    @CompareEquals
    @XmlElement(name = "Description")
    private String description;

    @CompareEquals
    @XmlElement(name = "VirtualHostPublicPages")
    private String virtualHostPublicPages;

    @CompareEquals
    @XmlElement(name = "VirtualHostPrivatePages")
    private String virtualHostPrivatePages;

    public SiteConfiguration() {
    }

    public SiteConfiguration(String friendlyURL, SITE_MEMBERSHIP_TYPE membershipType) {
        this.friendlyURL = friendlyURL;
        this.membershipType = membershipType;
    }

    public String getFriendlyURL() {
        return friendlyURL;
    }

    public void setFriendlyURL(String friendlyURL) {
        this.friendlyURL = friendlyURL;
    }

    public String getVirtualHostPrivatePages() {
        return virtualHostPrivatePages;
    }

    public void setVirtualHostPrivatePages(String virtualHostPrivatePages) {
        this.virtualHostPrivatePages = virtualHostPrivatePages;
    }

    public String getVirtualHostPublicPages() {
        return virtualHostPublicPages;
    }

    public void setVirtualHostPublicPages(String virtualHostPublicPages) {
        this.virtualHostPublicPages = virtualHostPublicPages;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public SITE_MEMBERSHIP_TYPE getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(SITE_MEMBERSHIP_TYPE membershipType) {
        this.membershipType = membershipType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "SiteConfiguration{" +
                "friendlyURL='" + friendlyURL + '\'' +
                ", active=" + active +
                ", membershipType=" + membershipType +
                ", description='" + description + '\'' +
                ", virtualHostPublicPages='" + virtualHostPublicPages + '\'' +
                ", virtualHostPrivatePages='" + virtualHostPrivatePages + '\'' +
                "}";
    }
}
