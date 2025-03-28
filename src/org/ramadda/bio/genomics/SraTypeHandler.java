/*
* Copyright (c) 2008-2025 Geode Systems LLC
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
