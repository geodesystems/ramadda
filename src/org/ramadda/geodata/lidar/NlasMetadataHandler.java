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

package org.ramadda.geodata.lidar;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;

import org.w3c.dom.*;


/**
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class NlasMetadataHandler extends MetadataHandler {

    /** _more_ */
    public static final String TYPE_TRACKS = "nlas_tracks";

    /**
     * _more_
     *
     * @param repository ramadda
     *
     * @throws Exception _more_
     */
    public NlasMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        if ( !metadata.getType().equals(TYPE_TRACKS)) {
            return super.getHtml(request, entry, metadata);
        }

        return new String[] { "Tracks", metadata.getAttr1() };
    }
}
