/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.wiki;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class WikiPageTypeHandler extends ExtensibleGroupTypeHandler {

    /**
     *   The url argument of the text area. Note: this has to be of the form that the Column class handles in a save
     * _more_
     */
    public static final String ARG_WIKI_TEXTAREA = Column.ARG_EDIT_PREFIX
                                                   + "wikipage_wikitext";


    /** _more_ */
    public static String ASSOC_WIKILINK = "wikilink";

    /** _more_ */
    public static String TYPE_WIKIPAGE = "wikipage";


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public WikiPageTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     *
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void getTextCorpus(Entry entry, Appendable sb, boolean...args) throws Exception {
        super.getTextCorpus(entry, sb,args);
        sb.append(getEntryText(entry));
        sb.append("\n");
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    @Override
    public String getEntryText(Entry entry) {
        return (String) entry.getValue(getAdminRequest(),0);
    }


    @Override
    public String getExtraText(Entry entry) {
        return (String) entry.getValue(getAdminRequest(),0);
    }    



    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry group,  Entries children) 
            throws Exception {
        return getRepository().getOutputHandler(
            WikiPageOutputHandler.OUTPUT_WIKI).outputEntry(
            request, request.getOutput(), group);
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
    @Override
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        return getRepository().getOutputHandler(
            WikiPageOutputHandler.OUTPUT_WIKI).outputEntry(
            request, request.getOutput(), entry);
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("wikitext")) {
            return (String) entry.getStringValue(request,0, "");
        }

        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getTextForWiki(Request request, Entry entry,
                                 Hashtable properties)
            throws Exception {
        return (String) entry.getStringValue(request,0, "");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, Entry entry)
            throws Exception {
        super.deleteEntry(request, statement, entry);
        String query =
            SqlUtil.makeDelete(Tables.WIKIPAGEHISTORY.NAME,
                               Tables.WIKIPAGEHISTORY.COL_ENTRY_ID,
                               SqlUtil.quote(entry.getId()));
        statement.execute(query);
    }


    /**
     * _more_
     *
     * @param newEntry _more_
     * @param idList _more_
     *
     * @return _more_
     */
    @Override
    public boolean convertIdsFromImport(Entry newEntry,
                                        List<String[]> idList) {

        boolean  changed = super.convertIdsFromImport(newEntry, idList);
        Object[] values  = newEntry.getValues();
        if (values != null) {
            String wikiText = (String) values[0];
            if (wikiText != null) {
                String converted = convertIdsFromImport(wikiText, idList);
                if ( !converted.equals(wikiText)) {
                    values[0] = converted;
                    changed   = true;
                }
            }
        }

        return changed;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {

        Object[] values       = entry.getValues();
        String   originalText = null;
        if (values != null) {
            originalText = (String) values[0];
        }
        boolean wasNew = (values == null);
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        String newText = (String) entry.getValues()[0];
        if ((originalText == null) || !Misc.equals(originalText, newText)) {

            /**
             * For now don't keep around the history as problably no human has ever viewed or used it
             *   String desc = "";
             *   if (wasNew) {
             *   desc = "Created";
             *   } else {
             *   desc = request.getString(
             *   WikiPageOutputHandler.ARG_WIKI_CHANGEDESCRIPTION, "");
             *   }
             *
             *   getDatabaseManager().executeInsert(Tables.WIKIPAGEHISTORY.INSERT,
             *   new Object[] { entry.getId(),
             *   request.getUser().getId(), new Date(),
             *   desc, newText });
             */
            WikiUtil wikiUtil =
                getWikiManager().initWikiUtil(request,
                    new WikiUtil(Misc.newHashtable(new Object[] {
                        OutputHandler.PROP_REQUEST,
                        request, OutputHandler.PROP_ENTRY, entry })), entry);


            getRepository().getWikiManager().wikifyEntry(request, entry,
                    wikiUtil, newText, true, null, true);


            Hashtable<Entry, Entry> links =
                (Hashtable<Entry, Entry>) wikiUtil.getProperty("wikilinks");
            if (links == null) {
                links = new Hashtable<Entry, Entry>();
            }
            Hashtable         ids             = new Hashtable();
            List<Association> newAssociations = new ArrayList<Association>();
            for (Enumeration keys = links.keys(); keys.hasMoreElements(); ) {
                Entry linkedEntry = (Entry) keys.nextElement();
                Association tmp = new Association(getRepository().getGUID(),
                                      "", ASSOC_WIKILINK, entry.getId(),
                                      linkedEntry.getId());
                newAssociations.add(tmp);
            }


            List<Association> associations =
                getAssociationManager().getAssociations(request, entry);
            for (Association oldAssociation :
                    (List<Association>) new ArrayList(associations)) {
                if (oldAssociation.getType().equals(ASSOC_WIKILINK)
                        && oldAssociation.getFromId().equals(entry.getId())) {
                    if ( !newAssociations.contains(oldAssociation)) {
                        //                        System.err.println("delete:" + oldAssociation);
                        getAssociationManager().deleteAssociation(request,
                                oldAssociation);
                    }
                }
            }
            for (Association newAssociation :
                    (List<Association>) new ArrayList(newAssociations)) {
                if ( !associations.contains(newAssociation)) {
                    getAuthManager().addAuthToken(request);
                    getAssociationManager().addAssociation(request,
                            newAssociation);
                }
            }

        }




    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tabTitles _more_
     * @param tabContents _more_
     */
    @Override
    public void addToInformationTabs(Request request, Entry entry,
                                     List<String> tabTitles,
                                     List<String> tabContents) {
        super.addToInformationTabs(request, entry, tabTitles, tabContents);
        try {
            StringBuilder sb = new StringBuilder();
            addReadOnlyWikiEditor(request, entry, sb, entry.getStringValue(request,0, ""));
            //       sb.append(HU.textArea("dummy", entry.getStringValue(request,0, ""), 10,   120));
            tabTitles.add("Wiki Text");
            tabContents.add(sb.toString());
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }


    @Override
    public   Entry.EntryHistory createHistory(Entry entry) {
	Entry.EntryHistory history = super.createHistory(entry);
	Object[] values = entry.getValues();
	if ((values != null) && (values.length > 0)
	    && (values[0] != null)) {
	    //	    System.err.println("value:" + values[0]);
	    history.putProperty("wikitext",values[0]);
	}

	return history;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param parentEntry _more_
     * @param entry _more_
     * @param formInfo _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToEntryForm(Request request, Appendable sb,
                               Entry parentEntry, Entry entry,
                               FormInfo formInfo)
            throws Exception {

        String size = HU.SIZE_70;
        String name;
        if (entry != null) {
            name = entry.getName();
        } else {
            name = request.getString(ARG_NAME, "");
            List tmp = new ArrayList();
            for (String tok :
                    (List<String>) StringUtil.split(name, " ", true, true)) {
                tmp.add(StringUtil.camelCase(tok));
            }
            name = StringUtil.join(" ", tmp);
        }

        String wikiText = "";
        if (entry != null) {
            Object[] values = entry.getValues();
            if ((values != null) && (values.length > 0)
                    && (values[0] != null)) {
                wikiText = (String) values[0];
            }
        }
	Entry.EntryHistory entryHistory = (Entry.EntryHistory) formInfo.getHistory();
	if(entryHistory!=null) {
	    wikiText = (String)entryHistory.getProperty("wikitext",wikiText);
	    //	    System.err.println("H:" + wikiText);
	}


        if (request.defined(WikiPageOutputHandler.ARG_WIKI_EDITWITH)) {
            Date dttm = new Date(
                            (long) request.get(
                                WikiPageOutputHandler.ARG_WIKI_EDITWITH,
                                0.0));
            WikiPageHistory wph = getHistory(entry, dttm);
            if (wph == null) {
                throw new IllegalArgumentException(
                    "Could not find wiki history");
            }
            wikiText = wph.getText();
            sb.append(
                HU.formEntry(
                    "",
                    msgLabel("Editing with text from version")
                    + getDateHandler().formatDate(wph.getDate())));
        }

	sb.append("<tr><td colspan=2>");
        sb.append(HU.input(ARG_NAME, name,
			   HU.attr("autofocus","true") +
			   HU.attr("placeholder", "Name") +
			   size));
	sb.append("</td></tr>");

        if (entry != null) {

            /**
             * sb.append(
             *   HU.formEntry(
             *       msgLabel("Edit&nbsp;Summary"),
             *       HU.input(
             *           WikiPageOutputHandler.ARG_WIKI_CHANGEDESCRIPTION, "",
             *           size)));
             */
        }






        StringBuilder tmpSB = new StringBuilder();
        addWikiEditor(request, entry, tmpSB, formInfo, ARG_WIKI_TEXTAREA,
                      wikiText, null, false, 256000, true);
	String edit = HU.b("Wiki Text:") + "<br>"+tmpSB.toString();
	
	sb.append(HU.row(HU.td(edit,"colspan=2")));
	//TODO:        addDateToEntryForm(request, sb, parentEntry,entry);
        addAreaWidget(request, parentEntry,entry, sb, formInfo);
        sb.append(formEntry(request, msgLabel("Order"),
                            HU.input(ARG_ENTRYORDER, ((entry != null)
                ? entry.getEntryOrder()
                : 999), HU.SIZE_5) + " 1-N"));
        //super.addToEntryForm(request, sb, parentEntry, entry, formInfo);
    }





    /**
     * _more_
     *
     * @param entry _more_
     * @param date _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public WikiPageHistory getHistory(Entry entry, Date date)
            throws Exception {
        List<WikiPageHistory> list = getHistoryList(entry, date, true);
        if (list.size() > 0) {
            return list.get(0);
        }

        return null;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param date _more_
     * @param includeText _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<WikiPageHistory> getHistoryList(Entry entry, Date date,
            boolean includeText)
            throws Exception {
        Statement statement =
            getDatabaseManager().select(SqlUtil.comma(includeText
                ? new String[] { Tables.WIKIPAGEHISTORY.COL_USER_ID,
                                 Tables.WIKIPAGEHISTORY.COL_DATE,
                                 Tables.WIKIPAGEHISTORY.COL_DESCRIPTION,
                                 Tables.WIKIPAGEHISTORY.COL_WIKITEXT }
                : new String[] { Tables.WIKIPAGEHISTORY.COL_USER_ID,
                                 Tables.WIKIPAGEHISTORY.COL_DATE,
                                 Tables.WIKIPAGEHISTORY
                                     .COL_DESCRIPTION }), Tables
                                         .WIKIPAGEHISTORY
                                         .NAME, ((date != null)
                ? Clause
                    .and(Clause
                        .eq(Tables.WIKIPAGEHISTORY.COL_ENTRY_ID,
                            entry.getId()), Clause
                                .eq(Tables.WIKIPAGEHISTORY.COL_DATE, date))
                : Clause.eq(
                    Tables.WIKIPAGEHISTORY.COL_ENTRY_ID,
                    entry.getId())), " order by "
                                     + Tables.WIKIPAGEHISTORY.COL_DATE
                                     + " asc ");

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet             results;
        List<WikiPageHistory> history = new ArrayList<WikiPageHistory>();
        int                   version = 1;
        while ((results = iter.getNext()) != null) {
            int col = 1;
            WikiPageHistory wph = new WikiPageHistory(
                                      version++,
                                      getUserManager().findUser(
                                          results.getString(col++),
                                          true), getDatabaseManager().getDate(
                                              results,
                                              col++), results.getString(
                                                  col++), (includeText
                    ? results.getString(col++)
                    : ""));
            history.add(wph);
        }

        return history;
    }



}
