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

package org.ramadda.service;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;


/**
 *
 */
public class ServiceFileTypeHandler extends ServiceTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ServiceFileTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param appendable _more_
     *
     * @throws Exception _more_
     */
    public void getServiceXml(Request request, Entry entry,
                              Appendable appendable)
            throws Exception {
        appendable.append(
            IOUtil.readContents(
                getStorageManager().getFileInputStream(
                    entry.getFile().toString())));
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
        Element root = XmlUtil.getRoot(
                           IOUtil.readContents(
                               getStorageManager().getFileInputStream(
                                   entry.getFile().toString())));
        Element service;
        if (root.getTagName().equals(Service.TAG_SERVICE)) {
            service = root;
        } else {
            service = XmlUtil.findChild(root, Service.TAG_SERVICE);
        }
        /*
<params>
<param  name="17ae4559-9ae0-499d-93b0-1295d00b4b60.imagemagick.convert.type.input_file_hidden" ><![CDATA[58607c20-77e3-4c70-8f96-c9307f6ec835]]></param>
        */

        String  paramsXml = "";
        Element params    = XmlUtil.findChild(service, Service.TAG_PARAMS);
        if (params != null) {
            paramsXml = XmlUtil.toString(params);
        }
        entry.getTypeHandler().getEntryValues(entry)[IDX_PARAMETERS] =
            paramsXml;
    }
}
