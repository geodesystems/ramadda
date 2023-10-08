/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.words;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;


import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 */
public class DictionaryWordTypeHandler extends GenericTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DictionaryWordTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    @Override
    public String getFormLabel(Entry parentEntry, Entry entry, String arg, String dflt) {
	if(arg.equals("other_word")) {
	    if(parentEntry.getTypeHandler().isType("type_dictionary")) {
		DictionaryTypeHandler dth  = (DictionaryTypeHandler) parentEntry.getTypeHandler();
		String target = dth.getTargetLabel(parentEntry);
		return target +" " + msg("Word");
	    }
	}
	if(arg.equals(FIELD_NAME)) {
	    if(parentEntry.getTypeHandler().isType("type_dictionary")) {
		DictionaryTypeHandler dth  = (DictionaryTypeHandler) parentEntry.getTypeHandler();
		String language = (String) parentEntry.getValue(DictionaryTypeHandler.IDX_LANGUAGE);
		if(stringDefined(language)) 
		    return language +" " + msg("Word");
	    }
	}


        return super.getFormLabel(parentEntry,entry,arg,dflt);
    }



}
