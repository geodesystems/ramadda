/*
* Copyright (c) 2008-2019 Geode Systems LLC
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

package org.ramadda.repository.type;


import org.ramadda.repository.*;




import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;


import java.util.Hashtable;





/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class DescriptionFromFileTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DescriptionFromFileTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param description _more_
     */
    public DescriptionFromFileTypeHandler(Repository repository, String type,
                           String description) {
        super(repository, type, description);
    }


    public void initEntryHasBeenCalled(Entry entry) {
	super.initEntryHasBeenCalled(entry);
	System.err.println("initEntry");
	if(entry.getResource().isFile()) {
	    try {
		entry.setDescription(getStorageManager().readFile(entry.getResource().getPath()));
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }
	}


    }


}
