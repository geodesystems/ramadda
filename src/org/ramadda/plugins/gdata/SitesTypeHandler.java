/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.gdata;


import org.ramadda.repository.util.SelectInfo;

import com.google.gdata.client.*;
import com.google.gdata.client.*;
import com.google.gdata.client.calendar.*;

import com.google.gdata.client.docs.*;
import com.google.gdata.client.photos.*;


import com.google.gdata.client.sites.*;
import com.google.gdata.data.BaseEntry;
import com.google.gdata.data.MediaContent;
//import com.google.gdata.data.*;
import com.google.gdata.data.Person;
import com.google.gdata.data.PlainTextConstruct;


import com.google.gdata.data.TextConstruct;
import com.google.gdata.data.TextContent;
import com.google.gdata.data.acl.*;

//import com.google.gdata.data.*;
import com.google.gdata.data.acl.*;
import com.google.gdata.data.docs.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.data.media.*;
import com.google.gdata.data.media.*;
import com.google.gdata.data.photos.*;
import com.google.gdata.data.sites.*;
import com.google.gdata.data.sites.CommentEntry;

import com.google.gdata.data.spreadsheet.Column;
import com.google.gdata.util.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;

import java.io.File;

import java.net.URL;





import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class SitesTypeHandler extends GdataTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public SitesTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param userId _more_
     * @param password _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected GoogleService doMakeService(String userId, String password)
            throws Exception {
        SitesService service = new SitesService("unavco-ramadda-v1");
        service.setUserCredentials(userId, password);

        return service;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {
        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }

        return getSynthIds(request, select, mainEntry, parentEntry, synthId,
                           new Hashtable<String, Entry>());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     * @param entryMap _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId,
                                    Hashtable<String, Entry> entryMap)
            throws Exception {
        List<String> ids      = new ArrayList<String>();
        SitesService client   = new SitesService("ramadda");
        String       user     = getUserId(mainEntry);
        String       password = getPassword(mainEntry);
        if ((user != null) && (password != null)) {
            client.setUserCredentials(user, password);
        }
        String      site        = mainEntry.getValue(2, "");
        String url = "https://sites.google.com/feeds/content/site/" + site;
        SiteFeed    contentFeed = client.getFeed(new URL(url),
                                      SiteFeed.class);
        List<Entry> entries     = new ArrayList<Entry>();
        for (BaseEntry entry : contentFeed.getEntries()) {
            entry = entry.getAdaptedEntry();
            String parentId = getParentId(entry);
            if (parentId == null) {
                parentId = mainEntry.getId();
            } else {
                parentId = getSynthId(mainEntry, parentId);
            }
            String title   = entry.getTitle().getPlainText();
            String entryId = getEntryId(entry);
            Entry newEntry = new Entry(getSynthId(mainEntry, entryId), this,
                                       true);
            String blob = "";
            if (entry instanceof BaseContentEntry) {
                blob = getContentBlob((BaseContentEntry) entry);
            }


            addMetadata(newEntry, entry);
            Resource resource = new Resource();
            if (entry instanceof AttachmentEntry) {
                URL attachmentUrl =
                    new URL(((com.google.gdata.data
                        .OutOfLineContent) entry.getContent()).getUri());
                //                System.err.println("url:" + attachmentUrl);
                String host = "https://" + attachmentUrl.getHost();
                String path = host + "/site/" + site + "/file-cabinet/"
                              + title;
                //                https://2864194698236861674-a-1802744773732722657-s-sites.googlegroups.com/feeds/media/content/site/nlaswiki/6304351182338674339
                //                https://2864194698236861674-a-1802744773732722657-s-sites.googlegroups.com/site/nlaswiki/file-cabinet/asprs_las_format_v12-1.pdf
                resource = new Resource(path);
            }
            Date now = new Date();

            newEntry.initEntry(title, blob, null, mainEntry.getUser(),
                               resource, "", Entry.DEFAULT_ORDER,
                               now.getTime(), now.getTime(), now.getTime(),
                               now.getTime(), null);
            newEntry.setParentEntryId(parentId);
            newEntry.setChildIds(new ArrayList<String>());
            entries.add(newEntry);
            entryMap.put(newEntry.getId(), newEntry);
        }
        String lookingForId = ((synthId != null)
                               ? getSynthId(mainEntry, synthId)
                               : mainEntry.getId());
        for (Entry entry : entries) {
            Entry parent = entryMap.get(entry.getParentEntryId());
            if (parent == null) {
                parent = mainEntry;
            }
            if (parent.getChildIds() == null) {
                parent.setChildIds(new ArrayList<String>());
            }
            parent.getChildIds().add(entry.getId());
            entry.setParentEntry(parent);
            if (entry.getParentEntryId().equals(lookingForId)) {
                ids.add(entry.getId());
            }
            getEntryManager().cacheSynthEntry(entry);
        }

        return ids;
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getTypeHandlerForCopy(Entry entry) throws Exception {
        //        if (entry.getId().indexOf(TYPE_FOLDER) >= 0 ||  !getEntryManager().isSynthEntry(entry.getId())) {
        //            return getRepository().getTypeHandler(TypeHandler.TYPE_GROUP);
        //        }
        return getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry mainEntry, String id)
            throws Exception {

        Hashtable<String, Entry> map = new Hashtable<String, Entry>();
        getSynthIds(request, mainEntry, null, "", map);

        return map.get(getSynthId(mainEntry, id));
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
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {
        String id = entry.getId();
        if ( !getEntryManager().isSynthEntry(id)) {
            return super.getIconUrl(request, entry);
        }
        if ((entry.getResource().getPath() != null)
                && (entry.getResource().getPath().length() > 0)) {
            return getIconUrlFromPath(entry.getName());
        }

        return getIconUrl("/icons/folder.png");
        /*
        if (id.indexOf(TYPE_FOLDER) >= 0) {

        }
        if (id.indexOf(TYPE_DOCUMENT) >= 0) {
            return getIconUrl("/gdata/document.gif");
        }
        if (id.indexOf(TYPE_PRESENTATION) >= 0) {
            return getIconUrl("/gdata/presentation.gif");
        }
        if (id.indexOf(TYPE_DRAWING) >= 0) {
            return getIconUrl("/gdata/drawing.gif");
        }
        if (id.indexOf(TYPE_SPREADSHEET) >= 0) {
            return getIconUrl("/gdata/spreadsheet.gif");
        }
        if (id.indexOf(TYPE_PDF) >= 0) {
            return getIconUrl("/icons/pdf.png");
            }*/
    }




    /**
     * _more_
     *
     * @param link _more_
     *
     * @return _more_
     */
    public static String getId(com.google.gdata.data.Link link) {
        if (link == null) {
            return null;
        }

        return getEntryId(link.getHref());
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public static String getParentId(BaseEntry entry) {
        String id = getParentIdInner(entry);
        if (id == null) {
            //            for(com.google.gdata.data.Link link: (List<com.google.gdata.data.Link>)entry.getLinks()) {
            //                System.err.println("link:" + link.getRel() +" " + link.getHref());
            //            }
        }

        return id;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public static String getParentIdInner(BaseEntry entry) {
        if (entry instanceof AnnouncementEntry) {
            return getId(((AnnouncementEntry) entry).getParentLink());
        }
        if (entry instanceof AnnouncementsPageEntry) {
            return getId(((AnnouncementsPageEntry) entry).getParentLink());
        }
        if (entry instanceof FileCabinetPageEntry) {
            return getId(((FileCabinetPageEntry) entry).getParentLink());
        }
        if (entry instanceof ListPageEntry) {
            return getId(((ListPageEntry) entry).getParentLink());
        }
        if (entry instanceof ListItemEntry) {
            return getId(((ListItemEntry) entry).getParentLink());
        }
        //        if(entry instanceof PageEntry) return getId(((PageEntry) entry).getParentLink());
        if (entry instanceof WebPageEntry) {
            return getId(((WebPageEntry) entry).getParentLink());
        }
        if (entry instanceof AttachmentEntry) {
            return getId(((AttachmentEntry) entry).getParentLink());
        }

        return null;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {

        SitesService client = new SitesService("ramadda");
        client.setUserCredentials("info@ramadda.org", args[0]);
        String   url = "https://sites.google.com/feeds/content/site/nlaswiki";
        SiteFeed contentFeed = client.getFeed(new URL(url), SiteFeed.class);
        for (BaseEntry entry : contentFeed.getEntries()) {
            entry = entry.getAdaptedEntry();
            String parentId = getParentId(entry);
            if (parentId != null) {
                System.out.println("title: "
                                   + entry.getTitle().getPlainText()
                                   + " id: " + getEntryId(entry) + " "
                                   + entry.getClass().getName() + " parent:"
                                   + parentId);
            } else {
                System.out.println("*** title: "
                                   + entry.getTitle().getPlainText()
                                   + " id: " + getEntryId(entry) + " "
                                   + entry.getClass().getName());
            }
        }


        if (true) {
            return;
        }

        for (WebPageEntry entry :
                contentFeed.getEntries(WebPageEntry.class)) {
            System.out.println("web page title: "
                               + entry.getTitle().getPlainText() + " id: "
                               + getEntryId(entry));
            if (entry.getParentLink() != null) {
                System.out.println(
                    "\tparent id: "
                    + getEntryId(entry.getParentLink().getHref()));
            }
            //            System.out.println(" author: " + entry.getAuthors().get(0).getEmail());
            //            System.out.println(" content: " + getContentBlob(entry));
        }

        for (ListPageEntry entry :
                contentFeed.getEntries(ListPageEntry.class)) {
            //            System.out.println(" List Page title: " + entry.getTitle().getPlainText() + " id: " + getEntryId(entry));
            for (com.google.gdata.data.spreadsheet.Column col :
                    entry.getData().getColumns()) {
                //                System.out.print(" [" + col.getIndex() + "] " + col.getName() + "\t");
            }
        }

        for (ListItemEntry entry :
                contentFeed.getEntries(ListItemEntry.class)) {
            for (com.google.gdata.data.spreadsheet.Field field :
                    entry.getFields()) {
                //                System.out.print(" [" + field.getIndex() + "] " + field.getValue() + "\t");
            }
            //            System.out.println("\n");
        }

        for (FileCabinetPageEntry entry :
                contentFeed.getEntries(FileCabinetPageEntry.class)) {
            System.out.println("FileCabinetPageEntry title: "
                               + entry.getTitle().getPlainText() + " id: "
                               + getEntryId(entry));
            //           System.out.println(" content: " + getContentBlob(entry));
        }


        for (CommentEntry entry :
                contentFeed.getEntries(CommentEntry.class)) {
            //            System.out.println("comment in-reply-to: " + entry.getInReplyTo().toString());
            //            System.out.println(" content: " + getContentBlob(entry));
        }

        for (AnnouncementsPageEntry entry :
                contentFeed.getEntries(AnnouncementsPageEntry.class)) {
            System.out.println("AnnouncementsPageEntry title: "
                               + entry.getTitle().getPlainText() + " id: "
                               + getEntryId(entry));
            //            System.out.println(" content: " + getContentBlob(entry));
        }

        for (AnnouncementEntry entry :
                contentFeed.getEntries(AnnouncementEntry.class)) {
            System.out.println("AnnouncementEntry title: "
                               + entry.getTitle().getPlainText() + " id: "
                               + getEntryId(entry));
            if (entry.getParentLink() != null) {
                System.out.println(
                    "\tparent id: "
                    + getEntryId(entry.getParentLink().getHref()));
            }
            //            System.out.println(" content: " + getContentBlob(entry));
        }

        for (AttachmentEntry entry :
                contentFeed.getEntries(AttachmentEntry.class)) {
            System.out.println("AttachmentEntry title: "
                               + entry.getTitle().getPlainText() + " id: "
                               + getEntryId(entry));
            if (entry.getParentLink() != null) {
                System.out.println(
                    "\tparent id: "
                    + getEntryId(entry.getParentLink().getHref()));
            }
            if (entry.getSummary() != null) {
                //                System.out.println(" description: " + entry.getSummary().getPlainText());
            }
            //            System.out.println(" revision: " + entry.getRevision().getValue());
            //            MediaContent content = (MediaContent) entry.getContent();
            //            System.out.println(" src: " + content.getUri());
            //            System.out.println(" content type: " + content.getMimeType().getMediaType());
        }


        for (WebAttachmentEntry entry :
                contentFeed.getEntries(WebAttachmentEntry.class)) {
            System.out.println("WebAttachmentEntry title: "
                               + entry.getTitle().getPlainText() + " id: "
                               + getEntryId(entry));
            if (entry.getParentLink() != null) {
                System.out.println(
                    "\tparent id: "
                    + getEntryId(entry.getParentLink().getHref()));
            }
            if (entry.getSummary() != null) {
                //                System.out.println(" description: " + entry.getSummary().getPlainText());
            }
            //            System.out.println(" src: " + ((MediaContent) entry.getContent()).getUri());
        }
        //        for (SiteEntry entry : siteFeed.getEntries()){        }

    }

    /**
     * _more_
     *
     * @param selfLink _more_
     *
     * @return _more_
     */
    private static String getEntryId(String selfLink) {
        return selfLink.substring(selfLink.lastIndexOf("/") + 1);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private static String getEntryId(com.google.gdata.data.BaseEntry entry) {
        return getEntryId(entry.getId());
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public static String getContentBlob(BaseContentEntry<?> entry) {
        Object content = entry.getContent();
        if (content == null) {
            return "";
        }
        if (content instanceof TextContent) {
            TextConstruct tc = ((TextContent) content).getContent();
            if (tc instanceof com.google.gdata.data.XhtmlTextConstruct) {
                return ((com.google.gdata.data.XhtmlTextConstruct) tc)
                    .getXhtml().getBlob();
            }
            if (tc instanceof com.google.gdata.data.PlainTextConstruct) {
                return ((com.google.gdata.data.PlainTextConstruct) tc)
                    .getPlainText();
            }
        }

        //        System.err.println(content.getClass().getName());
        return "";
        /*


        }
        return "";
        */
    }


}
