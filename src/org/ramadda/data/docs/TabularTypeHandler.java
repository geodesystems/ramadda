/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;
import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FileInfo;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.seesv.TextReader;
import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TabularTypeHandler extends ConvertibleTypeHandler {
    public static final String TYPE_TABULAR = "type_document_tabular";
    private static int IDX = ConvertibleTypeHandler.IDX_LAST + 1;
    public static final int IDX_LAST = IDX;
    private TabularOutputHandler tabularOutputHandler;

    public TabularTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    public static boolean isTabular(Entry entry) {
        if (entry == null) {
            return false;
        }

        return entry.getTypeHandler().isType(TabularTypeHandler.TYPE_TABULAR);
    }

    @Override
    public boolean processCommandView(org.ramadda.repository.harvester
            .CommandHarvester.CommandRequest request, Entry entry,
                org.ramadda.repository.harvester.CommandHarvester harvester,
                List<String> args, Appendable sb, List<FileInfo> files)
            throws Exception {
        return getTabularOutputHandler().processCommandView(request, entry,
                harvester, args, sb, files);
    }

    private TabularOutputHandler getTabularOutputHandler() {
        if (tabularOutputHandler == null) {
            tabularOutputHandler =
                (TabularOutputHandler) getRepository().getOutputHandler(
                    TabularOutputHandler.class);
        }

        return tabularOutputHandler;
    }

    public void visit(Request request, Entry entry, InputStream myxls,
                      TextReader visitInfo, TabularVisitor visitor)
            throws Exception {}

    @Override
    public String getSimpleDisplay(Request request, Hashtable requestProps,
                                   Entry entry)
            throws Exception {
	if(isWikiText(entry.getDescription())) return null;
	if(getWikiTemplate(request, entry)!=null) return null;
	String wiki = "+section title={{name}}\n{{description wikify=true}}\n+accordion decorate=false collapsible=true activeSegment=-1\n+segment Document Information\n{{information  details=\"true\"  showTitle=\"false\"  includeTools=true menus=\"service\" menusTitle=\"Services\"}} \n-segment\n-accordion\n-section\n+section\n+center\n<div style='margin-bottom:4px;'>{{tags}}</div>\n-center\n{{display_table max=5000 maxColumns=50}}\n-section";
	return getWikiManager().wikifyEntry(request,entry,wiki);
    }

    public boolean okToShowTable(Request request, Entry entry) {
        return true;
    }

    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        //        Misc.printStack ("TabularTypeHandler.getInnerWikiContent");
        String s = getSimpleDisplay(request, null, entry);
        if (s == null) {
            return null;
        }

        return new Result("", new StringBuilder(s));
    }

    public String getDelimiter(Entry entry) {
        return getTypeProperty("table.delimiter", (String) null);
    }

}
