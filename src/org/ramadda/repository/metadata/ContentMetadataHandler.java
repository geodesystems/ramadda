/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;

import org.ramadda.repository.output.OutputType;

import org.ramadda.repository.*;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.util.HtmlTemplate;
import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.TwoFacedObject;
import org.w3c.dom.*;


import ucar.unidata.util.Misc;


import java.awt.Image;


import java.io.*;

import java.net.URL;
import java.net.URLConnection;



import java.util.Comparator;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class ContentMetadataHandler extends MetadataHandler {


    public static final String TYPE_PROPERTY = "property";

    /** _more_ */
    public static final String TYPE_THUMBNAIL = "content.thumbnail";

    /** _more_ */
    public static final String TYPE_TOOLS = "output_tools";
    
    /** _more_ */
    public static final String TYPE_ICON = "content.icon";

    /** _more_ */
    public static final String TYPE_ATTACHMENT = "content.attachment";

    /** _more_ */
    public static final String TYPE_PAGESTYLE = "content.pagestyle";

    /** _more_ */
    public static final String TYPE_KEYWORD = "content.keyword";

    /** _more_ */
    public static final String TYPE_URL = "content.url";

    /** _more_ */
    public static final String TYPE_EMAIL = "content.email";

    /** _more_ */
    public static final String TYPE_AUTHOR = "content.author";

    /** _more_ */
    public static final String TYPE_LOGO = "content.logo";

    /** _more_ */
    public static final String TYPE_JYTHON = "content.jython";

    /** _more_ */
    public static final String TYPE_CONTACT = "content.contact";

    /** _more_ */
    public static final String TYPE_SORT = "content.sort";

    /** _more_ */
    public static final String TYPE_TIMEZONE = "content.timezone";

    /** _more_ */
    public static final String TYPE_ALIAS = "content.alias";

    /** _more_ */
    public static final String TYPE_TEMPLATE = "content.pagetemplate";

    /**  */
    public static final String TYPE_LICENSE = "content.license";

    /** _more_ */
    public static final String TYPE_TAG = "enum_tag";

    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public ContentMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        if (metadata.getType().equals(TYPE_LICENSE)) {
            String license    = metadata.getAttr1();
	    String desc = metadata.getAttr2();
	    String requireId = metadata.getAttr3();
	    String requireMessage = metadata.getAttr4();
	    String requireUrl = metadata.getAttr(5);
	    String logName = metadata.getAttr(6);
            MetadataType    type       = getType(metadata.getType());
            MetadataElement element    = type.getChildren().get(0);
            String          label      = element.getLabel(license);
            String          searchLink = getSearchLink(request, metadata,"");

            String contents = getMetadataManager().getLicenseHtml(license,
								  label,false);

	    StringBuilder wiki = new StringBuilder();
	    wiki.append("{{license license=\"" + license +"\" includeName=true decorate=true ");
	    wiki.append("showDescription=true ");
	    if(stringDefined(desc)) {
		wiki.append("textBefore=\"" + desc.replace("\n","<br>")+"\" ");
	    }
	    if(stringDefined(requireId)) {
		wiki.append("required=\"" + requireId+"\" ");
	    }
	    if(stringDefined(requireMessage)) {
		wiki.append("requireMessage=\"" + requireMessage+"\" ");
	    }
	    if(stringDefined(requireUrl)) {
		wiki.append("requireRedirect=\"" + requireUrl+"\" ");
	    }
	    if(stringDefined(logName)) {
		wiki.append("logName=\"" + logName+"\" ");
	    }	    	    
	    
	    wiki.append("}}");
            return new String[] { "License:&nbsp;",getWikiManager().wikifyEntry(request, entry, wiki.toString())};
        }


        if (metadata.getType().equals(TYPE_ALIAS)) {
            Hashtable props =
                (Hashtable) request.getExtraProperty("wiki.props");
            String title = "Alias";
            String a     = metadata.getAttr1();
            String label = a;
            if (props != null) {
                title = Misc.getProperty(props, "title", title);
                label = Misc.getProperty(props, "label", label);
            }

            if (a.startsWith("http:")) {
                return new String[] { title, HtmlUtils.href(a, label) };
            }
        }




        return super.getHtml(request, entry, metadata);
    }


    /**
     * _more_
     *
     * @param element _more_
     *
     * @return _more_
     */
    @Override
    public String getEnumerationValues(MetadataElement element) {
        if (element.getName().equals("entrytype")) {
	    try {
		StringBuffer sb = new StringBuffer("values:");
		List<TypeHandler> typeHandlers = getRepository().getTypeHandlersForDisplay(false);
		//Do the file types first
		for(int i=0;i<2;i++) {
		    for(TypeHandler typeHandler: typeHandlers) {
			if(i==0 && typeHandler.isGroup()) continue;
			if(i==1 && !typeHandler.isGroup()) continue;			
			sb.append(typeHandler.getType());
			sb.append(":");
			sb.append(typeHandler.getDescription());
			sb.append(",");
		    }
		}
		return sb.toString();
	    } catch(Exception exc) {
		throw new RuntimeException(exc);

	    }
	}

        if (element.getName().equals("template")) {
            StringBuffer sb = new StringBuffer();
            for (HtmlTemplate htmlTemplate :
                    getRepository().getPageHandler().getTemplates()) {
                sb.append(htmlTemplate.getId());
                sb.append(":");
                sb.append(htmlTemplate.getName());
                sb.append(",");
            }

            return sb.toString();
        }

	try {
	    if (element.getName().equals("output_type")) {
		StringBuffer sb = new StringBuffer();
		List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
		for(OutputType type:getRepository().getOutputTypes()) {
		    tfos.add(new TwoFacedObject(type.getLabel(), type.getId()));
		}

		tfos.sort(new Comparator() {
			public int compare(Object o1, Object o2) {
			    String s1 = (String) ((TwoFacedObject) o1).getLabel();
			    String s2 = (String) ((TwoFacedObject) o2).getLabel();
			    return s1.compareToIgnoreCase(s2);
			}
		    });

	    
		for(TwoFacedObject tfo: tfos) {
		    sb.append(tfo.getId());
		    sb.append(":");
		    sb.append(tfo.getLabel());
		    sb.append(",");
		}
		return sb.toString();
	    }
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
	
        return "";
    }

}
