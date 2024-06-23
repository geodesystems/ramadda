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

package org.ramadda.bio.image;


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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class DicomTypeHandler extends GenericTypeHandler {

    /** _more_ */
    private Hashtable<String, Tag> tags = new Hashtable<String, Tag>();

    /** _more_ */
    private HashSet<String> metadataTags = new HashSet<String>();




    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public DicomTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        init(node);
    }


    /** _more_ */
    public static final String TAG_ATTR = "attr";

    /** _more_ */
    public static final String ATTR_TAG = "tag";

    /** _more_ */
    public static final String ATTR_VR = "vr";

    /** _more_ */
    public static final String ATTR_LEN = "len";

    /** _more_ */
    private static final String DEFAULT_TAGS = "00100010,00100020";

    /**
     * _more_
     *
     * @param node _more_
     *
     * @throws Exception _more_
     */
    private void init(Element node) throws Exception {
        List<String> lines =
            StringUtil.split(
                getRepository().getResource(
                    "/org/ramadda/bio/image/dicomtags.txt"), "\n", true,
                        true);
        for (int i = 0; i < lines.size(); i += 2) {
            Tag tag = new Tag(lines.get(i), lines.get(i + 1));
            tags.put(tag.id, tag);
            tags.put(tag.name, tag);
        }

        String metadataString =
            getTypeProperty("dicom.metadata",
                            getRepository().getProperty("dicom.metadata",
                                ""));
        for (String tok : StringUtil.split(metadataString, ",", true, true)) {
            Tag tag = getTag(tok);
            metadataTags.add(tag.id);
        }
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
        String filename = entries.get(0).getFile().toString();
        if ( !filename.endsWith(".xml")) {
            return;
        }
        String xml = IOUtil.readContents(filename, getClass(), "");
        Element                   root       = root = XmlUtil.getRoot(xml);
        NodeList children = XmlUtil.getElements(root, TAG_ATTR);
        Hashtable<String, String> tagToValue = new Hashtable<String,
                                                   String>();







        //<attr tag="00020000" vr="UL" len="4">194</attr>
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element item = (Element) children.item(childIdx);
            String tagId = XmlUtil.getAttribute(item, ATTR_TAG,
                               (String) null);
            Tag tag = getTag(tagId);
            if (tag == null) {
                continue;
            }
            String value = XmlUtil.getChildText(item);
            tagToValue.put(tag.id, value);
            tagToValue.put(tag.name, value);
            if (metadataTags.contains(tag.id)
                    || metadataTags.contains(tag.name)) {
                Metadata metadata = new Metadata(getRepository().getGUID(),
						 entry.getId(), getMetadataManager().findType("bio_dicom_attr"),
                                        false, tag.name, value, null, null,
                                        null);

                getMetadataManager().addMetadata(request,entry, metadata);
            }
        }

        Object[]     values  = getEntryValues(entry);
        List<Column> columns = getColumns();
        for (Column column : columns) {
            String value = tagToValue.get(column.getName());
            if ((value == null) && (column.getAlias() != null)) {
                value = tagToValue.get(column.getAlias());
            }
            if (value == null) {
                continue;
            }
            column.setValue(entry, values, value);
        }

        String dateString = null;
        String timeString = null;

        //Look for date/time
        for (String[] pair : new String[][] {
            { "00080022", "00080032" }, { "00080020", "00080030" }
        }) {
            dateString = tagToValue.get(pair[0]);
            if (dateString == null) {
                continue;
            }
            timeString = tagToValue.get(pair[1]);

            break;
        }


        Date date = null;
        if (dateString != null) {
            dateString = dateString.replaceAll("\\.", "");
            //For now don't parse the time as there are a number of formats that I've seen
            timeString = null;
            if (timeString != null) {
                timeString = timeString.replaceAll(":", "");
            }
            try {
                if (timeString != null) {
                    date = RepositoryUtil.makeDateFormat(
                        "yyyyMMdd HHmmss").parse(
                        dateString + " " + timeString);
                } else {
                    date = RepositoryUtil.makeDateFormat("yyyyMMdd").parse(
                        dateString);
                }
            } catch (Exception exc) {
                getLogManager().logError("Dicom. parsing date:" + exc
                                         + " date:" + dateString + " time:"
                                         + timeString);
            }
        }

        if (date != null) {
            entry.setStartDate(date.getTime());
            entry.setEndDate(date.getTime());
        }


    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public Tag getTag(String s) {
        if (s == null) {
            return null;
        }

        return tags.get(s);
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Oct 2, '14
     * @author         Enter your name here...
     */
    public static class Tag {

        /** _more_ */
        String id;

        /** _more_ */
        String name;

        /**
         * _more_
         *
         * @param id _more_
         * @param name _more_
         */
        public Tag(String id, String name) {
            this.id   = id;
            this.name = name;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return id + ":" + name;
        }

    }


}
