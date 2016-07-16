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
import java.util.List;

@XmlRootElement(name = "Articles")
@XmlAccessorType(XmlAccessType.FIELD)
public class Articles extends ListType<Article> {

    @XmlElements({
            @XmlElement(name = "StaticArticle", type = StaticArticle.class),
            @XmlElement(name = "TemplateDrivenArticle", type = TemplateDrivenArticle.class)
    })
    private List<Article> articles;

    public Articles() {
    }

    public Articles(List<Article> articles) {
        this.articles = articles;
    }

    public List<Article> getArticles() {
        return articles;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }

    @Override
    public List<Article> getList() {
        return this.articles;
    }
}
