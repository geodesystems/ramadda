/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;

import java.util.zip.*;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class TextOutputHandler extends OutputHandler {

    public static final OutputType OUTPUT_TEXT =
        new OutputType("Annotated Text", "text", OutputType.TYPE_VIEW, "",
                       ICON_TEXT);

    public static final OutputType OUTPUT_WORDCLOUD =
        new OutputType("Word Cloud", "wordcloud", OutputType.TYPE_VIEW, "",
                       ICON_CLOUD);

    public static final OutputType OUTPUT_PRETTY =
        new OutputType("Pretty Print", "pretty", OutputType.TYPE_VIEW, "",
                       ICON_TEXT);

    public TextOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_TEXT);
        addType(OUTPUT_WORDCLOUD);
        addType(OUTPUT_PRETTY);
    }

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        if (state.entry == null) {
            return;
        }

        if ( !state.entry.isFile()) {
            return;
        }
        if ( !getRepository().getAccessManager().canAccessFile(request,
                state.entry)) {
            return;
        }

        String   path     = state.entry.getResource().getPath();
        String[] suffixes = new String[] {
            ".csv", ".txt", ".java", ".c", ".h", ".cc", ".f90", ".cpp", ".hh",
            ".cdl", ".sh", "m4"
        };

        String[] codesuffixes = new String[] {
            ".bsh", ".c", ".cc", ".cpp", ".cs", ".csh", ".cyc", ".cv", ".htm",
            ".html", ".java", ".js", ".m", ".mxml", ".perl", ".pl", ".pm",
            ".py", ".rb", ".sh", ".xhtml", ".xml", ".xsl"
        };

        for (int i = 0; i < codesuffixes.length; i++) {
            if (path.endsWith(codesuffixes[i])) {
                links.add(makeLink(request, state.entry, OUTPUT_PRETTY));

                return;
            }
        }

        for (int i = 0; i < suffixes.length; i++) {
            if (path.endsWith(suffixes[i])) {
                links.add(makeLink(request, state.entry, OUTPUT_TEXT));

                //                links.add(makeLink(request, state.entry, OUTPUT_WORDCLOUD));
                return;
            }
        }

        String suffix = IO.getFileExtension(path);
        String type   = getRepository().getMimeTypeFromSuffix(suffix);
        if ((type != null) && type.startsWith("text/")) {

            links.add(makeLink(request, state.entry, OUTPUT_TEXT));
            links.add(makeLink(request, state.entry, OUTPUT_WORDCLOUD));
        }

    }

    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            throw new AccessException("Cannot access data", request);
        }

        if (outputType.equals(OUTPUT_WORDCLOUD)) {
            return outputWordCloud(request, entry);
        }

        if (outputType.equals(OUTPUT_PRETTY)) {
            return outputPretty(request, entry);
        }

        String contents =
            getStorageManager().readSystemResource(entry.getFile());
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Annotated Text");

        sb.append("<pre>\n");
        int cnt = 0;
        for (String line :
                (List<String>) Utils.split(contents, "\n", false, false)) {
            cnt++;
            line = line.replace("\r", "");
            line = HtmlUtils.entityEncode(line);
            sb.append("<a " + HtmlUtils.attr("name", "line" + cnt)
                      + "></a><a href=#line" + cnt + ">" + cnt + "</a> "
                      + HtmlUtils.space(1) + line + "<br>");
        }
        sb.append("</pre>");
        getPageHandler().entrySectionClose(request, entry, sb);

        return makeLinksResult(request, msg("Text"), sb, new State(entry));
    }

    public Result outputWordCloud(Request request, Entry entry)
            throws Exception {
        String contents =
            getStorageManager().readSystemResource(entry.getFile());
        StringBuffer sb   = new StringBuffer();

        StringBuffer head = new StringBuffer();
        head.append("\n");
        head.append(
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://visapi-gadgets.googlecode.com/svn/trunk/wordcloud/wc.css\">\n");
        head.append(
            HtmlUtils.importJS(
                "https://visapi-gadgets.googlecode.com/svn/trunk/wordcloud/wc.js"));
        head.append("\n");
        getPageHandler().addGoogleJSImport(request, head);
        head.append("\n");

        sb.append("<div id=\"wcdiv\"></div>");
        sb.append("\n");
        StringBuffer js = new StringBuffer();
        js.append("google.load(\"visualization\", \"1\");\n");
        js.append("google.setOnLoadCallback(draw);\n");
        js.append("function draw() {\n");
        js.append("var data = new google.visualization.DataTable();\n");
        js.append("data.addColumn('string', 'Text1');\n");
        List<String> lines = (List<String>) Utils.split(contents, "\n",
                                 false, false);

        js.append("data.addRows(" + lines.size() + ");\n");
        int cnt = 0;
        for (String line : lines) {
            line = line.replace("\r", "");
            line = line.replace("'", "\\'");
            js.append("data.setCell(" + cnt + ", 0, '" + line + "');\n");
            cnt++;
        }
        js.append("var outputDiv = document.getElementById('wcdiv');\n");
        js.append("var wc = new WordCloud(outputDiv);\n");
        js.append("wc.draw(data, null);\n");
        js.append("      }");
        sb.append(HtmlUtils.script(js.toString()));
        Result result = makeLinksResult(request, msg("Word Cloud"), sb,
                                        new State(entry));
        request.appendHead(head.toString());

        return result;
    }

    public Result outputPretty(Request request, Entry entry)
            throws Exception {
        String contents =
            getStorageManager().readSystemResource(entry.getFile());
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb, "Pretty Print");

        StringBuffer head = new StringBuffer();
        head.append("\n");
        head.append(
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://visapi-gadgets.googlecode.com/svn/trunk/wordcloud/wc.css\">\n");
        head.append(
            HtmlUtils.importJS(
                getRepository().getHtdocsUrl("/lib/prettify/prettify.js")));
        head.append(
            HtmlUtils.cssLink(
                getRepository().getHtdocsUrl("/lib/prettify/prettify.css")));

        sb.append(head);
        sb.append("<pre class=\"prettyprint\">\n");

        int cnt = 0;
        for (String line :
                (List<String>) Utils.split(contents, "\n", false, false)) {
            cnt++;
            line = line.replace("\r", "");
            line = HtmlUtils.entityEncode(line);
            sb.append("<span class=nocode><a "
                      + HtmlUtils.attr("name", "line" + cnt)
                      + "></a><a href=#line" + cnt + ">" + cnt
                      + "</a></span>" + HtmlUtils.space(1) + line + "<br>");
        }
        sb.append("</pre>\n");
        sb.append(HtmlUtils.script("prettyPrint();"));
        getPageHandler().entrySectionClose(request, entry, sb);
        Result result = makeLinksResult(request, msg("Pretty Print"), sb,
                                        new State(entry));

        return result;
    }

}
