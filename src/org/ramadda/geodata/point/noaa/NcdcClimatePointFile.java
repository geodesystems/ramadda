/**                                                                                                
Copyright (c) 2008-2026 Geode Systems LLC                                                          
SPDX-License-Identifier: Apache-2.0                                                                
*/

package org.ramadda.geodata.point.noaa;

import org.ramadda.util.IO;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import java.io.*;


/**
 */
public class NcdcClimatePointFile extends HeaderPointFile {


    /**
     * ctor
     *
     *
     *
     * @throws IOException On badness
     */
    public NcdcClimatePointFile(IO.Path path) throws IOException {
        super(path);
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, NcdcClimatePointFile.class);
    }

}
