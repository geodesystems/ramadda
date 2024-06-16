package org.ramadda.geodata.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ramadda.geodata.cdmdata.CdmDataOutputHandler;
import org.ramadda.repository.ApiMethod;
import org.ramadda.repository.Association;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.Request;
import org.ramadda.repository.RequestHandler;
import org.ramadda.repository.Resource;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.ImageOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.CollectionTypeHandler;
import org.ramadda.repository.type.Column;
import org.ramadda.repository.type.GranuleTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.service.Service;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOperand;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.CFGridWriter2;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.visad.data.CalendarDateTime;

public class TestDataService extends Service {
	
	OutputHandler outputHandler = null;
	
    /**
     * Ordinal names for years
     */
    public static final String[] ordinalYears = {
        "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh",
        "Eighth", "Ninth", "Tenth"
    };

    /** area argument - north */
    private static final String ARG_AREA_NORTH = "area_north";

    /** area argument - south */
    private static final String ARG_AREA_SOUTH = "area_south";

    /** area argument - east */
    private static final String ARG_AREA_EAST = "area_east";

    /** area argument - west */
    private static final String ARG_AREA_WEST = "area_west";
    
    /** spatial arguments */
    private static final String[] SPATIALARGS = new String[] {
                                                    ARG_AREA_NORTH,
            ARG_AREA_WEST, ARG_AREA_SOUTH, ARG_AREA_EAST, };





	
    public TestDataService(Repository repository)
            throws Exception {
        super(repository, "TEST_DATA_SERVICE", "Test Me!");
        outputHandler = new OutputHandler(repository, "Test");
    }
    
    /**
     * Is this enabled?
     *
     * @return true if it is
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Add to form
     *
     * @param request  the Request
     * @param input    the ServiceInput
     * @param sb       the form
     * @param argPrefix  the argument prefix
     * @param label      the label
     *
     * @throws Exception  problem adding to the form
     */
    @Override
    public void addToForm(Request request, ServiceInput input, Appendable sb,
                          String argPrefix, String label)
            throws Exception {
        sb.append(HtmlUtils.formTable());
        makeInputForm(request, input, sb, argPrefix);
        sb.append(HtmlUtils.formTableClose());
    }

    /**
     * Make the input form
     *
     * @param request  the Request
     * @param input    the ServiceInput
     * @param sb       the StringBuilder
     * @param argPrefix  the argument prefix
     *
     * @throws Exception  problem making stuff
     */
    private void makeInputForm(Request request, ServiceInput input,
                               Appendable sb, String argPrefix)
            throws Exception {

        long millis = System.currentTimeMillis();
        String type =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();

        input = adjustInput(request, input, false);
        //System.err.println("Time to adjust input: "+(System.currentTimeMillis()-millis));
        List<NamedTimePeriod> periods = null;
        ApiMethod             api     = request.getApiMethod();
        if (api != null) {
            RequestHandler handler = api.getRequestHandler();
            if ((handler != null)
                    && (handler instanceof ClimateModelApiHandler)) {
                String group = null;
                if (request.defined(ClimateModelApiHandler.ARG_EVENT_GROUP)) {
                    group = request.getString(
                        ClimateModelApiHandler.ARG_EVENT_GROUP, null);
                    periods =
                        ((ClimateModelApiHandler) handler).getNamedTimePeriods(
                            group);
                }
            }
        }

        Entry first = input.getEntries().get(0);

        millis = System.currentTimeMillis();
        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        GridDataset dataset =
            dataOutputHandler.getCdmManager().getGridDataset(first,
                first.getResource().getPath());
        //System.out.println("Time to get dataset in makeInputForm: "+(System.currentTimeMillis()-millis) + " " + dataset.toString());
        dataOutputHandler.getCdmManager().returnGridDataset(
            first.getResource().getPath(), dataset);

        // if dataset is null, it will throw a NullPointerException on the next line.
        //if (dataset != null) {
        addVarLevelWidget(request, sb, dataset, CdmDataOutputHandler.ARG_LEVEL);
        //}
        GridDatatype grid  = dataset.getGrids().get(0);
        String       units = grid.getUnitsString();
        boolean hasPrecipUnits = (SimpleUnit.isCompatible(units, "kg m-2 s-1")
        //|| SimpleUnit.isCompatible(units, "mm/day")
        || units.equals("mm/day")
                                  || units.equals("mm"));  // for cpc global precip

        boolean     isAnom = first.getValue(request,3).toString().equals("anom");
        List<Entry> climos = findClimatology(request, first);
        boolean haveClimo = true;  // we make this true since we can create one on the fly
        if ((climos == null) || climos.isEmpty()) {
            haveClimo = false;
        }
        millis = System.currentTimeMillis();
        addMyStatsWidget(request, sb, input, hasPrecipUnits, isAnom, haveClimo,
                       type);
        //System.out.println("Time to add stats widget: "+(System.currentTimeMillis()-millis));

        millis = System.currentTimeMillis();
        addTimeWidget(request, sb, input, periods);
        //System.out.println("Time to add time widget: "+(System.currentTimeMillis()-millis));

        addMyMapWidget(request, sb, dataset);

        /*
        if (dataset != null) {
            dataset.close();
        }
        */

    }
    
    /**
     * Get the data output handler
     *
     * @return the handler
     *
     * @throws Exception Problem getting that
     */
    public CdmDataOutputHandler getDataOutputHandler() throws Exception {
        return (CdmDataOutputHandler) getRepository().getOutputHandler(
            CdmDataOutputHandler.OUTPUT_OPENDAP.toString());
    }


