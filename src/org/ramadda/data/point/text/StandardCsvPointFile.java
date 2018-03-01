/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.data.point.text;


import org.ramadda.data.point.*;




import org.ramadda.data.record.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.XlsUtil;

import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.image.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 *
 */
public abstract class StandardCsvPointFile extends TextFile {

    /**
     * _more_
     */
    public StandardCsvPointFile() {}

    /**
     * ctor
     *
     *
     * @param filename _more_
     *
     * @throws IOException _more_
     */
    public StandardCsvPointFile(String filename) throws IOException {
        super(filename);
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param properties _more_
     *
     * @throws IOException _more_
     */
    public StandardCsvPointFile(String filename, Hashtable properties)
            throws IOException {
        super(filename, properties);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isHeaderStandard() {
        return true;
    }

}
