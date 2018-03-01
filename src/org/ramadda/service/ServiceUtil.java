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

package org.ramadda.service;


import org.ramadda.repository.*;

import ucar.unidata.util.IOUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author RAMADDA Development Team
 */
public class ServiceUtil {

    /** _more_ */
    public static final String COMMAND_CP = "cp";


    /** _more_ */
    public static final String COMMAND_MV = "mv";

    /**
     * _more_
     */
    public ServiceUtil() {}


    /**
     * _more_
     *
     * @param request _more_
     * @param command _more_
     * @param input _more_
     * @param commands _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean evaluate(Request request, Service command,
                            ServiceInput input, List<String> commands)
            throws Exception {
        if (commands.size() <= 1) {
            return true;
        }

        String task = commands.get(1);
        if (task.equals(COMMAND_MV)) {
            File entryFile = new File(commands.get(2));
            if ( !IOUtil.isADescendent(input.getProcessDir(), entryFile)) {
                throw new IllegalArgumentException(
                    "Cannot move the entry file. Can only move temp files");
            }
            String newName = commands.get(3);
            IOUtil.moveFile(entryFile,
                            new File(IOUtil.joinDir(input.getProcessDir(),
                                newName)));
        } else if (task.equals(COMMAND_CP)) {
            File   entryFile = new File(commands.get(2));
            String newName   = commands.get(3);
            IOUtil.copyFile(entryFile,
                            new File(IOUtil.joinDir(input.getProcessDir(),
                                newName)));
        }

        return false;
    }

}
