/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.*;

import org.ramadda.service.Service;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;



import org.w3c.dom.*;

import ucar.nc2.units.DateUnit;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;


import java.util.Date;
import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class DemTypeHandler extends GdalTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public DemTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString("action", "");
        if ( !action.equals("dem.makehillshade")) {
            return super.processEntryAction(request, entry);
        }
        if ( !getAccessManager().canAccessFile(request, entry)) {
            throw new AccessException("No access to file", request);
        }
        request.setCORSHeaderOnResponse();
        String convert = getRepository().getScriptPath("service.gdal.gdaldem", "");
        if ( !Utils.stringDefined(convert)) {
            return returnNA(request);
        }
        String fileName = Utils.makeMD5(entry.getId()) + ".png";
        File cachedFile = getStorageManager().getCacheFile("hillshades",
                              fileName);
        if ( !cachedFile.exists()) {
            try {
                List<String> commands =
                    (List<String>) Utils.makeListFromValues(convert,
						  "hillshade",
						  getStorageManager().getEntryResourcePath(entry),
						  cachedFile.toString());
                String[] results = getRepository().runCommands(commands);
                if (Utils.stringDefined(results[0])) {
                    if (results[0].toLowerCase().indexOf("error") >= 0) {
                        System.err.println("Results running commands:"
                                           + commands + "\nError:"
                                           + results[0]);

                        return returnNA(request);
                    }
                }
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
                exc.printStackTrace();

                return returnNA(request);
            }
        }

        return new Result(
            BLANK,
            getStorageManager().getFileInputStream(cachedFile.toString()),
            "image/png");

    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private static double decodeLatLon(String s) {
        s = s.replace("d", ":");
        s = s.replace("'", ":");
        s = s.replace("\"", "");

        return Misc.decodeLatLon(s);
    }

}
