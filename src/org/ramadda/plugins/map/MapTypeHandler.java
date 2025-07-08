/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;

import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.geom.Rectangle2D;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 */
public class MapTypeHandler extends ExtensibleGroupTypeHandler {

    public MapTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    @Override
    public void childEntryChanged(Request request,Entry entry, boolean isNew)
            throws Exception {
        super.childEntryChanged(request,entry, isNew);
        Entry parent = entry.getParentEntry();
        List<Entry> children =
            getEntryManager().getChildren(getRepository().getTmpRequest(),
                                          parent);
        //For good measure
        children.add(entry);
        getEntryManager().setBoundsOnEntry(request,parent, children);
    }

    @Override
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        super.getServiceInfos(request, entry, services);
        /*
        String url = getEntryManager().getEntryURL(request,entry,
        ARG_OUTPUT,
        LidarOutputHandler.OUTPUT_LATLONALTCSV.toString(),
        LidarOutputHandler.ARG_LIDAR_SKIP,
        macro(LidarOutputHandler.ARG_LIDAR_SKIP), ARG_BBOX,
        macro(ARG_BBOX),
        );
        services.add(new ServiceInfo("pointcloud", "Point Cloud",
        request.getAbsoluteUrl(url),
                                 getIconUrl(LidarOutputHandler.ICON_POINTS)));
        */
    }

}
