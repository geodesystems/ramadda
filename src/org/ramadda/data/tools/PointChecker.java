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
     * @param args _more_
     *
     * @param argArray _more_
     *
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