    /**
     * Add the variable/level selector widget
     *
     * @param request  the Request
     * @param sb       the HTML
     * @param dataset  the dataset
     * @param levelArg the level argument
     *
     * @throws Exception  problems appending
     */
    public void addVarLevelWidget(Request request, Appendable sb,
                                  GridDataset dataset, String levelArg)
            throws Exception {
        List<GridDatatype> grids = dataset.getGrids();
        StringBuilder      varsb = new StringBuilder();
        //TODO: handle multiple variables
        //List<TwoFacedObject> varList = new ArrayList<TwoFacedObject>(grids.size());
        //for (GridDatatype grid : dataset.getGrids()) {
        //    varList.add(new TwoFacedObject(grid.getDescription(), grid.getName()));
        //}

        //varsb.append(HtmlUtils.select(ARG_CDO_PARAM, varList, request.getString(ARG_CDO_PARAM, null)));
        GridDatatype grid     = grids.get(0);
        String       longname = grid.getDescription();
        if ((longname == null) || longname.isEmpty()) {
            longname = grid.getName();
        }
        String formula =
            request.getString(ClimateModelApiHandler.ARG_FORMULA, "");
        if ( !formula.isEmpty()) {
            ApiMethod api = request.getApiMethod();
            if (api != null) {
                RequestHandler handler = api.getRequestHandler();
                if ((handler != null)
                        && (handler instanceof ClimateModelApiHandler)) {
                    String formulaName =
                        ((ClimateModelApiHandler) handler).getFormulaName(
                            formula);
                    if (formulaName != null) {
                        longname = formulaName;
                    }
                }
            }
        }
        varsb.append(longname);
        if (grid.getZDimension() != null) {
            varsb.append(HtmlUtils.space(5));
            varsb.append(msgLabel("Level"));
            GridCoordSystem      gcs    = grid.getCoordinateSystem();
            CoordinateAxis1D     zAxis  = gcs.getVerticalAxis();
            int                  sizeZ  = (int) zAxis.getSize();
            String               unit   = zAxis.getUnitsString();
            List<TwoFacedObject> levels =
                new ArrayList<TwoFacedObject>(sizeZ);
            boolean havePascals = false;
            String  unitLabel   = unit;
            // we'll convert pascals to hectopascals
            if (unit.toLowerCase().startsWith("pa")) {
                havePascals = true;
                unitLabel   = "hPa";
            }
            if (sizeZ > 1) {
                for (int i = 0; i < sizeZ; i++) {
                    int    lev   = (int) zAxis.getCoordValue(i);
                    String label = String.valueOf(havePascals
                            ? lev / 100
                            : lev);
                    levels.add(new TwoFacedObject(label,
                            String.valueOf(lev)));
                }
                varsb.append(HtmlUtils.select(levelArg, levels,
                        request.getSanitizedString(levelArg, null)));
                varsb.append(HtmlUtils.space(2));
            } else {
                int lev = (int) zAxis.getCoordValue(0);
                varsb.append(HtmlUtils.space(1));
                varsb.append(lev);
                varsb.append(HtmlUtils.space(1));
                varsb.append(HtmlUtils.hidden(levelArg, lev));
            }
            varsb.append(unitLabel);
            varsb.append(HtmlUtils.hidden(levelArg + "_unit", unit));
        }
        sb.append(HtmlUtils.formEntry(msgLabel("Variable"),
                                      varsb.toString()));
    }

