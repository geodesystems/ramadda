/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point.text;


import org.ramadda.util.IO;
import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;



/**
 */
public abstract class SingleSiteTextFile extends CsvFile {

    /**
     * _more_
     */
    public SingleSiteTextFile() {}

    /**
     * ctor
     *
     * @throws IOException _more_
     */
    public SingleSiteTextFile(IO.Path path) throws IOException {
        super(path);
    }

    /**
     * _more_
     *
     * @param properties _more_
     *
     * @throws IOException _more_
     */
    public SingleSiteTextFile(IO.Path path, Hashtable properties)
            throws IOException {
        super(path, properties);
    }


    /**
     *  Since these are single station files don't do bounds, etc
     *
     * @param action _more_
     *
     * @return _more_
     */
    public boolean isCapable(String action) {
        if (action.equals(ACTION_BOUNDINGPOLYGON)) {
            return false;
        }
        if (action.equals(ACTION_GRID)) {
            return false;
        }

        return super.isCapable(action);
    }


}
