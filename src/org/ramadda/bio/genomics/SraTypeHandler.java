/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.bio.genomics;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.TempDir;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.Date;
import java.util.List;



/**
 *
 *
 */
public class SraTypeHandler extends GenomicsTypeHandler {

    /** _more_ */
    public static final String PROP_SRA_BIN = "sra.bin";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public SraTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry, NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);

        String path = getTypeProperty(PROP_SRA_BIN, null);
        if (path == null) {
            return;
        }

        //If the file for the entry does not exist then return
        if ( !entry.isFile()) {
            return;
        }


        /*
          entry.getFile().toString();
          Object[] values = getEntryValues(entry);
        */

    }




}
