/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point.text;


import org.ramadda.data.point.*;




import org.ramadda.data.record.*;
import org.ramadda.util.HtmlUtils;
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
        //        super(filename, properties);
        System.err.println("STD");
    }



    /**
     * _more_
     *
     * @param filename _more_
     * @param context _more_
     * @param properties _more_
     */
    public StandardCsvPointFile(String filename, RecordFileContext context,
                                Hashtable properties) {
        super(filename, context, properties);
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
