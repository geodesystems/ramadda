/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.*;
import org.ramadda.repository.job.JobManager;
import org.ramadda.repository.output.*;


import org.ramadda.service.*;

import org.ramadda.service.Service;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.TempDir;


import org.w3c.dom.*;

import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;



/**
 *
 * @author Jeff McWhirter/ramadda.org
 */
public class NetcdfService extends Service {

    /**
     * ctor
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public NetcdfService(Repository repository, Element element)
            throws Exception {
        super(repository, element);
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
     * @param request _more_
     * @param input _more_
     * @param sb _more_
     * @param argPrefix _more_
     * @param label _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void addToForm(Request request, ServiceInput input, Appendable sb,
                          String argPrefix, String label)
            throws Exception {



        List<Entry> entries = input.getEntries();
        Entry       entry   = ((entries.size() == 0)
                               ? null
                               : entries.get(0));
        if (entry == null) {
            for (ServiceArg inputArg : getInputs()) {
                if (inputArg.isEntry()) {
                    entry = getEntry(request, argPrefix, inputArg);

                    break;
                }
            }
        }


        if (entry != null) {
            addMetadata(request, input, entry.getResource().getPath());
        }

        super.addToForm(request, input, sb, argPrefix, label);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param input _more_
     * @param path _more_
     *
     * @throws Exception _more_
     */
    private void addMetadata(Request request, ServiceInput input, String path)
            throws Exception {
        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        NetcdfDataset        dataset = NetcdfDataset.openDataset(path);
        List<Variable>       variables         = dataset.getVariables();
        List<TwoFacedObject> coordNames = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> varNames = new ArrayList<TwoFacedObject>();
        for (Variable var : variables) {
            if (var instanceof CoordinateAxis) {
                coordNames.add(new TwoFacedObject(var.getFullName(),
                        var.getShortName()));
            }
            varNames.add(new TwoFacedObject(var.getFullName(),
                                            var.getShortName()));
        }

        // If it's a grid, get the list of times.
        GridDataset        gds      = new GridDataset(dataset);
        List<CalendarDate> dates    = dataOutputHandler.getGridDates(gds);
        List<Date>         dateList = new ArrayList<Date>(dates.size());
        for (CalendarDate cdate : dates) {
            dateList.add(cdate.toDate());
        }
        gds.close();
        dataset.close();

        input.putProperty("varNames", varNames);
        input.putProperty("coordNames", coordNames);
        input.putProperty("dateList", dateList);

    }

}
