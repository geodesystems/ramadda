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
import org.ramadda.util.text.Seesv;

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
        String commands =
            (String) entry.getValue(ConvertibleTypeHandler.IDX_COMMANDS);
        if ( !Utils.stringDefined(commands)) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    "csv_commands", true);
            if ((metadataList != null) && (metadataList.size() > 0)) {
                commands = metadataList.get(0).getAttr2();
            }
        }


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
        commands = Utils.join(args, " ", false);
        if (debug) {
            System.err.println(
                "ConvertibleTypeHandler.getPathForRecordEntry entry:" + entry
                + " commands:" + commands);
        }
        IO.Request          path = getPathForRecordEntry(entry,
							 requestProperties);
        ConvertibleFile file = new ConvertibleFile(request, this, entry, args, path.getPath());

        return file;
    }


    public List<String> preprocessCsvCommands(Request request, List<String> args1) throws Exception {
	List<String> args = new ArrayList<String>();
	for (int j = 0; j < args1.size(); j++) {
	    String arg = args1.get(j);
	    String fileEntryId = null;
	    if (arg.startsWith("entry:")) {
		fileEntryId = arg.substring("entry:".length());
	    } else {
		if(arg.indexOf("entry:")>=0) {
		    //			    fileEntryId = StringUtil.findPattern(arg,"entry:[^\\s\"']+[\\s\"']");
		    fileEntryId = StringUtil.findPattern(arg,".*entry:([^\\s\"']+).*");
		    System.err.println("FOUND:" + fileEntryId +" arg:"+ arg);
		}			    
	    }
	    if(fileEntryId!=null) {
		Entry fileEntry =
		    getEntryManager().getEntry(request,fileEntryId);
		if (fileEntry == null) {
		    throw new IllegalArgumentException("Could not find " + arg);
		}
		File file = getStorageManager().getEntryFile(fileEntry);
		if (!file.exists()) {
		    throw new IllegalArgumentException("Entry not a file  " + arg);
		}
		arg = arg.replace("entry:" + fileEntryId,file.toString());
	    }
	    args.add(arg);
	}

	return args;
    }


    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if ( !tag.equals("convertform") && !tag.equals("seesv")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        ConvertibleOutputHandler coh =
            (ConvertibleOutputHandler) (ConvertibleOutputHandler) getRepository()
                .getOutputHandler(ConvertibleOutputHandler.class);
        StringBuilder sb = new StringBuilder();
        coh.makeConvertForm(request, entry, sb,props);

        return sb.toString();
    }



}
