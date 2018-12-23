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

package org.ramadda.geodata.model;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.Request;
import org.ramadda.repository.job.JobManager;
import org.ramadda.service.Service;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.Element;

import ucar.nc2.units.SimpleUnit;


import java.io.File;

import java.util.List;
import java.util.Map;


/**
 *  Superclass to hold common NCL items
 */
public class NCLDataService extends Service {

    /** The nclOutputHandler */
    protected NCLOutputHandler nclOutputHandler;

    /** prefix for ncl commands */
    public static final String ARG_NCL_PREFIX = "ncl_";

    /** output type */
    public static final String ARG_NCL_OUTPUT = ARG_NCL_PREFIX + "output";

    /** mask type */
    public static final String ARG_NCL_MASKTYPE = ARG_NCL_PREFIX + "masktype";

    /** units arg */
    public static final String ARG_NCL_UNITS = ARG_NCL_PREFIX + "units";

    /** contour interval argument */
    protected static final String ARG_NCL_CINT = ARG_NCL_PREFIX + "cint";

    /** contour minimum argument */
    protected static final String ARG_NCL_CMIN = ARG_NCL_PREFIX + "cmin";

    /** contour maximum argument */
    protected static final String ARG_NCL_CMAX = ARG_NCL_PREFIX + "cmax";

    /** colormap name */
    protected static final String ARG_NCL_COLORMAP = ARG_NCL_PREFIX
                                                     + "colormap";

    /** contour lines */
    protected static final String ARG_NCL_CLINES = ARG_NCL_PREFIX + "clines";

    /** contour labels */
    protected static final String ARG_NCL_CLABELS = ARG_NCL_PREFIX
                                                    + "clabels";

    /** colorfill */
    protected static final String ARG_NCL_CFILL = ARG_NCL_PREFIX + "cfill";

    /** output format */
    protected static final String ARG_NCL_IMAGEFORMAT = ARG_NCL_PREFIX
                                                        + "plot_format";

    /** reverse colormap argument */
    protected static final String ARG_NCL_REVERSE_CMAP = ARG_NCL_PREFIX
                                                         + "reverse_cmap";

    /** yaxis minimum argument */
    protected static final String ARG_NCL_YMIN = ARG_NCL_PREFIX + "ymin";

    /** yaxis maximum argument */
    protected static final String ARG_NCL_YMAX = ARG_NCL_PREFIX + "ymax";

    /** xaxis minimum argument */
    protected static final String ARG_NCL_XMIN = ARG_NCL_PREFIX + "xmin";

    /** xaxis maximum argument */
    protected static final String ARG_NCL_XMAX = ARG_NCL_PREFIX + "xmax";

    /** the line color argument */
    protected static final String ARG_NCL_LINECOLOR = ARG_NCL_PREFIX
                                                      + "lineColor";

    /** the line color argument */
    public static final String ARG_TIME_AVERAGE = ARG_NCL_PREFIX
                                                  + "time_average";

    /** 1 space */
    public final static String space1 = HtmlUtils.space(1);

    /** 2 space */
    public final static String space2 = HtmlUtils.space(2);

    /* red space, blue space */

