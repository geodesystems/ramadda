/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Class description
 *
 *
 * @version        $version$, Wed, Jul 8, '15
 * @author         Enter your name here...
 */
public class EntryXmlProcessor extends RowCollector {

    /** _more_ */
    SimpleDateFormat fsdf = new SimpleDateFormat("yyyy-MM-dd");

    /** _more_ */
    SimpleDateFormat sdf1 = new SimpleDateFormat("MM/yyyy");

    /** _more_ */
    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy");

    /**
     * _more_
     */
    public EntryXmlProcessor() {}

    /**
     * _more_
     *
     * @param ctx _more_
     *
     * @throws Exception _more_
     */
    public void finish(TextReader ctx) throws Exception {

        String template = null;
        if (new File("template.xml").exists()) {
            template = IO.readContents("template.xml");
        } else {
            template = IO.readContents("/org/ramadda/util/seesvtemplate.xml",
                                       EntryXmlProcessor.class);
        }
        List<Row>       rows = getRows();
        StringBuilder   sb   = new StringBuilder("<entries>\n");

        HashSet<String> seen = new HashSet<String>();

        for (int i = 1; i < rows.size(); i++) {
            StringBuilder extra  = new StringBuilder("");
            String        id     = "assessment" + i;
            Row           row    = rows.get(i);
            String        s      = template;
            List          values = row.getValues();
            for (int colIdx = 0; colIdx < values.size(); colIdx++) {
                Object v  = values.get(colIdx);
                String sv = v.toString();
                sv = Utils.removeNonAscii(sv);
                s  = s.replace("${" + colIdx + "}", sv);
            }
            StringBuilder content = new StringBuilder();
            String        date    = values.get(11).toString();
            Date          dttm    = null;
            if (Utils.stringDefined(date)) {
                try {
                    dttm = sdf1.parse(date);
                } catch (Exception exc) {
                    dttm = sdf2.parse(date);
                }
            }
            StringBuilder attrs = new StringBuilder();
            if (dttm != null) {
                attrs.append(" fromdate=\"" + fsdf.format(dttm) + "\" ");
            }

            String file = IOUtil.getFileTail(values.get(37).toString());

            String theory_development    = "no";
            String resilience_definition = "no";
            String assessment_type       = "oneoff";

            String keywords              = values.get(39).toString();
            for (String tok : Utils.split(keywords, ";", true, true)) {
                List<String> toks = Utils.splitUpTo(tok, ":", 2);
                String       n    = toks.get(0).toLowerCase();
                String       v    = ((toks.size() > 1)
                                     ? toks.get(1)
                                     : "");
                if (n.equals("one-off")) {
                    continue;
                }
                if (n.equals("theory?")) {
                    theory_development = "yes";

                    continue;
                }
                if (n.equals("purpose")) {
                    content.append(
                        "<metadata  type=\"assessment_purpose\"><attr encoded=\"false\" index=\"1\"><![CDATA["
                        + v + "]]></attr></metadata>\n");

                    continue;
                }
                if (n.equals("outcomes")) {
                    content.append(
                        "<metadata  type=\"assessment_outcome\"><attr encoded=\"false\" index=\"1\"><![CDATA["
                        + v + "]]></attr></metadata>\n");

                    continue;
                }
                if (n.equals("sponsor/clients")) {
                    content.append(
                        "<metadata  type=\"assessment_sponsor\"><attr encoded=\"false\" index=\"1\"><![CDATA["
                        + v + "]]></attr></metadata>\n");

                    continue;
                }
                if (n.equals("interaction")) {
                    content.append(
                        "<metadata  type=\"assessment_interaction\"><attr encoded=\"false\" index=\"1\"><![CDATA["
                        + v + "]]></attr></metadata>\n");

                    continue;
                }
                if (n.equals("design/practices")) {
                    content.append(
                        "<metadata  type=\"assessment_practice\"><attr encoded=\"false\" index=\"1\"><![CDATA["
                        + v + "]]></attr></metadata>\n");

                    continue;
                }
                if (n.equals("formal outputs")) {
                    content.append(
                        "<metadata  type=\"assessment_output\"><attr encoded=\"false\" index=\"1\"><![CDATA["
                        + v + "]]></attr></metadata>\n");

                    continue;
                }

                if (n.startsWith("resilience")) {
                    resilience_definition = v.toLowerCase();

                    continue;
                }
                //                System.err.println(n +"=" + v);
            }

            s = s.replace("${theory_development}", theory_development);
            s = s.replace("${resilience_definition}", resilience_definition);
            s = s.replace("${assessment_type}", assessment_type);

            if (seen.contains(file)) {
                //                System.err.println ("DUP:" + file);
                file = "2_" + file;
            }
            seen.add(file);
            if ( !new File("files/" + file).exists()) {
                System.err.println("no file:" + file);
            } else {
                extra.append(XmlUtil.tag("entry", XmlUtil.attrs(new String[] {
                    "name", file, "parent", id, "file", file
                })));
                extra.append("\n");
            }

            String doi = values.get(8).toString();
            if (Utils.stringDefined(doi)) {
                content.append(
                    "<metadata  type=\"doi_identifier\"><attr encoded=\"false\" index=\"2\"><![CDATA["
                    + doi + "]]></attr></metadata>\n");
            }

            for (String author :
                    Utils.split(values.get(3).toString(), ";", true, true)) {
                author = Utils.removeNonAscii(author);
                content.append(
                    "<metadata  type=\"metadata_author\"><attr encoded=\"false\" index=\"1\"><![CDATA["
                    + author + "]]></attr></metadata>\n");
            }

            s = s.replace("${id}", id);
            s = s.replace("${content}", content.toString());
            s = s.replace("${attrs}", attrs.toString());

            sb.append(s);
            sb.append(extra);
        }
        sb.append("</entries>\n");
        System.out.println(sb);

    }

}
