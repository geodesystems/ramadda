/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.*;

import org.ramadda.service.Service;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.nc2.units.DateUnit;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;
import java.awt.image.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


@SuppressWarnings("unchecked")
public class GdalTypeHandler extends GenericTypeHandler {

    public GdalTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {

        StringBuilder sb = new StringBuilder();
        if ( !tag.startsWith("geotiffproxy")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }

	if(Utils.getProperty(props,"useThumbnail",false)) {
	    String[]tuple = getMetadataManager().getThumbnailUrl(request, entry);
	    if(tuple!=null) {
		String imageUrl  = tuple[0];
		return HU.image(imageUrl,"width","90%");
	    }
	}
	int size= Utils.getProperty(props,"maxSize",-1);
	if(size>=0 && entry.getResource().getFileSize()>size) {
	    return Utils.getProperty(props,"message","");
	}
	String wikiText = "<img onload=\"HtmlUtils.initLoadingImage(this)\" class=\"ramadda-image-loading\" width=50% src=\"{{root}}/entry/action?action=geotiff.makeimage&entryid={{entryid}}\">";
	return getWikiManager().wikifyEntry(request, entry,wikiText);
    }

    @Override
    public void handleServiceResults(Request request, Entry entry,
                                     Service service, ServiceOutput output)
            throws Exception {
        super.handleServiceResults(request, entry, service, output);
        List<Entry> entries = output.getEntries();
        if (entries.size() != 0) {
            return;
        }
        String results = output.getResults();
        //      System.err.println("results:" + results);
        /*
Upper Left  (  -28493.167, 4255884.544) (117d38'27.05"W, 33d56'37.74"N)
Lower Left  (  -28493.167, 4224973.143) (117d38'27.05"W, 33d39'53.81"N)
Upper Right (    2358.212, 4255884.544) (117d18'28.38"W, 33d56'37.74"N)
Lower Right (    2358.212, 4224973.143) (117d18'28.38"W, 33d39'53.81"N)
        */

        double ulLat  = Double.NaN, ulLon = Double.NaN;
        double llLat  = Double.NaN, llLon = Double.NaN;
        double urLat  = Double.NaN, urLon = Double.NaN;
        double lrLat  = Double.NaN, lrLon = Double.NaN;			
        double uln  = Double.NaN;	
        double north = Double.NaN;
        double south = Double.NaN;
        double east  = Double.NaN;
        double west  = Double.NaN;
        for (String line : StringUtil.split(results, "\n", true, true)) {
            double[] latlon;
            if (line.indexOf("Upper Left") >= 0) {
                latlon = getLatLon(line);
                if (latlon != null) {
		    ulLat=latlon[1];
		    ulLon=latlon[0];		    
                    north = ((north != north)
                             ? latlon[1]
                             : Math.max(north, latlon[1]));
                    west  = ((west != west)
                             ? latlon[0]
                             : Math.min(west, latlon[0]));
                }
            } else if (line.indexOf("Lower Right") >= 0) {
                latlon = getLatLon(line);
                if (latlon != null) {
		    lrLat=latlon[1];
		    lrLon=latlon[0];		    
                    south = ((south != south)
                             ? latlon[1]
                             : Math.min(south, latlon[1]));
                    east  = ((east != east)
                             ? latlon[0]
                             : Math.max(east, latlon[0]));
                }
            } else if (line.indexOf("Upper Right") >= 0) {
                latlon = getLatLon(line);
                if (latlon != null) {
		    urLat=latlon[1];
		    urLon=latlon[0];		    
                    north = ((north != north)
                             ? latlon[1]
                             : Math.max(north, latlon[1]));
                    east  = ((east != east)
                             ? latlon[0]
                             : Math.max(east, latlon[0]));
                }
            } else if (line.indexOf("Lower Left") >= 0) {
                latlon = getLatLon(line);
                if (latlon != null) {
		    llLat=latlon[1];
		    llLon=latlon[0];		    
                    south = ((south != south)
                             ? latlon[1]
                             : Math.min(south, latlon[1]));
                    west  = ((west != west)
                             ? latlon[0]
                             : Math.min(west, latlon[0]));
                }

            } else {}
	}
	/*
	System.err.println("ul:" +ulLat +"," + ulLon +
			   " ur:" +urLat +"," + urLon +
			   " ll:" +llLat +"," + llLon +
			   " lr:" +lrLat +"," + lrLon);
	*/
        if ( !Double.isNaN(north)) {
            entry.setNorth(north);
        }
        if ( !Double.isNaN(south)) {
            entry.setSouth(south);
        }
        if ( !Double.isNaN(east)) {
            entry.setEast(east);
        }
        if ( !Double.isNaN(west)) {
            entry.setWest(west);
        }
    }

