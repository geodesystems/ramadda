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

package org.ramadda.geodata.gps;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.io.File;

import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class SolutionTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static final String TYPE_SOLUTION = "gps_solution";

    /** _more_ */
    private static int COLCNT = 0;

    /** _more_ */
    public static final int IDX_SITE_CODE = COLCNT++;

    /** _more_ */
    public static final int IDX_UTM_X = COLCNT++;

    /** _more_ */
    public static final int IDX_UTM_Y = COLCNT++;

    /** _more_ */
    public static final int IDX_REFERENCE_FRAME = COLCNT++;


    /** _more_ */
    public static final int IDX_X = COLCNT++;

    /** _more_ */
    public static final int IDX_Y = COLCNT++;

    /** _more_ */
    public static final int IDX_Z = COLCNT++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public SolutionTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



}
