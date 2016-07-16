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
import java.io.File;
import java.io.Serializable;

@XmlRootElement(name = "LiferayConfig")
@XmlAccessorType(XmlAccessType.FIELD)
public class LiferayConfig implements Serializable {

    public static final String MODEL_VERSION = "1.1";

    @XmlTransient
    private File sourceFile;

    @XmlAttribute(name = "modelVersion", required = true)
    private String modelVersion = MODEL_VERSION;

    @XmlAttribute(name = "sourceVersion", required = false)
    private String sourceVersion;

    @XmlElement(name = "Companies")
    private Companies companies;

    public LiferayConfig() {
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Companies getCompanies() {
        return companies;
    }

    public void setCompanies(Companies companies) {
        this.companies = companies;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }
}
