/*
 * Copyright (c) 2008-2016 Geode Systems LLC
 * This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file 
 * ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
 */

package gov.noaa.esrl.psd.repository;


import org.ramadda.repository.*;
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
public class EsrlMapPageDecorator extends PageDecorator {

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
            List<Object[]> mapProps = new ArrayList<Object[]>();
            mapProps.add(new Object[] {"scrollToZoom", "true"});
            /*
            mapProps.add(new Object[] { "entryClickHandler",
                                        Json.quote(
                                            "handlePsdStationClick") });
                                            */
            MapInfo map = getMapManager().getMap(request, children, sb,
                              width, height, mapProps, "detailed", "true",
                              "listEntries", Misc.getProperty(props, "listentries", "true"));

            sb.append(HtmlUtils.importJS(htdocsUrl("/noaa/psdstations.js")));

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


}
