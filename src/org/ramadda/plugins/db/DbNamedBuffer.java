/*
 * Copyright (c) 2008-2021 Geode Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ramadda.plugins.db;

import org.ramadda.util.NamedBuffer;


/**
 * Class description
 *
 *
 * @version        $version$, Tue, Nov 2, '21
 * @author         Enter your name here...    
 */
public class DbNamedBuffer extends NamedBuffer {

    /** _more_          */
    String anchor;

    /**
     * _more_
     *
     * @param name _more_
     */
    DbNamedBuffer(String name) {
	super(name);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param b _more_
     */
    DbNamedBuffer(String name, String b) {
	super(name, b);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param b _more_
     * @param anchor _more_
     */
    DbNamedBuffer(String name, String b, String anchor) {
	super(name, b);
	this.anchor = anchor;
    }
}

