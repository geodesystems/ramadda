/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
