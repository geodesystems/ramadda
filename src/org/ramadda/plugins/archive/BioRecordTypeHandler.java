/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.archive;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.Utils;
import org.w3c.dom.*;
import java.util.Hashtable;
import java.util.List;

public class BioRecordTypeHandler extends ExtensibleGroupTypeHandler {
    public BioRecordTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }

    @Override
    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry parentEntry,Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler sourceTypeHandler)
	throws Exception {
	super.addColumnToEntryForm(request, column, formBuffer, parentEntry, entry,
				   values, state, formInfo,
				   sourceTypeHandler);
	if(!column.getName().equals("common_name")) return;
	StringBuilder sb = new StringBuilder();
	HU.importJS(sb,getRepository().getHtdocsUrl("/archive/itis.js"));
	HU.script(sb,HU.call("Itis.init"));
	formBuffer.append(sb.toString());
    }

}
