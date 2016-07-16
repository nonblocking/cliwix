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

package at.nonblocking.cliwix.model.compare;

import javax.xml.bind.annotation.XmlTransient;

/**
 * A comparable object with a identifier which can be used to check if a given
 * object exists within a collection
 */
@XmlTransient
public abstract class ComparableAndIdentifiableObject extends ComparableObject {

    /**
     * The "natural" ID of the object.
     * <br/>
     * <b>Must not</b> be the generated primary key, which is different from server instance to server instance.
     *
     * @return String
     */
    public abstract String identifiedBy();

    @Override
    public int hashCode() {
        return identifiedBy() != null ? identifiedBy().hashCode() : 0;
    }

}
