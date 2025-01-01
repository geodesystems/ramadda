/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;


import org.ramadda.repository.*;


import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;




import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class AdminMetadataHandler extends MetadataHandler {


    public static final String TYPE_PREVENTDELETION = "preventdeletion";


    /** _more_ */
    public static final String TYPE_TEMPLATE = "admin.template";

    /** _more_ */
    public static final String TYPE_CONTENTTEMPLATE = "admin.contenttemplate";

    /** _more_ */
    public static final String TYPE_LOCALFILE_PATTERN =
        "admin.localfile.pattern";

    /** _more_ */
    public static final String TYPE_ANONYMOUS_UPLOAD =
        "admin.anonymousupload";

    public static final String TYPE_ENTRY_TYPE =
        "admin.entrytype";    



    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public AdminMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        String[] result = super.getHtml(request, entry, metadata);
        if (result != null) {
            return result;
        }
        MetadataType type = findType(metadata.getType());
        if (type == null) {
            return null;
        }
        String lbl = msgLabel(type.getLabel());
        if (type.isType(TYPE_TEMPLATE) || type.isType(TYPE_CONTENTTEMPLATE)) {
            return new String[] { lbl, "Has template" };
        }


        if (type.isType(TYPE_LOCALFILE_PATTERN)) {
            return new String[] { lbl, "Local File Pattern" };
        }

        String content = metadata.getAttr1();

        //        return new String[] { lbl, content };
        return null;
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getForm(Request request, Entry entry, Metadata metadata,
                            boolean forEdit)
            throws Exception {
        MetadataType type   = findType(metadata.getType());
        String       lbl    = msgLabel(type.getLabel());
        String       id     = metadata.getId();
        String       suffix = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }
        String submit = (forEdit
                         ? ""
                         : HtmlUtils.submit("Add" + HtmlUtils.space(1)
                                            + lbl));
        String cancel  = (forEdit
                          ? ""
                          : HtmlUtils.submit(LABEL_CANCEL, ARG_CANCEL));
        String arg1    = ARG_ATTR1 + suffix;
        String content = "";
        if (type.isType(TYPE_TEMPLATE)) {
            String value = metadata.getAttr1();
            if ( !forEdit || (value == null)) {
                value = getRepository().getResource(PROP_HTML_TEMPLATE);
            }
            if (value == null) {
                value = "";
            }
            value = value.replace("<", "&lt;");
            value = value.replace(">", "&gt;");
            value = value.replace("$", "&#36;");
            String textarea = HtmlUtils.textArea(arg1, value, 20, 80);
            content =
                HtmlUtils.row(HtmlUtils.colspan(submit, 2))
                + HtmlUtils.formEntry(lbl,
                                      "Note: must contain macro ${content}"
                                      + "<br>" + textarea);
        }
        if (type.isType(TYPE_LOCALFILE_PATTERN)) {
            if ((metadata.getEntry() == null)
                    || !metadata.getEntry().getIsLocalFile()) {
                return null;
            }
            String value = metadata.getAttr1();
            String input = HtmlUtils.input(arg1, value);
            content = HtmlUtils.row(HtmlUtils.colspan(submit, 2))
                      + HtmlUtils.formEntry(lbl, input);
        }
        if (type.isType(TYPE_ANONYMOUS_UPLOAD)) {
            content = "From:" + metadata.getAttr1() + " IP: "
                      + metadata.getAttr2();
        }


        if ( !forEdit) {
            content = content + HtmlUtils.row(HtmlUtils.colspan(cancel, 2));
        }
        String argtype = ARG_METADATA_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtils.hidden(argtype, type.getId())
                  + HtmlUtils.hidden(argid, metadata.getId());

        return new String[] { lbl, content };
    }






}
