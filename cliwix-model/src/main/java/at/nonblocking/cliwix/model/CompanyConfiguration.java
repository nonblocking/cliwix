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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "CompanyConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class CompanyConfiguration extends ComparableObject {

    @CompareEquals
    @XmlElement(name = "VirtualHost", required = true)
    private String virtualHost;

    @CompareEquals
    @XmlElement(name = "MailDomain", required = true)
    private String mailDomain;

    @CompareEquals
    @XmlElement(name = "HomeURL")
    private String homeUrl;

    @CompareEquals
    @XmlElement(name = "AccountName")
    private String accountName;

    @CompareEquals
    @XmlElement(name = "Active")
    private Boolean active = Boolean.TRUE;

    @CompareEquals
    @XmlElement(name = "DefaultLocale", required = true)
    private String defaultLocale;

    @CompareEquals
    @XmlElement(name = "DefaultTimezone", required = true)
    private String defaultTimezone;

    @CompareEquals
    @XmlElement(name = "DefaultGreeting")
    private String defaultGreeting;

    public CompanyConfiguration() {
    }

    public CompanyConfiguration(String virtualHost, String mailDomain, String defaultLocale, String defaultTimezone) {
        this.virtualHost = virtualHost;
        this.mailDomain = mailDomain;
        this.defaultLocale = defaultLocale;
        this.defaultTimezone = defaultTimezone;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getMailDomain() {
        return mailDomain;
    }

    public void setMailDomain(String mailDomain) {
        this.mailDomain = mailDomain;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public void setHomeUrl(String homeUrl) {
        this.homeUrl = homeUrl;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public String getDefaultGreeting() {
        return defaultGreeting;
    }

    public void setDefaultGreeting(String defaultGreeting) {
        this.defaultGreeting = defaultGreeting;
    }

    @Override
    public String toString() {
        return "CompanyConfiguration{" +
                "virtualHost='" + virtualHost + '\'' +
                ", mailDomain='" + mailDomain + '\'' +
                ", homeUrl='" + homeUrl + '\'' +
                ", accountName='" + accountName + '\'' +
                ", active=" + active +
                ", defaultLocale='" + defaultLocale + '\'' +
                ", defaultTimezone='" + defaultTimezone + '\'' +
                ", defaultGreeting='" + defaultGreeting + '\'' +
                '}';
    }
}
