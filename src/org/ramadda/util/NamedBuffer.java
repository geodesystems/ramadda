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


package org.ramadda.util;


import java.util.ArrayList;
import java.util.List;




/**
 * Class description
 *
 *
 * @param <T>
 *
 * @version        $version$, Wed, Mar 10, '21
 * @author         Enter your name here...    
 */
public class NamedBuffer {

    /** _more_          */
    private String name;

    /** _more_          */
    private StringBuilder buffer = new StringBuilder();

    /**
     * _more_
     *
     * @param name _more_
     */
    public NamedBuffer(String name) {
        this.name = name;
    }

    public NamedBuffer(String name, String b) {
	this(name);
	buffer.append(b);
    }    


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return name;
    }


    public StringBuilder getBuffer() {
	return buffer;
    }


    public void append(Object o) {
	buffer.append(o);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + " " + buffer;
    }
}
