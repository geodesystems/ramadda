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

package org.ramadda.geodata.fieldproject;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;

import java.io.File;

import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class ResearchFacilityTypeHandler extends ExtensibleGroupTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public ResearchFacilityTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    /*
    public void getEntryLinks(Request request, Entry entry, List<Link> links)
        throws Exception {
        super.getEntryLinks(request, entry, links);
        links.add(
                  new Link(
                           request.entryUrl(
                                            getRepository().URL_ENTRY_ACCESS, entry, "type",
                                            "kml"), getRepository().iconUrl(ICON_KML),
                           "Convert GPX to KML", OutputType.TYPE_FILE));
    }



    public Result processEntryAccess(Request request, Entry entry)
        throws Exception {
        File imageFile = getStorageManager().getTmpFile(request,
                                                        "icon.png");
        return new Result("",
                          getStorageManager().getFileInputStream(imageFile),
                          getRepository().getMimeTypeFromSuffix("png"));
    }
    */


}
