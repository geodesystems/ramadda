/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biblio;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;



import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.Date;
import java.util.Enumeration;


import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class BiblioOutputHandler extends OutputHandler implements BiblioConstants {




    /** _more_ */
    public static final OutputType OUTPUT_BIBLIO_EXPORT =
        new OutputType("Export Bibliography", "biblio_export",
                       OutputType.TYPE_VIEW, "", "/biblio/book.png");


    /**
     * _more_
     */
    public BiblioOutputHandler() {}

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public BiblioOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_BIBLIO_EXPORT);
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
        for (Entry entry : state.getAllEntries()) {
            if (entry.getTypeHandler().getType().equals("biblio")) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_BIBLIO_EXPORT));

                return;
            }
        }

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
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);

        return outputEntries(request, entries);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
        return outputEntries(request, children);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntries(Request request, List<Entry> entries)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        for (Entry entry : entries) {
            if ( !entry.getTypeHandler().getType().equals("biblio")) {
                continue;
            }
            appendExport(request, entry, sb);
        }
        request.setReturnFilename("bibliography.txt");
        Result result = new Result("", sb);
        result.setShouldDecorate(false);
        result.setMimeType("text/plain");

        return result;
    }



    /*
     <column name="type" type="enumerationplus"  label="Type" values="Generic,Journal Article,Report" />
     <column name="primary_author" type="string" size="500" changetype="true"  label="Primary Author"  cansearch="true"/>
     <column name="institution" type="string"  label="Institution"  cansearch="true"/>
     <column name="other_authors" type="list"  changetype="true" size="5000" label="Other Authors"  rows="5"/>
     <column name="publication" type="enumerationplus"  label="Publication"  />
     <column name="volume_number" type="string"  label="Volume"  />
     <column name="issue_number" type="string"  label="Issue"  />
     <column name="pages" type="string"  label="Pages"  />
     <column name="doi" type="string"  label="DOI"  />
     <column name="link" type="url"  label="Link"  />
    */

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void appendExport(Request request, Entry entry, StringBuffer sb)
            throws Exception {
        GregorianCalendar cal =
            new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
        cal.setTime(new Date(entry.getStartDate()));
        Object[] values = entry.getTypeHandler().getEntryValues(entry);
        int      idx    = 0;
        Object   author = values[idx++];
        Object   type   = values[idx++];
        appendTag(sb, TAG_BIBLIO_TYPE, type);
        appendTag(sb, TAG_BIBLIO_AUTHOR, author);
        appendTag(sb, TAG_BIBLIO_TITLE, entry.getName());
        appendTag(sb, TAG_BIBLIO_INSTITUTION, values[idx++]);
        if (values[idx] != null) {
            for (String otherAuthor :
                    StringUtil.split(values[idx].toString(), "\n", true,
                                     true)) {
                appendTag(sb, TAG_BIBLIO_AUTHOR, otherAuthor);
            }
        }
        idx++;

        appendTag(sb, TAG_BIBLIO_DATE, "" + cal.get(GregorianCalendar.YEAR));
        appendTag(sb, TAG_BIBLIO_PUBLICATION, values[idx++]);
        appendTag(sb, TAG_BIBLIO_VOLUME, values[idx++]);
        appendTag(sb, TAG_BIBLIO_ISSUE, values[idx++]);
        appendTag(sb, TAG_BIBLIO_PAGE, values[idx++]);
        appendTag(sb, TAG_BIBLIO_DOI, values[idx++]);
        appendTag(sb, TAG_BIBLIO_URL, values[idx++]);


        List<Metadata> metadataList = getMetadataManager().getMetadata(request,entry);
        if (metadataList != null) {
            boolean firstMetadata = true;
            for (Metadata metadata : metadataList) {
                if ( !metadata.getType().equals("enum_tag")) {
                    continue;
                }
                if (firstMetadata) {
                    sb.append(TAG_BIBLIO_TAG);
                    sb.append(" ");
                }
                sb.append(metadata.getAttr1());
                sb.append("\n");
                firstMetadata = false;
            }
        }
        appendTag(sb, TAG_BIBLIO_DESCRIPTION, entry.getDescription());
        sb.append("\n");
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param tag _more_
     * @param value _more_
     */
    private void appendTag(StringBuffer sb, String tag, Object value) {
        if (value == null) {
            return;
        }
        String s = value.toString();
        if (Utils.stringDefined(s)) {
            s = s.replaceAll("\n", " ");
            sb.append(tag);
            sb.append(" ");
            sb.append(s);
            sb.append("\n");
        }
    }




}
