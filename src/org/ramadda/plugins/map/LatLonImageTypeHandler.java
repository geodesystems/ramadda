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

package org.ramadda.plugins.map;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.job.*;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.GenericTypeHandler;
import org.ramadda.util.Bounds;
import org.ramadda.util.GeoUtils;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class LatLonImageTypeHandler extends GenericTypeHandler {



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public LatLonImageTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeNewEntry(Request request, Entry entry)
            throws Exception {
        super.initializeNewEntry(request, entry);
        String path = entry.getResource().getPath();
        String _path = path.toLowerCase();
        if ( !(
               _path.endsWith(".tif")   || _path.endsWith(".tiff")  || _path.endsWith(".grd"))) {
            
            return;
        }
        String gdal =
            getRepository().getProperty("service.gdal",
                                        (String) null);

        if (gdal == null) {
            System.err.println("no gdal");
            return;
        }

        String gdalWarp = gdal+"/gdalwarp";
        String gdalInfo =gdal +"/gdalinfo";
        String gdalTranslate = gdal+"/gdal_translate";
        String gdalDem = gdal+"/gdaldem";
        File         work =  getStorageManager().getScratchDir().getDir();
        File srcTiff = entry.getFile();
        List<String> commands;
        JobManager.CommandResults results;
        JobManager job = getRepository().getJobManager();
        
        if(_path.endsWith(".grd")) {
            File tmp = getStorageManager().getTmpFile(request, "tmp.tif");
            results = job.executeCommand((List<String>)Utils.makeList(gdalDem, "hillshade","-of","GTiff", srcTiff.toString(),tmp.toString()), work);
            

            srcTiff = tmp;
        }

        File tmpTiff = getStorageManager().getTmpFile(request, "tmp.tif");


        
        //gdalwarp f41078a1.tif outfile.tif -t_srs "+proj=longlat +ellps=WGS84"
        commands = (List<String>)Utils.add(null, gdalWarp, srcTiff.toString(),
                  tmpTiff.toString(),
                  "-t_srs",
                  "+proj=longlat +ellps=WGS84");
        results = job.executeCommand(commands, work);
        String err = results.getStderrMsg();
        if(err.length()>0) {
            throw new IllegalArgumentException("georeferencing geotiff failed:" + err);
        }

        File png = getStorageManager().getTmpFile(
                       request,
                       IOUtil.stripExtension(
                           getStorageManager().getFileTail(entry)) + ".png");

        commands = (List<String>)Utils.makeList(gdalTranslate, "-of", "PNG", tmpTiff.toString(),
                                  png.toString());
        results = job.executeCommand(commands,work);
        results = job.executeCommand((List<String>)Utils.makeList(gdalInfo, png.toString()),work);

        File newFile = getStorageManager().moveToEntryDir(entry, png);
        String attachment = getStorageManager().copyToEntryDir(entry,
                                entry.getFile()).getName();
        Metadata metadata =
            new Metadata(getRepository().getGUID(), entry.getId(),
                         ContentMetadataHandler.TYPE_ATTACHMENT, false,
                         attachment, null, null, null, null);

        getMetadataManager().addMetadata(entry, metadata);
        getStorageManager().deleteFile(entry.getFile());
        entry.getResource().setPath(newFile.toString());
        Bounds bounds =    GeoUtils.parseGdalInfo(results.getStdoutMsg());
        if (bounds != null) {
            entry.setBounds(bounds);
        }


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        try {
            if ((entry == null) || !entry.hasAreaDefined()) {
                return false;
            }

            //Only set the width if the latlonentry is the main displayed entry

            if (entry.getId().equals(request.getString(ARG_ENTRYID, ""))) {
                int width  = (int) entry.getValue(0, -1);
                int height = (int) entry.getValue(1, -1);
                if ((width > 0) && (height > 0)) {
                    map.setWidth(width);
                    map.setHeight(height);
                }
            }

            String url =
                getRepository().getHtmlOutputHandler().getImageUrl(request,
                    entry);

            boolean visible = true;
            /*
               if (request.getExtraProperty("wmslayershow") == null) {
                request.putExtraProperty("wmslayershow", "true");
                visible = true;
                }*/

            String desc =
                getRepository().getMapManager().makeInfoBubble(request,
                    entry, true);
            map.addJS(HtmlUtils.call("theMap.addImageLayer",
                                     HtmlUtils.jsMakeArgs(false,
                                         HtmlUtils.squote(entry.getId()),
                                         HtmlUtils.squote(entry.getName()),
                                         HtmlUtils.squote(desc),
                                         HtmlUtils.squote(url), "" + visible,
                                         "" + entry.getNorth(),
                                         "" + entry.getWest(),
                                         "" + entry.getSouth(),
                                         "" + entry.getEast(), "400",
                                         "400")));

            map.addJS("\n");

            return false;

        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMapSelector(Request request, Entry entry, MapInfo map)
            throws Exception {
        if (entry == null) {
            return false;
        }
        if (entry.hasAreaDefined()) {
            String url =
                getRepository().getHtmlOutputHandler().getImageUrl(request,
                    entry);
            map.addJS(HtmlUtils.call("theMap.addImageLayer",
                                     HtmlUtils.jsMakeArgs(false,
                                         HtmlUtils.squote(entry.getId()),
                                         HtmlUtils.squote(entry.getName()),
                                         HtmlUtils.squote(""),
                                         HtmlUtils.squote(url), "true",
                                         "" + entry.getNorth(),
                                         "" + entry.getWest(),
                                         "" + entry.getSouth(),
                                         "" + entry.getEast(), "400", "400",
                                         "{forSelect:true}")));

            map.addJS("\n");
        }

        return super.addToMapSelector(request, entry, map);
    }

}
