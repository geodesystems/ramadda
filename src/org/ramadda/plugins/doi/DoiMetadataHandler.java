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

package org.ramadda.plugins.doi;




import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.metadata.*;




import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;




import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import visad.Unit;
import visad.UnitException;


import visad.data.units.NoSuchUnitException;

import visad.jmet.MetUnits;

import java.io.File;


import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DoiMetadataHandler extends MetadataHandler {

    /** _more_ */
    public static final String ID_TYPE_DOI = "doi";

    /** _more_ */
    public static final String ID_TYPE_ARK = "ark";

    /** _more_ */
    public static final String TYPE_DOI = "doi_identifier";


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public DoiMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public DoiMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
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
    @Override
    public String[] getHtml(Request request, Entry entry, Metadata metadata)
            throws Exception {
        String type  = metadata.getAttr1();
        String label = (type.equals(ID_TYPE_DOI)
                        ? "DOI"
                        : "ARK");

        return new String[] { label, metadata.getAttr2() };
    }

    //http://n2t.net/ark:/99999/fk47h23wj
    //ark:/99999/fk47h23wj
    //https://doi.org/

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public static String getUrl(String id) {
        if (id.startsWith("doi:")) {
            return id.replace("doi:", "https://doi.org/");
        }

        return id.replace("ark:", "http://n2t.net/ark:");
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public static String getHref(String id) {
        return HtmlUtils.href(getUrl(id), id);
    }


}
