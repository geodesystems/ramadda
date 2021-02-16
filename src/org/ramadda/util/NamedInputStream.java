/*
* Copyright (c) 2008-2019 Geode Systems LLC
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


import java.io.*;


/**
 * @author Jeff McWhirter
 */

public class NamedInputStream {

    /** _more_          */
    String name;

    /** _more_          */
    InputStream inputStream;

    /**
     * _more_
     *
     * @param name _more_
     * @param inputStream _more_
     */
    public NamedInputStream(String name, InputStream inputStream) {
        this.name        = name;
        this.inputStream = inputStream;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void close() throws Exception {
        if (inputStream != System.in) {
            IO.close(inputStream);
        }
    }

}
