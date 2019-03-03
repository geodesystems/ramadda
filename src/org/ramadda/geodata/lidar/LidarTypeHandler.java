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


import org.ramadda.data.record.RecordFile;
import org.ramadda.data.record.RecordFileFactory;
import org.ramadda.data.services.*;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.w3c.dom.*;




/**
 *
 * @author Jeff McWhirter
 */
public class LidarTypeHandler extends PointTypeHandler {

    /** _more_ */
    public static final String TYPE_LIDAR = "lidar";

    /**
     * _more_
     *
     * @param repository ramadda
     * @param node _more_
     * @throws Exception On badness
     */
    public LidarTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    /*
    @Override
    public RecordOutputHandler doMakeRecordOutputHandler() {
        return (RecordOutputHandler) getRepository().getOutputHandler(
                                                                      LidarOutputHandler.class);
    }
    */

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public RecordFileFactory doMakeRecordFileFactory() {
        try {
            //            System.err.println ("making lidar factory");
            return new LidarFileFactory();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }




}
