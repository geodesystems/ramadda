/*
 * Copyright (c) 2008-2016 Geode Systems LLC
 * This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file 
 * ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
 */

package gov.noaa.esrl.psd.repository;


import org.ramadda.repository.*;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.map.MapLayer;
import org.ramadda.repository.output.MapOutputHandler;
import org.ramadda.util.HtmlUtils;


import org.ramadda.util.Json;
import org.ramadda.util.WikiUtil;

import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import java.util.List;



/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class EsrlMapPageDecorator extends PageDecorator implements WikiConstants {

    /**
     * _more_
     */
    public EsrlMapPageDecorator() {
        super(null);
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
     */
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props) {
        if ( !tag.equals("esrl.map")) {
            return null;
        }
        try {

            StringBuilder sb = new StringBuilder();
            int width = getWikiManager().getDimension(props, ATTR_WIDTH,
                            -100);
            int height = getWikiManager().getDimension(props, ATTR_HEIGHT,
                             300);
            List<Entry> children = getWikiManager().getEntries(request,
                                       originalEntry, entry, props, false,
                                       "");
            MapOutputHandler mapOutputHandler =
                (MapOutputHandler) getRepository().getOutputHandler(
                    MapOutputHandler.OUTPUT_MAP);
            Hashtable mapProps = new Hashtable();
            mapProps.put("scrollToZoom", "true");
            /*
              mapProps.put("entryClickHandler",
              Json.quote(
              "handlePsdStationClick"));
            */
            Hashtable argProps  = new Hashtable();
            argProps.put(ATTR_DETAILS,"true");
            argProps.put(ATTR_LISTENTRIES, Misc.getProperty(props, ATTR_LISTENTRIES, "true"));
            MapInfo map = getMapManager().getMap(request, entry, children, sb,
                                                 width, height, mapProps, argProps);

            sb.append(HtmlUtils.importJS(getHtdocsUrl("/noaa/psdstations.js")));

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


}
