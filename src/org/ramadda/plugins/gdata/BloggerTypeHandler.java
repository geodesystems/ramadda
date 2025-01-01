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
import com.google.gdata.data.BaseEntry;

import com.google.gdata.data.Feed;
import com.google.gdata.data.MediaContent;
//import com.google.gdata.data.*;
import com.google.gdata.data.Person;
import com.google.gdata.data.TextContent;
import com.google.gdata.data.acl.*;
import com.google.gdata.data.docs.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.data.media.*;
import com.google.gdata.data.photos.*;
import com.google.gdata.util.*;
import com.google.gdata.util.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.StringUtil;

import java.io.File;

import java.net.URL;







import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class BloggerTypeHandler extends GdataTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BloggerTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    protected GoogleService getService(Entry entry) throws Exception {
        GoogleService myService = new GoogleService("blogger",
                                      "exampleCo-exampleApp-1");

        //        myService.setUserCredentials("user@example.com", "secretPassword");
        return myService;
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
    public List<Comment> getComments(Request request, Entry entry)
            throws Exception {
        if ( !getEntryManager().isSynthEntry(entry.getId())) {
            return null;
        }
        String[]      pair      = getEntryManager().getSynthId(entry.getId());
        Entry         mainEntry = getEntryManager().getEntry(request,
                                      pair[0]);
        List<Comment> comments  = new ArrayList<Comment>();
        String        blogId    = mainEntry.getValue(2, (String) null);
        if (blogId == null) {
            return comments;
        }
        String commentsFeedUri = "http://www.blogger.com/feeds/" + blogId
                                 + "/" + pair[1] + "/comments/default";
        System.err.println(commentsFeedUri);
        URL  feedUrl    = new URL(commentsFeedUri);
        Feed resultFeed = getService(mainEntry).getFeed(feedUrl, Feed.class);
        // Display the results
        System.out.println(resultFeed.getTitle().getPlainText());
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            com.google.gdata.data.Entry commentEntry =
                resultFeed.getEntries().get(i);

            Comment comment =
                new Comment(commentEntry.getId(), entry, mainEntry.getUser(),
                            new Date(commentEntry.getUpdated().getValue()),
                            "",
                            ((TextContent) commentEntry.getContent())
                                .getContent().getPlainText());

            comments.add(comment);

        }

        return comments;
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
        if (synthId != null) {
            return ids;
        }
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();
        List<Entry> entries = getBlogEntries(request, mainEntry, parentEntry,
                                             synthId);
        for (Entry entry : entries) {
            ids.add(entry.getId());
        }

        return ids;
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
    public List<Entry> getBlogEntries(Request request, Entry mainEntry,
                                      Entry parentEntry, String synthId)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        String      blogId  = mainEntry.getValue(2, (String) null);
        if (blogId == null) {
            return entries;
        }
        URL feedUrl = new URL("http://www.blogger.com/feeds/" + blogId
                              + "/posts/default");
        System.err.println(feedUrl);
        Feed resultFeed = getService(mainEntry).getFeed(feedUrl, Feed.class);
        System.out.println(resultFeed.getTitle().getPlainText());
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            com.google.gdata.data.Entry entry =
                resultFeed.getEntries().get(i);
            List<String> toks = StringUtil.split(entry.getId(), "-");
            System.err.println("entry id:" + toks.get(toks.size() - 1));
            String entryId = getSynthId(mainEntry, toks.get(toks.size() - 1));
            String       title    = entry.getTitle().getPlainText();
            Entry        newEntry = new Entry(entryId, this, false);
            StringBuffer desc     = new StringBuffer();
            if ((entry.getContent() != null)
                    && (entry.getContent() instanceof TextContent)) {
                desc.append(((TextContent) entry.getContent()).getContent()
                    .getPlainText());
            }

            addMetadata(newEntry, entry, desc);
            entries.add(newEntry);
            Resource resource    = new Resource();

            Date     publishTime = new Date(entry.getPublished().getValue());
            Date     editTime    = new Date(entry.getUpdated().getValue());
            newEntry.initEntry(title, desc.toString(), mainEntry,
                               mainEntry.getUser(), resource, "",
                               Entry.DEFAULT_ORDER, publishTime.getTime(),
                               editTime.getTime(), publishTime.getTime(),
                               editTime.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }

    /*
    public TypeHandler getTypeHandlerForCopy(Entry entry) throws Exception {
                if(entry.getId().indexOf(TYPE_FOLDER)>=0)  {
            return getRepository().getTypeHandler(TypeHandler.TYPE_GROUP);
        }
        if(!getEntryManager().isSynthEntry(entry.getId())) return this;
        return getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
    }
    */




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
        List<Entry> entries = getBlogEntries(request, mainEntry, null, id);
        for (Entry entry : entries) {
            if (entry.getId().endsWith(id)) {
                return entry;
            }
        }

        return null;
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

        return super.getIconUrl(request, entry);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {}


}
