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

package org.ramadda.geodata.mb;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.*;



import org.ramadda.service.Service;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.Utils;



import org.w3c.dom.*;

import ucar.nc2.units.DateUnit;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;


import java.util.Date;
import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class MbTypeHandler extends GenericTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public MbTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception On badness
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {
        Object[] values = getEntryValues(entry);
        String suffix =
            IOUtil.getFileExtension(entry.getResource().getPath());
        suffix    = suffix.replace(".mb", "");
        values[0] = suffix;
        super.initializeNewEntry(request, entry);
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param service _more_
     * @param output _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void handleServiceResults(Request request, Entry entry,
                                     Service service, ServiceOutput output)
            throws Exception {
        super.handleServiceResults(request, entry, service, output);

        List<Entry> entries = output.getEntries();
        if (entries.size() == 0) {
            return;
        }

        String xml = null;

        for (Entry newEntry : entries) {
            String file = newEntry.getFile().toString();
            //            System.err.println("File:" + file);
            if (file.endsWith(".xml")) {
                xml = IOUtil.readContents(file, getClass(), "");

                break;
            }
        }
        if (xml == null) {
            //            System.err.println("No xml file found");
            return;
        }

        Element root = null;

        //        System.err.println(xml);
        try {
            root = XmlUtil.getRoot(xml);
        } catch (Exception exc) {
            xml  = Utils.removeNonAscii(xml);
            root = XmlUtil.getRoot(xml);
        }
        Element limits   = XmlUtil.findChild(root, MbUtil.TAG_LIMITS);

        Element fileInfo = XmlUtil.findChild(root, MbUtil.TAG_FILE_INFO);
        String desc = XmlUtil.getGrandChildText(fileInfo,
                          MbUtil.TAG_INFORMAL_DESCRIPTION);

        desc = Utils.removeNonAscii(desc);
        entry.setDescription(desc);

        for (String attr :
                StringUtil.split(XmlUtil.getGrandChildText(fileInfo,
                    MbUtil.TAG_ATTRIBUTES), ",", true, true)) {
            getMetadataManager().addMetadata(
                entry,
                new Metadata(
                    getRepository().getGUID(), entry.getId(), "enum_tag",
                    false, attr, "", "", "", ""));

        }

        entry.setNorth(Double.parseDouble(XmlUtil.getGrandChildText(limits,
                MbUtil.TAG_MAXIMUM_LATITUDE)));
        entry.setSouth(Double.parseDouble(XmlUtil.getGrandChildText(limits,
                MbUtil.TAG_MINIMUM_LATITUDE)));
        entry.setEast(Double.parseDouble(XmlUtil.getGrandChildText(limits,
                MbUtil.TAG_MAXIMUM_LONGITUDE)));
        entry.setWest(Double.parseDouble(XmlUtil.getGrandChildText(limits,
                MbUtil.TAG_MINIMUM_LONGITUDE)));


        Element startOfData = XmlUtil.findChild(root,
                                  MbUtil.TAG_START_OF_DATA);
        Element endOfData = XmlUtil.findChild(root, MbUtil.TAG_END_OF_DATA);


        Date startDate =
            DateUnit.getStandardOrISO(XmlUtil.getGrandChildText(startOfData,
                MbUtil.TAG_TIME_ISO));
        Date endDate =
            DateUnit.getStandardOrISO(XmlUtil.getGrandChildText(endOfData,
                MbUtil.TAG_TIME_ISO));
        entry.setStartDate(startDate.getTime());
        entry.setEndDate(endDate.getTime());


        int numberOfRecords =
            Integer.parseInt(
                XmlUtil.getGrandChildText(
                    XmlUtil.findChild(root, MbUtil.TAG_DATA_TOTALS),
                    MbUtil.TAG_NUMBER_OF_RECORDS));

        Object[] values = getEntryValues(entry);
        values[1] = new Integer(numberOfRecords);

        Element navTotals = XmlUtil.findChild(root,
                                MbUtil.TAG_TNAVIGATION_TOTALS);
        values[2] = new Double(XmlUtil.getGrandChildText(navTotals,
                MbUtil.TAG_TOTAL_TRACK_LENGTH_KM));

    }





}
