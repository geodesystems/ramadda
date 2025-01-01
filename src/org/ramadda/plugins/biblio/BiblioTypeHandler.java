/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biblio;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;


import org.w3c.dom.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 */
public class BiblioTypeHandler extends GenericTypeHandler {

    /** _more_ */
    private SimpleDateFormat dateFormat;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BiblioTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryListName(Request request, Entry entry) {
        return "NAME:" + entry.getName();
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param date _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Entry entry, Date date,
                             String extra) {
        return formatDate(request, date);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param d _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Date d) {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy-MM");
            dateFormat.setTimeZone(RepositoryBase.TIMEZONE_UTC);
        }
        synchronized (dateFormat) {
            return dateFormat.format(d);
        }
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
     * @param primary _more_
     * @param others _more_
     *
     * @return _more_
     */
    private String formatAuthors(String primary, String others) {
	if(stringDefined(primary) && stringDefined(others))
	    return primary + ", " + others;
	else if(stringDefined(others))
	    return others;
	return primary;
    }
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("biblio")) {
            StringBuilder sb     = new StringBuilder();
            Object[]      values = getEntryValues(entry);
            int           idx    = 0;
            String        author = (String) values[idx++];
            String        type   = (String) values[idx++];
            GregorianCalendar cal =
                new GregorianCalendar(RepositoryUtil.TIMEZONE_DEFAULT);
            cal.setTime(new Date(entry.getStartDate()));
            //        if (type.toString().equals("Journal Article")) {
            String title       = entry.getName();
            Object institution = values[idx++];
            String others      = Utils.toString(values[idx++]);
            String authors     = formatAuthors(author, others);
            String pub         = Utils.toString(values[idx++]);
            String volume      = Utils.toString(values[idx++]);
            String issue       = Utils.toString(values[idx++]);
            String pages       = Utils.toString(values[idx++]);
            String doi         = Utils.toString(values[idx++]);
            sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV));
	    append(sb,authors);
            sb.append(cal.get(GregorianCalendar.YEAR));
            sb.append(": ");
            sb.append(title);
            if ( !title.endsWith(".")) {
                sb.append(".");
            }
            sb.append(" ");
	    if(stringDefined(pub)) {
		append(sb,HU.italics(pub));
	    }
            if (stringDefined(volume)) {
                sb.append(HtmlUtils.bold(volume));
		sb.append(", ");
            }
            if (stringDefined(pages)) {
                sb.append(pages);
                sb.append(". ");
            }
            if (stringDefined(doi)) {
                sb.append(" doi: ");
                if ( !doi.startsWith("http")) {
                    doi = "https://doi.org/" + doi;
                }
                sb.append(HtmlUtils.href(doi, doi));
            }
            sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));

            return sb.toString();
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
    }

    private void append(StringBuilder sb, String s) {
	if(stringDefined(s)) {
	    sb.append(s);
            sb.append(", ");
	}
    }	

}
