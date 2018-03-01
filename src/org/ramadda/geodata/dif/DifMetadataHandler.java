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

package org.ramadda.geodata.dif;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;




import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;




import java.io.File;


import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DifMetadataHandler extends MetadataHandler {

    /** _more_ */
    public static final String TYPE_ENTRY_TITLE = "dif.entry_title";

    /** _more_ */
    public static final String TYPE_DATA_SET_CITATION =
        "dif.data_set_citation";

    /** _more_ */
    public static final String TYPE_PERSONNEL = "dif.personnel";

    /** _more_ */
    public static final String TYPE_PARAMETERS = "dif.parameters";

    /** _more_ */
    public static final String TYPE_ISO_TOPIC_CATEGORY =
        "dif.iso_topic_category";

    /** _more_ */
    public static final String TYPE_KEYWORD = "dif.keyword";

    /** _more_ */
    public static final String TYPE_DATA_SET_PROGRESS =
        "dif.data_set_progress";

    /** _more_ */
    public static final String TYPE_LOCATION = "dif.location";

    /** _more_ */
    public static final String TYPE_PROJECT = "dif.project";

    /** _more_ */
    public static final String TYPE_QUALITY = "dif.quality";

    /** _more_ */
    public static final String TYPE_DATA_SET_LANGUAGE =
        "dif.data_set_language";

    /** _more_ */
    public static final String TYPE_INSTRUMENT = "dif.instrument";

    /** _more_ */
    public static final String TYPE_PLATFORM = "dif.platform";

    /** _more_ */
    public static final String TYPE_ORIGINATING_CENTER =
        "dif.originating_center";

    /** _more_ */
    public static final String TYPE_DATA_CENTER = "dif.data_center";

    /** _more_ */
    public static final String TYPE_DISTRIBUTION = "dif.distribution";

    /** _more_ */
    public static final String TYPE_REFERENCE = "dif.reference";

    /** _more_ */
    public static final String TYPE_SUMMARY = "dif.summary";

    /** _more_ */
    public static final String TYPE_RELATED_URL = "dif.related_url";

    /** _more_ */
    public static final String TYPE_METADATA_NAME = "dif.metadata_name";

    /** _more_ */
    public static final String TYPE_METADATA_VERSION = "dif.metadata_version";

    /** _more_ */
    public static final String TYPE_ = "dif.";


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public DifMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public DifMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }




}