    private static double[] getLatLon(String line) {
        line = line.trim();
        line = StringUtil.findPattern(line, ".*\\(([^\\)]+)\\.*");
        //        System.err.println("TOK: " + line);
        if (line == null) {
            return null;
        }

        List<String> toks = StringUtil.split(line, ",", true, true);
        if (toks.size() != 2) {
            return null;
        }

        return new double[] { decodeLatLon(toks.get(0)),
                              decodeLatLon(toks.get(1)) };
    }

    public Result returnNA(Request request) throws Exception {
        return new Result(
            BLANK,
            Utils.getInputStream(
                "/org/ramadda/plugins/map/htdocs/map/notavailable.png",
                GdalTypeHandler.class), "image/png");
    }

    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString("action", "");
        if ( !action.equals("geotiff.makeimage")) {
            return super.processEntryAction(request, entry);
        }
        if ( !getAccessManager().canAccessFile(request, entry)) {
            throw new AccessException("No access to file", request);
        }
        request.setCORSHeaderOnResponse();
        String convert = getRepository().getScriptPath("ramadda.convert", "");
	String translate = getRepository().getScriptPath("service.gdal.gdal_translate","");
        if ( !Utils.stringDefined(convert) && !Utils.stringDefined(translate)) {
            return returnNA(request);
        }
        String fileName = Utils.makeMD5(entry.getId()) + ".png";
        File cachedFile = getStorageManager().getCacheFile("geotiffs",
							   fileName);
        if ( !cachedFile.exists()) {
            try {
		/*
		FileOutputStream out = new FileOutputStream(cachedFile);
		BufferedImage tif = ImageIO.read(getStorageManager().getInputStream(getStorageManager().getEntryResourcePath(entry)));
		ImageIO.write(tif, "png", out);
		out.close();
		*/
                List<String> commands;
		if(Utils.stringDefined(translate))
                    commands = (List<String>) Utils.makeListFromValues(translate,"-of","PNG",
							     getStorageManager().getEntryResourcePath(entry),
							     cachedFile.toString());
		else 		    
		    commands =
			(List<String>) Utils.makeListFromValues(convert,
						      getStorageManager().getEntryResourcePath(entry),
						      cachedFile.toString());
		//		System.err.println("geotiff-4:" + Utils.join(commands," "));
                String[] results = getRepository().runCommands(commands);
		//		System.err.println("done");
                if (Utils.stringDefined(results[0])) {
                    if (results[0].toLowerCase().indexOf("error") >= 0) {
                        System.err.println("Results running commands:"
                                           + commands + "\nError:"
                                           + results[0]);

                        return returnNA(request);
                    }
                }
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
                exc.printStackTrace();
                return returnNA(request);
            }
        }

	//	System.err.println("geotiff-done:" + cachedFile);
        return new Result(
            BLANK,
            getStorageManager().getFileInputStream(cachedFile.toString()),
            "image/png");

    }

    private static double decodeLatLon(String s) {
        s = s.replace("d", ":");
        s = s.replace("'", ":");
        s = s.replace("\"", "");

        return Misc.decodeLatLon(s);
    }


}
