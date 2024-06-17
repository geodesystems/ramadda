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

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class ServiceLinkTypeHandler extends ServiceTypeHandler {

    /** _more_ */
    private  static int IDX = ServiceTypeHandler.IDX_LAST + 1;

    public static final int IDX_LINK_ID = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ServiceLinkTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param column _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param values _more_
     * @param state _more_
     * @param formInfo _more_
     * @param baseHandler _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry parentEntry,Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler baseHandler)
            throws Exception {
        if ( !column.getName().equals("service_id")) {
            super.addColumnToEntryForm(request, column, formBuffer, parentEntry, entry,
                                       values, state, formInfo, baseHandler);

            return;
        }
        List<Service> services =
            getRepository().getJobManager().getServices();
        List<HtmlUtils.Selector> items = new ArrayList<HtmlUtils.Selector>();
        for (Service service : services) {
            items.add(new HtmlUtils.Selector(service.getLabel(),
                                             service.getId(),
                                             getIconUrl(service.getIcon())));
        }

        formBuffer.append(HU.formEntry(msgLabel("Service"),
				       HU.select(column.getEditArg(), items,
						 entry!=null?entry.getStringValue(request,column,""):"")));
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

        if ( !Utils.stringDefined(entry.getName())) {
            Service service = getService(getRepository().getTmpRequest(),
                                         entry);
            if (service != null) {
                if (service.getLink() != null) {
                    entry.setName(service.getLink().getLabel());
                } else {
                    entry.setName(service.getLabel());
                }
            }
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param tmpSb _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void formatColumnHtmlValue(Request request, Entry entry,
                                      Column column, Appendable tmpSb,
                                      Object[] values)
            throws Exception {
        if ( !column.getName().equals("service_id")) {
            super.formatColumnHtmlValue(request, entry, column, tmpSb,
                                        values);

            return;
        }
        Service service = getService(request, entry);
        if (service == null) {
            return;
        }
        tmpSb.append(service.getLabel());
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
    public Service getService(Request request, Entry entry) throws Exception {
        Service service = new Service(getRepository(), entry.getId(),
                                      entry.getName());
        service.setLinkId(entry.getStringValue(request,IDX_LINK_ID, ""));

        if (Utils.stringDefined(entry.getLabel())) {
            service.setLabel(entry.getLabel());
        }

        return service;
    }


}
