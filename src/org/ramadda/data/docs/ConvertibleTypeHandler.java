/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;


import org.ramadda.data.record.RecordFile;
import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.seesv.Seesv;

import ucar.unidata.util.StringUtil;
import org.w3c.dom.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;





/**
 *
 *
 */
public class ConvertibleTypeHandler extends PointTypeHandler {

    public static final String TYPE_CONVERTIBLE = "type_convertible";

    /** _more_ */
    private static int IDX = 0;

    //Have these here so we can act like a point entry type

    /** _more_ */
    public static final int IDX_NUMPOINTS = IDX++;

    /** _more_ */
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

    public List<String> getCsvCommands(Request request, Entry entry) throws Exception {
        String commands =
            (String) entry.getValue(request,ConvertibleTypeHandler.IDX_COMMANDS);
        if ( !Utils.stringDefined(commands)) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    "csv_commands", true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                commands = metadataList.get(0).getAttr2();
            }
        }

	if(commands==null) return null;

        List<StringBuilder> toks = Seesv.tokenizeCommands(commands);
        List<String>        args = new ArrayList<String>();
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
	return args;
    }	


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
	List<String> args = getCsvCommands(request, entry);
        if (debug) {
            System.err.println(
                "ConvertibleTypeHandler.getPathForRecordEntry entry:" + entry
                + " commands:" + Utils.join(args, " ", false));
        }


        IO.Path          path = getPathForRecordEntry(request,entry,requestProperties);
        ConvertibleFile file = new ConvertibleFile(request, this, entry, args, path);

        return file;
    }







}
