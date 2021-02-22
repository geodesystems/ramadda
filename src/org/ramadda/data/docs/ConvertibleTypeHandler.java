/*
* Copyright (c) 2008-2021 Geode Systems LLC
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

package org.ramadda.data.docs;


import org.ramadda.data.record.RecordFile;
import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.Utils;


import org.w3c.dom.*;


import ucar.unidata.util.StringUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;





/**
 *
 *
 */
public class ConvertibleTypeHandler extends PointTypeHandler {


    /** _more_ */
    private static int IDX = 0;  

    //Have these here so we can act like a point entry type

    /** _more_          */
    public static final int IDX_NUMPOINTS = IDX++;

    /** _more_          */
    public static final int IDX_PROPERTIES = IDX++;

    /** _more_ */
    public static final int IDX_COMMANDS = IDX++;

    /** _more_ */
    public static final int IDX_LAST = IDX_COMMANDS;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ConvertibleTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordFile doMakeRecordFile(Entry entry, Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        String commands =
            (String) entry.getValue(ConvertibleTypeHandler.IDX_COMMANDS);
        List<StringBuilder> toks    = tokenizeCommands(commands);
        Request             request = getRepository().getTmpRequest();

        List<String>        args    = new ArrayList<String>();
        for (int j = 0; j < toks.size(); j++) {
            String arg = toks.get(j).toString();
            if (arg.equals(Utils.MULTILINE_END)) {
                continue;
            }
            if (arg.startsWith("entry:")) {
                Entry fileEntry = getEntryManager().getEntry(request,
                                      arg.substring("entry:".length()));
                if (fileEntry == null) {
                    throw new IllegalArgumentException("Could not find "
                            + arg);
                }
                if (fileEntry.getFile() == null) {
                    throw new IllegalArgumentException("Entry not a file  "
                            + arg);
                }
                arg = fileEntry.getFile().toString();
            } else if (arg.equals("-run")) {
                continue;
            }
            args.add(arg);
        }
        commands = Utils.join(args, " ", false);
        String path = getPathForRecordEntry(entry, commands,
                                            requestProperties);
        ConvertibleFile file = new ConvertibleFile(entry, args, path);

        return file;
    }

    /**
     * _more_
     *
     * @param commandString _more_
     *
     * @return _more_
     */
    public List<StringBuilder> tokenizeCommands(String commandString) {
        StringBuilder tmp = new StringBuilder();
        for (String line : StringUtil.split(commandString, "\n")) {
            String tline = line.trim();
            if (tline.startsWith("-quit")) {
                break;
            }
            if ( !tline.startsWith("#")) {
                tmp.append(line);
                tmp.append("\n");
            }
        }
        List<StringBuilder> toks =
            Utils.parseMultiLineCommandLine(tmp.toString());

        return toks;
    }



}
