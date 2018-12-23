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
import org.ramadda.data.record.filter.*;
import org.ramadda.data.tools.*;

import java.io.*;

import java.util.List;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
public class PointPrinter extends RecordTool {

    /**
     * _more_
     *
     * @param argArray _more_
     *
     * @throws Exception _more_
     */
    public PointPrinter(String[] argArray) throws Exception {
        super(null);
        int          total      = 0;
        int          numRecords = 5;
        List<String> argList    = processArgs(argArray);
        for (int i = 0; i < argList.size(); i++) {
            String arg = argList.get(i);

            if (arg.equals("-records")) {
                numRecords = new Integer(argList.get(i + 1)).intValue();
                i++;

                continue;
            }

            PointFile file =
                (PointFile) getRecordFileFactory().doMakeRecordFile(
                    argList.get(i));

            final int     theNumRecords = numRecords;
            RecordVisitor visitor       = new RecordVisitor() {
                int cnt = 0;
                public boolean visitRecord(RecordFile file,
                                           VisitInfo visitInfo,
                                           Record record) {
                    try {
                        StringBuffer buff = new StringBuffer();
                        record.print(buff);
                        System.err.println(buff);
                        cnt++;
                    } catch (Exception exc) {
                        throw new RuntimeException(exc);
                    }
                    if (cnt >= theNumRecords) {
                        return false;
                    }

                    return true;

                }
            };
            VisitInfo visitInfo = new VisitInfo();
            visitInfo.setStop(numRecords);
            System.err.println("file:" + arg + " " + numRecords);
            file.visit(visitor, visitInfo, null);
        }
        //        System.err.println ("total #points:" +  total);

    }


    /**
     * _more_
     *
     * @param message _more_
     */
    public void usage(String message) {
        if ((message != null) && (message.length() > 0)) {
            System.err.println("Error:" + message);
        }
        System.err.println(
            "usage: PointPrinter\n\t[-records <number of records to print]  <one or more Point files>");
        System.exit(1);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        new PointPrinter(args);
    }
}
