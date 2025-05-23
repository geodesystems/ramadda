/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import ucar.unidata.xml.XmlUtil;
import org.w3c.dom.*;
import java.util.ArrayList;
import java.util.List;

public class TemplateOutputHandler extends OutputHandler {

    public static final String ATTR_ICON = "icon";

    public static final String ATTR_ID = "id";

    public static final String ATTR_EMBED = "embed";

    public static final String ATTR_NAME = "name";

    public static final String ATTR_TYPES = "types";

    public static final String TAG_WIKI = "wiki";

    public static final String TAG_WIKI_FOLDER = "wiki.folder";

    public static final String TAG_WIKI_FILE = "wiki.file";

    private boolean forGroups = false;

    private boolean forFiles = false;

    private List<String> types;

    private String folderWikiTemplate;

    private String fileWikiTemplate;

    private OutputType outputType;

    private boolean embed;

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
		    links.add(makeLink(request, state.getEntry(),
				       outputType));
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
