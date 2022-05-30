/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;


import org.ramadda.util.ProcessRunner;

import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 */
public class ZoomifyTypeHandler extends GenericTypeHandler {

    /**  */
    private static int IDX = 0;

    /**  */
    private static final int IDX_IMAGE_WIDTH = IDX++;

    /**  */
    private static final int IDX_IMAGE_HEIGHT = IDX++;

    /**  */
    private static final int IDX_TILES_URL = IDX++;

    /**  */
    private static final int IDX_STYLE = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ZoomifyTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }



    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,
                                   boolean fromImport)
            throws Exception {
        super.initializeNewEntry(request, entry, fromImport);
        if ( !entry.isFile()) {
            return;
        }
        String slicer = getRepository().getProperty("ramadda.image.slicer");
        if (slicer == null) {
            return;
        }
        File entryDir  = getStorageManager().getEntryDir(entry.getId(), true);
        File imagesDir = new File(entryDir, "images");
        imagesDir.mkdir();
        List<String> commands = new ArrayList<String>();
        Utils.add(commands, "sh", slicer, "-i",
                  entry.getResource().getPath(), "-o", imagesDir.toString());
	System.err.println(commands);
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        Process     process = pb.start();
        InputStream is      = process.getInputStream();
        String      result  = new String(IOUtil.readBytes(is));
        if (result.indexOf("unable to open image")<0 && result.trim().length() > 0) {
            throw new IllegalArgumentException("Error running image slicer:"
                    + result);
        }
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
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("zoomify") && !tag.equals("zoomable")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuilder sb     = new StringBuilder();
        String        width  = Utils.getProperty(props, "width", "800px");
        String        height = Utils.getProperty(props, "height", "600px");
        String style = HU.css("width", HU.makeDim(width, null), "height",
                              HU.makeDim(height, null), "border",
                              "1px solid #aaa", "color", "#333",
                              "background-color", "#fff");
        String s = (String) entry.getValue(IDX_STYLE);
        if (Utils.stringDefined(s)) {
            style += s;
        }
	style += Utils.getProperty(props, "style","");
        style = style.replaceAll("\n", " ");
        //      sb.append(HU.importCss( ".openseadragon {" + style +"}"));
        if (request.getExtraProperty("seadragon_added") == null) {
            HU.importJS(
                sb,
                getPageHandler().makeHtdocsUrl(
                    "/lib/openseadragon/openseadragon.min.js"));
            request.putExtraProperty("seadragon_added", "true");
        }

        String id = HU.getUniqueId("seadragon_div");
        sb.append("<center>\n");
        HU.div(sb, "", HU.attrs("id", id, "style", style));
        sb.append("\n</center>\n");
        List<String> jsonProps = new ArrayList<String>();
        List<String> tiles     = new ArrayList<String>();
        Utils.add(jsonProps, "id", JsonUtil.quote(id), "showNavigator",
                  "true", "maxZoomLevel", "18", "prefixUrl",
                  JsonUtil.quote(getRepository().getUrlBase()
                                 + "/lib/openseadragon/images/"));
        Utils.add(jsonProps, "showRotationControl", "true",
                  "gestureSettingsTouch",
                  JsonUtil.map(Utils.add(null, "pinchRotate", "true")));

        //If its a file then we did the tiling ourselves
        if (entry.isFile()) {
            Utils.add(jsonProps, "tileSources",
                      JsonUtil.quote(getRepository().getUrlBase()
                                     + "/entryfile/" + entry.getId()
                                     + "/images.dzi"));
        } else if (Utils.stringDefined("" + entry.getValue(IDX_TILES_URL))) {
            if (entry.getValue(IDX_IMAGE_WIDTH, 0) != 0) {
                width = "" + entry.getValue(IDX_IMAGE_WIDTH, 0);
            }
            if (entry.getValue(IDX_IMAGE_HEIGHT, 0) != 0) {
                height = "" + entry.getValue(IDX_IMAGE_HEIGHT, 0);
            }
            Utils.add(tiles, "type", JsonUtil.quote("zoomifytileservice"),
                      "tilesUrl", JsonUtil.quote(entry.getValue(2)));
            Utils.add(tiles, "width", width, "height", height);
            Utils.add(jsonProps, "tileSources", JsonUtil.map(tiles));
        } else {
            throw new IllegalArgumentException(
                "No image tile source defined");
        }
        String attrs = JsonUtil.map(jsonProps);

        String var   = HU.getUniqueId("seadragon");
        HU.script(sb, "var " + var + "=OpenSeadragon(" + attrs + ");\n");

        return sb.toString();
    }



}
