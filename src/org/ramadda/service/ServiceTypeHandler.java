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
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.HtmlOutputHandler;
import org.ramadda.repository.output.ServiceOutputHandler;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class ServiceTypeHandler extends OrderedGroupTypeHandler {

    /** _more_ */
    public static final String TYPE_SERVICE = "type_service";

    private static int IDX = 0;


    /** _more_ */
    public static final int IDX_SORT_ORDER = IDX++;

    /** _more_ */
    public static final int IDX_PARAMETERS = IDX++;

    /** _more_ */
    public static final int IDX_LAST = IDX_PARAMETERS;


    /** _more_ */
    private static final ServiceLinkTypeHandler dummy1 = null;

    /** _more_ */
    private static final ServiceFileTypeHandler dummy2 = null;

    /** _more_ */
    private static final ServiceContainerTypeHandler dummy3 = null;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ServiceTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getChildType() {
        return TYPE_SERVICE;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> postProcessEntries(Request request,
                                          List<Entry> entries)
            throws Exception {
        List<Entry> sorted =
            getEntryManager().getEntryUtil().sortEntriesOnField(entries,
                false, TYPE_SERVICE, 0);

        return sorted;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public String getListTitle() {
        return "Services";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry entry,
                                 Entries children)
            throws Exception {
        Service service = getService(request, entry);
        if (service == null) {
            return null;
        }
        StringBuilder xml = new StringBuilder();
        service.toXml(xml, null);

        ServiceOutputHandler soh = new ServiceOutputHandler(repository,
                                       service);
        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, null);


        addListForm(request, entry, children.get(), sb);

        String params = entry.getStringValue(request,IDX_PARAMETERS, "");

        if (Utils.stringDefined(params)) {
            Element  root  = XmlUtil.getRoot(params);
            NodeList nodes = XmlUtil.getElements(root, Service.TAG_PARAM);
            if (nodes.getLength() > 0) {
                request = request.cloneMe();
            }
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                request.put(XmlUtil.getAttribute(node, Service.ATTR_NAME),
                            XmlUtil.getChildText(node));
            }
            System.err.println("params:" + params);
        }


        if ( !soh.doExecute(request)) {
            soh.makeForm(request, service, entry, children.get(),
                         HtmlOutputHandler.OUTPUT_HTML, sb);

            getPageHandler().entrySectionClose(request, entry, sb);

            return new Result("", sb);
        }

        return soh.evaluateService(request, getRepository().URL_ENTRY_SHOW,
                                   HtmlOutputHandler.OUTPUT_HTML, entry,
                                   children.get(), service, "");

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        Service service = getService(request, entry);
        System.err.println("service:" + service);
        if (service == null) {
            return null;
        }
        ServiceOutputHandler soh = new ServiceOutputHandler(repository,
                                       service);
        StringBuilder sb = new StringBuilder();
        if ( !soh.doExecute(request)) {
            soh.makeForm(request, service, entry, null,
                         HtmlOutputHandler.OUTPUT_HTML, sb);

            return new Result("", sb);
        }

        return soh.evaluateService(request, getRepository().URL_ENTRY_SHOW,
                                   HtmlOutputHandler.OUTPUT_HTML, entry,
                                   null, service, "");
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
            throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Service getService(Request request, Entry entry) throws Exception {
        StringBuilder sb = new StringBuilder();
        getServiceXml(request, entry, sb);
        if (sb.length() == 0) {
            return null;
        }
        Element root = XmlUtil.getRoot(sb.toString());
        if (root.getTagName().equals(Service.TAG_SERVICES)) {
            root = XmlUtil.findChild(root, Service.TAG_SERVICE);
        }
        Service service = getRepository().makeService(root, false);

        service.setServiceEntry(entry);
        //IMPORTANT! Always do this because we don't allow a service xml entry file to have commands
        service.ensureSafeServices();
        if (Utils.stringDefined(entry.getLabel())) {
            service.setLabel(entry.getLabel());
        }

        return service;

    }


}
