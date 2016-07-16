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

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@XmlAccessorType(XmlAccessType.FIELD)
public class Password extends ComparableObject {

    @CompareEquals
    @XmlAttribute(name = "encrypted")
    private Boolean encrypted;

    @CompareEquals
    @XmlValue
    private String password;

    public static String generateSHA1Hash(String password) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        messageDigest.update(password.getBytes());
        byte[] digest = messageDigest.digest();
        return DatatypeConverter.printBase64Binary(digest);
    }

    public Password() {
    }

    public Password(Boolean encrypted, String password) {
        this.encrypted = encrypted;
        this.password = password;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Password{" +
                "encrypted=" + encrypted +
                ", password='" + password + '\'' +
                '}';
    }
}
