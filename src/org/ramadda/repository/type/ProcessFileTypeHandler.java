/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;


import org.ramadda.repository.*;

import org.ramadda.repository.metadata.*;

import org.ramadda.util.HtmlUtils;

import org.ramadda.util.sql.Clause;


import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.FilenameFilter;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;




import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class TypeHandler _more_
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class ProcessFileTypeHandler extends LocalFileTypeHandler {

    /** _more_ */
    public static final String TYPE_PROCESS = "type_process";


    /** _more_ */
    private String processId = "test";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ProcessFileTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public ProcessFileTypeHandler(Repository repository) throws Exception {
        super(repository, null);
        setType(TYPE_PROCESS);
        setForUser(false);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public LocalFileInfo doMakeLocalFileInfo(Entry entry) throws Exception {
        File dir = getStorageManager().getProcessDir();

        return new LocalFileInfo(getRepository(), dir);
    }



}