    /**
     * Create the service
     *
     * @param repository  the repository
     * @param element  the XML
     *
     * @throws Exception  problems
     */
    public NCLDataService(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    /**
     * Create the service
     *
     * @param repository repository
     * @param entry  the Entry
     */
    public NCLDataService(Repository repository, Entry entry) {
        super(repository, entry);
    }

    /**
     * Create a new map process
     *
     * @param repository  the repository
     * @param id  an id for this process
     * @param label  a label for this process
     *
     * @throws Exception  problem creating process
     */
    public NCLDataService(Repository repository, String id, String label)
            throws Exception {
        super(repository, id, label);
        nclOutputHandler = new NCLOutputHandler(repository);
    }

    /**
     * Create the service
     *
     * @param repository  the repository
     * @param parent      the parent Entry
     * @param element     this XML
     * @param index       some index
     *
     * @throws Exception problems
     */
    public NCLDataService(Repository repository, Service parent,
                          Element element, int index)
            throws Exception {
        super(repository, parent, element, index);
    }

    /**
     * Is this enabled?
     *
     * @return true if it is
     */
    public boolean isEnabled() {
        return nclOutputHandler.isEnabled();
    }

    /**
     * Run the process
     *
     * @param commands  the list of commands to run
     * @param envMap      the environment map
     * @param processDir  the processing directory
     * @param outFile     the outfile
     *
     * @throws Exception problem running commands
     */
    protected void runCommands(List<String> commands,
                               Map<String, String> envMap, File processDir,
                               File outFile)
            throws Exception {

        //System.err.println("cmds:" + commands);
        //System.err.println("env:" + envMap);

        long millis = System.currentTimeMillis();
        JobManager.CommandResults results =
            getRepository().getJobManager().executeCommand(commands, envMap,
                processDir, 60);
        //System.out.println("processing took: " + (System.currentTimeMillis()-millis));
        String errorMsg = results.getStderrMsg();
        String outMsg   = results.getStdoutMsg();
        if ( !outFile.exists()) {
            if (outMsg.length() > 0) {
                throw new IllegalArgumentException(outMsg);
            }
            if (errorMsg.length() > 0) {
                throw new IllegalArgumentException(errorMsg);
            }
            if ( !outFile.exists()) {
                throw new IllegalArgumentException(
                    "Humm, the NCL image generation failed for some reason");
            }
        }
    }

    /**
     * Get the mask options for the form
     * @param request the request
     * @param sb the string buffer
     * @return the form
     *
     * @throws Exception problems adding data mask widget
     */
    protected void addDataMaskWidget(Request request, Appendable sb)
            throws Exception {
        // Mask buttons
        StringBuilder mbuttons = new StringBuilder();
        mbuttons.append(
            HtmlUtils.radio(
                ARG_NCL_MASKTYPE, "none",
                RepositoryManager.getShouldButtonBeSelected(
                    request, ARG_NCL_MASKTYPE, "none", true)));
        mbuttons.append(space1);
        mbuttons.append(Repository.msg("All"));
        mbuttons.append(space2);
        mbuttons.append(
            HtmlUtils.radio(
                ARG_NCL_MASKTYPE, "ocean",
                RepositoryManager.getShouldButtonBeSelected(
                    request, ARG_NCL_MASKTYPE, "ocean", false)));
        mbuttons.append(space1);
        mbuttons.append(Repository.msg("Land only"));
        mbuttons.append(space2);
        mbuttons.append(
            HtmlUtils.radio(
                ARG_NCL_MASKTYPE, "land",
                RepositoryManager.getShouldButtonBeSelected(
                    request, ARG_NCL_MASKTYPE, "land", false)));
        mbuttons.append(space1);
        mbuttons.append(Repository.msg("Ocean only"));

        sb.append(HtmlUtils.formEntry(Repository.msgLabel("Plot Data"),
                                      mbuttons.toString()));
    }

    /**
     * Add the units widget to the form
     * @param request the request
     * @param units the units of the sample entry
     * @param sb  the form
     * @throws Exception problems appending to form
     */
    protected void addUnitsWidget(Request request, String units,
                                  Appendable sb)
            throws Exception {
        // units
        if (SimpleUnit.isCompatible(units, "K")) {
            String unit = request.getString(ARG_NCL_UNITS);
            // if the previous variable was not temperature and we are doing a reload, make sure one of them is selected
            Request uRequest = request.cloneMe();
            if ((unit != null)
                    && !(unit.equals("K") || unit.equals("degC"))) {
                uRequest.remove(ARG_NCL_UNITS);
            }
            StringBuilder unitsSB = new StringBuilder();
            unitsSB.append(
                HtmlUtils.radio(
                    ARG_NCL_UNITS, "degC",
                    RepositoryManager.getShouldButtonBeSelected(
                        uRequest, ARG_NCL_UNITS, "degC", true)));
            unitsSB.append(space1);
            unitsSB.append(Repository.msg("Celsius"));
            unitsSB.append(space2);
            unitsSB.append(
                HtmlUtils.radio(
                    ARG_NCL_UNITS, "K",
                    RepositoryManager.getShouldButtonBeSelected(
                        uRequest, ARG_NCL_UNITS, "K", false)));
            unitsSB.append(space1);
            unitsSB.append(Repository.msg("Kelvin"));


            sb.append(HtmlUtils.formEntry(Repository.msgLabel("Plot Units"),
                                          unitsSB.toString()));
        }
        // always convert these units
        if (SimpleUnit.isCompatible(units, "kg m-2 s-1")) {
            sb.append(HtmlUtils.hidden(ARG_NCL_UNITS, "mm/day"));
            // there are some precip units that are funky.  But we don't want
            // to test for compatibility with m/s
        } else if (units.equals("Kg/m^2/s") || units.equals("mm/s")
                   || units.equals("m/day")) {
            sb.append(HtmlUtils.hidden(ARG_NCL_UNITS, "mm/day"));
        } else if (SimpleUnit.isCompatible(units, "kg m-1 s-2")
                   || SimpleUnit.isCompatible(units, "Pa")) {
            sb.append(HtmlUtils.hidden(ARG_NCL_UNITS, "hPa"));
        } else if (SimpleUnit.isCompatible(units, "kg m-2")) {
            sb.append(HtmlUtils.hidden(ARG_NCL_UNITS, "mm"));
        }
    }

}
