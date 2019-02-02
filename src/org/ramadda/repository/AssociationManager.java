/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.repository;


import org.ramadda.repository.auth.*;

import org.ramadda.repository.database.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;




import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;



import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;



import java.io.*;

import java.net.*;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;






/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class AssociationManager extends RepositoryManager {

    /** _more_ */
    private List<String> types = null;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public AssociationManager(Repository repository) {
        super(repository);
    }

    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processAssociationAdd(Request request) throws Exception {
        Entry fromEntry = getEntryManager().getEntry(request,
                              request.getString(ARG_FROM, BLANK));
        Entry toEntry = getEntryManager().getEntry(request,
                            request.getString(ARG_TO, BLANK));
        if (fromEntry == null) {
            throw new RepositoryUtil.MissingEntryException(
                "Could not find entry:" + request.getString(ARG_FROM, BLANK));
        }
        if (toEntry == null) {
            throw new RepositoryUtil.MissingEntryException(
                "Could not find entry:" + request.getString(ARG_TO, BLANK));
        }




        String name = request.getString(ARG_NAME, (String) null);
        if (name != null) {
            String type = request.getString(ARG_TYPE_FREEFORM, "").trim();
            if (type.length() == 0) {
                type = request.getString(ARG_TYPE, "").trim();
            }
            request.ensureAuthToken();
            addAssociation(request, fromEntry, toEntry, name, type);

            //            return new Result(request.entryUrl(getRepository().URL_ENTRY_SHOW, fromEntry));
            return new Result(
                request.entryUrl(
                    getRepositoryBase().URL_ENTRY_SHOW, fromEntry,
                    ARG_MESSAGE,
                    getRepository().translate(
                        request, MSG_ASSOCIATION_ADDED)));
        }


        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, fromEntry, sb,
                                          msg("Add Association"), false);
        sb.append("Add association between: " + fromEntry.getLabel());
        sb.append(" and:  " + toEntry.getLabel());
        request.formPostWithAuthToken(sb,
                                      getRepository().URL_ASSOCIATION_ADD,
                                      BLANK);
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.formTable());

        sb.append(HtmlUtils.formEntry(msgLabel("Association Name"),
                                      HtmlUtils.input(ARG_NAME)));

        List types = getAssociationManager().getTypes();
        types.add(0, new TwoFacedObject("None", ""));
        String select = ((types.size() == 1)
                         ? ""
                         : HtmlUtils.select(ARG_TYPE, types)
                           + HtmlUtils.space(1) + "Or:" + HtmlUtils.space(1));
        sb.append(HtmlUtils.formEntry(msgLabel("Type"),
                                      select
                                      + HtmlUtils.input(ARG_TYPE_FREEFORM,
                                          "", HtmlUtils.SIZE_20)));

        sb.append(HtmlUtils.formTableClose());

        sb.append(HtmlUtils.hidden(ARG_FROM, fromEntry.getId()));
        sb.append(HtmlUtils.hidden(ARG_TO, toEntry.getId()));
        sb.append(HtmlUtils.space(1));
        sb.append(HtmlUtils.submit(msg("Add Association")));
        sb.append(HtmlUtils.formClose());

        getPageHandler().entrySectionClose(request, fromEntry, sb);

        return getEntryManager().addEntryHeader(request, fromEntry,
                new Result("Add Association", sb));


    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processAssociationDelete(Request request) throws Exception {
        String associationId = request.getString(ARG_ASSOCIATION, "");
        Clause clause = Clause.eq(Tables.ASSOCIATIONS.COL_ID, associationId);
        List<Association> associations = getAssociations(request, clause);
        if (associations.size() == 0) {
            return new Result(
                msg("Delete Associations"),
                new StringBuilder(
                    getPageHandler().showDialogError(
                        "Could not find assocation")));
        }

        Entry fromEntry = getEntryManager().getEntry(request,
                              associations.get(0).getFromId());
        Entry toEntry = getEntryManager().getEntry(request,
                            associations.get(0).getToId());

        if (request.exists(ARG_CANCEL)) {
            return new Result(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, fromEntry));
        }


        if (request.exists(ARG_DELETE_CONFIRM)) {
            request.ensureAuthToken();
            getDatabaseManager().delete(Tables.ASSOCIATIONS.NAME, clause);
            fromEntry.setAssociations(null);
            toEntry.setAssociations(null);

            return new Result(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, fromEntry));
        }
        StringBuilder sb     = new StringBuilder();
        StringBuilder hidden = new StringBuilder();

        getPageHandler().entrySectionOpen(request, fromEntry, sb,
                                          msg("Delete Association"), false);

        getRepository().addAuthToken(request, hidden);
        hidden.append(HtmlUtils.hidden(ARG_ASSOCIATION, associationId));
        String form = PageHandler.makeOkCancelForm(request,
                          getRepository().URL_ASSOCIATION_DELETE,
                          ARG_DELETE_CONFIRM, hidden.toString());
        sb.append(
            getPageHandler().showDialogQuestion(
                msg("Are you sure you want to delete the assocation?"),
                form));

        sb.append(associations.get(0).getName());
        sb.append(HtmlUtils.br());
        sb.append(fromEntry.getLabel());
        sb.append(HtmlUtils.pad(HtmlUtils.img(getIconUrl(ICON_ARROW))));
        sb.append(toEntry.getLabel());

        getPageHandler().entrySectionClose(request, fromEntry, sb);

        return new Result(msg("Delete Associations"), sb);
    }




    /**
     * _more_
     *
     * @param request The request
     * @param node _more_
     * @param entries _more_
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String processAssociationXml(Request request, Element node,
                                        Hashtable entries, Hashtable files)
            throws Exception {

        String fromId    = XmlUtil.getAttribute(node, ATTR_FROM);
        String toId      = XmlUtil.getAttribute(node, ATTR_TO);
        Entry  fromEntry = (Entry) entries.get(fromId);
        Entry  toEntry   = (Entry) entries.get(toId);
        if (fromEntry == null) {
            fromEntry = getEntryManager().getEntry(request, fromId);
        }

        if (fromEntry == null) {
            throw new RepositoryUtil.MissingEntryException(
                "Could not find from entry:" + fromId);
        }
        if (toEntry == null) {
            toEntry = getEntryManager().getEntry(request, toId);
        }
        if (toEntry == null) {
            throw new RepositoryUtil.MissingEntryException(
                "Could not find to entry:" + toId);
        }

        return addAssociation(request, fromEntry, toEntry,
                              XmlUtil.getAttribute(node, ATTR_NAME, ""),
                              XmlUtil.getAttribute(node, ATTR_TYPE, ""));
    }



    /**
     * Add an association between the two entries
     *
     * @param request request
     * @param fromEntry _more_
     * @param toEntry _more_
     * @param name _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String addAssociation(Request request, Entry fromEntry,
                                 Entry toEntry, String name, String type)
            throws Exception {
        if ( !getAccessManager().canDoAction(request, fromEntry,
                                             Permission.ACTION_NEW)) {
            throw new IllegalArgumentException("Cannot add association to "
                    + fromEntry);
        }
        if ( !getAccessManager().canDoAction(request, toEntry,
                                             Permission.ACTION_NEW)) {
            throw new IllegalArgumentException("Cannot add association to "
                    + toEntry);
        }
        //Clear the cached associations
        String result =
            addAssociation(request,
                           new Association(getRepository().getGUID(), name,
                                           type, fromEntry.getId(),
                                           toEntry.getId()));
        fromEntry.clearAssociations();
        toEntry.clearAssociations();

        return result;
    }


    /**
     * _more_
     *
     * @param request The request
     * @param association _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String addAssociation(Request request, Association association)
            throws Exception {
        request.ensureAuthToken();
        String id = getRepository().getGUID();
        getDatabaseManager().executeInsert(Tables.ASSOCIATIONS.INSERT,
                                           new Object[] { association.getId(),
                association.getName(), association.getType(),
                association.getFromId(),
                association.getToId() });

        return id;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<String> getTypes() throws Exception {
        if (types == null) {
            Statement stmt =
                getDatabaseManager().select(
                    SqlUtil.distinct(Tables.ASSOCIATIONS.COL_TYPE),
                    Tables.ASSOCIATIONS.NAME, (Clause) null);
            String[] values =
                SqlUtil.readString(getDatabaseManager().getIterator(stmt), 1);
            types = (List<String>) Misc.toList(values);
            types.remove("");
        }

        return new ArrayList<String>(types);
    }




    /**
     * _more_
     *
     * @param request The request
     * @param association _more_
     *
     * @throws Exception On badness
     */
    public void associationChanged(Request request, Association association)
            throws Exception {
        types = null;
        Entry fromEntry = getEntryManager().getEntry(request,
                              association.getFromId());
        if (fromEntry != null) {
            fromEntry.setAssociations(null);
        }
        Entry toEntry = getEntryManager().getEntry(request,
                            association.getToId());
        if (toEntry != null) {
            toEntry.setAssociations(null);
        }

    }


    /**
     * _more_
     *
     * @param request The request
     * @param association _more_
     *
     * @throws Exception On badness
     */
    public void deleteAssociation(Request request, Association association)
            throws Exception {
        request.ensureAuthToken();
        getDatabaseManager().delete(Tables.ASSOCIATIONS.NAME,
                                    Clause.eq(Tables.ASSOCIATIONS.COL_ID,
                                        association.getId()));
        types = null;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param association _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String getAssociationLinks(Request request, String association)
            throws Exception {
        if (true) {
            return BLANK;
        }
        String search =
            HtmlUtils.href(
                request.makeUrl(
                    getRepository().getSearchManager().URL_SEARCH_FORM,
                    ARG_ASSOCIATION,
                    HtmlUtils.urlEncode(association)), HtmlUtils.img(
                        getIconUrl(ICON_SEARCH),
                        msg("Search in association")));

        return search;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Association> getAssociationsWithType(Request request,
            Entry entry, String type)
            throws Exception {
        return getAssociationsWithType(getAssociations(request,
                entry.getId()), type);
    }


    /**
     * _more_
     *
     * @param associations _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Association> getAssociationsWithType(
            List<Association> associations, String type)
            throws Exception {
        List<Association> results = new ArrayList<Association>();
        for (Association association : associations) {
            if ( !Misc.equals(association.getType(), type)) {
                continue;
            }
            results.add(association);
        }

        return results;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getHeadEntriesWithAssociationType(Request request,
            Entry entry, String type)
            throws Exception {

        List<Entry> results = new ArrayList<Entry>();
        for (Association association :
                getAssociationsWithType(request, entry, type)) {
            if ( !association.getFromId().equals(entry.getId())) {
                continue;
            }
            Entry otherEntry = getEntryManager().getEntry(request,
                                   association.getToId());
            if (otherEntry != null) {
                results.add(otherEntry);
            }
        }

        return results;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getTailEntriesWithAssociationType(Request request,
            Entry entry, String type)
            throws Exception {
        List<Entry> results = new ArrayList<Entry>();
        for (Association association :
                getAssociationsWithType(request, entry, type)) {
            if ( !association.getToId().equals(entry.getId())) {
                continue;
            }
            Entry otherEntry = getEntryManager().getEntry(request,
                                   association.getFromId());
            if (otherEntry != null) {
                results.add(otherEntry);
            }
        }

        return results;
    }



    /**
     * _more_
     *
     * @param request The request
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Association> getAssociations(Request request, String entryId)
            throws Exception {
        Entry entry = getEntryManager().getEntry(request, entryId);
        if (entry == null) {
            getLogManager().logError("getAssociations Entry is null:"
                                     + entryId);

            return new ArrayList<Association>();
        }

        return getAssociations(request, entry);
    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Association> getAssociations(Request request, Entry entry)
            throws Exception {
        if (entry.getAssociations() != null) {
            return entry.getAssociations();
        }
        if (entry.isDummy()) {
            return new ArrayList<Association>();
        }

        List<Association> associations =
            getAssociations(
                request,
                Clause.or(
                    Clause.eq(
                        Tables.ASSOCIATIONS.COL_FROM_ENTRY_ID,
                        entry.getId()), Clause.eq(
                            Tables.ASSOCIATIONS.COL_TO_ENTRY_ID,
                            entry.getId())));
        entry.setAssociations(associations);

        return associations;
    }


    /**
     * _more_
     *
     * @param request The request
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public List<Association> getAssociations(Request request, Clause clause)
            throws Exception {
        int max = request.get(ARG_MAX, DB_MAX_ROWS);
        String orderBy = " ORDER BY " + Tables.ASSOCIATIONS.COL_TYPE
                         + " ASC ," + Tables.ASSOCIATIONS.COL_NAME + " ASC ";
        Statement stmt = getDatabaseManager().select(
                             Tables.ASSOCIATIONS.COLUMNS,
                             Tables.ASSOCIATIONS.NAME, clause,
                             orderBy + " "
                             + getDatabaseManager().getLimitString(
                                 request.get(ARG_SKIP, 0), max));
        List<Association> associations = new ArrayList();
        SqlUtil.Iterator  iter = getDatabaseManager().getIterator(stmt);
        ResultSet         results;
        while ((results = iter.getNext()) != null) {
            Association association = new Association(results.getString(1),
                                          results.getString(2),
                                          results.getString(3),
                                          results.getString(4),
                                          results.getString(5));

            Entry fromEntry = getEntryManager().getEntry(request,
                                  association.getFromId());
            Entry toEntry = getEntryManager().getEntry(request,
                                association.getToId());
            if ((fromEntry != null) && (toEntry != null)) {
                associations.add(association);
            }
        }

        return associations;
    }





    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String[] getAssociations(Request request) throws Exception {
        TypeHandler  typeHandler = getRepository().getTypeHandler(request);
        List<Clause> where       = typeHandler.assembleWhereClause(request);
        if (where.size() > 0) {
            where.add(0, Clause.eq(Tables.ASSOCIATIONS.COL_FROM_ENTRY_ID,
                                   Tables.ENTRIES.COL_ID));
            where.add(0, Clause.eq(Tables.ASSOCIATIONS.COL_TO_ENTRY_ID,
                                   Tables.ENTRIES.COL_ID));
        }

        return SqlUtil.readString(
            getDatabaseManager().getIterator(
                typeHandler.select(
                    request, SqlUtil.distinct(Tables.ASSOCIATIONS.COL_NAME),
                    where, "")), 1);
    }



    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param text _more_
     *
     * @return _more_
     */
    public String processText(Request request, Entry entry, String text) {
        int idx = text.indexOf("<more>");
        if (idx >= 0) {
            String first  = text.substring(0, idx);
            String base   = "" + (HtmlUtils.blockCnt++);
            String divId  = "morediv_" + base;
            String linkId = "morelink_" + base;
            String second = text.substring(idx + "<more>".length());
            String moreLink = "javascript:showMore(" + HtmlUtils.squote(base)
                              + ")";
            String lessLink = "javascript:hideMore(" + HtmlUtils.squote(base)
                              + ")";
            text = first + "<br><a " + HtmlUtils.id(linkId) + " href="
                   + HtmlUtils.quote(moreLink)
                   + ">More...</a><div style=\"\" class=\"moreblock\" "
                   + HtmlUtils.id(divId) + ">" + second + "<br>" + "<a href="
                   + HtmlUtils.quote(lessLink) + ">...Less</a>" + "</div>";
        }

        return text;
    }



    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public StringBuilder getAssociationBlock(Request request, Entry entry)
            throws Exception {

        boolean canEdit = getAccessManager().canDoAction(request, entry,
                              Permission.ACTION_EDIT);
        List<Association> associations =
            getAssociationManager().getAssociations(request, entry);
        if (associations.size() == 0) {
            StringBuilder sb = new StringBuilder();

            return sb;
        }

        return getAssociationList(request, associations, entry, canEdit);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param associations _more_
     * @param entry _more_
     * @param canEdit _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public StringBuilder getAssociationList(Request request,
                                            List<Association> associations,
                                            Entry entry, boolean canEdit)
            throws Exception {

        List cols1 = new ArrayList();
        List cols2 = new ArrayList();

        Hashtable<String, StringBuilder> rowMap = new Hashtable<String,
                                                      StringBuilder>();
        List<String> rows         = new ArrayList<String>();
        boolean      lastFromIsMe = false;
        boolean      lastToIsMe   = false;
        for (Association association : associations) {
            Entry  fromEntry = null;
            Entry  toEntry   = null;

            String fromId    = association.getFromId();
            String toId      = association.getToId();
            List   cols      = null;
            if (fromId.equals("this")) {
                cols      = cols1;
                fromEntry = entry;
            }
            if (toId.equals("this")) {
                toEntry = entry;
                cols    = cols2;
            }


            if (fromEntry == null) {
                if ((entry != null) && fromId.equals(entry.getId())) {
                    cols      = cols1;
                    fromEntry = entry;
                } else {
                    fromEntry = getEntryManager().getEntry(request,
                            association.getFromId());
                    cols = cols2;
                }
            }

            if (toEntry == null) {
                if ((entry != null)
                        && association.getToId().equals(entry.getId())) {
                    toEntry = entry;
                } else {
                    toEntry = getEntryManager().getEntry(request,
                            association.getToId());
                }
            }

            //            System.err.println("fromEntry:" + fromEntry +" id:" +fromId);
            //            System.err.println("toEntry:" + toEntry +" id:" + toId);

            if ((fromEntry == null) || (toEntry == null)) {
                continue;
            }
            if (canEdit) {
                cols.add(
                    HtmlUtils.pad(
                        HtmlUtils.href(
                            request.makeUrl(
                                getRepository().URL_ASSOCIATION_DELETE,
                                ARG_ASSOCIATION,
                                association.getId()), HtmlUtils.img(
                                    getRepository().getIconUrl(ICON_DELETE),
                                    msg("Delete association")))) + "&nbsp;");
            } else {
                cols.add("");
            }

            boolean fromIsMe = Misc.equals(fromEntry, entry);
            boolean toIsMe   = Misc.equals(toEntry, entry);
            String  fromLabel;
            String  toLabel;
            if (fromIsMe) {
                fromLabel = lastFromIsMe
                            ? "&nbsp;...&nbsp;"
                            : HtmlUtils.b(fromEntry.getLabel());
            } else {
                fromLabel = getEntryManager().getEntryLink(request,
                        fromEntry, ARG_SHOW_ASSOCIATIONS, "true");
            }
            if (toIsMe) {
                toLabel = lastToIsMe
                          ? "&nbsp;...&nbsp;"
                          : HtmlUtils.b(toEntry.getLabel());
            } else {
                toLabel = getEntryManager().getEntryLink(request, toEntry,
                        ARG_SHOW_ASSOCIATIONS, "true");
            }

            lastFromIsMe = fromIsMe;
            lastToIsMe   = toIsMe;
            cols.add(HtmlUtils.img(getPageHandler().getIconUrl(request,
                    fromEntry)) + HtmlUtils.pad(fromLabel));
            cols.add("&nbsp;&nbsp;" + association.getType() + "&nbsp;&nbsp;");
            //            cols.add(association.getLabel());
            cols.add(HtmlUtils.img(getRepository().getIconUrl(ICON_ARROW)));
            cols.add(HtmlUtils.img(getPageHandler().getIconUrl(request,
                    toEntry)) + HtmlUtils.pad(toLabel));
        }

        List cols = Misc.toList(new Object[] { "&nbsp;",
                HtmlUtils.bold(msg("From")),
                HtmlUtils.bold(msg("&nbsp;&nbsp;Type&nbsp;&nbsp;")),
        /*HtmlUtils.bold(msg("Name")),*/
        "&nbsp;", HtmlUtils.bold(msg("To")) });

        cols.addAll(cols1);
        cols.addAll(cols2);

        return HtmlUtils.table(
            cols, 5,
            HtmlUtils.attr(HtmlUtils.ATTR_CELLSPACING, "3")
            + HtmlUtils.attr(HtmlUtils.ATTR_CELLPADDING, "3"));
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processSearchAssociations(Request request)
            throws Exception {
        StringBuilder sb      = new StringBuilder();
        String        type    = request.getString(ARG_TYPE, "").trim();
        String        name    = request.getString(ARG_NAME, "").trim();
        List<Clause>  clauses = new ArrayList<Clause>();
        if (type.length() > 0) {
            clauses.add(Clause.eq(Tables.ASSOCIATIONS.COL_TYPE, type));
        }

        if (name.length() > 0) {
            if (request.get(ARG_EXACT, false)) {
                clauses.add(Clause.eq(Tables.ASSOCIATIONS.COL_NAME, name));
            } else {
                clauses.add(Clause.like(Tables.ASSOCIATIONS.COL_NAME,
                                        "%" + name + "%"));
            }
        }
        List<Association> associations =
            getAssociationManager().getAssociations(request,
                Clause.and(clauses));
        int     max = request.get(ARG_MAX, DB_MAX_ROWS);
        int     cnt = associations.size();
        boolean showingAll;
        if ((cnt > 0) && ((cnt == max) || request.defined(ARG_SKIP))) {
            showingAll = false;
        } else {
            showingAll = true;
        }

        if (associations.size() == 0) {
            sb.append(
                getPageHandler().showDialogNote(
                    msg("No associations found")));
            getAssociationsSearchForm(request, sb);
        } else {
            getAssociationsSearchForm(request, sb);
            sb.append(HtmlUtils.sectionOpen(null, false));
            getRepository().getHtmlOutputHandler().showNext(request, cnt, sb);
            sb.append(getAssociationManager().getAssociationList(request,
                    associations, null, false));
            sb.append(HtmlUtils.sectionClose());
        }

        return getSearchManager().makeResult(request,
                                             msg("Search Associations"), sb);
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processSearchAssociationsForm(Request request)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        getAssociationsSearchForm(request, sb);

        return getSearchManager().makeResult(request,
                                             msg("Search Associations"), sb);
    }



    /**
     * _more_
     *
     * @param request The request
     * @param sb buffer to append to
     *
     * @throws Exception On badness
     */
    private void getAssociationsSearchForm(Request request, Appendable sb)
            throws Exception {

        sb.append(HtmlUtils.sectionOpen(null, false));
        sb.append(HtmlUtils
            .form(request
                .makeUrl(getRepository().getSearchManager()
                    .URL_SEARCH_ASSOCIATIONS, ARG_NAME,
                        WHAT_ENTRIES), " name=\"searchform\" "));

        sb.append(HtmlUtils.formTable());

        String searchExact = " "
                             + HtmlUtils.checkbox(ARG_EXACT, "true",
                                 request.get(ARG_EXACT, false)) + " "
                                     + msg("Match exactly");
        sb.append(HtmlUtils.formEntry(msgLabel("Name"),
                                      HtmlUtils.input(ARG_NAME,
                                          request.getString(ARG_NAME, ""),
                                          HtmlUtils.SIZE_40) + searchExact));


        List types = getAssociationManager().getTypes();
        types.add(0, new TwoFacedObject(msg("None"), ""));
        if (types.size() > 1) {
            sb.append(HtmlUtils.formEntry(msgLabel("Type"),
                                          HtmlUtils.select(ARG_TYPE, types,
                                              request.getString(ARG_TYPE,
                                                  ""))));
        }


        sb.append(HtmlUtils.formTableClose());

        OutputType output  = request.getOutput(BLANK);
        String     buttons = HtmlUtils.submit(msg("Search"), "submit");
        sb.append(HtmlUtils.p());
        sb.append(buttons);
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.formClose());
        sb.append(HtmlUtils.sectionClose());

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param association _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getOtherEntry(Request request, Association association,
                               Entry entry)
            throws Exception {
        String id;
        if ( !association.getFromId().equals(entry.getId())) {
            id = association.getFromId();
        } else if ( !association.getToId().equals(entry.getId())) {
            id = association.getToId();
        } else {
            return null;
        }

        return getEntryManager().getEntry(request, id);
    }



}
