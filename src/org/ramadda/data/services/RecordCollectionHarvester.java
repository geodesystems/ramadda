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

package org.ramadda.data.services;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


import java.util.regex.*;


/**
 * This class extends the RAMADDA file harvester to harvest Point data files.
 * It does a couple of things. It overrides getLastGroupType() to specify the
 * PointCollection type. This way the RAMADDA entry folders  that get created
 * will be of this type.
 * Secondly, for text point files it looks for the <filename>.properties file which
 * is our way of simply describing the CRS of the data file (e.g., UTM, geographic, etc).
 *
 * @author Jeff McWhirter
 */
public abstract class RecordCollectionHarvester extends PatternHarvester {

    /** _more_ */
    private boolean makeRecordCollection = true;

    /** _more_ */
    private static final String ATTR_MAKERECORDCOLLECTION =
        "makerecordcollection";

    /**
     * ctor
     *
     * @param repository the repository
     * @param id harvester id
     *
     * @throws Exception on badness
     */
    public RecordCollectionHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
    }

    /**
     * ctor
     *
     * @param repository the repository
     * @param element xml node that defines this harvester
     *
     * @throws Exception on badness
     */
    public RecordCollectionHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }



    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    protected void init(Element element) throws Exception {
        super.init(element);

        makeRecordCollection = XmlUtil.getAttribute(element,
                ATTR_MAKERECORDCOLLECTION, makeRecordCollection);
    }


    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        element.setAttribute(ATTR_MAKERECORDCOLLECTION,
                             "" + makeRecordCollection);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getMakeRecordCollection() {
        return makeRecordCollection;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);

        if (request.exists(ATTR_MAKERECORDCOLLECTION)) {
            makeRecordCollection = request.get(ATTR_MAKERECORDCOLLECTION,
                    makeRecordCollection);
        } else {
            makeRecordCollection = false;
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        super.createEditForm(request, sb);
        sb.append(
            HtmlUtils.formEntry(
                msgLabel("Make Record Collection"),
                HtmlUtils.checkbox(
                    ATTR_MAKERECORDCOLLECTION, "true",
                    makeRecordCollection)));
    }

    /**
     * This makes the list of entry types that can be created.
     *
     * @param request _more_
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeEntryTypeSelector(Request request,
                                        TypeHandler typeHandler)
            throws Exception {
        Class typeHandlerClass = getTypeHandlerClass();
        if (typeHandlerClass == null) {
            return super.makeEntryTypeSelector(request, typeHandler);
        }
        String selected = typeHandler.getType();
        List   tmp      = new ArrayList();
        for (TypeHandler th : getRepository().getTypeHandlers()) {
            if (typeHandlerClass.isAssignableFrom(th.getClass())) {
                tmp.add(new TwoFacedObject(th.getLabel(), th.getType()));
            }
        }

        return HtmlUtils.select(ARG_TYPE, tmp, selected);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Class getTypeHandlerClass() {
        return null;
    }


    /**
     * harvester description
     *
     * @return harvester description
     */
    public String getDescription() {
        return "Record Collection";
    }


    /**
     * Should this harvester harvest the given file
     *
     * @param fileInfo file information
     * @param f the actual file
     * @param matcher pattern matcher
     *
     * @return the new entry or null if nothing is harvested
     *
     * @throws Exception on badness
     */
    @Override
    public Entry harvestFile(HarvesterFile fileInfo, File f, Matcher matcher)
            throws Exception {
        if (f.toString().endsWith(".properties")) {
            return null;
        }

        return super.harvestFile(fileInfo, f, matcher);
    }



    /**
     * Check for a .properties file that corresponds to the given data file.
     * If it exists than add it to the entry
     *
     * @param fileInfo File information
     * @param originalFile Data file
     * @param entry New entry
     *
     * @return The entry
     */
    @Override
    public Entry initializeNewEntry(HarvesterFile fileInfo,
                                    File originalFile, Entry entry) {
        try {
            getRepository().getLogManager().logInfo(
                "RecordCollectonHarvester:initializeNewEntry:"
                + entry.getResource());
            if (entry.getTypeHandler() instanceof RecordTypeHandler) {
                ((RecordTypeHandler) entry.getTypeHandler())
                    .initializeRecordEntry(entry, originalFile,false);
            }
            getRepository().getLogManager().logInfo(
                "RecordCollectonHarvester:initializeNewEntry done");

            return entry;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

}
