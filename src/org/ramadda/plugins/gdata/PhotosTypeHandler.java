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
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.acl.*;
import com.google.gdata.data.docs.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.data.extensions.*;
//import com.google.gdata.data.*;
import com.google.gdata.data.media.*;
import com.google.gdata.data.photos.*;
import com.google.gdata.util.*;
import com.google.gdata.util.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;


import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;

import java.io.File;

import java.net.URL;







import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class PhotosTypeHandler extends GdataTypeHandler {

    /** _more_ */
    public static final String PICASA_ROOT =
        "https://picasaweb.google.com/data/feed/api/user/";

    /** _more_ */
    public static final String TYPE_ALBUM = "album";

    /** _more_ */
    public static final String TYPE_PHOTO = "photo";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public PhotosTypeHandler(Repository repository, Element entryNode)
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
        PicasawebService myService = new PicasawebService("ramadda");
        myService.setUserCredentials(userId, password);

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
    public List<String> getAlbumIds(Request request, Entry entry)
            throws Exception {
        List<String> ids = entry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();
        for (Entry album : getAlbumEntries(request, entry)) {
            ids.add(album.getId());
        }
        entry.setChildIds(ids);

        return ids;
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
    public List<Entry> getAlbumEntries(Request request, Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        //        System.err.println("getAlbumEntries from picasa");
        String userId = getUserId(entry);
        if (userId == null) {
            return entries;
        }

        URL feedUrl = new URL(PICASA_ROOT + userId + "?kind=album");
        UserFeed userFeed =
            ((PicasawebService) getService(entry)).getFeed(feedUrl,
                UserFeed.class);
        for (AlbumEntry album : userFeed.getAlbumEntries()) {
            String albumEntryId = getSynthId(entry, TYPE_ALBUM,
                                             album.getGphotoId());
            String       title    = album.getTitle().getPlainText();
            Entry        newEntry = new Entry(albumEntryId, this, true);
            StringBuffer desc     = new StringBuffer();
            addMetadata(newEntry, album, desc);
            entries.add(newEntry);
            newEntry.setIcon("/gdata/picasa.png");
            Date dttm = album.getDate();
            Date now  = new Date();
            //            System.err.println ("Desc:" + desc);
            newEntry.initEntry(title, desc.toString(), entry,
                               getUserManager().getLocalFileUser(),
                               new Resource(), "", Entry.DEFAULT_ORDER,
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), dttm.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
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
        if (synthId == null) {
            return getAlbumIds(request, mainEntry);
        }

        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();


        List<String> toks    = StringUtil.split(synthId, ":");
        String       type    = toks.get(0);
        String       albumId = toks.get(1);

        //        System.err.println("getSynthIds:  parent:" + parentEntry +" ID:" + synthId);

        if (type.equals(TYPE_PHOTO)) {
            return ids;
        }

        for (Entry photoEntry :
                getPhotoEntries(request, mainEntry, parentEntry, albumId)) {
            ids.add(photoEntry.getId());
        }
        parentEntry.setChildIds(ids);

        return ids;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param albumId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getPhotoEntries(Request request, Entry mainEntry,
                                       Entry parentEntry, String albumId)
            throws Exception {
        //        System.err.println("getPhotoEntries from picasa:" + albumId);
        //        System.err.println("mainEntry:" + mainEntry.getName() +" parentEntry:" + parentEntry.getName());
        List<Entry> entries = new ArrayList<Entry>();
        String      userId  = getUserId(mainEntry);
        URL feedUrl = new URL(PICASA_ROOT + userId + "/albumid/" + albumId);
        AlbumFeed feed =
            ((PicasawebService) getService(mainEntry)).getFeed(feedUrl,
                AlbumFeed.class);
        for (PhotoEntry photo : feed.getPhotoEntries()) {
            String name = photo.getTitle().getPlainText();
            String newId = getSynthId(mainEntry, TYPE_PHOTO,
                                      photo.getAlbumId() + ":"
                                      + photo.getGphotoId());
            Entry newEntry = new Entry(newId, this);
            StringBuffer desc =
                new StringBuffer(
                    "<wiki>\n+section label={{name}}\n{{image}}\n");
            addMetadata(newEntry, photo, desc);
            desc.append("\n-section\n");
            entries.add(newEntry);
            //            newEntry.setIcon("/gdata/picasa.png");
            Date dttm = new Date();
            Date timestamp = photo.getTimestamp();
            Resource resource = new Resource();
            java.util.List<com.google.gdata.data.media.mediarss.MediaContent> media =
                photo.getMediaContents();
            if (media.size() > 0) {
                resource = new Resource(media.get(0).getUrl());
                resource.setFileSize(photo.getSize());
            }




            newEntry.initEntry(name, desc.toString(), parentEntry,
                               getUserManager().getLocalFileUser(), resource,
                               "", Entry.DEFAULT_ORDER, dttm.getTime(),
                               dttm.getTime(), timestamp.getTime(),
                               timestamp.getTime(), null);
            com.google.gdata.data.geo.Point point = photo.getGeoLocation();
            if (point != null) {
                newEntry.setNorth(point.getLatitude().doubleValue());
                newEntry.setSouth(point.getLatitude().doubleValue());
                newEntry.setWest(point.getLongitude().doubleValue());
                newEntry.setEast(point.getLongitude().doubleValue());
            }

            java.util.List<com.google.gdata.data.media.mediarss.MediaThumbnail> thumbs =
                photo.getMediaThumbnails();
            //            for(int i=0;i<thumbs.size();i++) {
            if (thumbs.size() > 0) {
                Metadata thumbnailMetadata =
                    new Metadata(getRepository().getGUID(), newId,
                                 ContentMetadataHandler.TYPE_THUMBNAIL,
                                 false, thumbs.get(0).getUrl(), null, null,
                                 null, null);
                getMetadataManager().addMetadata(newEntry, thumbnailMetadata);
            }
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
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
        if ((entry.getId().indexOf(TYPE_ALBUM) >= 0)
                || !getEntryManager().isSynthEntry(entry.getId())) {
            return getRepository().getTypeHandler(TypeHandler.TYPE_GROUP);
        }

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
        String       userId = getUserId(mainEntry);
        List<String> toks   = StringUtil.split(id, ":");
        String       type   = toks.get(0);
        if (type.equals(TYPE_ALBUM)) {
            for (Entry album : getAlbumEntries(request, mainEntry)) {
                if (album.getId().endsWith(id)) {
                    return album;
                }
            }

            return null;
        }

        String albumId      = toks.get(1);
        String albumEntryId = getSynthId(mainEntry, TYPE_ALBUM, albumId);
        Entry  albumEntry = getEntryManager().getEntry(request, albumEntryId);

        //        System.err.println("makeSynth: albumId:" +  albumId +" albumEntryId:" + albumEntryId +" album entry:" + albumEntry);

        String photoEntryId = getSynthId(mainEntry, TYPE_PHOTO,
                                         toks.get(1) + ":" + toks.get(2));
        for (Entry photoEntry :
                getPhotoEntries(request, mainEntry, albumEntry, albumId)) {
            if (photoEntry.getId().equals(photoEntryId)) {
                return photoEntry;
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
        if (entry.getId().indexOf(TYPE_PHOTO) >= 0) {
            return getIconUrl("/icons/jpg.png");
        }

        return getIconUrl("/gdata/picasa.png");
    }




}
