/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.io.*;


/**
 * @author Jeff McWhirter
 */

public class NamedInputStream {

    /** _more_ */
    String name;

    /** _more_ */
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

    /**
      * @return _more_
     */
    public String toString() {
        return "NamedInputStream:" + name;
    }

}
