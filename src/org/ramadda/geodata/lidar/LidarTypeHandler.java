/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
