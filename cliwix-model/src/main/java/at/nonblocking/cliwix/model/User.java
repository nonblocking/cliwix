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
import java.util.Date;

@XmlRootElement(name = "User")
@XmlAccessorType(XmlAccessType.FIELD)
public class User extends LiferayEntity implements CompanyMember, Group {

    @XmlTransient
    private Long userId;

    @XmlTransient
    private Long userGroupId;

    @XmlTransient
    private Long ownerCompanyId;

    @CompareEquals
    @XmlAttribute(name = "screenName", required = true)
    private String screenName;

    @CompareEquals
    @XmlElement(name = "EmailAddress", required = true)
    private String emailAddress;

    @CompareEquals
    @XmlElement(name = "FirstName", required = true)
    private String firstName;

    @CompareEquals
    @XmlElement(name = "MiddleName")
    private String middleName;

    @CompareEquals
    @XmlElement(name = "LastName")
    private String lastName;

    @CompareEquals
    @XmlElement(name = "Password")
    private Password password;

    @CompareEquals
    @XmlElement(name = "JobTitle")
    private String jobTitle;

    @CompareEquals
    @XmlElement(name = "BirthDate")
    private Date birthDate;

    @CompareEquals
    @XmlElement(name = "Gender")
    private GENDER gender = GENDER.F;

    @CompareEquals
    @XmlElement(name = "Language")
    private String language;

    @CompareEquals
    @XmlElement(name = "Timezone")
    private String timezone;

    @CompareEquals
    @XmlElement(name = "Greeting")
    private String greeting;

    public User() {
    }

    public User(String screenName, String emailAddress, String firstName) {
        this.screenName = screenName;
        this.emailAddress = emailAddress;
        this.firstName = firstName;
    }

    public User(String screenName, String emailAddress, Password password,
                String jobTitle, String firstName, String middleName, String lastName,
                Date birthDate, GENDER gender, String language, String timezone, String greeting) {
        this.screenName = screenName;
        this.emailAddress = emailAddress;
        this.password = password;
        this.jobTitle = jobTitle;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.language = language;
        this.timezone = timezone;
        this.greeting = greeting;
    }

    @Override
    public String identifiedBy() {
        return this.screenName;
    }

    @Override
    public Long getDbId() {
        return this.userId;
    }

    @Override
    public Long getGroupId() {
        return this.userGroupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Password getPassword() {
        return password;
    }

    public void setPassword(Password password) {
        this.password = password;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public GENDER getGender() {
        return gender;
    }

    public void setGender(GENDER gender) {
        this.gender = gender;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public Long getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(Long userGroupId) {
        this.userGroupId = userGroupId;
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
        this.userId = ((User) other).userId;
        this.userGroupId = ((User) other).userGroupId;
        this.ownerCompanyId = ((User) other).ownerCompanyId;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ",ownerCompanyId='" + ownerCompanyId + '\'' +
                ",userGroupId='" + userGroupId + '\'' +
                ",screenName='" + screenName + '\'' +
                ",emailAddress='" + emailAddress + '\'' +
                ",password=" + password +
                ",jobTitle='" + jobTitle + '\'' +
                ",firstName='" + firstName + '\'' +
                ",middleName='" + middleName + '\'' +
                ",lastName='" + lastName + '\'' +
                ",birthDate=" + birthDate +
                ",gender=" + gender +
                ",language='" + language + '\'' +
                ",timezone='" + timezone + '\'' +
                ",greeting='" + greeting + '\'' +
                '}';
    }
}
