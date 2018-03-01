/*
* Copyright (c) 2008-2018 Geode Systems LLC
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




    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public MapTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    public void childEntryChanged(Entry entry, boolean isNew)
            throws Exception {
        super.childEntryChanged(entry, isNew);
        Entry parent = entry.getParentEntry();
        List<Entry> children =
            getEntryManager().getChildren(getRepository().getTmpRequest(),
                                          parent);
        //For good measure
        children.add(entry);
        getEntryManager().setBoundsOnEntry(parent, children);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param services _more_
     *
     */
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
