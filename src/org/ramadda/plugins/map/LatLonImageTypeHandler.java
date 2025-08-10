/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.job.*;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.GenericTypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.GeoUtils;

import org.w3c.dom.Element;
import ucar.unidata.util.IOUtil;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import java.util.zip.*;

@SuppressWarnings("unchecked")
//public class LatLonImageTypeHandler extends GenericTypeHandler {
public class LatLonImageTypeHandler extends GdalTypeHandler {
    boolean addedScriptPaths = false;
    public LatLonImageTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry, NewType newType)
	throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(isType("geo_geotiff")) return;
        String  path  = entry.getResource().getPath();
        String  _path = path.toLowerCase();
        boolean isKmz = _path.endsWith(".kmz");
        if ( !(isKmz || _path.endsWith(".tif") || _path.endsWith(".tiff")
                || _path.endsWith(".grd") || _path.endsWith(".asc")
                || _path.endsWith(".adf"))) {
            return;
        }
        String gdal = getRepository().getScriptPath("service.gdal");

        if (!stringDefined(gdal)) {
            System.err.println("no gdal");
            return;
        }

        String                    gdalWarp      = gdal + "/gdalwarp";
        String                    gdalInfo      = gdal + "/gdalinfo";
        String                    gdalTranslate = gdal + "/gdal_translate";
        String                    gdalDem       = gdal + "/gdaldem";
	if(!addedScriptPaths) {
	    getRepository().addScriptPath(gdalWarp, gdalInfo, gdalTranslate, gdalDem);
	    addedScriptPaths = true;
	}

        File work = getStorageManager().getScratchDir().getDir();
        File                      srcTiff       = entry.getFile();
        List<String>              commands;
        JobManager.CommandResults results;
        JobManager                job       = getRepository().getJobManager();

        File                      imageFile = null;
        if (isKmz) {
            KmlTypeHandler.initializeKmlEntry(request, entry, true);
            ZipInputStream zin =
                new ZipInputStream(
                    repository.getStorageManager().getFileInputStream(
                        entry.getResource().getPath()));
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                String name  = ze.getName().toLowerCase();
                String _name = name.toLowerCase();
                if (IOUtil.isImageFile(_name)) {
                    imageFile = getStorageManager().getTmpFile(request,
                            getStorageManager().getFileTail(name));
                    IOUtil.writeTo(zin, new FileOutputStream(imageFile));

                    break;
                }
            }
            IOUtil.close(zin);
            if (imageFile == null) {
                throw new IllegalArgumentException("no image file found");
            }

        } else {
            if (request.get("makehillshade", false)) {
                File tmp = getStorageManager().getTmpFile(request, "tmp.tif");
                results =
                    job.executeCommand((List<String>) Utils.makeListFromValues(gdalDem,
                        "hillshade", "-of", "GTiff", srcTiff.toString(),
                        tmp.toString()), work);
                srcTiff = tmp;
            }
            File tmpTiff = getStorageManager().getTmpFile(request, "tmp.tif");
            commands = (List<String>) Utils.add(null, gdalWarp,
                    srcTiff.toString(), tmpTiff.toString(), "-t_srs",
                    "+proj=longlat +ellps=WGS84");
            results = job.executeCommand(commands, work);
            String err = results.getStderrMsg();
            if (err.length() > 0) {
		System.err.println("TYPE:" + getType() +" entry:" + entry);
                throw new IllegalArgumentException(
						   "georeferencing geotiff failed:" + err+" type:" + this.getType() + " src tiff:" + srcTiff);
            }
            imageFile = getStorageManager().getTmpFile(
                request,
                IOUtil.stripExtension(getStorageManager().getFileTail(entry))
                + ".png");
            commands = (List<String>) Utils.makeListFromValues(gdalTranslate, "-of",
                    "PNG", tmpTiff.toString(), imageFile.toString());
            results = job.executeCommand(commands, work);
            results =
                job.executeCommand((List<String>) Utils.makeListFromValues(gdalInfo,
                    imageFile.toString()), work);
            Bounds bounds = GeoUtils.parseGdalInfo(results.getStdoutMsg());
            if (bounds != null) {
                entry.setBounds(bounds);
            }
        }

        File newFile = getStorageManager().moveToEntryDir(entry, imageFile);
        String attachment = getStorageManager().copyToEntryDir(entry,
                                entry.getFile()).getName();
        Metadata metadata =
            new Metadata(getRepository().getGUID(), entry.getId(),
                         getMetadataManager().findType(ContentMetadataHandler.TYPE_ATTACHMENT), false,
                         attachment, null, null, null, null);

        getMetadataManager().addMetadata(request,entry, metadata);
        getStorageManager().deleteFile(entry.getFile());
        entry.getResource().setPath(newFile.toString());

    }

    @Override
    public void getFileExtras(Request request, Entry entry, Appendable sb)
            throws Exception {
        super.getFileExtras(request, entry,sb);
        String mine = "If data then:<br>"
                      + HtmlUtils.checkbox("makehillshade", "true", false)
                      + " Make hillshade<br>";

        sb.append(mine);
	sb.append("<br>");
    }

    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        try {
            if ((entry == null) || !entry.hasAreaDefined(request)) {
		System.err.println(" no area");
                return false;
            }

	    Double rotation = (Double) entry.getValue(request,"rotation");
	    String url=getImageUrl(request, entry);
	    if(!stringDefined(url)) return false;

            //Only set the width if the latlonentry is the main displayed entry
            if (entry.getId().equals(request.getString(ARG_ENTRYID, ""))) {
                int width  = (int) entry.getIntValue(request,"map_width", -1);
                int height = (int) entry.getIntValue(request,"map_height", -1);
                if ((width > 0) && (height > 0)) {
                    map.setWidth("" + width);
                    map.setHeight("" + height);
                }
            }

            boolean visible = true;
            /*
               if (request.getExtraProperty("wmslayershow") == null) {
                request.putExtraProperty("wmslayershow", "true");
                visible = true;
                }*/

            String desc =
                getRepository().getMapManager().makeInfoBubble(request,
							       entry, null,true);
	    String args="{}";
	    if(rotation!=null) args = "{rotation:" + rotation+"}";
            map.addJS(HtmlUtils.call("theMap.addImageLayer",
                                     HtmlUtils.jsMakeArgs(false,
                                         HtmlUtils.squote(entry.getId()),
                                         HtmlUtils.squote(entry.getName()),
                                         HtmlUtils.squote(desc),
                                         HtmlUtils.squote(url), "" + visible,
                                         "" + entry.getNorth(request),
                                         "" + entry.getWest(request),
                                         "" + entry.getSouth(request),
                                         "" + entry.getEast(request),
							  "400",
							  "400",args)));

            map.addJS("\n");

            return false;

        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private String getImageUrl(Request request, Entry entry) throws Exception {
	String url=null;
	if(isType("geo_geotiff")|| isType("geo_envi_data")) {
	    String[]tuple = getMetadataManager().getThumbnailUrl(request, entry);
	    if(tuple!=null) url  = tuple[0];
	} else {
	    url =
		getRepository().getHtmlOutputHandler().getImageUrl(request,
								   entry);

	}
	return url;
    }

    @Override
    public boolean addToMapSelector(Request request, Entry entry, Entry forEntry, MapInfo map)
            throws Exception {
        if (entry == null) {
            return false;
        }
        if (entry.hasAreaDefined(request)) {
	    String url=getImageUrl(request, entry);
	    if(!stringDefined(url)) return false;
	    boolean mine = (entry!=null && forEntry!=null && entry.getId().equals(forEntry.getId()));
            map.addJS(HtmlUtils.call("theMap.addImageLayer",
                                     HtmlUtils.jsMakeArgs(false,
                                         HtmlUtils.squote(entry.getId()),
                                         HtmlUtils.squote(entry.getName()),
                                         HtmlUtils.squote(""),
                                         HtmlUtils.squote(url), "true",
                                         "" + entry.getNorth(request),
                                         "" + entry.getWest(request),
                                         "" + entry.getSouth(request),
                                         "" + entry.getEast(request), "400", "400",
							  "{forSelect:" + mine+"}")));

            map.addJS("\n");
        }

        return super.addToMapSelector(request, entry, forEntry, map);
    }

}