    /**
     * Add the statistics widget
     *
     * @param request  the Request
     * @param sb       the HTML
     * @param si       input parameters
     * @param usePct  use percentage
     * @param isAnom  is this an anomaly file
     * @param haveClimo  do we have a climo
     * @param type     the type of request
     * @throws Exception problems
     */
    public void addMyStatsWidget(Request request, Appendable sb,
                               ServiceInput si, boolean usePct,
                               boolean isAnom, boolean haveClimo, String type)
            throws Exception {

        if (ModelUtil.getFrequency(request,
                                   si.getEntries().get(0)).equals(
                                       CDOOutputHandler.FREQUENCY_DAILY)) {
            sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_PERIOD,
                                       request.getSanitizedString(
                                           CDOOutputHandler.ARG_CDO_PERIOD,
                                           CDOOutputHandler.PERIOD_YDAY)));
        } else if ( !(type.equals(
                ClimateModelApiHandler.ARG_ACTION_MULTI_TIMESERIES)
                      || type.equals(
                          ClimateModelApiHandler.ARG_ACTION_TIMESERIES))) {
            sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_PERIOD,
                                       request.getSanitizedString(
                                           CDOOutputHandler.ARG_CDO_PERIOD,
                                           CDOOutputHandler.PERIOD_TIM)));
        }
        addStatsWidget(request, sb, usePct, isAnom, haveClimo, si,
                             type);
    }



    /**
     * Find the associated climatology for the input
     *
     * @param request  the Request
     * @param granule  the entry
     *
     * @return the climatology entry or null
     *
     * @throws Exception  problems
     */
    protected List<Entry> findClimatology(Request request, Entry granule)
            throws Exception {
        return findStatisticEntry(request, granule, "clim");
    }

    /**
     * Find the associated stat (e.g., clim or sprd) for the input
     *
     * @param request  the Request
     * @param granule  the entry
     * @param stat     the stat type (sprd, clim, etc)
     *
     * @return the stat entry or null
     *
     * @throws Exception  problems
     */
    protected List<Entry> findStatisticEntry(Request request, Entry granule,
                                             String stat)
            throws Exception {
        TypeHandler gh = granule.getTypeHandler();
        if ( !(gh instanceof ClimateModelFileTypeHandler)) {
            return null;
        }
        Entry collection = GranuleTypeHandler.getCollectionEntry(request,
                               granule);
        CollectionTypeHandler ctypeHandler =
            (CollectionTypeHandler) collection.getTypeHandler();
        List<Clause>    clauses   = new ArrayList<Clause>();
        List<Column>    columns   = ctypeHandler.getGranuleColumns();
        HashSet<String> seenTable = new HashSet<String>();
        Object[]        values    = granule.getValues();
        for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
            Column column = columns.get(colIdx);
            // first column is the collection ID
            int    valIdx      = colIdx + 1;
            String dbTableName = column.getTableName();
            if ( !seenTable.contains(dbTableName)) {
                clauses.add(
                    Clause.eq(ctypeHandler.getCollectionIdColumn(column),
                              collection.getId()));
                clauses.add(Clause.join(Tables.ENTRIES.COL_ID,
                                        dbTableName + ".id"));
                seenTable.add(dbTableName);
            }
            String v = values[valIdx].toString();
            if (column.getName().equals("ensemble")) {
                clauses.add(Clause.eq(column.getName(), stat));
            } else {
                if (v.length() > 0) {
                    clauses.add(Clause.eq(column.getName(), v));
                }
            }

        }

        return outputHandler.getEntryManager().getEntriesFromDb(request, clauses,
                ctypeHandler.getGranuleTypeHandler());

    }


    /**
     * Add the statitics widget  - use instead of CDOOutputHandler
     *
     * @param request  the Request
     * @param sb       the HTML
     * @param addPct   true add a percent normal option
     * @param isAnom   is this an anomaly file
     * @param haveClimo true if we have a climatology
     * @param si      the input
     * @param type    the request type
     * @throws Exception problem creating widget
     */
    public void addStatsWidget(Request request, Appendable sb,
                               boolean addPct, boolean isAnom,
                               boolean haveClimo, ServiceInput si,
                               String type)
            throws Exception {

        boolean isMonthly = ModelUtil.getFrequency(
                                request, si.getEntries().get(0)).equals(
                                CDOOutputHandler.FREQUENCY_MONTHLY);
        if ( !isAnom) {
            List<TwoFacedObject> stats = new ArrayList<TwoFacedObject>();
            if ( !isMonthly
                    && type.equals(
                        ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)) {
                // don't show widget if doing a pdf
                return;
                /*
                stats.add(new TwoFacedObject("None",
                                         CDOOutputHandler.STAT_NONE));
                                         */
            } else {
                stats.add(new TwoFacedObject("Average",
                                             CDOOutputHandler.STAT_MEAN));
            }
            if ( !isMonthly && addPct) {  // for now, don't allow this
                stats.add(new TwoFacedObject("Accumulation",
                                             CDOOutputHandler.STAT_SUM));
            }
            if (haveClimo) {
                stats.add(new TwoFacedObject("Anomaly",
                                             CDOOutputHandler.STAT_ANOM));
                if (isMonthly) {  // for now, don't allow this
                    stats.add(new TwoFacedObject("Standardized Anomaly",
                            CDOOutputHandler.STAT_STDANOM));
                }
                if (addPct) {
                    stats.add(new TwoFacedObject("Percent of Normal",
                            CDOOutputHandler.STAT_PCTANOM));
                }
                if ( !type.equals(
                        ClimateModelApiHandler.ARG_ACTION_MULTI_TIMESERIES)
                        && isMonthly) {
                    stats.add(new TwoFacedObject("Standard Deviation",
                            CDOOutputHandler.STAT_STD));
                }
            }

            StringBuilder statForm = new StringBuilder();
            statForm.append(HtmlUtils.select(CDOOutputHandler.ARG_CDO_STAT,
                                             stats,
                                             request.getString(
                                             CDOOutputHandler.ARG_CDO_STAT,
                                             CDOOutputHandler.STAT_MEAN)));

            if ((si != null)
                    && (type.equals(
                    ClimateModelApiHandler
                        .ARG_ACTION_ENS_COMPARE) || type.equals(
                            ClimateModelApiHandler
                                .ARG_ACTION_MULTI_TIMESERIES) || (type.equals(
                                    ClimateModelApiHandler
                                        .ARG_ACTION_COMPARE) && (si.getOperands()
                                            .size() <= 2) && isMonthly))) {
                StringBuilder climyearsSB = new StringBuilder();
                statForm.append(HtmlUtils.br());
                if (type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
                        || (type.equals(ClimateModelApiHandler
                            .ARG_ACTION_COMPARE) && (si.getOperands().size()
                                == 2))) {
                    climyearsSB.append(Repository.msgLabel("Relative to"));
                    climyearsSB.append(
                        HtmlUtils.radio(
                            CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                            "1",
                            RepositoryManager.getShouldButtonBeSelected(
                                request,
                                CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "1",
                                false)));
                    climyearsSB.append(HtmlUtils.space(1));
                    climyearsSB.append(Repository.msg("Dataset 1"));
                    climyearsSB.append(HtmlUtils.space(2));
                    climyearsSB.append(
                        HtmlUtils.radio(
                            CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                            "2",
                            RepositoryManager.getShouldButtonBeSelected(
                                request,
                                CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "2",
                                false)));
                    climyearsSB.append(HtmlUtils.space(1));
                    climyearsSB.append(Repository.msg("Dataset 2"));
                    climyearsSB.append(HtmlUtils.space(2));
                    climyearsSB.append(
                        HtmlUtils.radio(
                            CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                            "0",
                            RepositoryManager.getShouldButtonBeSelected(
                                request,
                                CDOOutputHandler.ARG_CLIMATE_DATASET_NUMBER,
                                "0",
                                true)));
                    climyearsSB.append(HtmlUtils.space(1));
                    climyearsSB.append(Repository.msg("Own Dataset"));
                    climyearsSB.append(HtmlUtils.br());
                }
                climyearsSB.append(Repository.msgLabel("Reference Period"));
                climyearsSB.append(HtmlUtils.space(1));
                addClimYearsWidget(request, climyearsSB, si);
                statForm.append(HtmlUtils.div(climyearsSB.toString(),
                        HtmlUtils.cssClass("ref-years")));
                statForm.append(HtmlUtils.script("$('select[name=\""
                        + CDOOutputHandler.ARG_CDO_STAT
                        + "\"]').on('change', function() {\n"
                        + "$('.ref-years').toggle(!($(this).val() == \""
                        + CDOOutputHandler.STAT_MEAN
                        + "\" || $(this).val() == \""
                        + CDOOutputHandler.STAT_STD
                        + "\" || $(this).val() == \""
                        + CDOOutputHandler.STAT_NONE + "\" ));\n"
                        + "}).change();\n"));
            }
            sb.append(HtmlUtils.formEntry(Repository.msgLabel("Statistic"),
                                          statForm.toString()));
        } else {
            sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_STAT,
                                       CDOOutputHandler.STAT_ANOM));
        }

    }

    /**
     * Add a widget for selecting the climatology years
     * @param request   the request
     * @param sb        the form to add to
     * @param si        the ServiceInput with all the grids
     * @throws Exception  problems
     */
    protected void addClimYearsWidget(Request request, Appendable sb,
                                      ServiceInput si)
            throws Exception {

        List<List<ServiceOperand>> sortedOps =
            ModelUtil.sortOperandsByCollection(request, si.getOperands());
        int                numOps    = sortedOps.size();
        List<List<String>> dataYears = new ArrayList<List<String>>(numOps);
        for (List<ServiceOperand> ops : sortedOps) {
            List<String> years = new ArrayList<String>();
            Entry        first = ops.get(0).getEntries().get(0);

            //TODO: Get the intersection of all the grid times
            CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
            GridDataset dataset =
                dataOutputHandler.getCdmManager().getGridDataset(first,
                    first.getResource().getPath());

            if (dataset != null) {
                List<CalendarDate> dates =
                    CdmDataOutputHandler.getGridDates(dataset);
                SortedSet<String> uniqueYears =
                    Collections.synchronizedSortedSet(new TreeSet<String>());
                if ((dates != null) && !dates.isEmpty()) {
                    for (CalendarDate d : dates) {
                        try {  // shouldn't get an exception
                            String year =
                                new CalendarDateTime(d).formattedString(
                                    "yyyy",
                                    CalendarDateTime.DEFAULT_TIMEZONE);
                            uniqueYears.add(year);
                        } catch (Exception e) {}
                    }
                }
                if ( !uniqueYears.isEmpty()) {
                    years.addAll(uniqueYears);
                }
                dataOutputHandler.getCdmManager().returnGridDataset(
                    first.getResource().getPath(), dataset);
                //dataset.close();
            }
            // TODO:  make a better list of years
            if (years.isEmpty()) {
                for (int i =
                        Integer.parseInt(ClimateModelApiHandler
                            .DEFAULT_CLIMATE_START_YEAR);
                        i <= Integer.parseInt(
                            ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR);
                        i++) {
                    years.add(String.valueOf(i));
                }
            }
            dataYears.add(years);
        }
        List<String> commonYears = new ArrayList<String>();
        for (int opNum = 0; opNum < sortedOps.size(); opNum++) {
            List<String> years = dataYears.get(opNum);
            if (opNum == 0) {
                commonYears.addAll(years);
            } else {
                commonYears.retainAll(years);
            }
        }

        StringBuilder yearsWidget = new StringBuilder();
        yearsWidget.append(
            HtmlUtils.select(CDOOutputHandler.ARG_CDO_CLIM_STARTYEAR,
                             commonYears,
                             ClimateModelApiHandler
                                 .DEFAULT_CLIMATE_START_YEAR,
                             HtmlUtils.title(
                                 "Select the starting reference year")));
        yearsWidget.append(Repository.msg(" to "));
        yearsWidget.append(
            HtmlUtils.select(CDOOutputHandler.ARG_CDO_CLIM_ENDYEAR,
                             commonYears,
                             ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR,
                             HtmlUtils.title(
                                 "Select the ending reference year")));
        sb.append(yearsWidget.toString());

    }

    /**
     * Add a time widget
     *
     * @param request  the Request
     * @param sb       the HTML page
     * @param input    the input
     * @param periods  the time periods
     *
     * @throws Exception  problem making datasets
     */
    public void addTimeWidget(Request request, Appendable sb,
                              ServiceInput input,
                              List<NamedTimePeriod> periods)
            throws Exception {

        String type =
            input.getProperty(
                "type", ClimateModelApiHandler.ARG_ACTION_COMPARE).toString();
        String frequency = ModelUtil.getFrequency(
                               request,
                               input.getOperands().get(0).getEntries().get(
                                   0));
        if ((periods == null) || (periods.isEmpty())) {
            if (frequency.equals(CDOOutputHandler.FREQUENCY_DAILY)) {
                CDOOutputHandler.makeMonthDaysWidget(request, sb, null);
            } else {
                CDOOutputHandler.makeMonthsWidget(request, sb, null);
            }
            makeYearsWidget(request, sb, input, type);
        } else {
            makeEventsWidget(request, sb, periods, type);
        }
    }

    /**
     * Make a widget for named time periods
     *
     * @param request  the request
     * @param sb       the form
     * @param periods  the periods
     * @param type     the type of request
     *
     * @throws Exception problems
     */
    private void makeEventsWidget(Request request, Appendable sb,
                                  List<NamedTimePeriod> periods, String type)
            throws Exception {
        String group = request.getSanitizedString(
                           ClimateModelApiHandler.ARG_EVENT_GROUP, null);
        List<TwoFacedObject> values        = new ArrayList<TwoFacedObject>();
        NamedTimePeriod      selectedEvent = periods.get(0);
        String               event         = null;
        if (request.defined(ClimateModelApiHandler.ARG_EVENT)) {
            event =
                request.getSanitizedString(ClimateModelApiHandler.ARG_EVENT,
                                           "");
        }
        for (NamedTimePeriod period : periods) {
            String value = period.getId() + ";" + period.getStartMonth()
                           + ";" + period.getEndMonth() + ";"
                           + period.getYears();
            TwoFacedObject item = new TwoFacedObject(period.getName(), value);
            values.add(item);
        }
        /*
        sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_STARTMONTH,
                                   selectedEvent.getStartMonth()));
        sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                   selectedEvent.getEndMonth()));
        sb.append(HtmlUtils.hidden(CDOOutputHandler.ARG_CDO_YEARS,
                                   selectedEvent.getYears()));
        */
        if ((group == null) || group.equals("all")) {
            group = "Events";
        }
        sb.append(HtmlUtils.formEntry(Repository.msgLabel(group),
                                      HtmlUtils.select(
                                          ClimateModelApiHandler.ARG_EVENT,
                                          values,
                                          event)));

    }

    /**
     * Add the year selection widget
     *
     * @param request  the Request
     * @param sb       the StringBuilder to add to
     * @param input    the service input
     * @param type     the type of request
     *
     * @throws Exception problems
     */
    private void makeYearsWidget(Request request, Appendable sb,
                                 ServiceInput input, String type)
            throws Exception {

        /* If we are doing a compare, we make widgets for each operand.  If we are doing
         * a multi compare, we make one widget from the intersection of all grids
         */
        //int numOps = input.getOperands().size();
        long millis = System.currentTimeMillis();
        //System.out.println("Ops size before sorting = "+ input.getOperands().size());
        List<List<ServiceOperand>> sortedOps =
            ModelUtil.sortOperandsByCollection(request, input.getOperands());
        //System.out.println("Time to sort ops: "+(System.currentTimeMillis()-millis));
        int numOps = sortedOps.size();
        //System.out.println("New ops size after sorting = "+numOps);
        boolean isCompare =
            type.equals(ClimateModelApiHandler.ARG_ACTION_COMPARE)
            || type.equals(ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE);
        int numWidgets = !isCompare
                         ? 1
                         : numOps;
        int grid       = 0;
        CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
        List<List<String>> dataYears  = new ArrayList<List<String>>(numOps);
        int[]              datesIndex = new int[numWidgets];
        int                dateIdx    = 0;
        int                idx        = 0;
        for (List<ServiceOperand> ops : sortedOps) {
            datesIndex[idx] = dateIdx;
            //System.out.println("sortedOps["+idx+"] has "+ops.size()+" operands");
            int opIdx = 0;
            for (ServiceOperand op : ops) {
                //System.out.println("op["+opIdx+"] has "+op.getEntries().size()+" entries");
                List<GridDataset> grids = new ArrayList<GridDataset>();
                for (Entry first : op.getEntries()) {
                    millis = System.currentTimeMillis();
                    GridDataset dataset =
                        dataOutputHandler.getCdmManager().getGridDataset(
                            first, first.getResource().getPath());
                    //System.out.println("Get dataset took in makeYearsWidget: "+(System.currentTimeMillis()-millis) + " "  + dataset.toString());
                    if (dataset != null) {
                        grids.add(dataset);
                    }
                    dataOutputHandler.getCdmManager().returnGridDataset(
                        first.getResource().getPath(), dataset);
                }
                List<String> opYears = new ArrayList<String>();
                for (GridDataset dataset : grids) {
                    List<CalendarDate> dates =
                        CdmDataOutputHandler.getGridDates(dataset);
                    if ( !dates.isEmpty() && (grid == 0)) {
                        CalendarDate cd  = dates.get(0);
                        Calendar     cal = cd.getCalendar();
                        if (cal != null) {
                            sb.append(
                                HtmlUtils.hidden(
                                    CdmDataOutputHandler.ARG_CALENDAR,
                                    request.getSanitizedString(
                                        CdmDataOutputHandler.ARG_CALENDAR,
                                        cal.toString())));
                        }
                    }
                    SortedSet<String> uniqueYears =
                        Collections.synchronizedSortedSet(
                            new TreeSet<String>());
                    if ((dates != null) && !dates.isEmpty()) {
                        for (CalendarDate d : dates) {
                            try {  // shouldn't get an exception
                                String year =
                                    new CalendarDateTime(d).formattedString(
                                        "yyyy",
                                        CalendarDateTime.DEFAULT_TIMEZONE);
                                if ( !uniqueYears.contains(year)) {
                                    uniqueYears.add(year);
                                }
                            } catch (Exception e) {}
                        }
                    }
                    List<String> years = new ArrayList<String>(uniqueYears);
                    // TODO:  make a better list of years
                    if (years.isEmpty()) {
                        for (int i = 1979; i <= 2012; i++) {
                            years.add(String.valueOf(i));
                        }
                    }
                    opYears.addAll(years);
                    grid++;
                    //dataset.close();

                }
                dataYears.add(opYears);
                dateIdx++;
                opIdx++;
                // Assumes that each ensemble member has the same time range
                if (isCompare) {
                    break;
                }
            }
            idx++;
        }
        for (int wnum = 0; wnum < numWidgets; wnum++) {
            List<String> commonYears = new ArrayList<String>();
            if ( !isCompare) {
                for (int opNum = 0; opNum < dataYears.size(); opNum++) {
                    List<String> years = dataYears.get(opNum);
                    if (opNum == 0) {
                        commonYears.addAll(years);
                    } else {
                        commonYears.retainAll(years);
                    }
                }
            } else {
                commonYears.addAll(dataYears.get(datesIndex[wnum]));
            }

            String yearNum = (wnum == 0)
                             ? ""
                             : String.valueOf(wnum + 1);
            String yrLabel = "Start";
            if (numWidgets > 1) {
                if ((wnum < ordinalYears.length) && false) {
                    yrLabel = ordinalYears[wnum] + " Dataset:<br>Start";
                } else {
                    yrLabel = "Dataset " + (wnum + 1) + ":<br>Start";
                }
            }
            yrLabel = Repository.msgLabel(yrLabel);
            if ((wnum > 0) && isCompare) {
                commonYears.add(0, "");
            }
            int endIndex = 0;
            //int endIndex = (grid == 0)
            //               ? years.size() - 1
            //               : 0;
            StringBuilder yearsWidget = new StringBuilder();
            yearsWidget.append(yrLabel);
            yearsWidget.append(
                HtmlUtils.select(CDOOutputHandler.ARG_CDO_STARTYEAR + yearNum,
                                 commonYears,
                                 request.getSanitizedString(
                                     CDOOutputHandler.ARG_CDO_STARTYEAR
                                     + yearNum,
                                     request.getSanitizedString(
                                         CDOOutputHandler.ARG_CDO_STARTYEAR,
                                         commonYears.get(0))),
                                 HtmlUtils.title("Select the starting year")));
            yearsWidget.append(HtmlUtils.space(3));
            yearsWidget.append(Repository.msgLabel("End"));
            yearsWidget.append(
                HtmlUtils.select(CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum,
                                 commonYears,
                                 request.getSanitizedString(
                                     CDOOutputHandler.ARG_CDO_ENDYEAR
                                     + yearNum,
                                     request.getSanitizedString(
                                         CDOOutputHandler.ARG_CDO_ENDYEAR,
                                         commonYears.get(endIndex))),
                                 HtmlUtils.title("Select the ending year")));
            yearsWidget.append(HtmlUtils.p());
            yearsWidget.append(Repository.msgLabel("or List"));
            yearsWidget.append(HtmlUtils.input(CDOOutputHandler.ARG_CDO_YEARS
                    + yearNum,
                    request.getSanitizedString(CDOOutputHandler.ARG_CDO_YEARS
                    + yearNum,
                            ""),
                    20,
                    HtmlUtils.title(
                    "Input a set of years separated by commas (e.g. 1980,1983,2012)")));
            yearsWidget.append(HtmlUtils.space(2));
            yearsWidget.append(Repository.msg("(comma separated)"));

            sb.append(HtmlUtils.formEntry(Repository.msgLabel("Years"),
                                          yearsWidget.toString()));
            sb.append(
                HtmlUtils.script(
                    "$('select[name=\"" + CDOOutputHandler.ARG_CDO_STARTYEAR
                    + yearNum + "\"]').on('change',\n function() {\n"
                    + "  var endYearWidget = $('select[name=\""
                    + CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum + "\"]');\n"
                    + "  if ($(this).val() > endYearWidget.val()) {\n"
                    + "      endYearWidget.val($(this).val());\n" + "  };\n"
                    + "});\n"));
        }

    }

    /**
     * Adjust the input to handle operands with multiple files
     *
     * @param request  the request
     * @param input the old input
     * @param adjustDaily adjust the daily
     * @return a new input or the old
     *
     * @throws Exception problem adjusting the input
     */
    protected ServiceInput adjustInput(Request request, ServiceInput input,
                                       boolean adjustDaily)
            throws Exception {
        ServiceInput newInput = new ServiceInput(input.getProcessDir());
        List<ServiceOperand> newOps =
            new ArrayList<ServiceOperand>(input.getOperands().size());
        int opNum = 0;
        for (ServiceOperand so : input.getOperands()) {
            int collectionNum     = ModelUtil.getOperandCollectionNumber(so);
            List<Entry> opEntries = so.getEntries();
            Entry       oneOfThem = opEntries.get(0);
            if (oneOfThem.getTypeHandler()
                    instanceof ClimateModelFileTypeHandler) {
                if ((opEntries.size() == 1)
                        || request.defined(
                            ClimateModelApiHandler.ARG_FORMULA)) {
                    newOps.add(so);
                } else {
                    // Aggregate by time via ncml
                    String id =
                        ModelUtil.makeValuesKey(oneOfThem.getValues(), true,
                            "_");
                    id += "_" + collectionNum;
                    List<Entry> aggEntries = opEntries;
                    // reduce the daily files to just the years requested
                    if (ModelUtil.getFrequency(request,
                            oneOfThem).equals(
                                CDOOutputHandler.FREQUENCY_DAILY)) {
                        if (adjustDaily) {
                            aggEntries = extractDailyEntries(request,
                                    opEntries, collectionNum);
                            id += "_reduced_" + opNum;
                        }
                        List<Entry> newEntries = new ArrayList<Entry>();
                        for (Entry e : aggEntries) {
                            newEntries.add(e);
                        }
                        ServiceOperand newOp =
                            new ServiceOperand(so.getDescription(),
                                newEntries);
                        copyServiceOperandProperties(so, newOp);
                        newOps.add(newOp);
                    } else {
                        Entry agg = ModelUtil.aggregateEntriesByTime(request,
                                        aggEntries, id,
                                        input.getProcessDir());
                        List<Entry> newEntries = new ArrayList<Entry>();
                        newEntries.add(agg);
                        ServiceOperand newOp =
                            new ServiceOperand(so.getDescription(),
                                newEntries);
                        copyServiceOperandProperties(so, newOp);
                        newOps.add(newOp);
                    }
                }
            } else {
                newOps.add(so);
            }
            opNum++;
        }
        newInput.setOperands(newOps);
        ModelUtil.copyServiceInputProperties(input, newInput, "type",
                                             "actionId");
        /*
        // make sure to set the type of service if not null
        if (input.getProperty("type") != null) {
            newInput.putProperty("type", input.getProperty("type"));
        }
        if (input.getProperty("actionId") != null) {
            newInput.putProperty("actionId", input.getProperty("actionId"));
        }
        */

        return newInput;
    }


    /**
     * Copy over the service operand properties to the new ServiceOperand
     *
     * @param oldOp  old operand
     * @param newOp  new operand
     */
    protected void copyServiceOperandProperties(ServiceOperand oldOp,
            ServiceOperand newOp) {
        String prop = oldOp.getProperty(
                          ClimateModelApiHandler.ARG_COLLECTION).toString();
        //System.err.println(prop);
        if (prop != null) {
            newOp.putProperty(ClimateModelApiHandler.ARG_COLLECTION, prop);
            for (Entry e : newOp.getEntries()) {
                e.putTransientProperty(ClimateModelApiHandler.ARG_COLLECTION,
                                       prop);
            }
        }
    }
    
    /**
     * Extract only those daily entries that are needed for the request
     * @param request  the request
     * @param opEntries the operand entries
     * @param opNum  which op number
     * @return
     *
     * @throws Exception problems, problems, problems
     */
    private List<Entry> extractDailyEntries(Request request,
                                            List<Entry> opEntries, int opNum)
            throws Exception {

        // for makeInputForm
        if ( !(request.defined(ClimateModelApiHandler.ARG_ACTION_COMPARE)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_ENS_COMPARE)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_MULTI_COMPARE)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_CORRELATION)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_MULTI_TIMESERIES)
                || request.defined(
                    ClimateModelApiHandler.ARG_ACTION_TIMESERIES))) {
            return opEntries;
        }
        return opEntries;
        
   }

    /**
     * Calculated the latlon rectangle for this request
     *
     * @param request _more_
     * @param sb _more_
     * @param dataset _more_
     *
     * @throws Exception _more_
     */
    protected void addMyMapWidget(Request request, Appendable sb,
                                GridDataset dataset)
            throws Exception {
        // if a custom map region was used before, set llr to that.
        LatLonRect llr = null;
        if (request.defined("mapregion")
                && request.getSanitizedString("mapregion",
                        null).equals("CUSTOM")) {
            double maxlon = request.get(CDOOutputHandler.ARG_AREA_EAST,
                                        Double.NaN);
            double minlon = request.get(CDOOutputHandler.ARG_AREA_WEST,
                                        Double.NaN);
            double maxlat = request.get(CDOOutputHandler.ARG_AREA_NORTH,
                                        Double.NaN);
            double minlat = request.get(CDOOutputHandler.ARG_AREA_SOUTH,
                                        Double.NaN);
            if ( !(Double.isNaN(maxlat)
                    || Double.isNaN(minlat)
                    || Double.isNaN(maxlon)
                    || Double.isNaN(minlon))) {
                llr = new LatLonRect(new LatLonPointImpl(maxlat,
                        minlon), new LatLonPointImpl(minlat, maxlon));
            }
        } else {
            if (dataset != null) {
                llr = dataset.getBoundingBox();
            } else {
                llr = new LatLonRect(new LatLonPointImpl(90.0,
                        -180.0), new LatLonPointImpl(-90.0, 180.0));
            }
        }
        addMapWidget(request, sb, llr, false);
    }

    /**
     * Add the map widget
     *
     * @param request   The request
     * @param sb        the HTML
     * @param llr       the lat/lon rectangle
     * @param usePopup  use a popup
     *
     * @throws Exception  problems appending
     */
    public void addMapWidget(Request request, Appendable sb, LatLonRect llr,
                             boolean usePopup)
            throws Exception {

        //TODO: This should be a parameter to the method.
        //If its null then all map regions are used 
        //If non-null then only map regions with the group
        //String mapRegionGroup = null;
        String  mapRegionGroup = "model regions";

        MapInfo map;
        if ( !usePopup) {
            map = getRepository().getMapManager().createMap(request, null,
                    "250", "150", true, null);
            String maplayers = getRepository().getProperty(PROP_MAP_LAYERS,
                                   null);
            String defaultMap =
                getRepository().getProperty(PROP_MAP_DEFAULTLAYER, null);
            if (maplayers != null) {
                map.addProperty("mapLayers", Misc.newList(maplayers));
            } else {
                if (defaultMap != null) {
                    map.addProperty("mapLayers", Misc.newList(defaultMap));
                }
            }
            // remove some of the widgets
            map.addProperty("showScaleLine", "false");
            map.addProperty("showLayerSwitcher", "false");
            map.addProperty("showZoomPanControl", "false");
            map.addProperty("showZoomOnlyControl", "true");
        } else {
            map = getRepository().getMapManager().createMap(request, null,
                    true, null);
        }

        map.setMapRegions(getPageHandler().getMapRegions(mapRegionGroup));
        map.setDefaultMapRegion(request.getSanitizedString("mapregion",
                null));

        //map.addBox("", "", llr, new MapProperties("blue", false, true));
        String[] points = new String[] { "" + llr.getLatMax(),
                                         "" + llr.getLonMin(),
                                         "" + llr.getLatMin(),
                                         "" + llr.getLonMax(), };

        for (int i = 0; i < points.length; i++) {
            sb.append(HtmlUtils.hidden(SPATIALARGS[i] + ".original",
                                       points[i]));
        }
        StringBuilder mapDiv = new StringBuilder();
        String        llb = map.makeSelector(ARG_AREA, usePopup, points);
        mapDiv.append(HtmlUtils.div(llb));
        sb.append(HtmlUtils.formEntry(msgLabel("Region"), mapDiv.toString(),
                                      4));
    }

    /**
     * Can we handle this input
     *
     * @param input  the input
     *
     * @return true if we can, otherwise false
     */
    public boolean canHandle(ServiceInput input) {
        return true;
    }

    /**
     * Process the request
     *
     * @param request  The request
     * @param input  the  data process input
     * @param argPrefix the argument prefix
     *
     * @return  the processed data
     *
     * @throws Exception  problem processing
     */
    @Override
    public ServiceOutput evaluate(Request request, ServiceInput input,
                                  String argPrefix)
            throws Exception {
    	
    	List<Entry> outputEntries = new ArrayList<Entry>();
    	// Write out beek
    	 InputStream testIS =  Utils.getInputStream("/org/ramadda/geodata/model/htdocs/model/beek.gif", getClass());
    	//Get the destination file and outputstream          
        String myid      = getRepository().getGUID();
    	File dest = new File(input.getProcessDir()+"/beek_"+myid+".gif");
    	FileOutputStream destOS = new FileOutputStream(dest);
    	//Copy the input to the output                                                                                                    
    	 IOUtil.writeTo(testIS,destOS);
    	 destOS.close();
         String outType = "type_image";
         Resource resource = new Resource(dest,
                                          Resource.TYPE_LOCAL_FILE);
         TypeHandler myHandler = getRepository().getTypeHandler(outType,
                                     true);
         Entry outputEntry = new Entry(myHandler, true,
                                       dest.toString());
         outputEntry.setResource(resource);
         outputHandler.getEntryManager().writeEntryXmlFile(request,
                 outputEntry);
         outputEntries.add(outputEntry);

    	List<ServiceOperand> ops = input.getOperands();
        LatLonRect rect = new LatLonRect(new LatLonPointImpl(45, -110), new LatLonPointImpl(40, -100));
    	for (ServiceOperand op : ops) {
    		List<Entry> entries = op.getEntries();
    		Entry first = entries.get(0);
            String tail = getStorageManager().getFileTail(first);
            String id      = getRepository().getGUID();
            String newName = IOUtil.stripExtension(tail) + "_" + id + ".nc";
            newName = cleanName(newName);
            String fullName = input.getProcessDir()+"/"+newName;
    	    File outFile = new File(fullName);

            Object actionId = input.getProperty("actionId", null);
            if (actionId != null) {
                getActionManager().setActionMessage(actionId,
                        "Processing: " + tail);
            }

            CdmDataOutputHandler dataOutputHandler = getDataOutputHandler();
            GridDataset dataset =
                    dataOutputHandler.getCdmManager().getGridDataset(first,
                        first.getResource().getPath());
            GridDatatype grid = dataset.getGrids().get(0);
            String gridName = grid.getName();
            List<String> gridList = new ArrayList<String>();
            gridList.add(gridName);
            //GridDatatype subset = grid.makeSubset(null, null, rect, 1,1,1);
            NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, fullName);
            CFGridWriter2.writeFile(dataset, gridList, rect, null, 1, null, null, 1, true, writer);
            dataOutputHandler.getCdmManager().returnGridDataset(first.getResource().getPath(), dataset);

            
            resource = new Resource(outFile, Resource.TYPE_LOCAL_FILE);
            myHandler = getRepository().getTypeHandler("cdm_grid",
                                        true);
            //TypeHandler myHandler = sample.getTypeHandler();
            outputEntry = new Entry(myHandler, true, newName);
            outputEntry.setResource(resource);
            outputEntry.setValues(first.getValues());
            // Add in lineage and associations
            outputEntry.addAssociation(new Association(getRepository().getGUID(),
                    "generated product",
                    "product generated from",
                    first.getId(), outputEntry.getId()));
                    outputHandler.getEntryManager().writeEntryXmlFile(request,
                    outputEntry);

                    outputEntries.add(outputEntry);
    	}
        ServiceOperand newOp = new ServiceOperand("Test this!",
                                   outputEntries);


        return new ServiceOutput(newOp);
    }

    /**
     *     _more_
     *
     *     @param name _more_
     *
     *     @return _more_
     */
    public static String cleanName(String name) {
        name = name.replaceAll("\\s+", "_").replaceAll(",", "_");
        name = name.replaceAll("__+", "_");

        return name;
    }



}
