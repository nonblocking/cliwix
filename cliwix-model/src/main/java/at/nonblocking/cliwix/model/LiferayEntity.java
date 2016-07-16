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

import at.nonblocking.cliwix.model.compare.ComparableAndIdentifiableObject;

import java.io.Serializable;

/**
 * A top-level Liferay entity.
 *
 */
public abstract class LiferayEntity extends ComparableAndIdentifiableObject implements Serializable {

    /**
     * The internal database id
     *
     * @return Serializable
     */
    public abstract Long getDbId();

    /**
     * Copy internal Ids from given other entity
     *
     * @param other LiferayEntity
     */
    public abstract void copyIds(LiferayEntity other);

}
