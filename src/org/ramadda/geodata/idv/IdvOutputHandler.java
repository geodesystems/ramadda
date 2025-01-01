/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.idv;


import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.geodata.cdmdata.CdmManager;
import org.ramadda.geodata.cdmdata.GridAggregationTypeHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.EntryManager;
import org.ramadda.repository.Link;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryUtil;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.auth.AccessException;
import org.ramadda.repository.map.MapProperties;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.metadata.MetadataType;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDatasetPoint;

import ucar.unidata.data.*;
import ucar.unidata.data.gis.WmsSelection;
import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.data.point.NetcdfPointDataSource;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.gis.maps.MapData;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.IdvServer;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewState;
import ucar.unidata.idv.ui.ImageGenerator;
import ucar.unidata.idv.ui.ImageSequenceGrabber;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.ThreeDSize;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import visad.Unit;


import java.awt.Color;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * This class provides IDV based server side visualization services. It works
 * for both gridded data and point data though the point data needs alot of work.
 * The main entry point is the method outputEntry. This gets called by the IDV
 * and handles all of the html and image generation
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class IdvOutputHandler extends OutputHandler implements IdvConstants {

    /** IDV enabled property identifier */
    public static final String PROP_IDV_ENABLED = "ramadda.idv.enabled";

    /** mutex */
    private static Object INIT_MUTEX = new Object();


    /** grid output id */
    public static final OutputType OUTPUT_IDV_GRID =
        new OutputType("Grid Displays", "idv.grid",
                       OutputType.TYPE_OTHER | OutputType.TYPE_IMPORTANT,
                       OutputType.SUFFIX_NONE, "/idv/grid.gif", GROUP_DATA);


    /** point output id */
    public static final OutputType OUTPUT_IDV_POINT =
        new OutputType("Point Displays", "idv.point", OutputType.TYPE_OTHER,
                       OutputType.SUFFIX_NONE, ICON_PLANVIEW, GROUP_DATA);

    /** bundle image id */
    public static final OutputType OUTPUT_IDV_BUNDLE_IMAGE =
        new OutputType("Show as Image", "idv.bundle.image",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       ICON_IMAGE, GROUP_DATA);

    /** bundle movie id */
    public static final OutputType OUTPUT_IDV_BUNDLE_MOVIE =
        new OutputType("Show as Movie", "idv.bundle.movie",
                       OutputType.TYPE_OTHER, OutputType.SUFFIX_NONE,
                       ICON_MOVIE, GROUP_DATA);


    /** false if there is no graphics environment */
    private boolean idvOk = false;

    /** The IDV */
    IdvServer idvServer;

    /** background images available */
    private List backgrounds;

    /** except arguments */
    private HashSet<String> exceptArgs = new HashSet<String>();


    /** The display controls that we can use */
    private HashSet<String> okControls = new HashSet<String>();

    /** The display controls that we can use for 3D data */
    private HashSet<String> vertControls = new HashSet<String>();


    /** Holds previously generated images */
    private Hashtable<String, File> imageCache = new Hashtable<String,
                                                     File>();

    /** frame rate argument */
    private static final String ARG_FRAMERATE = "framerate";

    /** end frame pause argument */
    private static final String ARG_ENDPAUSE = "endframepause";

    /**
     *    Ctor
     *
     *     @param repository ramadda
     *     @param element outputhandlers.xml node
     *     @throws Exception On badness
     */
    public IdvOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);

        okControls.add(DISPLAY_XS_CONTOUR);
        okControls.add(DISPLAY_XS_FILLEDCONTOUR);
        okControls.add(DISPLAY_XS_COLOR);
        vertControls.add(DISPLAY_XS_CONTOUR);
        vertControls.add(DISPLAY_XS_FILLEDCONTOUR);
        vertControls.add(DISPLAY_XS_COLOR);
        vertControls.add(DISPLAY_ISOSURFACE);
        okControls.add("planviewflow");
        okControls.add("streamlines");
        okControls.add("windbarbplan");
        okControls.add("planviewcontour");
        okControls.add("planviewcontourfilled");
        okControls.add("planviewcolor");
        okControls.add("valuedisplay");
        okControls.add("isosurface");
        okControls.add("volumerender");
        okControls.add("pointvolumerender");
        for (String notArg : NOTARGS) {
            exceptArgs.add(notArg);
        }

        //To turn off the idv define the property ramadda.idv.enabled=false

        //Call this in a thread because if there is any problem with xvfb this will just hang
        //Run in a couple of seconds because we are deadlocking deep down in Java on the mac
        if (getRepository().getProperty(PROP_IDV_ENABLED, true)) {
            Misc.runInABit(5000, this, "checkIdv", null);
        }
    }


    /**
     * Check if there is a graphics environment
     */
    public void checkIdv() {
        //For now just don't try the IDV as the IDV init code bombs out because of the missing jythonlib.jar
        if (true) {
            return;
        }

        //Synchronize for the case where we have multiple ramadda  servlets under the same server
        //See if that fixes the problem with too many jythons being initialized at once
        synchronized (INIT_MUTEX) {
            try {
                //See if we have a graphics environment
                java.awt.GraphicsEnvironment e =
                    java.awt.GraphicsEnvironment
                        .getLocalGraphicsEnvironment();
                e.getDefaultScreenDevice();
                idvOk = true;
                idvServer = new IdvServer(
                    getStorageManager().getPluginResourceDir("idv"));
                //Only add the output types after we create the server
                addType(OUTPUT_IDV_GRID);
                addType(OUTPUT_IDV_POINT);
                addType(OUTPUT_IDV_BUNDLE_IMAGE);
                addType(OUTPUT_IDV_BUNDLE_MOVIE);
                getRepository().addOutputHandlerTypes(this);
                getRepository().getLogManager().logInfo(
                    "IDV visualization is enabled");
            } catch (java.awt.HeadlessException jahe) {
                idvOk = false;
                //                jahe.printStackTrace();
                getRepository().getLogManager().logWarning(
                    "To run the IdvOutputHandler a graphics environment is needed");

                return;
            } catch (Throwable exc) {
                idvOk = false;
                logError("Creating IdvOutputHandler", exc);

                return;
            }
        }
    }

    /**
     * This is from the ../cdmdata plugin. It provides routines for checking if an Entry is a grid
     * and provides a netcdf file cache
     *
     * @return The DataOutputHandler
     *
     * @throws Exception On badness
     */
    public CdmManager getCdmManager() throws Exception {
        return getDataOutputHandler().getCdmManager();
    }

    /**
     * Get the data output handler for this object
     *
     * @return the data output handler
     *
     * @throws Exception  problem getting output handler
     */
    public CdmDataOutputHandler getDataOutputHandler() throws Exception {
        return (CdmDataOutputHandler) getRepository().getOutputHandler(
            CdmDataOutputHandler.OUTPUT_OPENDAP.toString());
    }

    /**
     * Gets an IDV instance from the IdvServer
     *
     * @return The IDV to use
     *
     * @throws Exception On badness
     */
    private IntegratedDataViewer getIdv() throws Exception {
        return idvServer.getIdv();
    }


    /**
     * Called by ramadda to determine if this OutputHandler is applicable to the given entry
     *
     * @param request The request
     * @param state holds the Entry or the list of Entries
     * @param links List to add the applicable links to
     *
     * @throws Exception On badness
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if ( !idvOk) {
            return;
        }
        Entry entry = state.entry;

        //Check if its an aggregation
        if ((state.group != null)
                && state.group.getType().equals(
                    GridAggregationTypeHandler.TYPE_GRIDAGGREGATION)) {
            entry = state.group;
        }

        if (entry != null) {
            if (IdvBundlesOutputHandler.isBundle(entry)) {
                links.add(makeLink(request, entry, OUTPUT_IDV_BUNDLE_IMAGE));
                links.add(makeLink(request, entry, OUTPUT_IDV_BUNDLE_MOVIE));

                return;
            }
            if ( !getCdmManager().canLoadAsGrid(entry)) {
                if (getCdmManager().canLoadAsPoint(entry)) {
                    links.add(makeLink(request, entry, OUTPUT_IDV_POINT));
                }

                return;
            }
            links.add(makeLink(request, entry, OUTPUT_IDV_GRID));
        }
    }




    /**
     * Get the DataSourceDescriptor for the entry
     *
     * @param entry The entry
     *
     * @return  the DataSourceDescriptor
     *
     * @throws Exception On badness
     */
    private DataSourceDescriptor getDescriptor(Entry entry) throws Exception {
        String path = entry.getResource().getPath();
        if (path.length() > 0) {
            List<DataSourceDescriptor> descriptors =
                getIdv().getDataManager().getDescriptors();
            for (DataSourceDescriptor descriptor : descriptors) {
                if ((descriptor.getPatternFileFilter() != null)
                        && descriptor.getPatternFileFilter().match(path)) {
                    return descriptor;
                }
            }
        }

        return null;
    }


    /**
     * The main entry point
     *
     * @param request The request
     * @param outputType what output
     * @param entry The entry
     *
     * @return The Result
     *
     * @throws Exception On badness
     */
    @Override
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {

        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_IDV_GRID)) {
            return outputGrid(request, entry);
        }
        if (output.equals(OUTPUT_IDV_POINT)) {
            return outputPoint(request, entry);
        }
        if (output.equals(OUTPUT_IDV_BUNDLE_IMAGE)) {
            return outputBundleImage(request, entry);
        }
        if (output.equals(OUTPUT_IDV_BUNDLE_MOVIE)) {
            return outputBundleMovie(request, entry);
        }

        return super.outputEntry(request, outputType, entry);
    }


    /**
     * Output a bundle movie
     *
     * @param request  the Request
     * @param entry    the Entry
     *
     * @return  the image
     *
     * @throws Exception  problem loading image
     */
    private Result outputBundleMovie(Request request, Entry entry)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       action = request.getString(ARG_IDV_ACTION, "");
        if (action.equals(ACTION_BUNDLE_MAKEMOVIE)) {

            String       suffix  = getImageSuffix(request);
            String       retname = "bundlemovie" + suffix;
            File imageFile = getStorageManager().getTmpFile(request, retname);
            StringBuffer isl = makeBundleIsl(request, entry, imageFile, true);
            long         t1      = System.currentTimeMillis();
            idvServer.evaluateIsl(isl);
            long t2 = System.currentTimeMillis();
            System.err.println("isl time:" + (t2 - t1));

            return new Result(
                retname, getStorageManager().getFileInputStream(imageFile),
                getRepository().getMimeTypeFromSuffix(suffix));

        }

        String formUrl = getEntryManager().getFullEntryShowUrl(request);
        sb.append(HtmlUtils.form(formUrl, ""));
        sb.append(entry.getDescription());
        sb.append(HtmlUtils.submit("Make Movie", ARG_SUBMIT));
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_IDV_BUNDLE_MOVIE));
        sb.append(HtmlUtils.hidden(ARG_IDV_ACTION, ACTION_BUNDLE_MAKEMOVIE));
        sb.append(HtmlUtils.formTable());
        List<TwoFacedObject> imageTypes = new ArrayList<TwoFacedObject>();
        imageTypes.add(new TwoFacedObject("Animated GIF", "gif"));
        imageTypes.add(new TwoFacedObject("QuickTime Movie", "mov"));
        imageTypes.add(new TwoFacedObject("AVI Movie", "avi"));

        sb.append(HtmlUtils.formEntry(msgLabel("Format"),
                                      htmlSelect(request, ARG_IMAGE_TYPE,
                                          imageTypes)));
        sb.append(HtmlUtils.formEntry(msgLabel("Frames per second"),
                                      htmlInput(request, ARG_FRAMERATE,
                                          "2")));
        sb.append(HtmlUtils.formEntry(msgLabel("End Frame Pause"),
                                      htmlInput(request, ARG_ENDPAUSE, "2")
                                      + HtmlUtil.space(2) + "seconds"));
        sb.append(HtmlUtils.formEntry(msgLabel("Movie Size"),
                                      htmlInput(request, ARG_IMAGE_WIDTH,
                                          "600") + HtmlUtils.space(1) + "X"
                                              + HtmlUtils.space(1)
                                                  + htmlInput(request,
                                                      ARG_IMAGE_HEIGHT,
                                                          "400")));
        if ( !request.getUser().getAnonymous()) {
            try {
                addPublishWidget(
                    request, entry, sb,
                    msg("Select a folder to publish the product to"));
            } catch (Exception e) {}
        }
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.submit("Make Movie", ARG_SUBMIT));
        sb.append(HtmlUtils.formClose());

        return new Result("Bundle As Movie", sb);
    }


    /**
     * Output a bundle as an image
     *
     * @param request  the Request
     * @param entry    the entry
     *
     * @return  an image
     *
     * @throws Exception  problems generating the image
     */
    private Result outputBundleImage(Request request, Entry entry)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       action = request.getString(ARG_IDV_ACTION, "");
        if (action.equals(ACTION_BUNDLE_MAKEIMAGE)) {

            String       suffix  = getImageSuffix(request);
            String       retname = "bundleimage" + suffix;
            File imageFile = getStorageManager().getTmpFile(request, retname);
            StringBuffer isl = makeBundleIsl(request, entry, imageFile,
                                             false);

            long t1 = System.currentTimeMillis();
            idvServer.evaluateIsl(isl);
            long t2 = System.currentTimeMillis();
            System.err.println("isl time:" + (t2 - t1));

            return new Result(
                retname, getStorageManager().getFileInputStream(imageFile),
                getRepository().getMimeTypeFromSuffix(suffix));

        }

        getPageHandler().entrySectionOpen(request, entry, sb, "Make Image");
        String formUrl = getEntryManager().getFullEntryShowUrl(request);
        sb.append(HtmlUtils.form(formUrl, ""));
        sb.append(entry.getDescription());
        sb.append(HtmlUtils.submit("Make Image", ARG_SUBMIT));
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_IDV_BUNDLE_IMAGE));
        sb.append(HtmlUtils.hidden(ARG_IDV_ACTION, ACTION_BUNDLE_MAKEIMAGE));
        sb.append(HtmlUtils.formTable());
        List<TwoFacedObject> imageTypes = new ArrayList<TwoFacedObject>();
        imageTypes.add(new TwoFacedObject("GIF", "gif"));
        imageTypes.add(new TwoFacedObject("PNG", "png"));
        imageTypes.add(new TwoFacedObject("JPEG", "jpg"));

        sb.append(HtmlUtils.formEntry(msgLabel("Format"),
                                      htmlSelect(request, ARG_IMAGE_TYPE,
                                          imageTypes)));
        sb.append(HtmlUtils.formEntry(msgLabel("Time Step"),
                                      htmlInput(request, ARG_TIME_STEP,
                                          "1")));
        sb.append(HtmlUtils.formEntry(msgLabel("Image Size"),
                                      htmlInput(request, ARG_IMAGE_WIDTH,
                                          "600") + HtmlUtils.space(1) + "X"
                                              + HtmlUtils.space(1)
                                                  + htmlInput(request,
                                                      ARG_IMAGE_HEIGHT,
                                                          "400")));
        if ( !request.getUser().getAnonymous()) {
            try {
                addPublishWidget(
                    request, entry, sb,
                    msg("Select a folder to publish the product to"));
            } catch (Exception e) {}
        }
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.submit("Make Image", ARG_SUBMIT));
        sb.append(HtmlUtils.formClose());

        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("Bundle As Image", sb);

    }

    /**
     * Make the bundle isl
     *
     * @param request the Request
     * @param entry   the bundle entry
     * @param imageFile _more_
     * @param isMovie _more_
     *
     * @return  the isl
     */
    private StringBuffer makeBundleIsl(Request request, Entry entry,
                                       File imageFile, boolean isMovie) {
        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");
        isl.append("<bundle file=\""
                   + getEntryManager().getEntryResourceUrl(request, entry,
							   EntryManager.ARG_INLINE_DFLT,true,
							   EntryManager.ARG_ADDPATH_DFLT) + "\"");
        isl.append(" clear=\"true\"");
        if ( !isMovie) {
            String timeIdx = request.getString(ARG_TIME_STEP, "1");
            int    tidx    = 0;
            try {
                tidx = Integer.parseInt(timeIdx) - 1;
                tidx = Math.max(tidx, 0);
            } catch (NumberFormatException nfe) {
                tidx = 0;
            }
            isl.append(" times=\"" + tidx + "\"");
        }
        int width  = request.get(ARG_IMAGE_WIDTH, 600);
        int height = request.get(ARG_IMAGE_HEIGHT, 400);
        isl.append(XmlUtil.attrs(ImageGenerator.ATTR_WIDTH, "" + width)
                   + XmlUtil.attrs(ImageGenerator.ATTR_HEIGHT, "" + height));
        isl.append(" />\n");
        isl.append("<pause/>\n");
        if (isMovie) {
            isl.append("<movie ");
        } else {
            isl.append("<image ");
        }
        String suffix = getImageSuffix(request);
        isl.append(" file=\"");
        isl.append(imageFile.toString());
        isl.append("\" ");
        if (isMovie) {
            isl.append(" framerate=\"");
            isl.append(request.getString(ARG_FRAMERATE, "2"));
            isl.append("\" ");
            isl.append(" endframepause=\"");
            isl.append(request.getString(ARG_ENDPAUSE, "2"));
            isl.append("\" ");
        }
        isl.append(" />\n");
        isl.append("</isl>\n");
        System.err.println(isl);

        return isl;
    }

    /**
     * Get the image suffix
     *
     * @param request  the request with the suffix parameter
     *
     * @return  the image file suffix
     */
    private String getImageSuffix(Request request) {
        String type = request.getString(ARG_IMAGE_TYPE, "gif");

        return "." + type;
    }


    /**
     * Handles all grid related requests
     *
     * @param request The request
     * @param entry The entry
     *
     * @return The Result
     *
     * @throws Exception On badness
     */
    public Result outputGrid(final Request request, Entry entry)
            throws Exception {
        //Check the data file path
        String path = getCdmManager().getPath(request, entry);
        if (path == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Could not load grid");

            return new Result("Grid Displays", sb);
        }

        String action = request.getString(ARG_IDV_ACTION,
                                          ACTION_MAKEINITFORM);

        //Get the dataset and create the data source
        GridDataset dataset = getCdmManager().getGridDataset(entry, path);
        DataSourceDescriptor descriptor =
            getIdv().getDataManager().getDescriptor("File.Grid");
        DataSource dataSource = new GeoGridDataSource(descriptor, dataset,
                                    entry.getName(), path);

        //See what we need to do
        try {
            if (action.equals(ACTION_MAKEINITFORM)) {
                return outputGridInitForm(request, entry, dataSource);
            } else if (action.equals(ACTION_MAKEFORM)) {
                return outputGridForm(request, entry, dataSource);
            } else if (action.equals(ACTION_MAKEPAGE)) {
                return outputGridPage(request, entry, dataSource);
            } else {
                return outputGridImage(request, entry, dataSource);
            }
        } finally {
            getCdmManager().returnGridDataset(path, dataset);
        }

    }



    /**
     * Makes the form
     *
     * @param request The request
     * @param entry The entry
     * @param dataSource The IDV DataSource
     *
     * @return The Result
     *
     * @throws Exception On badness
     */
    private Result outputGridForm(final Request request, Entry entry,
                                  DataSource dataSource)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb, "Make Image");
        makeGridForm(request, sb, entry, dataSource);
        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("Grid Displays", sb);
    }


    /**
     * Make the grid image form
     *
     * @param request The request
     * @param sb buffer to append to
     * @param entry The entry
     * @param dataSource The IDV DataSource
     *
     * @throws Exception On badness
     */
    private void makeGridForm(Request request, StringBuffer sb, Entry entry,
                              DataSource dataSource)
            throws Exception {


        String formUrl = getEntryManager().getFullEntryShowUrl(request);
        sb.append(HtmlUtils.form(formUrl, ""));
        sb.append(HtmlUtils.submit("Make Image", ARG_SUBMIT));
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_IDV_GRID));
        sb.append(HtmlUtils.hidden(ARG_IDV_ACTION, ACTION_MAKEPAGE));

        StringBuffer basic = new StringBuffer();
        basic.append(HtmlUtils.formTable());
        // check if we can add in GE Plugin
        List   productList = Misc.toList(products);
        String gekeys = getRepository().getProperty(PROP_GOOGLEAPIKEYS, null);
        if (gekeys != null) {
            productList.add(gePluginProduct);
        }

        basic.append(
            HtmlUtils.formEntry(
                msgLabel("Product"), htmlSelect(
                    request, ARG_IDV_PRODUCT, productList) + HtmlUtils.space(
                    2) + msg(
                    "Note: For Google Earth, make sure to set the view bounds")));


        String viewPointHtml = "";
        List   vms           = getIdv().getVMManager().getVMState();
        if (vms.size() >= 0) {
            List viewPoints = new ArrayList<String>();
            viewPoints.add(new TwoFacedObject("--none--", ""));
            for (int i = 0; i < vms.size(); i++) {
                ViewState viewState = (ViewState) vms.get(i);
                viewPoints.add(viewState.getName());
            }
            viewPointHtml = msgLabel("Viewpoint") + HtmlUtils.space(2)
                            + htmlSelect(request, ARG_VIEW_VIEWPOINT,
                                         viewPoints);
        }
        String imageTrans =
            HtmlUtils.space(3)
            + htmlCheckbox(request, ARG_BACKGROUND_TRANSPARENT, false)
            + HtmlUtils.space(2) + msg("Background Transparent");

        basic.append(
            HtmlUtils.formEntry(
                msgLabel("Image Size"),
                htmlInput(request, ARG_IMAGE_WIDTH, "600")
                + HtmlUtils.space(1) + "X" + HtmlUtils.space(1)
                + htmlInput(request, ARG_IMAGE_HEIGHT, "400") + imageTrans));


        basic.append(HtmlUtils.formEntry(msgLabel("Make globe"),
                                         htmlCheckbox(request,
                                             ARG_VIEW_GLOBE,
                                             false) + HtmlUtils.space(2)
                                                 + viewPointHtml));



        List projections =
            getIdv().getIdvProjectionManager().getProjections();

        Hashtable<String, List> projCatMap = new Hashtable<String, List>();
        List<String>            projCats   = new ArrayList<String>();
        for (int i = 0; i < projections.size(); i++) {
            ProjectionImpl proj = (ProjectionImpl) projections.get(i);
            String         name = proj.getName();
            List<String>   toks = StringUtil.split(name, ">", true, true);
            String         cat;
            String         label;
            if (toks.size() <= 1) {
                cat   = "Misc";
                label = name;
            } else {
                label = toks.remove(toks.size() - 1);
                cat   = StringUtil.join(">", toks);
            }
            List tfos = projCatMap.get(cat);
            if (tfos == null) {
                projCatMap.put(cat, tfos = new ArrayList());
                projCats.add(cat);
            }
            tfos.add(new TwoFacedObject(HtmlUtils.space(4) + label, name));
        }

        List projectionOptions = new ArrayList();
        projectionOptions.add(new TwoFacedObject("--none--", ""));
        for (String projCat : projCats) {
            projectionOptions.add(new TwoFacedObject(projCat, ""));
            projectionOptions.addAll(projCatMap.get(projCat));
        }

        basic.append(HtmlUtils.formEntry(msgLabel("Projection"),
                                         htmlSelect(request,
                                             ARG_VIEW_PROJECTION,
                                             projectionOptions)));


        basic.append(HtmlUtils.formEntry(msgLabel("Azimuth/Tilt"),
                                         htmlInput(request, ARG_AZIMUTH, "",
                                             6) + " "
                                                 + htmlInput(request,
                                                     ARG_TILT, "", 6)));


        List viewOptions = new ArrayList();
        viewOptions.add(new TwoFacedObject("--none--", ""));
        viewOptions.add(new TwoFacedObject("north"));
        viewOptions.add(new TwoFacedObject("south"));
        viewOptions.add(new TwoFacedObject("east"));
        viewOptions.add(new TwoFacedObject("west"));
        viewOptions.add(new TwoFacedObject("bottom"));
        viewOptions.add(new TwoFacedObject("top"));

        basic.append(HtmlUtils.formEntry(msgLabel("View"),
                                         htmlSelect(request, ARG_VIEWDIR,
                                             viewOptions)));

        /*
          basic.append(HtmlUtils.formEntry(msgLabel("Clip image"),
                                        htmlCheckbox(request, ARG_CLIP,
                                            false)));
        */

        double   zoom     = request.get(ARG_ZOOM, 1.0);
        Object[] zoomList = new Object[] {
            new TwoFacedObject("Current", "" + zoom),
            new TwoFacedObject("Reset", "1.0"),
            new TwoFacedObject("Zoom in", "" + (zoom * 1.25)),
            new TwoFacedObject("Zoom in more", "" + (zoom * 1.5)),
            new TwoFacedObject("Zoom in even more", "" + (zoom * 1.75)),
            new TwoFacedObject("Zoom out", "" + (zoom * 0.9)),
            new TwoFacedObject("Zoom out more", "" + (zoom * 0.7)),
            new TwoFacedObject("Zoom out even more", "" + (zoom * 0.5))
        };


        basic.append(
            HtmlUtils.formEntry(
                msgLabel("Zoom Level"),
                HtmlUtils.select(
                    ARG_ZOOM, Misc.toList(zoomList),
                    Misc.newList(request.defined(ARG_ZOOM)
                                 ? "" + zoom
                                 : ""), "")));



        basic.append(HtmlUtils.formTableClose());


        StringBuffer mapSB = new StringBuffer();
        List<MapData> maps = getIdv().getResourceManager().getMaps();
        Hashtable<String, List<TwoFacedObject>> mapCatMap =
            new Hashtable<String, List<TwoFacedObject>>();
        List<String> mapCats = new ArrayList<String>();
        for (MapData mapData : maps) {
            List<TwoFacedObject> mapCatList =
                mapCatMap.get(mapData.getCategory());
            if (mapCatList == null) {
                mapCatList = new ArrayList<TwoFacedObject>();
                mapCats.add(mapData.getCategory());
                mapCatMap.put(mapData.getCategory(), mapCatList);
            }
            mapCatList.add(new TwoFacedObject("&nbsp;&nbsp;"
                    + mapData.getDescription(), mapData.getSource()));
        }

        List<TwoFacedObject> mapOptions = new ArrayList<TwoFacedObject>();
        for (String cat : mapCats) {
            mapOptions.add(new TwoFacedObject(cat, ""));
            mapOptions.addAll(mapCatMap.get(cat));
        }

        //      mapSB.append(msgHeader("Maps"));
        String mapSelect =
            htmlSelect(request, ARG_MAPS, mapOptions,
                       HtmlUtils.attrs(HtmlUtils.ATTR_MULTIPLE, "true",
                                       HtmlUtils.ATTR_SIZE, "10"));
        StringBuffer mapAttrs = new StringBuffer();
        mapAttrs.append(HtmlUtils.formTable());
        mapAttrs.append(
            HtmlUtils.formEntry(
                msgLabel("Map Line Width"),
                HtmlUtils.select(
                    ARG_MAPWIDTH, Misc.newList("1", "2", "3", "4"),
                    request.getString(ARG_MAPWIDTH, "1"))));
        mapAttrs.append(
            HtmlUtils.formEntry(
                msgLabel("Map Color"),
                HtmlUtils.colorSelect(
                    ARG_MAPCOLOR,
                    request.getString(
                        ARG_MAPCOLOR, StringUtil.toHexString(Color.red)))));

        mapAttrs.append(
            HtmlUtils.formEntry(
                msgLabel("Background Color"),
                HtmlUtils.colorSelect(
                    ARG_VIEW_BACKGROUND,
                    request.getString(
                        ARG_VIEW_BACKGROUND,
                        StringUtil.toHexString(Color.black)))));



        if (backgrounds == null) {
            backgrounds = new ArrayList();
            backgrounds.add(new TwoFacedObject("--none--", ""));
            for (WmsSelection selection :
                    (List<WmsSelection>) getIdv().getBackgroundImages()) {
                if (selection.getLayer().indexOf("fixed") >= 0) {
                    backgrounds.add(new TwoFacedObject(selection.getTitle(),
                            selection.getLayer()));
                }
            }
        }


        mapAttrs.append(HtmlUtils.formEntry(msgLabel("Background Image"),
                                            htmlSelect(request,
                                                ARG_VIEW_BACKGROUNDIMAGE,
                                                    backgrounds)));

        mapAttrs.append(
            HtmlUtils.formEntry(
                msgLabel("Wireframe"),
                HtmlUtils.checkbox(
                    ARG_WIREFRAME, "true",
                    request.get(ARG_WIREFRAME, false))));

        mapAttrs.append(
            HtmlUtils.formEntry(
                msgLabel("Lat/Lon Lines"),
                HtmlUtils.checkbox(
                    ARG_LATLON_VISIBLE, "true",
                    request.get(ARG_LATLON_VISIBLE, false)) + "  "
                        + msgLabel("Spacing") + " "
                        + htmlInput(request, ARG_LATLON_SPACING, "")));



        mapAttrs.append(HtmlUtils.formTableClose());

        mapSB.append(HtmlUtils.table(new Object[] { mapSelect, mapAttrs },
                                     10));
        //      basic =new StringBuffer(HtmlUtils.table(new Object[]{basic, mapSB},10));
        List<String> tabLabels   = new ArrayList<String>();
        List<String> tabContents = new ArrayList<String>();
        tabLabels.add(msg("Basic"));
        tabContents.add(basic.toString());

        StringBuffer bounds = new StringBuffer();
        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, true, null);
        map.addBox(request,entry, new MapProperties("blue", false));
        map.centerOn(request,entry);
        String llb = map.makeSelector(ARG_VIEW_BOUNDS, true, null,
                                      htmlCheckbox(request,
                                          ARG_VIEW_JUSTCLIP, false) + " "
                                              + msg("Just subset data")
                                              + HtmlUtils.space(2), "");
        bounds.append(llb);


        tabLabels.add(msg("View Bounds"));
        tabContents.add(bounds.toString());

        tabLabels.add(msg("Maps and Background"));
        tabContents.add(mapSB.toString());

        List colorTables = getIdv().getColorTableManager().getColorTables();


        Hashtable<String, DataChoice> idToChoice = new Hashtable<String,
                                                       DataChoice>();
        List<DataChoice> choices =
            (List<DataChoice>) dataSource.getDataChoices();
        for (DataChoice dataChoice : choices) {
            if (dataChoice instanceof DerivedDataChoice) {
                idToChoice.put(
                    StringUtil.camelCase(dataChoice.getDescription()),
                    dataChoice);
            } else {
                idToChoice.put(dataChoice.getName(), dataChoice);
            }
        }

        List params = request.get(ARG_PARAM, new ArrayList());
        //System.out.print("YYYYYYYY " + params.size() + "  HH " + params.get(0).toString());
        int displayIdx = -1;
        for (int i = 0; i < params.size(); i++) {
            String param = (String) params.get(i);
            if (param.length() == 0) {
                continue;
            }
            displayIdx++;


            List<String> innerTabTitles   = new ArrayList<String>();
            List<String> innerTabContents = new ArrayList<String>();

            StringBuffer tab              = new StringBuffer();
            DataChoice   choice           = idToChoice.get(param);

            if (choice == null) {
                continue;
            }


            List descriptors =
                new ArrayList(
                    ControlDescriptor.getApplicableControlDescriptors(
                        choice.getCategories(),
                        getIdv().getControlDescriptors(true)));


            List<TwoFacedObject> displays = new ArrayList<TwoFacedObject>();
            displays.add(new TwoFacedObject("--skip--", ""));
            for (ControlDescriptor controlDescriptor :
                    (List<ControlDescriptor>) descriptors) {
                String controlId = controlDescriptor.getControlId();
                if ( !okControls.contains(controlId)) {
                    continue;
                }
                displays.add(new TwoFacedObject(controlDescriptor.getLabel(),
                        controlId));
            }


            tab.append(HtmlUtils.hidden(ARG_PARAM, param));


            List options = new ArrayList();
            options.add("");
            tab.append(HtmlUtils.br());
            tab.append(msgLabel("Display Type"));
            tab.append(HtmlUtils.space(1));
            if ((displayIdx == 0) && (displays.size() > 1)) {
                //Set the default display for the first param
                if (request.defined(ARG_IDV_DISPLAY + displayIdx)) {
                    tab.append(htmlSelect(request,
                                          ARG_IDV_DISPLAY + displayIdx,
                                          displays));
                } else {
                    tab.append(HtmlUtils.select(ARG_IDV_DISPLAY + displayIdx,
                            displays, displays.get(1).getId().toString()));
                }
            } else {
                tab.append(htmlSelect(request, ARG_IDV_DISPLAY + displayIdx,
                                      displays));
            }
            tab.append(HtmlUtils.p());

            List times = choice.getAllDateTimes();
            if ((times != null) && (times.size() > 0)) {

                List<Object[]> tuples = new ArrayList<Object[]>();
                int            cnt    = 0;
                for (Object time : times) {
                    tuples.add(new Object[] { time, Integer.valueOf(cnt++) });
                }
                tuples = (List<Object[]>) Misc.sortTuples(tuples, true);
                List tfoTimes = new ArrayList();
                for (Object[] tuple : tuples) {
                    tfoTimes.add(new TwoFacedObject(tuple[0].toString(),
                            tuple[1]));
                }
                innerTabTitles.add(msg("Times"));
                innerTabContents.add(htmlSelect(request,
                        ARG_TIMES + displayIdx, tfoTimes, true,
                        HtmlUtils.attrs(HtmlUtils.ATTR_MULTIPLE, "true",
                                        HtmlUtils.ATTR_SIZE, "5")));
            }


            List spatialComps = new ArrayList();
            List ensMembers = (List) choice.getProperty(
                                  GeoGridDataSource.PROP_ENSEMBLEMEMBERS);
            if ((ensMembers != null) && !ensMembers.isEmpty()) {
                spatialComps.add(msgLabel("Ensemble Member"));
                String ensComp =
                    htmlSelect(request, ARG_ENS + displayIdx, ensMembers,
                               false,
                               HtmlUtils.attrs(HtmlUtils.ATTR_MULTIPLE,
                                   "true", HtmlUtils.ATTR_SIZE, "5"));
                spatialComps.add(ensComp);
            }

            List levels = choice.getAllLevels();
            if ((levels != null) && (levels.size() > 0)) {
                List tfoLevels = new ArrayList();
                int  cnt       = 0;
                for (Object level : levels) {
                    tfoLevels.add(new TwoFacedObject(level.toString(),
                            Integer.valueOf(cnt++)));
                }
                String levelWidget =
                    htmlSelect(request, ARG_LEVELS + displayIdx, tfoLevels,
                               true,
                               HtmlUtils.attrs(HtmlUtils.ATTR_MULTIPLE,
                                   "false", HtmlUtils.ATTR_SIZE, "5"));
                spatialComps.add(msgLabel("Levels"));
                spatialComps.add(levelWidget);
            }


            ThreeDSize size = (ThreeDSize) choice.getProperty(
                                  GeoGridDataSource.PROP_GRIDSIZE);
            spatialComps.add(msgLabel("X/Y Stride"));
            String strideComp = htmlInput(request, ARG_STRIDE + displayIdx,
                                          "");

            if (size != null) {
                strideComp = strideComp + HtmlUtils.br() + size;
            }
            spatialComps.add(strideComp);

            String spatial =
                HtmlUtils.table(StringUtil.listToStringArray(spatialComps),
                                5);
            innerTabTitles.add(msg("Coordinates"));
            innerTabContents.add(spatial);

            ColorTable dfltColorTable =
                getIdv().getDisplayConventions().getParamColorTable(
                    choice.getName());
            Range range = getIdv().getDisplayConventions().getParamRange(
                              choice.getName(), null);

            List<String> ctCats = new ArrayList<String>();
            Hashtable<String, StringBuffer> ctCatMap = new Hashtable<String,
                                                           StringBuffer>();
            for (ColorTable colorTable : (List<ColorTable>) colorTables) {
                StringBuffer catSB = ctCatMap.get(colorTable.getCategory());
                if (catSB == null) {
                    catSB = new StringBuffer();
                    ctCatMap.put(colorTable.getCategory(), catSB);
                    ctCats.add(colorTable.getCategory());
                }
                String icon = IOUtil.cleanFileName(colorTable.getName())
                              + ".png";
                icon = icon.replace(" ", "_");
                String img = "<img border=0 src="
                             + getRepository().getUrlBase()
                             + "/idv/colortables/" + icon + ">";
                String div = HtmlUtils.div(img + " " + colorTable.getName(),
                                           "");
                String call1 = HtmlUtils.call(
                                   "HtmlUtil.setFormValue",
                                   HtmlUtils.squote(
                                       ARG_COLORTABLE + displayIdx) + ","
                                           + HtmlUtils.squote(
                                               colorTable.getName()));
                String call2 = HtmlUtils.call(
                                   "HtmlUtil.setHtml",
                                   HtmlUtils.squote(
                                       ARG_COLORTABLE + "_html"
                                       + displayIdx) + ","
                                           + HtmlUtils.squote(
                                               colorTable.getName() + " "
                                               + img));
                String call = call1 + ";" + call2;
                catSB.append(HtmlUtils.mouseClickHref(call, div));
            }




            StringBuffer ctsb = new StringBuffer();
            ctsb.append(msgLabel("Range") + HtmlUtils.space(1)
                        + htmlInput(request, ARG_RANGE_MIN + displayIdx,
                                    ((range == null)
                                     ? ""
                                     : "" + range.getMin())) + " - "
                                     + htmlInput(request,
                                         ARG_RANGE_MAX + displayIdx,
                                         ((range == null)
                                          ? ""
                                          : range.getMax() + "")));

            if ( !request.defined(ARG_COLORTABLE + displayIdx)
                    && (dfltColorTable != null)) {
                request.put(ARG_COLORTABLE + displayIdx,
                            dfltColorTable.getName());
            }

            ctsb.append(
                HtmlUtils.hidden(
                    ARG_COLORTABLE + displayIdx,
                    request.getString(ARG_COLORTABLE + displayIdx, ""),
                    HtmlUtils.id(ARG_COLORTABLE + displayIdx)));
            ctsb.append(HtmlUtils.br());
            String ctDiv = "-default-";
            if (request.defined(ARG_COLORTABLE + displayIdx)) {
                String icon =
                    IOUtil.cleanFileName(request.getString(ARG_COLORTABLE
                        + displayIdx, "")) + ".png";
                String img = HtmlUtils.img(getRepository().getUrlBase()
                                           + "/idv/colortables/" + icon);
                ctDiv = request.getString(ARG_COLORTABLE + displayIdx,
                                          "-default-") + " " + img;

            }
            ctsb.append(HtmlUtils.table(new Object[] {
                msgLabel("Color Table"),
                HtmlUtils.div(ctDiv,
                              HtmlUtils.id(ARG_COLORTABLE + "_html"
                                           + displayIdx)) }, 2));

            String call =
                HtmlUtils.call("HtmlUtil.setFormValue",
                               "'" + ARG_COLORTABLE + displayIdx + "','" + ""
                               + "'") + ";"
                                      + HtmlUtils.call("HtmlUtil.setHtml",
                                          "'" + ARG_COLORTABLE + "_html"
                                          + displayIdx + "','" + "-default-"
                                          + "'");
            ctsb.append(HtmlUtils.mouseClickHref(call, "Use default"));
            for (String ctcat : ctCats) {
                ctsb.append(HtmlUtils.makeShowHideBlock(ctcat,
                        ctCatMap.get(ctcat).toString(), false));

            }

            StringBuffer scalesb = new StringBuffer();
            scalesb.append(msgHeader("Color Scale"));
            scalesb.append(HtmlUtils.formTable());
            scalesb.append(HtmlUtils.formEntry(msgLabel("Visible"),
                    htmlCheckbox(request, ARG_SCALE_VISIBLE + displayIdx,
                                 false)));
            scalesb.append(HtmlUtils.formEntry(msgLabel("Place"),
                    htmlSelect(request, ARG_SCALE_PLACEMENT + displayIdx,
                               Misc.newList("top", "left", "bottom",
                                            "right"))));
            scalesb.append(HtmlUtils.formTableClose());





            StringBuffer contoursb = new StringBuffer();
            ContourInfo ci =
                getIdv().getDisplayConventions().findDefaultContourInfo(
                    choice.getName());
            contoursb.append(HtmlUtils.formTable());
            contoursb.append(HtmlUtils.formEntry(msgLabel("Interval"),
                    htmlInput(request, ARG_CONTOUR_INTERVAL + displayIdx,
                              ((ci == null)
                               ? ""
                               : ci.getInterval() + ""), 3)));
            contoursb.append(HtmlUtils.formEntry(msgLabel("Base"),
                    htmlInput(request, ARG_CONTOUR_BASE + displayIdx,
                              ((ci == null)
                               ? ""
                               : ci.getBase() + ""), 3)));
            contoursb.append(HtmlUtils.formEntry(msgLabel("Min"),
                    htmlInput(request, ARG_CONTOUR_MIN + displayIdx,
                              ((ci == null)
                               ? ""
                               : ci.getMin() + ""), 3)));

            contoursb.append(HtmlUtils.formEntry(msgLabel("Max"),
                    htmlInput(request, ARG_CONTOUR_MAX + displayIdx,
                              ((ci == null)
                               ? ""
                               : ci.getMax() + ""), 3)));
            contoursb.append(HtmlUtils.formEntry(msgLabel("Line Width"),
                    htmlSelect(request, ARG_CONTOUR_WIDTH + displayIdx,
                               Misc.newList("1", "2", "3", "4"))));
            contoursb.append(HtmlUtils.formEntry(msgLabel("Dashed"),
                    htmlCheckbox(request, ARG_CONTOUR_DASHED + displayIdx,
                                 ((ci == null)
                                  ? false
                                  : ci.getDashOn()))));
            contoursb.append(HtmlUtils.formEntry(msgLabel("Labels"),
                    htmlCheckbox(request, ARG_CONTOUR_LABELS + displayIdx,
                                 ((ci == null)
                                  ? true
                                  : ci.getIsLabeled()))));
            contoursb.append(HtmlUtils.formTableClose());


            StringBuffer misc = new StringBuffer();
            misc.append(HtmlUtils.formTable());
            misc.append(HtmlUtils.formEntry(msgLabel("Display List Label"),
                                            htmlInput(request,
                                                ARG_IDV_DISPLAYLISTLABEL
                                                    + displayIdx, "", 30)));
            String unitString = "";
            Unit displayUnit =
                getIdv().getDisplayConventions().getDisplayUnit(
                    choice.getName(), null);
            if (displayUnit != null) {
                unitString = displayUnit.toString();
            }
            misc.append(HtmlUtils.formEntry(msgLabel("Display Unit"),
                                            htmlInput(request,
                                                ARG_IDV_DISPLAYUNIT
                                                    + displayIdx, unitString,
                                                        6)));

            misc.append(
                HtmlUtils.formEntry(
                    msgLabel("Display Color"),
                    HtmlUtils.colorSelect(
                        ARG_IDV_DISPLAYCOLOR + displayIdx,
                        request.getString(
                            ARG_IDV_DISPLAYCOLOR + displayIdx,
                            StringUtil.toHexString(Color.red)))));


            misc.append(HtmlUtils.formEntry(msgLabel("Isosurface Value"),
                                            htmlInput(request,
                                                ARG_ISOSURFACEVALUE
                                                    + displayIdx, "", 3)));
            misc.append(
                HtmlUtils.formEntry(
                    msgLabel("XS Selector"),
                    "Lat 1: "
                    + htmlInput(request, ARG_LAT1 + displayIdx, "", 6)
                    + "Lon 1: "
                    + htmlInput(request, ARG_LON1 + displayIdx, "", 6)
                    + "     " + "Lat 2: "
                    + htmlInput(request, ARG_LAT2 + displayIdx, "", 6)
                    + "Lon 2: "
                    + htmlInput(request, ARG_LON2 + displayIdx, "", 6)));


            misc.append(HtmlUtils.formEntry(msgLabel("Vector/Barb Size"),
                                            htmlInput(request,
                                                ARG_FLOW_SCALE + displayIdx,
                                                    "4", 3)));
            misc.append(HtmlUtils.formEntry(msgLabel("Streamline Density"),
                                            htmlInput(request,
                                                ARG_FLOW_DENSITY
                                                    + displayIdx, "1", 3)));
            misc.append(HtmlUtils.formEntry(msgLabel("Flow Skip"),
                                            htmlInput(request,
                                                ARG_FLOW_SKIP + displayIdx,
                                                    "0", 3)));


            misc.append(HtmlUtils.formTableClose());

            innerTabTitles.add(msg("Color Table"));
            innerTabContents.add(HtmlUtils.table(new Object[] { ctsb,
                    scalesb }, 5));

            innerTabTitles.add(msg("Contours"));
            innerTabContents.add(contoursb.toString());

            innerTabTitles.add(msg("Misc"));
            innerTabContents.add(misc.toString());



            String innerTab = OutputHandler.makeTabs(innerTabTitles,
                                  innerTabContents, true);
            tab.append(HtmlUtils.inset(HtmlUtils.p() + innerTab, 10));

            tabLabels.add(
                StringUtil.camelCase(
                    StringUtil.shorten(choice.getDescription(), 25)));
            tabContents.add(tab.toString());
        }
        if ( !request.getUser().getAnonymous()) {
            StringBuffer publishSB = new StringBuffer();
            publishSB.append(HtmlUtils.formTable());
            addPublishWidget(
                request, entry, publishSB,
                msg("Select a folder to publish the product to"));
            publishSB.append(HtmlUtils.formEntry("",
                    HtmlUtils.submit("Publish image",
                                     ARG_SUBMIT_PUBLISH)));

            if (getAccessManager().canDoEdit(request, entry)) {

                publishSB.append(
                    HtmlUtils.row(HtmlUtils.colspan(HtmlUtils.p(), 2)));
                publishSB.append(
                    HtmlUtils.row(
                        HtmlUtils.colspan(
                            msgHeader("Or save these settings"), 2)));
                publishSB.append(
                    HtmlUtils.formEntry(
                        msgLabel("Settings name"),
                        HtmlUtils.input(ARG_SAVE_NAME, "", 30)));
                publishSB.append(HtmlUtils.formEntry(msg("Attach image"),
                        HtmlUtils.checkbox(ARG_SAVE_ATTACH, "true", false)));
                publishSB.append(HtmlUtils.formEntry("",
                        HtmlUtils.submit("Save settings",
                                         ARG_SUBMIT_SAVE)));

            }
            publishSB.append(HtmlUtils.formTableClose());


            tabLabels.add(msg("Publish"));
            tabContents.add(publishSB.toString());
        }


        sb.append(OutputHandler.makeTabs(tabLabels, tabContents, true));

        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.submit("Make Image", ARG_SUBMIT));
        sb.append(HtmlUtils.formClose());
    }


    /**
     * The initial grid form. Lists the data choices available
     *
     * @param request The request
     * @param entry The entry
     * @param dataSource The IDV DataSource
     *
     * @return The Result
     *
     * @throws Exception On badness
     */
    private Result outputGridInitForm(final Request request, Entry entry,
                                      DataSource dataSource)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb,
                                          "Select Fields");

        String formUrl = request.makeUrl(getRepository().URL_ENTRY_SHOW);
        //        String       formUrl = getEntryManager().getFullEntryShowUrl(request);
        sb.append(HtmlUtils.form(formUrl, ""));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_IDV_GRID));
        sb.append(HtmlUtils.hidden(ARG_IDV_ACTION, ACTION_MAKEFORM));

        List<DataChoice> choices =
            (List<DataChoice>) dataSource.getDataChoices();

        sb.append(msgHeader("Select one or more fields to view"));

        StringBuffer fields  = new StringBuffer();
        List         options = new ArrayList();
        //            options.add(new TwoFacedObject("--Pick one--", ""));
        List<String> cats = new ArrayList<String>();
        Hashtable<String, List<TwoFacedObject>> catMap =
            new Hashtable<String, List<TwoFacedObject>>();
        for (DataChoice dataChoice : choices) {
            String label = StringUtil.camelCase(dataChoice.getDescription());
            DataCategory cat = dataChoice.getDisplayCategory();
            String       catName;
            if (cat != null) {
                catName = cat.toString();
            } else {
                catName = "Data";
            }
            List<TwoFacedObject> tfos = catMap.get(catName);
            if (tfos == null) {
                tfos = new ArrayList<TwoFacedObject>();
                catMap.put(catName, tfos);
                cats.add(catName);
            }
            if (dataChoice instanceof DerivedDataChoice) {
                tfos.add(new TwoFacedObject("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                                            + label, label));
            } else {
                tfos.add(new TwoFacedObject("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                                            + label, dataChoice.getName()));
            }
        }

        for (String cat : cats) {
            options.add(new TwoFacedObject(cat.replace("-", "&gt;"), ""));
            options.addAll(catMap.get(cat));
        }


        fields.append(htmlSelect(request, ARG_PARAM, options,
                                 HtmlUtils.attrs(HtmlUtils.ATTR_MULTIPLE,
                                     "true", HtmlUtils.ATTR_SIZE, "10")));
        fields.append(HtmlUtils.p());

        sb.append(HtmlUtils.insetLeft(fields.toString(), 10));
        sb.append(HtmlUtils.submit("Select Fields", ARG_SUBMIT));
        sb.append(HtmlUtils.formClose());



        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                METADATA_TYPE_VISUALIZATION, false);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            sb.append(HtmlUtils.p());
            sb.append(msgHeader("Or select a predefined visualization"));
            sb.append(HtmlUtils.open(HtmlUtils.TAG_UL));
            MetadataType metadataType =
                getMetadataManager().findType(METADATA_TYPE_VISUALIZATION);
            for (Metadata metadata : metadataList) {
                String url =
                    HtmlUtils.url(getRepository().URL_ENTRY_SHOW.toString(),
                                  new String[] {
                    ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
                    OUTPUT_IDV_GRID.toString(), ARG_IDV_ACTION,
                    ACTION_MAKEPAGE, ARG_PREDEFINED, metadata.getId()
                });
                sb.append(HtmlUtils.li(HtmlUtils.href(url,
                        metadata.getAttr1()), ""));
                metadataType.decorateEntry(request, entry, sb, metadata,
                                           true, true);
            }
            sb.append(HtmlUtils.close(HtmlUtils.TAG_UL));
        }

        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("Grid Displays", sb);
    }



    /**
     * Get the request arguments
     *
     * @param request The request
     *
     * @return  the arguments to use
     */
    private Hashtable getRequestArgs(Request request) {
        Hashtable requestArgs = new Hashtable(request.getArgs());
        requestArgs.remove(ARG_IDV_ACTION);
        requestArgs.remove(ARG_ENTRYID);
        requestArgs.remove(ARG_SUBMIT);
        requestArgs.remove(ARG_OUTPUT);
        requestArgs.remove(ARG_PUBLISH_ENTRY);
        requestArgs.remove(ARG_SUBMIT_PUBLISH);
        requestArgs.remove(ARG_SUBMIT_SAVE);
        requestArgs.remove(ARG_PUBLISH_ENTRY + "_hidden");
        requestArgs.remove(ARG_PUBLISH_NAME);

        return requestArgs;
    }


    /**
     * Output the grid page
     *
     * @param request The request
     * @param entry The entry
     * @param dataSource The IDV DataSource
     *
     * @return  the result
     *
     * @throws Exception On badness
     */
    private Result outputGridPage(final Request request, Entry entry,
                                  DataSource dataSource)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_SUBMIT_PUBLISH) && doingPublish(request)) {
            File imageFile = (File) generateGridImage(request, entry,
                                 dataSource);

            return getEntryManager().processEntryPublish(request, imageFile,
                    null, entry, "derived product");
        }


        if (request.defined(ARG_PREDEFINED)) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(request, entry,
                    METADATA_TYPE_VISUALIZATION, false);
            String args = null;
            if ((metadataList != null) && (metadataList.size() > 0)) {
                for (Metadata metadata : metadataList) {
                    if (metadata.getId().equals(
                            request.getString(ARG_PREDEFINED, ""))) {
                        args = metadata.getAttr2();

                        break;
                    }
                }
            }
            if (args != null) {
                Hashtable urlArgs = new Hashtable();
                for (String pair : StringUtil.split(args, "&", true, true)) {
                    List<String> toks = StringUtil.splitUpTo(pair, "=", 2);
                    if ((toks == null) || (toks.size() != 2)) {
                        continue;
                    }
                    String name = java.net.URLDecoder.decode(toks.get(0),
                                      "UTF-8");
                    String value = java.net.URLDecoder.decode(toks.get(1),
                                       "UTF-8");
                    Object o = urlArgs.get(name);
                    if (o == null) {
                        urlArgs.put(name, value);
                    } else if (o instanceof List) {
                        ((List) o).add(value);
                    } else {
                        List l = new ArrayList();
                        l.add(o);
                        l.add(value);
                        urlArgs.put(name, l);
                    }
                }
                for (Enumeration keys = urlArgs.keys();
                        keys.hasMoreElements(); ) {
                    String arg   = (String) keys.nextElement();
                    Object value = urlArgs.get(arg);
                    request.put(arg, value);
                }
                request.remove(ARG_SUBMIT_SAVE);
            }
        }


        String baseName = IOUtil.stripExtension(entry.getName());
        String product  = request.getString(ARG_IDV_PRODUCT, PRODUCT_IMAGE);
        //        String url      = getEntryManager().getFullEntryShowUrl(request);
        String url     = request.makeUrl(getRepository().URL_ENTRY_SHOW);


        String islUrl  = url + "/" + baseName + ".isl";
        String jnlpUrl = url + "/" + baseName + ".jnlp";

        if (product.equals(PRODUCT_IMAGE)) {
            url = url + "/" + baseName + ".gif";
        } else if (product.equals(PRODUCT_MOV)) {
            url = url + "/" + baseName + ".mov";
        } else if (product.equals(PRODUCT_KMZ)) {
            url = url + "/" + baseName + ".kmz";
        }

        String args = request.getUrlArgs(exceptArgs, null, ".*_gvdflt");
        url = url + "?" + ARG_IDV_ACTION + "=" + ACTION_MAKEIMAGE + "&"
              + args;

        if (request.defined(ARG_SUBMIT_SAVE)) {
            if ( !getAccessManager().canDoEdit(request, entry)) {
                throw new AccessException("No access", request);
            }

            String fileName = "";
            if (request.get(ARG_SAVE_ATTACH, false)) {
                Object fileOrResult = generateGridImage(request, entry,
                                          dataSource);
                if (fileOrResult instanceof Result) {
                    throw new IllegalArgumentException(
                        "You need to specify an image or movie product");
                }

                File imageFile = (File) fileOrResult;

                fileName = getStorageManager().copyToEntryDir(entry,
                        imageFile).getName();
            }

            Metadata metadata = new Metadata(getRepository().getGUID(),
                                             entry.getId(),
                                             getMetadataManager().findType(METADATA_TYPE_VISUALIZATION),
                                             false,
                                             request.getString(ARG_SAVE_NAME,
                                                 ""), args, fileName, "", "");
            getMetadataManager().insertMetadata(metadata);
            getMetadataManager().addMetadata(request,entry, metadata);

            return new Result(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                    OUTPUT_IDV_GRID.toString()));

        }

        islUrl = islUrl + "?" + ARG_IDV_ACTION + "=" + ACTION_MAKEIMAGE + "&"
                 + args + "&" + ARG_IDV_TARGET + "=" + TARGET_ISL;
        jnlpUrl = jnlpUrl + "?" + ARG_IDV_ACTION + "=" + ACTION_MAKEIMAGE
                  + "&" + args + "&" + ARG_IDV_TARGET + "=" + TARGET_JNLP;

        boolean showForm = true;

        getPageHandler().entrySectionOpen(request, entry, sb, "Product");
        if (product.equals(PRODUCT_IMAGE)) {
            sb.append(HtmlUtils.img(url, "Image is being processed...",
                                    HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                        request.getString(ARG_IMAGE_WIDTH,
                                            "600"))));
            showForm = false;
        } else if (product.equals(PRODUCT_MOV)) {
            sb.append(HtmlUtils.href(url,
                                     "Click here to retrieve the movie"));
        } else if (product.equals(PRODUCT_KMZ)) {
            sb.append(HtmlUtils.href(url,
                                     "Click here to retrieve the KMZ file"));

	}


        StringBuffer formSB = new StringBuffer();
        makeGridForm(request, formSB, entry, dataSource);
        sb.append(HtmlUtils.div("",
                                HtmlUtils.cssClass("image_edit_box")
                                + HtmlUtils.id("image_edit_box")));

        formSB.append(HtmlUtils.space(2));
        formSB.append(HtmlUtils.href(jnlpUrl, msg("Launch in the IDV")));
        formSB.append(HtmlUtils.space(2));
        formSB.append(HtmlUtils.href(islUrl, msg("Download IDV ISL script")));


        sb.append("\n");
        sb.append(HtmlUtils.br());
        sb.append(HtmlUtils.makeShowHideBlock(msg("Image Settings"),
                formSB.toString(), showForm));

        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("Grid Displays", sb);

    }


    /**
     * This makes the image
     *
     * @param request The request
     * @param entry The entry
     * @param dataSource The IDV DataSource
     *
     * @return The Result
     *
     * @throws Exception On badness
     */
    public Result outputGridImage(final Request request, Entry entry,
                                  DataSource dataSource)
            throws Exception {


        Object fileOrResult = generateGridImage(request, entry, dataSource);
        if (fileOrResult instanceof Result) {
            return (Result) fileOrResult;
        }
        File   imageFile = (File) fileOrResult;
        String extension = IOUtil.getFileExtension(imageFile.toString());

        return new Result("",
                          getStorageManager().getFileInputStream(imageFile),
                          getRepository().getMimeTypeFromSuffix(extension));
    }

    /**
     * Generate the grid image
     *
     * @param request The request
     * @param entry The entry
     * @param dataSource The IDV DataSource
     *
     * @return the result
     *
     * @throws Exception On badness
     */
    private Object generateGridImage(Request request, Entry entry,
                                     DataSource dataSource)
            throws Exception {

        String id      = entry.getId();
        String product = request.getString(ARG_IDV_PRODUCT, PRODUCT_IMAGE);
        boolean forIsl = request.getString(ARG_IDV_TARGET,
                                           "").equals(TARGET_ISL);
        boolean forJnlp = request.getString(ARG_IDV_TARGET,
                                            "").equals(TARGET_JNLP);
        if (forJnlp) {
            forIsl = true;
        }


        Hashtable    requestArgs = getRequestArgs(request);
        List<String> argList     = new ArrayList<String>();
        List<String> valueList   = new ArrayList<String>();

        for (Enumeration keys =
                requestArgs.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if (exceptArgs.contains(arg)) {
                continue;
            }
            Object value = requestArgs.get(arg);
            String s     = value.toString();
            if (s.trim().length() == 0) {
                continue;
            }
            argList.add(arg);
        }
        Collections.sort(argList);
        StringBuffer fileKey = new StringBuffer();
        for (String arg : argList) {
            Object value = requestArgs.get(arg);
            String s     = value.toString();
            fileKey.append(arg);
            fileKey.append("=");
            fileKey.append(s);
            fileKey.append(";");
        }

        //      System.err.println ("fileKey: " + fileKey);
        boolean multipleTimes = false;
        String  suffix        = ".gif";
        if (product.equals(PRODUCT_IMAGE)) {
            suffix = ".gif";
        } else if (product.equals(PRODUCT_MOV)) {
            multipleTimes = true;
            suffix        = ".mov";
        } else if (product.equals(PRODUCT_KMZ)) {
            multipleTimes = true;
            suffix        = ".kmz";
        }

        String imageKey  = fileKey.toString();
        File   imageFile = null;
        synchronized (imageCache) {
            imageFile = imageCache.get(imageKey);
        }
        if (imageFile == null) {
            imageFile = getStorageManager().getTmpFile(request,
                    "gridimage" + suffix);
        }

        if ( !forIsl && imageFile.exists()) {
            //      System.err.println ("got  file");
            return imageFile;
        }


        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");

        StringBuffer viewProps = new StringBuffer();
        if (request.defined(ARG_VIEW_PROJECTION)) {
            viewProps.append(
                makeProperty(
                    "defaultProjectionName",
                    request.getString(ARG_VIEW_PROJECTION, "")));

        }

        viewProps.append(makeProperty("wireframe",
                                      "" + request.get(ARG_WIREFRAME,
                                          false)));



        if (request.get(ARG_LATLON_VISIBLE, false)) {
            viewProps.append(makeProperty("initLatLonVisible", "true"));
            if (request.defined(ARG_LATLON_SPACING)) {
                viewProps.append(makeProperty("initLatLonSpacing",
                        "" + request.get(ARG_LATLON_SPACING, 15.0)));
            }
        }

        viewProps.append("\n");

        if (request.get(ARG_VIEW_GLOBE, false)) {
            viewProps.append(makeProperty("useGlobeDisplay", true));
            viewProps.append("\n");
            if (request.defined(ARG_VIEW_VIEWPOINT)) {
                viewProps.append(makeProperty("initViewStateName",
                        request.getString(ARG_VIEW_VIEWPOINT, "")));
                viewProps.append("\n");
            }
        }



        viewProps.append(makeProperty("background",
                                      request.getString(ARG_VIEW_BACKGROUND,
                                          "black")));
        viewProps.append("\n");

        viewProps.append(makeProperty("initMapPaths",
                                      StringUtil.join(",",
                                          request.get(ARG_MAPS,
                                              new ArrayList()))));
        viewProps.append("\n");

        if (request.defined(ARG_MAPWIDTH)) {
            viewProps.append(makeProperty("initMapWidth",
                                          request.getString(ARG_MAPWIDTH,
                                              "1")));
            viewProps.append("\n");
        }
        if (request.defined(ARG_MAPCOLOR)) {
            viewProps.append(makeProperty("initMapColor",
                                          request.getString(ARG_MAPCOLOR,
                                              "")));
            viewProps.append("\n");
        }

        double zoom = request.get(ARG_ZOOM, 1.0);
        if (zoom != 1.0) {
            viewProps.append(makeProperty("displayProjectionZoom",
                                          "" + zoom));
            viewProps.append("\n");
        }

        String clip = "";
        if (request.get(ARG_CLIP, false)) {
            clip = XmlUtil.tag("clip", "");
        }

        // TODO:  When the IDV selects png automatically for kmz, then there is no need for
        // the bgSuffix
        String bgTrans  = "";
        String bgSuffix = "";
        if (request.get(ARG_BACKGROUND_TRANSPARENT, false)) {
            bgTrans = XmlUtil.tag(ImageGenerator.TAG_BGTRANSPARENT, "");
            bgSuffix = XmlUtil.attr(ImageSequenceGrabber.ATTR_IMAGESUFFIX,
                                    ".png");
        }

        String       dataSourceExtra = "";
        StringBuffer dataSourceProps = new StringBuffer();


        int          width           = request.get(ARG_IMAGE_WIDTH, 400);
        int          height          = request.get(ARG_IMAGE_HEIGHT, 400);
        if (request.defined(ARG_VIEW_BOUNDS + "_south")
                && request.defined(ARG_VIEW_BOUNDS + "_north")
                && request.defined(ARG_VIEW_BOUNDS + "_east")
                && request.defined(ARG_VIEW_BOUNDS + "_west")) {
            double south   = request.get(ARG_VIEW_BOUNDS + "_south", 0.0);
            double north   = request.get(ARG_VIEW_BOUNDS + "_north", 0.0);
            double east    = request.get(ARG_VIEW_BOUNDS + "_east", 0.0);
            double west    = request.get(ARG_VIEW_BOUNDS + "_west", 0.0);
            double bwidth  = Math.abs(east - west);
            double bheight = Math.abs(north - south);

            if ( !request.get(ARG_VIEW_JUSTCLIP, false)) {
                if ( !request.defined(ARG_VIEW_PROJECTION)) {
                    viewProps.append(makeProperty("initLatLonBounds",
                            west + "," + north + "," + bwidth + ","
                            + bheight));
                    viewProps.append("\n");
                }
            }
            dataSourceProps.append(makeProperty("defaultSelectionBounds",
                    west + "," + north + "," + bwidth + "," + bheight));
            dataSourceProps.append("\n");

            height = (int) (width * bheight / bwidth);
        }



        //Create a new viewmanager
        //For now don't do this if we are doing jnlp
        if ( !forJnlp) {
            isl.append(
                XmlUtil.tag(
                    ImageGenerator.TAG_VIEW,
                    XmlUtil.attrs(ImageGenerator.ATTR_WIDTH, "" + width)
                    + XmlUtil.attrs(
                        ImageGenerator.ATTR_HEIGHT,
                        "" + height), viewProps.toString()));
        }


        if (request.defined(ARG_VIEW_BACKGROUNDIMAGE)) {
            StringBuffer propSB = new StringBuffer();
            propSB.append(makeProperty("id", "backgroundimage"));
            propSB.append(
                makeProperty(
                    "theLayer",
                    request.getString(ARG_VIEW_BACKGROUNDIMAGE, "")));
            StringBuffer attrs = new StringBuffer();
            attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TYPE,
                                       "wmscontrol"));


            isl.append(XmlUtil.tag(ImageGenerator.TAG_DISPLAY,
                                   attrs.toString(), propSB.toString()));
        }



        if (forIsl) {
            isl.append(
                XmlUtil.openTag(
                    ImageGenerator.TAG_DATASOURCE,
                    XmlUtil.attrs(
                        "id", "datasource", "url",
                        request.getAbsoluteUrl(
                            getDataOutputHandler().getOpendapUrl(entry)))));
        } else {
            isl.append(XmlUtil.openTag(ImageGenerator.TAG_DATASOURCE,
                                       XmlUtil.attrs("id", "datasource",
                                           "times", "0")));
        }

        isl.append(dataSourceProps);



        Hashtable props = new Hashtable();
        props.put("datasource", dataSource);
        StringBuffer firstDisplays  = new StringBuffer();
        StringBuffer secondDisplays = new StringBuffer();




        List         params         = request.get(ARG_PARAM, new ArrayList());
        int          displayIdx     = -1;
        for (int i = 0; i < params.size(); i++) {
            String param = (String) params.get(i);
            if (param.length() == 0) {
                continue;
            }
            displayIdx++;

            if ( !request.defined(ARG_IDV_DISPLAY + displayIdx)) {
                continue;
            }
            String display = request.getString(ARG_IDV_DISPLAY + displayIdx,
                                 "");

            StringBuffer propSB = new StringBuffer();
            propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                                      XmlUtil.attrs("name", "id", "value",
                                          "thedisplay" + displayIdx)));



            if (request.get(ARG_SCALE_VISIBLE + displayIdx, false)) {
                /*
                  visible=true|false;
                  color=somecolor;
                  orientation=horizontal|vertical;
                  placement=top|left|bottom|right
                */
                String placement = request.getString(ARG_SCALE_PLACEMENT
                                       + displayIdx, "");
                String orientation;
                if (placement.equals("top") || placement.equals("bottom")) {
                    orientation = "horizontal";
                } else {
                    orientation = "vertical";
                }
                String s = "visible=true;orientation=" + orientation
                           + ";placement=" + placement;
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                                          XmlUtil.attrs("name",
                                              "colorScaleInfo", "value",
                                                  s.toString())));




            }


            if (display.equals(DISPLAY_PLANVIEWCONTOUR)) {
                StringBuffer s = new StringBuffer();
                s.append("width="
                         + request.getString(ARG_CONTOUR_WIDTH + displayIdx,
                                             "1") + ";");
                if (request.defined(ARG_CONTOUR_INTERVAL + displayIdx)) {
                    String intString = request.getString(ARG_CONTOUR_INTERVAL
                                           + displayIdx, "");
                    if ((intString.indexOf(";") >= 0)
                            || (intString.indexOf("/") >= 0)) {
                        intString = intString.replaceAll(";", ",");
                        s.append("levels=");
                    } else {
                        s.append("interval=");
                    }
                    s.append(intString + ";");
                }
                if (request.defined(ARG_CONTOUR_BASE + displayIdx)) {
                    s.append("base="
                             + request.getString(ARG_CONTOUR_BASE
                                 + displayIdx, "") + ";");
                }
                if (request.defined(ARG_CONTOUR_MIN + displayIdx)) {
                    s.append("min="
                             + request.getString(ARG_CONTOUR_MIN
                                 + displayIdx, "") + ";");
                }
                if (request.defined(ARG_CONTOUR_MAX + displayIdx)) {
                    s.append("max="
                             + request.getString(ARG_CONTOUR_MAX
                                 + displayIdx, "") + ";");
                }

                s.append("dashed="
                         + request.get(ARG_CONTOUR_DASHED + displayIdx,
                                       false) + ";");
                s.append("labels="
                         + request.get(ARG_CONTOUR_LABELS + displayIdx,
                                       false) + ";");
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                                          XmlUtil.attrs("name",
                                              "contourInfoParams", "value",
                                                  s.toString())));
            }

            /*
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                                          XmlUtil.attrs("name",
                                              "contourInfoParams", "value",
                                                  s.toString())));
            */


            if (request.defined(ARG_RANGE_MIN + displayIdx)
                    && request.defined(ARG_RANGE_MAX + displayIdx)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "range", "value",
                            request.getString(
                                ARG_RANGE_MIN + displayIdx, "").trim() + ":"
                                    + request.getString(
                                        ARG_RANGE_MAX + displayIdx,
                                        "").trim())));



            }


            if (request.defined(ARG_COLORTABLE + displayIdx)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "colorTableName", "value",
                            request.getString(
                                ARG_COLORTABLE + displayIdx, ""))));;

            }



            StringBuffer attrs = new StringBuffer();

            propSB.append(makeProperty("color",
                                       request.getString(ARG_IDV_DISPLAYCOLOR
                                           + displayIdx, "")));


            if (display.equals(DISPLAY_XS_FILLEDCONTOUR)
                    || display.equals(DISPLAY_XS_CONTOUR)
                    || display.equals(DISPLAY_XS_COLOR)) {
                propSB.append(makeProperty("lineVisible", "false"));
                if (request.defined(ARG_LAT1 + displayIdx)
                        && request.defined(ARG_LON1 + displayIdx)
                        && request.defined(ARG_LAT2 + displayIdx)
                        && request.defined(ARG_LON2 + displayIdx)) {
                    propSB.append(makeProperty("initLat1",
                            request.getString(ARG_LAT1 + displayIdx, "")));

                    propSB.append(makeProperty("initLon1",
                            request.getString(ARG_LON1 + displayIdx, "")));
                    propSB.append(makeProperty("initLat2",
                            request.getString(ARG_LAT2 + displayIdx, "")));

                    propSB.append(makeProperty("initLon2",
                            request.getString(ARG_LON2 + displayIdx, "")));
                }
            }

            if (display.equals(DISPLAY_PLANVIEWFLOW)
                    || display.equals(DISPLAY_STREAMLINES)
                    || display.equals(DISPLAY_WINDBARBPLAN)) {
                propSB.append(makeProperty("flowScale",
                                           request.getString(ARG_FLOW_SCALE
                                               + displayIdx, "")));

                propSB.append(makeProperty("streamlineDensity",
                                           request.getString(ARG_FLOW_DENSITY
                                               + displayIdx, "")));

                propSB.append(makeProperty("skipValue",
                                           request.getString(ARG_FLOW_SKIP
                                               + displayIdx, "")));

            }




            if (display.equals(DISPLAY_ISOSURFACE)) {
                if (request.defined(ARG_ISOSURFACEVALUE + displayIdx)) {
                    propSB.append(
                        XmlUtil.tag(
                            ImageGenerator.TAG_PROPERTY,
                            XmlUtil.attrs(
                                "name", "surfaceValue", "value",
                                request.getString(
                                    ARG_ISOSURFACEVALUE + displayIdx, ""))));
                }
            }
            if ( !vertControls.contains(display)) {
                String level = request.getString(ARG_LEVELS + displayIdx,
                                   "0");
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_LEVEL_FROM,
                                           "#" + level,
                                           ImageGenerator.ATTR_LEVEL_TO,
                                           "#" + level));

            }


            if (request.defined(ARG_IDV_DISPLAYLISTLABEL + displayIdx)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "displayListTemplate", "value",
                            request.getString(
                                ARG_IDV_DISPLAYLISTLABEL + displayIdx, ""))));
            }



            if (request.defined(ARG_IDV_DISPLAYUNIT + displayIdx)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "settingsDisplayUnit", "value",
                            request.getString(
                                ARG_IDV_DISPLAYUNIT + displayIdx, ""))));
            }




            attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TYPE, display,
                                       ImageGenerator.ATTR_PARAM, param));


            if (request.defined(ARG_STRIDE + displayIdx)) {
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_STRIDE,
                                           request.getString(ARG_STRIDE
                                               + displayIdx, "1")));
            }

            List members = request.get(ARG_ENS + displayIdx, new ArrayList());
            if ( !members.isEmpty()) {
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_ENSEMBLES,
                                           StringUtil.join(",", members)));
            }

            List times = request.get(ARG_TIMES + displayIdx, new ArrayList());
            if (times.size() > 0) {
                if (times.size() > 1) {
                    multipleTimes = true;
                }
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TIMES,
                                           StringUtil.join(",", times)));
            }


            StringBuffer which =
                ((display.equals(DISPLAY_PLANVIEWCONTOURFILLED)
                  || display.equals(DISPLAY_PLANVIEWCOLOR))
                 ? firstDisplays
                 : secondDisplays);

            which.append(XmlUtil.tag(ImageGenerator.TAG_DISPLAY,
                                     attrs.toString(), propSB.toString()));
        }

        isl.append(firstDisplays);
        isl.append(secondDisplays);

        isl.append("</datasource>\n");
        isl.append("<pause/>\n");

        if (request.defined(ARG_AZIMUTH) && request.defined(ARG_TILT)) {
            isl.append(XmlUtil.tag(ImageGenerator.TAG_VIEWPOINT,
                                   XmlUtil.attrs(ImageGenerator.ATTR_AZIMUTH,
                                       request.getString(ARG_AZIMUTH, ""),
                                       ImageGenerator.ATTR_TILT,
                                       request.getString(ARG_TILT, ""))));
        }

        if (request.defined(ARG_VIEWDIR)) {
            isl.append(XmlUtil.tag(ImageGenerator.TAG_VIEWPOINT,
                                   XmlUtil.attrs(ImageGenerator.ATTR_VIEWDIR,
                                       request.getString(ARG_VIEWDIR, ""))));
        }


        if ( !forIsl) {
            isl.append(XmlUtil.tag((multipleTimes
                                    ? "movie"
                                    : "image"), XmlUtil.attr(
                                        "file", imageFile.toString()) + " "
                                            + bgSuffix, clip + bgTrans));
        }
        isl.append("</isl>\n");
        //System.out.println(isl);



        if (forJnlp) {
            String jnlp =
                IdvWebstartOutputHandler.getJnlpTemplate(getRepository());
            StringBuffer args = new StringBuffer();
            args.append("<argument>-b64isl</argument>");
            args.append("<argument>" + Utils.encodeBase64(isl.toString())
                        + "</argument>");
            jnlp = jnlp.replace("${args}", args.toString());

            return new Result("data.jnlp", new StringBuffer(jnlp),
                              "application/x-java-jnlp-file");
        }

        if (forIsl) {
            return new Result("data.isl", new StringBuffer(isl), "text/xml");
        }



        long t1 = System.currentTimeMillis();
        idvServer.evaluateIsl(isl, props);
        long t2 = System.currentTimeMillis();
        System.err.println("isl time:" + (t2 - t1));


        synchronized (imageCache) {
            if (imageCache.size() > 1000) {
                imageCache = new Hashtable<String, File>();
            }
            imageCache.put(imageKey, imageFile);
        }

        return imageFile;
    }


    /**
     * Output the group
     *
     * @param request The request
     * @param outputType  the output type
     * @param group  the group
     *
     * @return  the Result
     *
     * @throws Exception On badness
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {

        if (group.getType().equals(
                GridAggregationTypeHandler.TYPE_GRIDAGGREGATION)) {
            return outputEntry(request, outputType, group);
        }

        return super.outputGroup(request, outputType, group, children);
    }





    /**
     * Output a point data page
     *
     * @param request The request
     * @param entry The entry
     *
     * @return the Result
     *
     * @throws Exception On badness
     */
    public Result outputPointPage(final Request request, Entry entry)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        getPageHandler().entrySectionOpen(request, entry, sb, "Make Image");
        String       formUrl = getEntryManager().getFullEntryShowUrl(request);
        StringBuffer formSB  = new StringBuffer();

        formSB.append(HtmlUtils.form(formUrl, ""));
        formSB.append(HtmlUtils.submit("Make Image", ARG_SUBMIT));
        formSB.append(HtmlUtils.p());
        formSB.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        formSB.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_IDV_POINT));
        formSB.append(HtmlUtils.hidden(ARG_IDV_ACTION,
                                       ACTION_POINT_MAKEPAGE));
        formSB.append(HtmlUtils.formTable());
        StationModelManager smm = getIdv().getStationModelManager();
        List                layoutModels     = smm.getStationModels();
        List                layoutModelNames = new ArrayList();
        for (StationModel sm : (List<StationModel>) layoutModels) {
            layoutModelNames.add(sm.getName());
        }


        formSB.append(HtmlUtils.formEntry(msgLabel("Layout Model"),
                                          htmlSelect(request,
                                              ARG_POINT_LAYOUTMODEL,
                                                  layoutModelNames)));

        formSB.append(HtmlUtils.formEntry(msgLabel("Animate"),
                                          htmlCheckbox(request,
                                              ARG_POINT_DOANIMATION, false)));


        formSB.append(HtmlUtils.formEntry(msgLabel("Image Size"),
                                          htmlInput(request, ARG_IMAGE_WIDTH,
                                              "600") + HtmlUtils.space(1)
                                                  + "X" + HtmlUtils.space(1)
                                                      + htmlInput(request,
                                                          ARG_IMAGE_HEIGHT,
                                                              "400")));


        formSB.append(HtmlUtils.formTableClose());
        formSB.append(HtmlUtils.formClose());


        String url = getEntryManager().getFullEntryShowUrl(request);
        String islUrl = url + "/" + IOUtil.stripExtension(entry.getName())
                        + ".isl";
        String jnlpUrl = url + "/" + IOUtil.stripExtension(entry.getName())
                         + ".jnlp";

        HashSet<String> exceptArgs = new HashSet<String>();
        exceptArgs.add(ARG_IDV_ACTION);
        String args = request.getUrlArgs(exceptArgs, null, ".*_gvdflt");


        url = url + "?" + ARG_IDV_ACTION + "=" + ACTION_POINT_MAKEIMAGE + "&"
              + args;
        islUrl = islUrl + "?" + ARG_IDV_ACTION + "=" + ACTION_POINT_MAKEIMAGE
                 + "&" + args + "&" + ARG_IDV_TARGET + "=" + TARGET_ISL;
        jnlpUrl = jnlpUrl + "?" + ARG_IDV_ACTION + "="
                  + ACTION_POINT_MAKEIMAGE + "&" + args + "&"
                  + ARG_IDV_TARGET + "=" + TARGET_JNLP;

        StringBuffer imageSB = new StringBuffer();

        imageSB.append(HtmlUtils.img(url, "",
                                     HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                         request.getString(ARG_IMAGE_WIDTH,
                                             "400"))));

        if ( !request.exists(ARG_SUBMIT)) {
            sb.append(formSB);
        } else {
            sb.append(HtmlUtils.table(new Object[] { imageSB, formSB }, 10));
        }



        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("Point Display", sb);

    }



    /**
     * Output a point image
     *
     * @param request The request
     * @param entry The entry
     *
     * @return  the Result
     *
     * @throws Exception On badness
     */
    public Result outputPointImage(final Request request, Entry entry)
            throws Exception {

        Trace.addNot(".*ShadowFunction.*");
        //      Trace.addNot(".*GeoGrid.*");
        //      Trace.addOnly(".*MapProjection.*");
        //      Trace.addOnly(".*ProjectionCoordinateSystem.*");
        Trace.startTrace();




        String action = request.getString(ARG_IDV_ACTION,
                                          ACTION_POINT_MAKEPAGE);
        String path = getCdmManager().getPath(entry);
        if (path == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Could not load point data");

            return new Result("", sb);
        }


        FeatureDatasetPoint dataset = getCdmManager().getPointDataset(entry,
                                          path);
        DataSourceDescriptor descriptor =
            getIdv().getDataManager().getDescriptor("NetCDF.POINT");
        NetcdfPointDataSource dataSource = new NetcdfPointDataSource(dataset,
                                               descriptor, new Hashtable());
        try {


            String id = entry.getId();
            File image = getStorageManager().getThumbFile("preview_"
                             + id.replace("/", "_") + ".gif");

            boolean forIsl = request.getString(ARG_IDV_TARGET,
                                 "").equals(TARGET_ISL);
            boolean forJnlp = request.getString(ARG_IDV_TARGET,
                                  "").equals(TARGET_JNLP);
            if (forJnlp) {
                forIsl = true;
            }

            StringBuffer isl = new StringBuffer();
            isl.append(
                "<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");

            StringBuffer viewProps = new StringBuffer();
            viewProps.append(XmlUtil.tag("property",
                                         XmlUtil.attrs("name", "wireframe",
                                             "value", "true")));


            int width  = request.get(ARG_IMAGE_WIDTH, 400);
            int height = request.get(ARG_IMAGE_HEIGHT, 400);
            if (request.defined(ARG_VIEW_BOUNDS + "_south")
                    && request.defined(ARG_VIEW_BOUNDS + "_north")
                    && request.defined(ARG_VIEW_BOUNDS + "_east")
                    && request.defined(ARG_VIEW_BOUNDS + "_west")) {
                double south   = request.get(ARG_VIEW_BOUNDS + "_south", 0.0);
                double north   = request.get(ARG_VIEW_BOUNDS + "_north", 0.0);
                double east    = request.get(ARG_VIEW_BOUNDS + "_east", 0.0);
                double west    = request.get(ARG_VIEW_BOUNDS + "_west", 0.0);
                double bwidth  = Math.abs(east - west);
                double bheight = Math.abs(north - south);
                viewProps.append(makeProperty("initLatLonBounds",
                        west + ";" + north + ";" + bwidth + ";" + bheight));
                height = (int) (width * bheight / bwidth);
            }




            viewProps.append(
                makeProperty(
                    "background",
                    request.getString(ARG_VIEW_BACKGROUND, "black")));


            //Create a new viewmanager
            //For now don't do this if we are doing jnlp
            if ( !forJnlp) {
                isl.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_VIEW,
                        XmlUtil.attrs(
                            ImageGenerator.ATTR_WIDTH, "" + width,
                            ImageGenerator.ATTR_HEIGHT,
                            "" + height), viewProps.toString()));
            }


            if (forIsl) {
                isl.append(
                    XmlUtil.openTag(
                        ImageGenerator.TAG_DATASOURCE,
                        XmlUtil.attrs(
                            "id", "datasource", "url",
                            request.getAbsoluteUrl(
                                getDataOutputHandler().getOpendapUrl(
                                    entry)))));
            } else {
                isl.append(XmlUtil.openTag(ImageGenerator.TAG_DATASOURCE,
                                           XmlUtil.attrs("id",
                                               "datasource")));
            }

            Hashtable props = new Hashtable();
            props.put("datasource", dataSource);
            StringBuffer propSB = new StringBuffer();
            StringBuffer attrs  = new StringBuffer();
            propSB.append(makeProperty("id", "thedisplay"));
            propSB.append(
                makeProperty(
                    "stationModelName",
                    request.getString(ARG_POINT_LAYOUTMODEL, "Location")));


            attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TYPE,
                                       "stationmodelcontrol",
                                       ImageGenerator.ATTR_PARAM, "*"));

            isl.append(XmlUtil.tag(ImageGenerator.TAG_DISPLAY,
                                   attrs.toString(), propSB.toString()));
            isl.append("</datasource>\n");
            isl.append("<pause/>\n");



            String clip = "";
            if (request.get(ARG_CLIP, false)) {
                clip = XmlUtil.tag("clip", "");
            }

            boolean multipleTimes = request.get(ARG_POINT_DOANIMATION, false);
            isl.append(XmlUtil.tag((multipleTimes
                                    ? "movie"
                                    : "image"), XmlUtil.attr("file",
                                    image.toString()), clip));
            isl.append("</isl>\n");

            if (forJnlp) {
                String jnlp =
                    getRepository().getResource(
                        "/org/ramadda/repository/idv/template.jnlp");
                StringBuffer args = new StringBuffer();
                args.append("<argument>-b64isl</argument>");
                args.append("<argument>" + Utils.encodeBase64(isl.toString())
                            + "</argument>");
                jnlp = jnlp.replace("${args}", args.toString());

                return new Result("data.jnlp", new StringBuffer(jnlp),
                                  "application/x-java-jnlp-file");
            }
            if (forIsl) {
                return new Result("data.isl", new StringBuffer(isl),
                                  "text/xml");
            }

            long t1 = System.currentTimeMillis();
            idvServer.evaluateIsl(isl, props);
            long t2 = System.currentTimeMillis();
            System.err.println("isl time:" + (t2 - t1));

            Trace.stopTrace();

            return new Result("preview.gif",
                              getStorageManager().getFileInputStream(image),
                              "image/gif");
        } finally {
            getCdmManager().returnPointDataset(path, dataset);
        }
    }


    /**
     * Output the point
     *
     * @param request The request
     * @param entry The entry
     *
     * @return the Result
     *
     * @throws Exception On badness
     */
    public Result outputPoint(final Request request, Entry entry)
            throws Exception {
        String action = request.getString(ARG_IDV_ACTION,
                                          ACTION_POINT_MAKEPAGE);
        if (action.equals(ACTION_POINT_MAKEPAGE)) {
            return outputPointPage(request, entry);
        } else {
            return outputPointImage(request, entry);
        }
    }




    /**
     * Make a boolean property string
     *
     * @param name the property name
     * @param value  the property value
     *
     * @return  the property as a String
     */
    private String makeProperty(String name, boolean value) {
        return makeProperty(name, "" + value);
    }




    /**
     * Make a String property
     *
     * @param name the property name
     * @param value  the property value
     *
     * @return  the property as a String
     */
    private String makeProperty(String name, String value) {
        return XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                           XmlUtil.attrs("name", name, "value", value));
    }


    /**
     * utility method
     *
     * @param request The request
     * @param arg     the argument identifier
     * @param dflt    the default value
     *
     * @return  the checkbox HTML
     */
    private String htmlCheckbox(Request request, String arg, boolean dflt) {
        boolean value = dflt;
        if (request.exists(arg)) {
            value = request.get(arg, dflt);
        } else if (request.exists(arg + "_gvdflt")) {
            value = false;
        }

        return HtmlUtils.checkbox(arg, "true", value)
               + HtmlUtils.hidden(arg + "_gvdflt", "" + value);
    }


    /**
     * utility method
     *
     * @param request The request
     * @param arg     the argument identifier
     * @param items   the list of items
     * @param selectFirstOne  true to select the first one
     * @param extra   extra properties
     *
     * @return  the select box HTML
     */
    private String htmlSelect(Request request, String arg, List items,
                              boolean selectFirstOne, String extra) {
        List selected = request.get(arg, new ArrayList());
        if ((selected.size() == 0) && selectFirstOne && (items.size() > 0)) {
            selected.add(items.get(0));
        }

        return HtmlUtils.select(arg, items, selected, extra);
    }


    /**
     * utility method
     *
     * @param request The request
     * @param arg     the argument identifier
     * @param items   the list of items
     * @param extra   extra special sauce
     *
     * @return  the select box HTML
     */
    private String htmlSelect(Request request, String arg, List items,
                              String extra) {
        return htmlSelect(request, arg, items, false, extra);
    }

    /**
     * utility method
     *
     * @param request The request
     * @param arg     the argument identifier
     * @param items   the list of items
     *
     * @return  the select box HTML
     */
    private String htmlSelect(Request request, String arg, List items) {
        return htmlSelect(request, arg, items, "");
    }






}
