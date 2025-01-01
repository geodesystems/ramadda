/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

import org.ramadda.util.geo.KmlUtil;

import org.w3c.dom.*;

import ucar.unidata.gis.*;
import ucar.unidata.gis.shapefile.*;
import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;

import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 *
 *
 */
public class KmlEntryOutputHandler extends ZipFileOutputHandler {
    public static final KmlUtil KU=null;


    /** Map output type */
    public static final OutputType OUTPUT_KML_HTML =
        new OutputType("Display as HTML", "kml.html", OutputType.TYPE_VIEW,
                       "", ICON_KML);

    /** _more_ */
    public static final OutputType OUTPUT_KMZ_IMAGE =
        new OutputType("Display as HTML", "kml.image", OutputType.TYPE_VIEW,
                       "", ICON_KML);

    public static final OutputType OUTPUT_KML_EXTRACT =
        new OutputType("Display as HTML", "kml.extract", OutputType.TYPE_ACTION,
                       "", ICON_KML);

    public static final OutputType OUTPUT_KML_DOC =
        new OutputType("Display as HTML", "kml.doc", OutputType.TYPE_ACTION,
                       "", ICON_KML);    




    /**
     * Create a MapOutputHandler
     *
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public KmlEntryOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element,true);
        addType(OUTPUT_KML_HTML);
        addType(OUTPUT_KML_EXTRACT);
        addType(OUTPUT_KML_DOC);		
    }



    /**
     * Get the entry links
     *
     * @param request  the Request
     * @param state    the repository State
     * @param links    the links
     *
     * @throws Exception  problem creating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.entry == null) {
            return;
        }
        if (state.entry.getTypeHandler().isType("geo_kml")) {
            links.add(makeLink(request, state.entry, OUTPUT_KML_HTML));
        }
    }


    /**
     * Output the entry
     *
     * @param request      the Request
     * @param outputType   the type of output
     * @param entry        the Entry to output
     *
     * @return  the Result
     *
     * @throws Exception  problem outputting entry
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if (outputType.equals(OUTPUT_KML_EXTRACT)) {
            return outputKmlExtract(request, entry);
	}

        if (outputType.equals(OUTPUT_KML_DOC)) {
            return outputKmlDoc(request, entry);
	}
	

        if (outputType.equals(OUTPUT_KML_HTML)) {
            return outputKmlHtml(request, entry);
        }

        return null;
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
    private Result outputKmlHtml(Request request, Entry entry)
            throws Exception {

        StringBuffer sb   = new StringBuffer();
        Element      root = KmlTypeHandler.readKml(getRepository(), entry);
        if (root == null) {
            sb.append(
                getPageHandler().showDialogError(
                    "Could not read KML/KMZ file"));

            return new Result("KML/KMZ Error", sb);
        }
        getPageHandler().entrySectionOpen(request, entry, sb, "KML Display");
	sb.append("<ul>");
        walkTree(request, entry, sb, root);
	sb.append("</ul>");
        getPageHandler().entrySectionClose(request, entry, sb);
        Result result = new Result("", sb);

        return result;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param node _more_
     */
    private void walkTree(Request request, Entry entry, StringBuffer sb,
                          Element node) {
        String tagName = node.getTagName();
        if (tagName.equals(KU.TAG_KML)) {
            walkChildren(request, entry, sb, node);
            return;
        }

        if (tagName.equals(KU.TAG_FOLDER)
                || tagName.equals(KU.TAG_DOCUMENT)
                || tagName.equals(KU.TAG_TOUR)) {
            //TODO: encode the text
            sb.append("<li> ");
            appendName(node, sb, tagName);
            sb.append("<ul>");
            walkChildren(request, entry, sb, node);
            sb.append("</ul>");
        } else if (tagName.equals(KU.TAG_PLACEMARK)) {
            sb.append("<li> ");
            appendName(node, sb, tagName);
        } else if (tagName.equals(KU.TAG_GROUNDOVERLAY)) {
            sb.append("<li> ");
	    Element iconNode = XmlUtil.findChild(node,KU.TAG_ICON);
	    if(iconNode!=null) {
		String href = XmlUtil.getGrandChildText(iconNode, KU.TAG_HREF,null);
		if(href!=null) {
		    String name = IOUtil.getFileTail(href);
		    appendName(node, sb, tagName);
		    String url = repository.URL_ENTRY_SHOW + "/" + name;
		    url = HtmlUtils.url(url, ARG_ENTRYID, entry.getId(),
					ARG_FILE, href, ARG_OUTPUT,
					OUTPUT_LIST.getId());
		    sb.append("<br>");
		    sb.append(HU.image(url,HU.attr("width","500px")));
		}
	    }
        } else {
            //            sb.append("<li> ");
            //            sb.append(tagName);
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param sb _more_
     * @param tagName _more_
     */
    private void appendName(Node node, StringBuffer sb, String tagName) {
        sb.append(tagName + ": ");
        sb.append(XmlUtil.getGrandChildText(node, KU.TAG_NAME, tagName));
        String desc = XmlUtil.getGrandChildText(node,
                          KU.TAG_DESCRIPTION, null);
        if (desc != null) {
            sb.append(HtmlUtils.div(desc,
                                    HtmlUtils.cssClass("kml-description")));
        }

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param node _more_
     */
    private void walkChildren(Request request, Entry entry, StringBuffer sb,
                              Element node) {
        NodeList children = XmlUtil.getElements(node);
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            walkTree(request, entry, sb, child);
        }
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
    private Result outputKmlExtract(Request request, Entry entry)
            throws Exception {
	String href=request.getString(ARG_FILE,"");
	return fetchFile(request, entry, href);
    }


    private Result outputKmlDoc(Request request, Entry entry)
            throws Exception {
	InputStream inputStream = KmlTypeHandler.readDoc(getRepository(), entry);
	if(request.get("converthref",false)) {
	    String kml  = IO.readInputStream(inputStream);
	    String url =  getRepository().getUrlBase()+"/entry/show?entryid=" + entry.getId() +"&output=kml.extract&" + ARG_FILE+"=";
	    url = url.replace("&","&amp;");
	    String regex = "<href>(.*?)</href>";
	    kml = kml.replaceAll("<href>http","_DUMMYHTTP_");
	    kml = kml.replaceAll(regex,
				 "<href>" + url +"$1</href>");
			      
	    kml = kml.replace("_DUMMYHTTP_","<href>http");
	    InputStream tmp = inputStream;
	    inputStream = new ByteArrayInputStream(kml.getBytes());
	    tmp.close();
	}
	return new Result(inputStream,"application/vnd.google-earth.kml+xml");
    }
    




}
