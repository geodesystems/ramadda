/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.tools;


import org.ramadda.data.point.*;

import org.ramadda.data.record.*;
import org.ramadda.data.tools.*;

import java.io.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class PointChecker extends RecordTool {

    /**
     * _more_
     *
     *
     * @param argArray _more_
     * @throws Exception _more_
     */
    public PointChecker(String[] argArray) throws Exception {
        super(null);
        List<String> args = processArgs(argArray);
        for (String arg : args) {
            System.err.println("Checking:" + arg);
            PointFile    pointFile = (PointFile) doMakeRecordFile(arg);
            StringBuffer sb        = new StringBuffer();
            pointFile.runCheck(arg, sb);
            System.err.println(sb);
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        new PointChecker(args);
    }
}
