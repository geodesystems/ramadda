/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.apache.commons.net.ftp.*;

import org.python.core.*;
import org.python.util.*;

import org.ramadda.geodata.cdmdata.*;
import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;


import org.w3c.dom.*;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;

import ucar.unidata.data.DataSource;

import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Pool;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @version $Revision: 1.3 $
 */
public class DataJythonTypeHandler extends JythonTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DataJythonTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
        LogUtil.setTestMode(true);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public CdmDataOutputHandler getDataOutputHandler() throws Exception {
        return (CdmDataOutputHandler) getRepository().getOutputHandler(
            CdmDataOutputHandler.OUTPUT_OPENDAP.toString());
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JythonTypeHandler.ProcessInfo doMakeProcessInfo() {
        return new MyProcessInfo();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param interp _more_
     * @param info _more_
     * @param processInfo _more_
     * @param theEntry _more_
     *
     * @throws Exception _more_
     */
    protected void processEntry(Request request, PythonInterpreter interp,
                                InputInfo info,
                                JythonTypeHandler.ProcessInfo processInfo,
                                Entry theEntry)
            throws Exception {
        super.processEntry(request, interp, info, processInfo, theEntry);
        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        MyProcessInfo        dataProcessInfo   = (MyProcessInfo) processInfo;

        String path = dataOutputHandler.getCdmManager().getPath(theEntry);
        if (path != null) {
            //Try it as grid first
            GridDataset gds =
                dataOutputHandler.getCdmManager().getGridDataset(theEntry,
                    path);
            NetcdfDataset     ncDataset  = null;
            GeoGridDataSource dataSource = null;
            interp.set(info.id + "_griddataset", gds);
            processInfo.variables.add(info.id + "_griddataset");
            if (gds == null) {
                //Else try it as a ncdataset
                ncDataset =
                    dataOutputHandler.getCdmManager().getNetcdfDataset(
                        theEntry, path);
            } else {
                dataSource = new GeoGridDataSource(gds);
                dataProcessInfo.dataSources.add(dataSource);
            }
            interp.set(info.id + "_datasource", dataSource);
            interp.set(info.id + "_ncdataset", ncDataset);
            processInfo.variables.add(info.id + "_datasource");
            processInfo.variables.add(info.id + "_ncdataset");
            if (ncDataset != null) {
                dataProcessInfo.ncPaths.add(path);
                dataProcessInfo.ncData.add(ncDataset);
            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param interp _more_
     * @param processInfo _more_
     *
     * @throws Exception _more_
     */
    protected void cleanup(Request request, Entry entry,
                           PythonInterpreter interp,
                           JythonTypeHandler.ProcessInfo processInfo)
            throws Exception {
        super.cleanup(request, entry, interp, processInfo);
        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        MyProcessInfo        dataProcessInfo   = (MyProcessInfo) processInfo;
        for (DataSource dataSource : dataProcessInfo.dataSources) {
            dataSource.doRemove();
        }
        for (int i = 0; i < dataProcessInfo.ncPaths.size(); i++) {
            dataOutputHandler.getCdmManager().returnNetcdfDataset(
                dataProcessInfo.ncPaths.get(i),
                dataProcessInfo.ncData.get(i));
        }
        for (int i = 0; i < dataProcessInfo.gridPaths.size(); i++) {
            dataOutputHandler.getCdmManager().returnGridDataset(
                dataProcessInfo.gridPaths.get(i),
                dataProcessInfo.gridData.get(i));
        }

    }





    /**
     * Class description
     *
     *
     * @version        Enter version here..., Mon, May 3, '10
     * @author         Enter your name here...
     */
    private static class MyProcessInfo extends JythonTypeHandler.ProcessInfo {

        /** _more_ */
        List<String> ncPaths = new ArrayList<String>();

        /** _more_ */
        List<NetcdfDataset> ncData = new ArrayList<NetcdfDataset>();

        /** _more_ */
        List<String> gridPaths = new ArrayList<String>();

        /** _more_ */
        List<GridDataset> gridData = new ArrayList<GridDataset>();

        /** _more_ */
        List<DataSource> dataSources = new ArrayList<DataSource>();
    }




}
