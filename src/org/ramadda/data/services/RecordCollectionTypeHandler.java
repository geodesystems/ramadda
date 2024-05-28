/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


import org.ramadda.data.record.RecordField;
import org.ramadda.data.services.*;


import org.ramadda.repository.*;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;


import java.util.Date;
import java.util.HashSet;
import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public abstract class RecordCollectionTypeHandler extends ExtensibleGroupTypeHandler implements RecordConstants {

    /** _more_ */
    public static final String METADATA_URL = "nlas_url";

    /** _more_ */
    public static final String TAG_FIELDS = "fields";

    /** _more_ */
    public static final String TAG_FIELD = "field";

    /** _more_ */
    public static final String TAG_ATTRIBUTE = "attribute";

    /** _more_ */
    public static final String ATTR_DATATYPE = "datatype";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_VALUE = "value";




    /**
     * ctor
     *
     * @param repository the repository
     * @param node the xml node from the types.xml file
     * @throws Exception On badness
     */
    public RecordCollectionTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public abstract RecordOutputHandler getRecordOutputHandler();


    /** _more_ */
    private HashSet entryChanged = new HashSet();

    /**
     * notify that one of the children has changed. This recalculates the bounding box of the parent entry
     *
     * @param entry the grouop entry
     * @param isNew is this a new entry
     *
     * @throws Exception On badness
     */
    @Override
    public void childEntryChanged(Request request,Entry entry, boolean isNew)
            throws Exception {
        super.childEntryChanged(request,entry, isNew);
        //for now get out of here to fix an infinite loop problem
        if (true) {
            return;
        }
        if (entryChanged.contains(entry.getId())) {
            return;
        }
        entryChanged.add(entry.getId());
        Entry parent = entry.getParentEntry();
        List<Entry> children =
            getEntryManager().getChildren(getRepository().getTmpRequest(),
                                          parent);
        //For good measure
        children.add(entry);
        getEntryManager().setBoundsOnEntry(request,parent, children);
        entryChanged.remove(entry.getId());
    }





    /**
     * _more_
     *
     * @param collectionEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordEntry getChildRecordEntry(Entry collectionEntry)
            throws Exception {
        Request tmpRequest = getRepository().getTmpRequest();
        List<Entry> children =
            getEntryManager().getChildrenEntries(tmpRequest, collectionEntry);
        for (Entry child : children) {
            if (child.getTypeHandler()
                    instanceof RecordCollectionTypeHandler) {
                return getRecordOutputHandler().doMakeEntry(tmpRequest,
                        child);
            }
        }

        return null;
    }


    /**
     * This method gets called when we are creating some xml encoding of the entry.
     *
     * @param collectionEntry the entry
     * @param root xml root
     * @param extraXml buffer to tack on extra xml
     * @param metadataType Specifies what xml metadata we are creating, e.g., "atom", "rss"
     */
    public void addMetadataToXml(Entry collectionEntry, Element root,
                                 StringBuffer extraXml, String metadataType) {
        try {
            if (metadataType.equals("atom")) {
                addMetadataToAtomXml(collectionEntry, root);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * add ATOM metadata for the given collectionEntry
     *
     * @param collectionEntry the collection entry
     * @param root xml document root
     *
     * @throws Exception on badness
     */
    public void addMetadataToAtomXml(Entry collectionEntry, Element root)
            throws Exception {
        Document doc = root.getOwnerDocument();
        Element temporalCoverage =
            XmlUtil.create(AtomUtil.TAG_GML_TIMEPERIOD, root);
        XmlUtil.create(
            doc, AtomUtil.TAG_GML_BEGIN, temporalCoverage,
            Utils.format(new Date(collectionEntry.getStartDate())), null);
        XmlUtil.create(doc, AtomUtil.TAG_GML_END, temporalCoverage,
                       Utils.format(new Date(collectionEntry.getEndDate())),
                       null);

        //Get one of the children entries so we can access the RecordField list
        RecordEntry childEntry = getChildRecordEntry(collectionEntry);
        if (childEntry != null) {
            /*
<parameter name="RELIABILITY" datatype="r4b">
  <attribute name="label" value="Reliability" />
  <attribute name="description" value="blah blah" />
  <attribute name="units" value="" />
  <attribute name="minimum" value="0" />
  <attribute name="maximum" value="254" />
  <attribute name="searchable" value="true" />
 </parameter>
            */
            Element fieldsNode       = XmlUtil.create(doc, TAG_FIELDS, root);
            List<RecordField> fields = childEntry.getRecordFile().getFields();
            for (RecordField field : fields) {
                //                System.err.println ("field: " +field);
                //Add the xml node to the above fieldsNode with a name attribute
                Element fieldNode = XmlUtil.create(doc, TAG_FIELD,
                                        fieldsNode, new String[] { ATTR_NAME,
                        field.getName() });
                String[][] attrs = new String[][] {
                    { "label", field.getLabel() },
                    { "datatype", field.getRawType() },
                    { "javatype", field.getTypeName() },
                    { "description", field.getDescription() },
                    { "units", field.getUnit() },
                    { "searchable", field.getSearchable()
                                    ? "" + field.getSearchable()
                                    : null }, { "searchlabel",
                                        field.getSearchable()
                                        ? field.getSearchSuffix()
                                        : null }, { "chartable",
                                            field.getChartable()
                                            ? "" + field.getChartable()
                                            : null }, { "arity",
                                                (field.getArity() > 1)
                            ? "" + field.getArity()
                            : null },
                    //                    {"",field.get()},
                };

                for (String[] pair : attrs) {
                    if ((pair[1] != null) && (pair[1].length() > 0)) {
                        XmlUtil.create(doc, TAG_ATTRIBUTE, fieldNode,
                                       new String[] { ATTR_NAME,
                                pair[0], ATTR_VALUE, pair[1] });
                    }
                }

            }


        }
    }



    /**
     * utility to make a string macro
     *
     * @param s string
     *
     * @return macro
     */
    public String macro(String s) {
        return "${" + s + "}";
    }

    /**
     * get the icon url
     *
     *
     * @param request _more_
     * @param icon icon
     *
     * @return url
     */
    public String getAbsoluteIconUrl(Request request, String icon) {
        return request.getAbsoluteUrl(getRepository().getIconUrl(icon));
    }


}
