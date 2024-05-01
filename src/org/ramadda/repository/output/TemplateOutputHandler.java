/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import ucar.unidata.xml.XmlUtil;


import org.w3c.dom.*;
import java.io.*;
import java.io.File;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class TemplateOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String ATTR_ICON = "icon";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_EMBED = "embed";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_TYPES = "types";

    /** _more_ */
    public static final String TAG_WIKI = "wiki";

    /** _more_ */
    public static final String TAG_WIKI_FOLDER = "wiki.folder";

    /** _more_ */
    public static final String TAG_WIKI_FILE = "wiki.file";

    /** _more_ */
    private boolean forGroups = true;

    /** _more_ */
    private boolean forFiles = true;

    /** _more_ */
    private List<String> types;

    /** _more_ */
    private String folderWikiTemplate;

    /** _more_ */
    private String fileWikiTemplate;


    /** _more_ */
    private OutputType outputType;

    /** _more_ */
    private boolean embed;


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public TemplateOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        init(element);
    }

    public TemplateOutputHandler(Repository repository, String id, String name,
				 List<String> types,
				 String wiki, String icon)
				 throws Exception {
        super(repository, name);
	this.types=types;
	fileWikiTemplate = folderWikiTemplate = wiki;
        outputType = new OutputType(name, id, OutputType.TYPE_VIEW, "",
				    icon!=null?icon:"/icons/page.png");
        addType(outputType);
    }




    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    private void init(Element element) throws Exception {
        embed = XmlUtil.getAttribute(element, ATTR_EMBED, false);
        String id = XmlUtil.getAttribute(element, ATTR_ID);
        String wikiTemplate = XmlUtil.getGrandChildText(element, TAG_WIKI,"no wiki");

        folderWikiTemplate = XmlUtil.getGrandChildText(element,
                TAG_WIKI_FOLDER, wikiTemplate);
        fileWikiTemplate = XmlUtil.getGrandChildText(element, TAG_WIKI_FILE,
                wikiTemplate);
        types = Utils.split(XmlUtil.getAttribute(element, ATTR_TYPES,
                "file,folder"), ",", true, true);

        forGroups = types.contains("folder");
        forFiles  = types.contains("file");
        outputType = new OutputType(XmlUtil.getAttribute(element, ATTR_NAME,
                id), id, OutputType.TYPE_VIEW, "",
                     XmlUtil.getAttribute(element, ATTR_ICON, "/icons/file.gif"));
        addType(outputType);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.group != null) {
            if (forGroups) {
                if (state.getAllEntries().size() > 1) {
                    links.add(makeLink(request, state.getEntry(),
                                       outputType));

                    return;
                }
            }
            for (String type : types) {
                if (state.group.getTypeHandler().isType(type)) {
                    if (state.getAllEntries().size() > 1) {
                        links.add(makeLink(request, state.getEntry(),
                                           outputType));
                    }

                    return;
                }
            }
        }

        if (state.entry != null) {
            if (forFiles) {
                links.add(makeLink(request, state.entry, outputType));

                return;
            }
            for (String type : types) {
                if (state.entry.getTypeHandler().isType(type)) {
                    links.add(makeLink(request, state.entry, outputType));

                    return;
                }
            }
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param children _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        String wiki = folderWikiTemplate;
        if (embed) {
            String outerTemplate = getPageHandler().getWikiTemplate(request,
                                       group, PageHandler.TEMPLATE_DEFAULT);
            wiki = outerTemplate.replace("${innercontent}", wiki);
        }

        //TODO: what to do with the children
        wiki = getWikiManager().wikifyEntry(request, group, wiki, false);

        return new Result("", new StringBuffer(wiki));

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        String wiki = fileWikiTemplate;
        if (embed) {
            String outerTemplate = getPageHandler().getWikiTemplate(request,
                                       entry, PageHandler.TEMPLATE_DEFAULT);
            wiki = outerTemplate.replace("${innercontent}", wiki);
        }
        wiki = getWikiManager().wikifyEntry(request, entry, wiki);

        return new Result("", new StringBuffer(wiki));
    }




}
