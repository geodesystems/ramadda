/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.cdmdata;


import org.ramadda.repository.Entry;
import org.ramadda.repository.DateHandler;
import org.ramadda.repository.Repository;
import org.ramadda.repository.RepositoryManager;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.repository.metadata.ContentMetadataHandler;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.ObjectPool;
import org.ramadda.util.TempDir;
import org.ramadda.util.Utils;

import thredds.servlet.ThreddsConfig;

import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.util.DiskCache2;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.Cache;
import ucar.unidata.util.Counter;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;


/**
 * A manager for netCDF-Java CDM data
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class CdmManager extends RepositoryManager implements CdmConstants {

    public static final String SUFFIX_EXACT ="_exact";

    /** NCML suffix */
    public static final String SUFFIX_NCML = ".ncml";

    /** GrADS CTL suffix */
    public static final String SUFFIX_CTL = ".ctl";



    /** CDM Type */
    public static final String TYPE_CDM = "cdm";

    /** CDM Type */
    public static final String TYPE_CDM_GRID = "cdm_grid";

    /** GRID type */
    public static final String TYPE_GRID = "grid";

    /** TRAJECTORY type */
    public static final String TYPE_TRAJECTORY = "trajectory";

    /** RADAR type */
    public static final String TYPE_RADAR = "cdm_radar";

    /** POINT_TYPE */
    public static final String TYPE_POINT = "point";

    /** GrADS type */
    public static final String TYPE_GRADS = "gradsbinary";

    /** set of suffixes */
    private HashSet<String> suffixSet;

    /** hash of patterns */
    private Hashtable<String, List<Pattern>> patterns;

    /** not patterns */
    private Hashtable<String, List<Pattern>> notPatterns;


    /** cdm cache */
    private Cache<String, Boolean> cdmEntries = new Cache<String,
                                                    Boolean>(5000);

    /** cdm cache */
    private Cache<String, Boolean> cdmGridEntries = new Cache<String,
                                                        Boolean>(5000);

    /** grid entries cache */
    private Cache<String, Boolean> gridEntries = new Cache<String,
                                                     Boolean>(5000);


    /** point entries cache */
    private Cache<String, Boolean> pointEntries = new Cache<String,
                                                      Boolean>(5000);

    /** trajectory entries cache */
    private Cache<String, Boolean> trajectoryEntries = new Cache<String,
                                                           Boolean>(5000);

    /** trajectory entries cache */
    private Cache<String, Boolean> radarEntries = new Cache<String,
                                                      Boolean>(5000);

    /** nj cache directory */
    private TempDir nj22Dir;

    /** data cache directory */
    private TempDir dataCacheDir;


    //TODO: When we close a ncfile some thread might be using it
    //Do we have to actually close it??

    /** nc counter */
    Counter ncCounter = new Counter();

    /** nc create counter */
    Counter ncCreateCounter = new Counter();

    /** nc remove counter */
    Counter ncRemoveCounter = new Counter();

    /** nc get counter */
    Counter ncGetCounter = new Counter();

    /** nc put counter */
    Counter ncPutCounter = new Counter();

    /** ext counter */
    Counter extCounter = new Counter();

    /** opendap counter */
    Counter opendapCounter = new Counter();

    /** grid open counter */
    Counter gridOpenCounter = new Counter();

    /** grid close counter */
    Counter gridCloseCounter = new Counter();


    /** point open counter */
    Counter pointOpenCounter = new Counter();

    /** point close counter */
    Counter pointCloseCounter = new Counter();

    /** the cdm manager id */
    public static final String CDMMANAGER_ID = "cdmmanager";

    /** climate model file type */
    private static final String TYPE_CLIMATE_MODEL_GRANULE =
        "climate_modelfile";

    /** disk cache */
    private DiskCache2 diskCache2 = null;

    /**
     * Create a new CdmManager
     *
     * @param repository  the repository
     *
     * @throws Exception problem creating class
     */
    public CdmManager(Repository repository) throws Exception {
        super(repository);
        //TODO: what other global configuration should be done?
        nj22Dir = getRepository().getStorageManager().makeTempDir("nj22");
        nj22Dir.setMaxFiles(500);

        String tdsConfig =
            getStorageManager().readSystemResource(
                "/org/ramadda/geodata/cdmdata/resources/threddsConfig.xml");
        String outdir =
            getRepository().getStorageManager().getScratchDir().getDir()
                .toString();
        tdsConfig = tdsConfig.replaceAll("%ncssdir%", outdir);
        File outputFile = new File(IOUtil.joinDir(nj22Dir.getDir(),
                              "threddsConfig.xml"));
        InputStream is = new ByteArrayInputStream(tdsConfig.getBytes());
        OutputStream os =
            getStorageManager().getUncheckedFileOutputStream(outputFile);
        IOUtil.writeTo(is, os);
        ThreddsConfig.init(outputFile.toString());


        // Apply settings for the NetcdfDataset
        //        ucar.nc2.dataset.NetcdfDataset.setHttpClient(getRepository().getHttpClient());


        // Apply settings for the opendap.dap
        //        opendap.dap.DConnect2.setHttpClient(getRepository().getHttpClient());

        //Set the temp file and the cache policy
        ucar.nc2.util.DiskCache.setRootDirectory(nj22Dir.getDir().toString());
        ucar.nc2.util.DiskCache.setCachePolicy(true);
        //        ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache(true);
        ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache(true);

        // for ncj 4.3
        diskCache2 = new DiskCache2(nj22Dir.getDir().toString(), false, -1,
                                    -1);
        diskCache2.setPolicy(DiskCache2.CachePathPolicy.NestedDirectory);
        diskCache2.setAlwaysUseCache(true);
        ucar.nc2.grib.GribIndexCache.setDiskCache2(diskCache2);


        dataCacheDir =
            getRepository().getStorageManager().makeTempDir("visaddatacache");
        dataCacheDir.setMaxFiles(2000);

        NetcdfDataset.disableNetcdfFileCache();


        visad.SampledSet.setCacheSizeThreshold(10000);
        visad.util.ThreadManager.setGlobalMaxThreads(4);
        visad.data.DataCacheManager.getCacheManager().setCacheDir(
            dataCacheDir.getDir());
        visad.data.DataCacheManager.getCacheManager().setMemoryPercent(0.1);
    }


    /** nc dataset pool */
    private DatedObjectPool<String, NetcdfDataset> ncDatasetPool =
        new DatedObjectPool<String, NetcdfDataset>(10) {
        protected void removeValue(String key, NetcdfDataset dataset) {
            try {
                super.removeValue(key, dataset);
                ncRemoveCounter.incr();
                dataset.close();
            } catch (Exception exc) {
                System.err.println("Error closing:" + key);
                exc.printStackTrace();
            }
        }

        /*
          public synchronized void put(String key, NetcdfDataset file) {
            ncPutCounter.incr();
            super.put(key, file);
            }*/

        protected NetcdfDataset getFromPool(List<NetcdfDataset> list) {
            NetcdfDataset dataset = super.getFromPool(list);
            ncGetCounter.incr();
            //try {
            //    dataset.sync();

            return dataset;
            //} catch (Exception exc) {
            //    throw new RuntimeException(exc);
            //}
        }


        protected NetcdfDataset createValue(String path) {
            try {
                long t1 = System.currentTimeMillis();
                getStorageManager().dirTouched(nj22Dir, null);
                NetcdfDataset dataset = NetcdfDataset.openDataset(path);
                long          t2      = System.currentTimeMillis();
                //Utils.printTimes("CDM: ncDatasetPool", t1,t2);
                ncCreateCounter.incr();

                return dataset;
            } catch (Exception exc) {
                System.err.println("CDM: ncDatasetPool error " + exc);

                throw new RuntimeException(exc);
            }
        }
    };


    /** nc file pool */
    private DatedObjectPool<String, NetcdfFile> ncFilePool =
        new DatedObjectPool<String, NetcdfFile>(10) {
        protected void removeValue(String key, NetcdfFile ncFile) {
            try {
                super.removeValue(key, ncFile);
                ncRemoveCounter.incr();
                ncFile.close();
            } catch (Exception exc) {
                System.err.println("Error closing:" + key);
                exc.printStackTrace();
            }
        }

        /*
        public synchronized void put(String key, NetcdfFile ncFile) {
            ncPutCounter.incr();
            super.put(key, ncFile);
            }*/

        protected NetcdfFile getFromPool(List<NetcdfFile> list) {
            NetcdfFile ncFile = super.getFromPool(list);
            ncGetCounter.incr();
            //try {
            //    ncFile.sync();

            return ncFile;
            //} catch (Exception exc) {
            //    throw new RuntimeException(exc);
            //}
        }

        protected NetcdfFile createValue(String path) {
            try {
                long t1 = System.currentTimeMillis();
                getStorageManager().dirTouched(nj22Dir, null);
                //                NetcdfDataset dataset = NetcdfDataset.openDataset(path);
                NetcdfFile ncFile = NetcdfDataset.openFile(path, null);
                long       t2     = System.currentTimeMillis();
                //Utils.printTimes("CDM: ncFilePool", t1,t2);
                ncCreateCounter.incr();

                return ncFile;
            } catch (Exception exc) {
                System.err.println("CDM: ncFilePool Error:" + exc);

                throw new RuntimeException(exc);
            }
        }
    };




    /** grid pool flag */
    private boolean doGridPool = true;

    /** grid pool */
    private DatedObjectPool<String, GridDataset> gridPool =
        new DatedObjectPool<String, GridDataset>(10) {
        protected void removeValue(String key, GridDataset dataset) {
            try {
                super.removeValue(key, dataset);
                gridCloseCounter.incr();
                dataset.close();
            } catch (Exception exc) {}
        }

        protected GridDataset getFromPool(List<GridDataset> list) {
            GridDataset dataset = super.getFromPool(list);
            //try {
            //    dataset.sync();

            return dataset;
            //} catch (Exception exc) {
            //    throw new RuntimeException(exc);
            //}

        }


        protected GridDataset createValue(String path) {
            try {
                getStorageManager().dirTouched(nj22Dir, null);
                gridOpenCounter.incr();

                long        t1  = System.currentTimeMillis();
                GridDataset gds = GridDataset.open(path);
                long        t2  = System.currentTimeMillis();
                //Utils.printTimes("CDM: gridPool", t1,t2);

                if (gds.getGrids().iterator().hasNext()) {
                    return gds;
                } else {
                    gridCloseCounter.incr();
                    gds.close();

                    return null;
                }
            } catch (Exception exc) {
                System.err.println("CDM: gridPool error:" + exc);

                throw new RuntimeException(exc);
            }
        }
    };

    /**
     * Create a netCDF file from the location
     *
     * @param location  the path
     *
     * @return the NetcdfFile
     *
     * @throws Exception  problem getting data
     */
    public NetcdfFile createNetcdfFile(String location) throws Exception {
        return ncFilePool.get(location);
    }

    /**
     * Return the NetcdfFile object for the location to the pool
     *
     * @param location  the the path
     * @param ncf       the NetcdfFile object
     *
     * @throws Exception  problem adding to the pool
     */
    public void returnNetcdfFile(String location, NetcdfFile ncf)
            throws Exception {
        ncFilePool.put(location, ncf);
    }


    /**
     * Create the GridDataset from the file
     *
     * @param path file path
     *
     * @return  the GridDataset
     */
    public GridDataset createGrid(String path) {
        try {
            getStorageManager().dirTouched(nj22Dir, null);
            //            gridOpenCounter.incr();

            long        t1  = System.currentTimeMillis();
            GridDataset gds = GridDataset.open(path);
            long        t2  = System.currentTimeMillis();
            //Utils.printTimes("CDM: createGrid ", t1,t2);
            if (gds.getGrids().iterator().hasNext()) {
                return gds;
            } else {
                //                gridCloseCounter.incr();
                gds.close();

                return null;
            }
        } catch (Exception exc) {
            System.err.println("CDM: createGrid: " + exc.getMessage());

            throw new RuntimeException(exc);
        }
    }



    /** point pool */
    private DatedObjectPool<String, FeatureDatasetPoint> pointPool =
        new DatedObjectPool<String, FeatureDatasetPoint>(10) {
        protected void removeValue(String key, FeatureDatasetPoint dataset) {
            try {
                super.removeValue(key, dataset);
                dataset.close();
            } catch (Exception exc) {}
        }

        /*
        protected  FeatureDatasetPoint getFromPool(List<FeatureDatasetPoint> list) {
            FeatureDatasetPoint dataset = super.getFromPool(list);
            //dataset.sync();
            return dataset;
            }*/

        protected FeatureDatasetPoint createValue(String path) {
            try {
                Formatter buf = new Formatter();
                getStorageManager().dirTouched(nj22Dir, null);

                long t1 = System.currentTimeMillis();
                FeatureDatasetPoint pods =
                    (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                        ucar.nc2.constants.FeatureType.POINT, path, null,
                        buf);
                if (pods == null) {  // try as ANY_POINT
                    pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager
                        .open(ucar.nc2.constants.FeatureType.ANY_POINT, path,
                              null, buf);
                }

                long t2 = System.currentTimeMillis();
                //Utils.printTimes("CDM: pointPool", t1,t2);

                return pods;
            } catch (Exception exc) {
                System.err.println("CDM: pointPool error " + exc);

                throw new RuntimeException(exc);
            }
        }


    };


    /** radar pool */
    private DatedObjectPool<String, RadialDatasetSweep> radarPool =
        new DatedObjectPool<String, RadialDatasetSweep>(10) {
        protected void removeValue(String key, RadialDatasetSweep dataset) {
            try {
                super.removeValue(key, dataset);
                dataset.close();
            } catch (Exception exc) {}
        }



        protected RadialDatasetSweep createValue(String path) {
            try {
                long      t1  = System.currentTimeMillis();
                Formatter buf = new Formatter();

                getStorageManager().dirTouched(nj22Dir, null);

                RadialDatasetSweep pods =
                    (RadialDatasetSweep) ucar.nc2.dt.TypedDatasetFactory.open(
                        ucar.nc2.constants.FeatureType.RADIAL, path, null,
                        new StringBuilder());

                long t2 = System.currentTimeMillis();

                //Utils.printTimes("CDM: radarPool", t1,t2);
                return pods;
            } catch (Exception exc) {
                System.err.println("CDM: radarPool error " + exc);

                throw new RuntimeException(exc);
            }
        }


    };

    /**
     * trajectory pool 
     *
     * @param sb _more_
     */
    /*
    private DatedObjectPool<String, TrajectoryObsDataset> trajectoryPool =
        new DatedObjectPool<String, TrajectoryObsDataset>(10) {
        protected void removeValue(String key, TrajectoryObsDataset dataset) {
            try {
                super.removeValue(key, dataset);
                dataset.close();
            } catch (Exception exc) {}
        }

        protected TrajectoryObsDataset createValue(String path) {
            try {
                getStorageManager().dirTouched(nj22Dir, null);

                long t1 = System.currentTimeMillis();
                //                System.err.println("track:" + path);
                TrajectoryObsDataset dataset =
                    (TrajectoryObsDataset) ucar.nc2.dt.TypedDatasetFactory.open(
                        FeatureType.TRAJECTORY, path, null,
                        new StringBuilder());

                long t2 = System.currentTimeMillis();

                //Utils.printTimes("CDM: trajPool", t1,t2);
                //                System.err.println("Create trajectoryPool: " + path);
                //                System.err.println("got it? " + (dataset!=null));
                return dataset;
            } catch (Exception exc) {
                System.err.println("CDM: trajPool error " + exc);

                //                System.err.println("oops");
                throw new RuntimeException(exc);
            }
        }


    };
    */


    /**
     * Get the system stats
     *
     * @param sb  the stats
     */
    public void getSystemStats(StringBuffer sb) {
        //        super.getSystemStats(sb);
        StringBuffer poolStats = new StringBuffer("<pre>");
        ncFilePool.getStats(poolStats);
        ncDatasetPool.getStats(poolStats);
        poolStats.append("</pre>");
        sb.append(
            HtmlUtils.formEntryTop(
                "Data Cache Size:",
                "NC File Pool:" + ncFilePool.getSize()
                + " have ncfile cache:"
                + (NetcdfDataset.getNetcdfFileCache() != null) + " "
                + " Count:  Create:" + ncCreateCounter.getCount()
                + " Remove:" + ncRemoveCounter.getCount() + "<br>" + " Get:"
                + ncGetCounter.getCount() + " Put:" + ncPutCounter.getCount()
                + "<br>" + " Ext Count:" + extCounter.getCount()
                + " Dap Count:" + opendapCounter.getCount() + poolStats
                + HtmlUtils.br() + "Grid Pool:" + gridPool.getSize()
                + HtmlUtils.br() + "Point Pool:" + pointPool.getSize()
                + HtmlUtils.br()));

    }


    /**
     * clear the cache
     */
    @Override
    public void clearCache() {
        ncFilePool.clear();
        ncDatasetPool.clear();
        gridPool.clear();
        pointPool.clear();
        cdmEntries.clear();
        gridEntries.clear();
        pointEntries.clear();
        trajectoryEntries.clear();
        radarEntries.clear();
    }

    /**
     * Check to see if an Entry is an aggregation
     *
     * @param entry  the Entry
     *
     * @return  true if an aggregation
     */
    public boolean isAggregation(Entry entry) {
        return entry.getType().equals(
            GridAggregationTypeHandler.TYPE_GRIDAGGREGATION);
    }



    /**
     * Check if we can load the Entry
     *
     * @param entry  the Entry
     *
     * @return true if we can load it
     */
    private boolean canLoadEntry(Entry entry) {
        String url = entry.getResource().getPath();

        if (url == null) {
            return false;
        }
        if (url.endsWith("~")) {
            return false;
        }
        if (url.endsWith("#")) {
            return false;
        }
        if (entry.isGroup()) {
            return false;
        }

        if (entry.getResource().isRemoteFile()) {
            return true;
        }

        if (entry.getResource().isFileType()) {
	    //Make sure we have the check for S3 before we check the getFile
	    //as the getFile would trigger a S3 file copy
            return entry.getResource().isS3() || entry.getFile().exists();
        }
        if ( !entry.getResource().isUrl()) {
            return false;
        }
        if (url.indexOf("dods") >= 0) {
            return true;
        }

        return true;
    }


    /**
     * Can the given entry be served by the tds
     *
     *
     * @param entry The entry
     *
     * @return Can the given entry be served by the tds
     */
    public boolean canLoadAsCdm(Entry entry) {
	boolean debug = false;
	if(debug) System.err.println("canLoadAsCdm:" + entry);

	if(isCdmGrid(entry)) {
	    if(debug) System.err.println("\tisCdmGrid");
	    return true;
	}
        if (entry.getTypeHandler().isType(
                OpendapLinkTypeHandler.TYPE_OPENDAPLINK)) {
	    if(debug) System.err.println("\tis opendap link");
            return true;
        }
	if(entry.getTypeHandler().isType("cdm_radar_level2")) {
	    if(debug) System.err.println("\tis level2 radar");
            return true;
	}


        if (entry.getTypeHandler().isType(TYPE_CLIMATE_MODEL_GRANULE)) {
            return true;
        }

        if (isGrads(entry)) {
            return true;
        }

        if (isAggregation(entry)) {
            return true;
        }
        if ( !entry.isFile()) {
	    if(debug) System.err.println("\tnot file");
            return false;
        }
        if (excludedByPattern(entry, TYPE_CDM)) {
	    if(debug) System.err.println("\texcluded by type_cdm");
            return false;
        }

        String[] types = {
            TYPE_CDM, TYPE_CDM_GRID, TYPE_GRID, TYPE_TRAJECTORY, TYPE_POINT,
            TYPE_RADAR
        };
        for (int i = 0; i < types.length; i++) {
            if (includedByPattern(entry, types[i])) {
		if(debug) System.err.println("\tincluded by:" + types[i]);
                return true;
            }
        }

        if (entry.getResource().isRemoteFile()) {
            String path = entry.getResource().getPath();
            if (path.endsWith(".nc")) {
		if(debug) System.err.println("\tisRemoteFile:"+ path);
                return true;
            }
        }

        Boolean b = (Boolean) cdmEntries.get(entry.getId());
        if (b == null) {
            boolean ok = false;
            if (canLoadEntry(entry)) {
		if(debug) System.err.println("\tcanLoadEntry");
                try {
                    String path = entry.getResource().getPath();
                    //Exclude zip files becase canOpen tries to unzip them (?)
                    if ( !(path.endsWith(".zip"))) {
                        //                        System.err.println  ("checking file:" + path);
                        ok = NetcdfDataset.canOpen(path);
			if(debug) System.err.println("\tNetcdfDataset.canOpen:" + ok);
                    }
                } catch (Exception ignoreThis) {
		    if(debug) System.err.println("\terror from NetcdfDataset.canOpen:" + ignoreThis);
                    //                    System.err.println("   error:" + ignoreThis);
                    //                    System.err.println("error:" + ignoreThis);
                }
            }
            b = Boolean.valueOf(ok);
            cdmEntries.put(entry.getId(), b);
        }

        return b.booleanValue();
    }

    /**
     *  Is this a GrADS entry
     *
     * @param e the Entry
     *
     * @return true if GrADS type
     */
    private boolean isGrads(Entry e) {
        return e.getType().equals(TYPE_GRADS);
    }


    /**
     *  Is this a CDM Grid entry
     *
     * @param e the Entry
     *
     * @return true if GrADS type
     */
    private boolean isCdmGrid(Entry e) {
        return e.getTypeHandler().isType(TYPE_CDM_GRID)
	    || e.getTypeHandler().isType(TYPE_CLIMATE_MODEL_GRANULE) ||
	    e.getTypeHandler().isType("earth_satellite_modis_aqua");
    }

    /**
     * Can the Entry be loaded a point data?
     *
     * @param entry  the Entry
     *
     * @return true if can load as point
     */
    public boolean canLoadAsPoint(Entry entry) {
        if (excludedByPattern(entry, TYPE_POINT)) {
            return false;
        }
        if (includedByPattern(entry, TYPE_POINT)) {
            return true;
        }
        if ( !canLoadAsCdm(entry)) {
            return false;
        }
	//a hack
	if(entry.getTypeHandler().isType("cdm_radar_level2")) {
	    return false;
	}


        Boolean b = (Boolean) pointEntries.get(entry.getId());
        if (b == null) {
            boolean ok = false;
            if ( !canLoadEntry(entry)) {
                ok = false;
            } else {
                try {
                    ok = pointPool.containsOrCreate(getPath(entry));
                } catch (Exception ignore) {}
            }
            pointEntries.put(entry.getId(), b =  Boolean.valueOf(ok));
        }

        return b.booleanValue();
    }

    /**
     * Can the Entry be loaded a radar data?
     *
     * @param entry  the Entry
     *
     * @return true if can load as point
     */
    public boolean canLoadAsRadar(Entry entry) {
        if (excludedByPattern(entry, TYPE_RADAR)) {
            return false;
        }
        if (includedByPattern(entry, TYPE_RADAR)) {
            return true;
        }
        if ( !canLoadAsCdm(entry)) {
            return false;
        }

        Boolean b = (Boolean) radarEntries.get(entry.getId());
        if (b == null) {
            boolean ok = false;
            if ( !canLoadEntry(entry)) {
                ok = false;
            } else {
                try {
                    ok = radarPool.containsOrCreate(getPath(entry));
                } catch (Exception ignore) {}
            }
            radarEntries.put(entry.getId(), b = Boolean.valueOf(ok));
        }

        return b.booleanValue();
    }




    /**
     * See if an Entry is excluded by pattern for a type
     *
     * @param entry   the Entry
     * @param type    the type to check
     *
     * @return true if excluded
     */
    private boolean excludedByPattern(Entry entry, String type) {
        return hasSuffixForType(entry, type, true);
    }

    /**
     * See if an Entry is included by pattern for a type
     *
     * @param entry   the Entry
     * @param type    the type to check
     *
     * @return true if included
     */
    private boolean includedByPattern(Entry entry, String type) {
        return hasSuffixForType(entry, type, false);
    }

    /**
     * See if the Entry has a suffix for this type
     *
     * @param entry  the Entry
     * @param type   the type
     * @param forNot true if not for that type
     *
     * @return  true if has suffix
     */
    private boolean hasSuffixForType(Entry entry, String type,
                                     boolean forNot) {
        String url = entry.getResource().getPath();
        if (url == null) {
            return false;
        }

        return hasSuffixForType(url, type, forNot);
    }

    /**
     * See if the URL has a suffix for this type
     *
     * @param url    the URL
     * @param type   the type
     * @param forNot true if not for that type
     *
     * @return  true if has suffix
     */
    private boolean hasSuffixForType(String url, String type,
                                     boolean forNot) {
        if (suffixSet == null) {
            HashSet<String> tmpSuffixSet = new HashSet<String>();

            Hashtable<String, List<Pattern>> tmpPatterns =
                new Hashtable<String, List<Pattern>>();
            Hashtable<String, List<Pattern>> tmpNotPatterns =
                new Hashtable<String, List<Pattern>>();




            String[] types = {
                TYPE_CDM, TYPE_GRID, TYPE_TRAJECTORY, TYPE_POINT, TYPE_RADAR,
                TYPE_CDM_GRID
            };
            for (int i = 0; i < types.length; i++) {
                List toks = StringUtil.split(
                                getRepository().getProperty(
                                    "ramadda.data." + types[i] + ".suffixes",
                                    ""), ",", true, true);
                for (String tok : (List<String>) toks) {
                    if ((tok.length() == 0) || tok.equals("!")) {
                        continue;
                    }
                    String key = types[i] + "." + tok;
                    tmpSuffixSet.add(key);
                }
            }



            for (int i = 0; i < types.length; i++) {
                tmpPatterns.put(types[i], new ArrayList<Pattern>());
                tmpNotPatterns.put(types[i], new ArrayList<Pattern>());
                List patterns = StringUtil.split(
                                    getRepository().getProperty(
                                        "ramadda.data." + types[i]
                                        + ".patterns", ""), ",", true, true);
                for (String pattern : (List<String>) patterns) {
                    if ((pattern.length() == 0) || pattern.equals("!")) {
                        continue;
                    }
                    Hashtable<String, List<Pattern>> tmp;
                    if (pattern.startsWith("!")) {
                        tmp     = tmpNotPatterns;
                        pattern = pattern.substring(1);
                    } else {
                        tmp = tmpPatterns;
                    }
                    tmp.get(types[i]).add(Pattern.compile(pattern));
                }
            }

            patterns    = tmpPatterns;
            notPatterns = tmpNotPatterns;
            suffixSet   = tmpSuffixSet;
            //            System.err.println ("not:"  + notPatterns);
            //            System.err.println ("suffix:"  + suffixSet);
        }

        url = url.toLowerCase();

        //First check the patterns
        List<Pattern> patternList = (forNot
                                     ? notPatterns.get(type)
                                     : patterns.get(type));
        for (Pattern pattern : patternList) {
            if (pattern.matcher(url).find()) {
                return true;
            }
        }


        String ext    = IOUtil.getFileExtension(url);
        String key    = type + "." + ext;
        String notKey = type + ".!" + ext;
        if (forNot) {
            if (suffixSet.contains(notKey)) {
                return true;
            }
        } else {
            if (suffixSet.contains(key)) {
                return true;
            }
        }


        return false;
    }

    /**
     * Check if this Entry can load as a grid
     *
     * @param entry  the Entry
     *
     * @return true if grid is supported
     */
    public boolean canLoadAsGrid(Entry entry) {
	boolean debug = false;
	if (isCdmGrid(entry)) {
	    return true;
	}
	if(entry.getTypeHandler().isType("type_point")) {
	    if(debug) System.err.println("\texcluded  by type_point");
	    return false;
	}

	if(debug) System.err.println("canLoadAsGrid:" +entry);
        if (isAggregation(entry)) {
	    if(debug) System.err.println("\tisAgg");
            return true;
        }
        if (isGrads(entry)) {
	    if(debug) System.err.println("\tisGrads");
            return true;
        }
        String type = entry.getTypeHandler().getType();
        if ((type.indexOf("trajectory") >= 0) || (type.indexOf("point") >= 0)
                || (type.indexOf("track") >= 0)) {
	    if(debug) System.err.println("\tisTrack");
            return false;
        }


        if (excludedByPattern(entry, TYPE_GRID)) {
	    if(debug) System.err.println("\texcluded by grid");
            return false;
        }
        if (includedByPattern(entry, TYPE_GRID)) {
	    if(debug) System.err.println("\tincluded by grid");
            return true;
        }
        if ( !canLoadAsCdm(entry)) {
	    if(debug) System.err.println("\tcan't load as cdm");
            return false;
        }

	//a hack
	if(entry.getTypeHandler().isType("cdm_radar_level2")) {
	    return false;
	}

        Boolean b = (Boolean) gridEntries.get(entry.getId());
        if (b == null) {
            boolean ok = false;
            if ( !canLoadEntry(entry)) {
                ok = false;
            } else {
                try {
                    if (doGridPool) {
                        ok = gridPool.containsOrCreate(getPath(entry));
                    } else {
                        ok = (createGrid(getPath(entry)) != null);
                    }
                } catch (Exception ignoreThis) {}
            }
            b = Boolean.valueOf(ok);
            gridEntries.put(entry.getId(), b);
        }

        return b.booleanValue();
    }

    /**
     * Check if this Entry can load as a CDM grid
     *
     * @param entry  the Entry
     *
     * @return true if grid is supported
     */
    public boolean canLoadAsCdmGrid(Entry entry) {
	boolean debug = false;

	//a hack
	if(entry.getTypeHandler().isType("cdm_radar_level2")) {
	    return false;
	}

	if(entry.getTypeHandler().isType("type_point")) {
	    if(debug) System.err.println("\texcluded  by type_point");
	    return false;
	}


	if(debug) System.err.println("canLoadAsCdmGrid:" + entry);
        if (isCdmGrid(entry)) {
	    if(debug) System.err.println("\tisCdmGrid");
            return true;
        }
        if (isAggregation(entry)) {
	    if(debug) System.err.println("\tisAgg");
            return true;
        }
        if (isGrads(entry)) {
	    if(debug) System.err.println("\tisGrads");
            return true;
        }
        if (excludedByPattern(entry, TYPE_CDM_GRID)) {
	    if(debug) System.err.println("\texcluded  by TYPE_CDM_GRID");
            return false;
        }
        if (includedByPattern(entry, TYPE_CDM_GRID)) {
	    if(debug) System.err.println("\tincluded  by TYPE_CDM_GRID");
            return true;
        }
        if ( !canLoadAsCdm(entry)) {
	    if(debug) System.err.println("\texcluded  by canLoadAsCdm");
            return false;
        }


        Boolean b = (Boolean) gridEntries.get(entry.getId());
        if (b == null) {
            boolean ok = false;
            if ( !canLoadEntry(entry)) {
                ok = false;
            } else {
                try {
                    if (doGridPool) {
                        ok = gridPool.containsOrCreate(getPath(entry));
                    } else {
                        ok = (createGrid(getPath(entry)) != null);
                    }
                } catch (Exception ignoreThis) {}
            }
            b = Boolean.valueOf(ok);
            cdmGridEntries.put(entry.getId(), b);
        }

        return b.booleanValue();
    }

    /**
     * Get the NetcdfDataset for the Entry
     *
     * @param entry  the Entry
     * @param path   the path
     *
     * @return  the NetcdfDataset
     */
    public NetcdfDataset getNetcdfDataset(Entry entry, String path) {
        if ( !canLoadAsCdm(entry)) {
            return null;
        }
        extCounter.incr();

        return ncDatasetPool.get(path);
    }

    /**
     * Get the NetcdfDataset from the pool for the given file path
     *
     * @param path  the file path
     *
     * @return  the corresponding NetcdfDataset
     */
    public NetcdfDataset createNetcdfDataset(String path) {
        return ncDatasetPool.get(path);
    }


    /**
     * Return the NetcdfDataset
     *
     * @param path  the path
     * @param ncd   the NetcdfDataset
     */
    public void returnNetcdfDataset(String path, NetcdfDataset ncd) {
        extCounter.decr();
        ncDatasetPool.put(path, ncd);
    }


    /**
     * Get the Entry as a GridDataset
     *
     * @param entry  the Entry
     * @param path   the path
     *
     * @return  the GridDataset
     *
     * @throws Exception problems making GridDataset
     */
    public GridDataset getGridDataset(Entry entry, String path)
            throws Exception {
        if ( !canLoadAsGrid(entry) || !canLoadAsCdmGrid(entry)) {
            return null;
        }
        //Don't cache the aggregations
        //Not now...
        //        if (isAggregation(entry)) {
        //            return GridDataset.open(path);
        //        }
        if (doGridPool) {
            GridDataset dataset = gridPool.get(path);
            return dataset;
            //            String location = dataset
        } else {
            return createGrid(path);
        }
    }

    /**
     * Return the GridDataset back to the pool
     *
     * @param path  the path
     * @param ncd   The GridDataset
     */
    public void returnGridDataset(String path, GridDataset ncd) {
        if (doGridPool) {
            gridPool.put(path, ncd);
        }
    }

    /**
     * Get the TrajectoryDataset
     *
     *
     * @param entry _more_
     * @param path  the path to the file
     *
     * @return  the Trajectory Dataset
     */
    /*
    private TrajectoryObsDataset getTrajectoryDataset(String path) {
        return trajectoryPool.get(path);
    }
    */

    /**
     * Return the TrajectoryDataset to the pool
     *
     * @param path  the file path
     * @param tod   the dataset
     */
    /*
    private void returnTrajectoryDataset(String path,
                                        TrajectoryObsDataset tod) {
        trajectoryPool.put(path, tod);
    }
    */


    /**
     * Get the Entry as a point dataset
     *
     * @param entry  the Entry
     * @param path   the path
     *
     * @return  the point dataset
     */
    public FeatureDatasetPoint getPointDataset(Entry entry, String path) {
        if ( !canLoadAsPoint(entry)) {
            return null;
        }

        return pointPool.get(path);
    }

    /**
     * Return the point dataset to the pool
     *
     * @param path  the path
     * @param ncd   the point dataset
     */
    public void returnPointDataset(String path, FeatureDatasetPoint ncd) {
        pointPool.put(path, ncd);
    }


    /**
     * Get the path to the data
     *
     * @param entry  the Entry
     *
     * @return the path
     *
     * @throws Exception problemo
     */
    public String getPath(Entry entry) throws Exception {
        return getPath(null, entry);
    }


    /**
     * Get the path for the Entry
     *
     *
     * @param request the Request
     * @param entry   the Entry
     *
     * @return   the path
     *
     * @throws Exception problem getting the path
     */
    public String getPath(Request request, Entry entry) throws Exception {
        String location;
        if (entry.getType().equals(OpendapLinkTypeHandler.TYPE_OPENDAPLINK)) {
            Resource resource = entry.getResource();
            location = resource.getPath();
            String ext = IOUtil.getFileExtension(location).toLowerCase();
            if (ext.equals(".html") || ext.equals(".das")
                    || ext.equals(".dds")) {
                location = IOUtil.stripExtension(location);
            }
        } else if (isAggregation(entry)) {
            GridAggregationTypeHandler gridAggregation =
                (GridAggregationTypeHandler) entry.getTypeHandler();
            long[] timestamp = { 0 };
            location = gridAggregation.getNcmlFile(request, entry,
                    timestamp).toString();
            // Something must be fixed to check if its empty
        } else {
            location = getStorageManager().getFastResourcePath(entry);
        }
        getStorageManager().checkPath(location);

        //        System.err.println("nd:" + metadataList);
        String ncml         = null;
        String ncmlFileName = null;

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                ContentMetadataHandler.TYPE_ATTACHMENT, true);
        if (metadataList != null) {
            for (Metadata metadata : metadataList) {
                String  fileAttachment = metadata.getAttr1();
                boolean isNcml         = fileAttachment.endsWith(SUFFIX_NCML);
                boolean isCtl          = fileAttachment.endsWith(SUFFIX_CTL);
                if (isNcml || isCtl) {
                    File templateNcmlFile =
                        new File(IOUtil
                            .joinDir(getRepository().getStorageManager()
                                .getEntryDir(metadata.getEntryId(),
                                             false), metadata.getAttr1()));
                    ncml = getStorageManager().readSystemResource(
                        templateNcmlFile);
                    if ( !isNcml) {
                        int dsetIdx = ncml.indexOf("${location}");
                        if (dsetIdx < 0) {
                            ncml = Pattern.compile(
                                "^dset.*$",
                                Pattern.MULTILINE
                                | Pattern.CASE_INSENSITIVE).matcher(
                                    ncml).replaceAll("DSET " + location);
                        }
                    }
                    //                System.err.println("ncml:" + ncml);
                    //Use the last modified time of the ncml file so we pick up any updated file
                    String dttm = templateNcmlFile.lastModified() + "";
                    ncmlFileName = dttm + "_" + entry.getId() + "_"
                                   + metadata.getId() + (isNcml
                            ? SUFFIX_NCML
                            : SUFFIX_CTL);

                    break;
                }
            }
        }


        if (ncml == null) {
            String property =
                entry.getTypeHandler().getTypeProperty("netcdf.ncml",
                    (String) null);
            if (property != null) {
                ncml = getStorageManager().readUncheckedSystemResource(
                    property);
                ncmlFileName = IOUtil.getFileTail(property);
            }
        }

        if (ncml != null) {
            ncml = ncml.replace("${location}", location);
            File ncmlFile = getStorageManager().getScratchFile(ncmlFileName);
            IOUtil.writeBytes(ncmlFile, ncml.getBytes());
            location = ncmlFile.toString();
        }


        return location;
    }

    /**
     * Format a date
     *
     * @param request  the request
     * @param date     the date object (CalendarDate or Date)
     *
     * @return the formatted date
     */
    public String formatDate(Request request, Object date) {
        if (date == null) {
            return BLANK;
        }
        if (date instanceof CalendarDate) {
            String dateFormat = getRepository().getProperty(PROP_DATE_FORMAT,
                                    DateHandler.DEFAULT_TIME_FORMAT);

            return new CalendarDateFormatter(dateFormat).toString(
                (CalendarDate) date);
        } else if (date instanceof Date) {
            return getDateHandler().formatDate(request, (Date) date);
        } else {
            return date.toString();
        }
    }



    public  void setDates(Request request,
			  List<CalendarDate> allDates,
			  CalendarDate[]     dates) {
        String   calString = request.getString(ARG_CALENDAR, null);
        if (allDates.isEmpty()) return;
	if (calString == null) {
	    calString = allDates.get(0).getCalendar().toString();
	}

	if (request.defined(ARG_FROMDATE+SUFFIX_EXACT)) {
	    dates[0] = CdmManager.closest(CalendarDate.of(Utils.parseDate(request.getString(ARG_FROMDATE+SUFFIX_EXACT))),
					  allDates);
	} else  if (request.defined(ARG_FROMDATE)) {
	    String fromDateString = request.getString(ARG_FROMDATE,
						      formatDate(request,
								 allDates.get(0)));
	    dates[0] = CalendarDate.parseISOformat(calString, fromDateString);
	} else {
	    dates[0] = allDates.get(0);
	}
	if (request.defined(ARG_TODATE+SUFFIX_EXACT)) {
	    dates[1] = CdmManager.closest(CalendarDate.of(Utils.parseDate(request.getString(ARG_TODATE+SUFFIX_EXACT))),
					  allDates);
	} else if (request.defined(ARG_TODATE)) {
	    String toDateString = request.getString(ARG_TODATE,
						    formatDate(request,
							       allDates.get(allDates.size()
									    - 1)));
	    dates[1] = CalendarDate.parseISOformat(calString,
						   toDateString);
	} else {
	    dates[1] = allDates.get(allDates.size() - 1);
	}
        //have to have both dates
        if ((dates[0] != null) && (dates[1] == null)) {
            dates[0] = null;
        }
        if ((dates[1] != null) && (dates[0] == null)) {
            dates[1] = null;
        }


    }



    public static CalendarDate closest(CalendarDate date, List<CalendarDate> dates) {
	if(dates==null) return null;
	CalendarDate closest=null;
	long min=0;
	for(int i=0;i<dates.size();i++) {
	    long diff = Math.abs(date.getDifferenceInMsecs(dates.get(i)));
	    if(i==0 || diff<min) {
		min=diff;
		closest=dates.get(i);
	    }
	}
	return closest;
    }

    /**
     * Make a time widget for grid subsetting
     *
     * @param request  the Request
     * @param dates    the list of dates
     * @param sb       the HTML to add to
     */
    public void addTimeWidget(Request request, List<CalendarDate> dates,
                               StringBuffer sb) {
        long millis = System.currentTimeMillis();
        if (dates == null ||  dates.size() ==0) return;
	CalendarDate cd  = dates.get(0);
	Calendar     cal = cd.getCalendar();
	if (cal != null) {
	    sb.append(HU.hidden(ARG_CALENDAR, cal.toString()));
	}
	List formattedDates = new ArrayList();
	formattedDates.add(new TwoFacedObject("---", ""));
	for (CalendarDate date : dates) {
	    formattedDates.add(formatDate(request, date));
	}
	String fromDate = request.getUnsafeString(ARG_FROMDATE, "");
	String toDate   = request.getUnsafeString(ARG_TODATE, "");
	
	if(formattedDates.size()<1000) {
	    HU.formEntry(sb, msgLabel("Time Range"),
			 HU.select(ARG_FROMDATE, formattedDates, fromDate)
			 + HU.SPACE+HU.img(getIconUrl(ICON_ARROW))+HU.SPACE
			 + HU.select(ARG_TODATE, formattedDates, toDate));
	    
	    HU.formEntry(sb,"",HU.div("Or select specific times",HU.clazz("ramadda-form-help")));
	} else {
	    HU.formEntry(sb,"",HU.div("Select specific times",HU.clazz("ramadda-form-help")));
	    HU.formEntry(sb,"",HU.b("Start date: ") + formattedDates.get(1)
			 + HU.SPACE+HU.img(getIconUrl(ICON_ARROW))+HU.SPACE +
			 formattedDates.get(formattedDates.size()-1)+HU.SPACE +
			 "# times: " +(formattedDates.size()-1));
	}

	HU.formEntry(sb,   msgLabel("Times"),
		     HU.input(ARG_FROMDATE+CdmManager.SUFFIX_EXACT, "",HU.attrs("id",ARG_FROMDATE+CdmManager.SUFFIX_EXACT,"placeholder","yyyy-MM-dd HH:mm"))
		     + HU.SPACE+HU.img(getIconUrl(ICON_ARROW))+HU.SPACE +
		     HU.input(ARG_TODATE+CdmManager.SUFFIX_EXACT, "",HU.attrs("id",ARG_TODATE+CdmManager.SUFFIX_EXACT,"placeholder","yyyy-MM-dd HH:mm")));
	HU.script(sb,
		  HU.call("HtmlUtils.datePickerInit",HU.squote(ARG_FROMDATE+CdmManager.SUFFIX_EXACT))+"\n"+
		  HU.call("HtmlUtils.datePickerInit",HU.squote(ARG_FROMDATE+CdmManager.SUFFIX_EXACT))+"\n");
    }





    /**
     * get the parameter name from the raw name
     *
     * @param rawname the raw name
     *
     * @return  the parameter name
     */
    public String getParamName(String rawname) {
        String name  = rawname;
        int    index = rawname.indexOf("[unit=");
        if (index >= 0) {
            name = rawname.substring(0, index);
        }

        return name;
    }

    /**
     * Get the parameter unit from the raw name
     *
     * @param rawname  the raw name
     *
     * @return  the unit or null
     */
    private String getUnitFromName(String rawname) {
        String unit  = null;
        int    index = rawname.indexOf("[unit=");
        if (index >= 0) {
            unit = rawname.substring(index + 6, rawname.indexOf("]"));
            unit = unit.replaceAll("\"", "");
        }

        return unit;
    }




    /**
     * Class description
     *
     *
     * @param <KeyType>
     * @param <ValueType>
     *
     * @version        $version$, Thu, Oct 31, '13
     * @author         Enter your name here...
     */
    public static class DatedObjectPool<KeyType,
            ValueType> extends ObjectPool<KeyType, ValueType> {

        /** _more_ */
        private Hashtable<String, Date> fileDate = new Hashtable<String,
                                                       Date>();

        /**
         * _more_
         *
         * @param size _more_
         */
        public DatedObjectPool(int size) {
            super(10);
        }

        /**
         * _more_
         *
         * @param key _more_
         *
         * @return _more_
         */
        public ValueType get(KeyType key) {
            Date      lastOpenTime = fileDate.get(key.toString());
            ValueType object       = super.get(key);
            if (object == null) {
                return null;
            }

            if (lastOpenTime != null) {
                if (new File(key.toString()).lastModified()
                        != lastOpenTime.getTime()) {
                    System.err.println("CDM: Cache is out of date for file: "
                                       + key);
                    removeValue(key, object);
                    object = super.get(key);
                }
            }

            return object;
        }

        /**
         * _more_
         *
         * @param key _more_
         * @param value _more_
         */
        public void put(KeyType key, ValueType value) {
            super.put(key, value);
            File file = new File(key.toString());
            if (file.exists()) {
                Date dttm = new Date(file.lastModified());
                fileDate.put(key.toString(), dttm);
            }
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param prefix _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryFromPath(Request request, String prefix)
            throws Exception {
        String path = request.getRequestPath();
        path = HtmlUtils.urlDecode(path);

        //System.err.println("CdmManager.findEntryFromPath decoded path:" + path);
        boolean doLatest = false;
        path = path.substring(prefix.length());
        if (path.startsWith("/latest")) {
            path     = path.substring("/latest".length());
            doLatest = true;
        }

        path = IOUtil.getFileRoot(path);
        //Check for the dodsC in the path.
        if (path.endsWith("dodsC")) {
            path = IOUtil.getFileRoot(path);
        }
        path = path.replaceAll("\\+", " ");
        Entry entry;

        if (request.exists(ARG_ENTRYID)) {
            entry = getEntryManager().getEntry(request);
        } else {
	    //	    System.err.println("PATH:" + path.substring(1));
            entry = getEntryManager().getEntry(request, path.substring(1));
            if (entry == null) {
                entry = getEntryManager().findEntryFromName(request, null, path);
            }
        }

        if (entry == null) {
            throw new IllegalArgumentException("Could not find entry:"
                    + path);
        }

        if (doLatest && entry.getTypeHandler().isGroup()) {
            List<Entry> entries = getEntryManager().getChildren(request,
                                      entry);
            entries = getEntryUtil().sortEntriesOnDate(entries, true);
            Entry theEntry = null;
            for (Entry child : entries) {
                if (canLoadAsCdm(child)) {
                    theEntry = child;

                    break;
                }
            }
            if (theEntry == null) {
                throw new IllegalArgumentException(
                    "Could not find any CDM child entries when doing latest");
            }
            entry = theEntry;
            System.err.println("OPENDAP: using latest:" + entry.getName());
        }

        return entry;
    }





    /**
     * Get the global disk cache
     * @return the disk cache
     */
    public DiskCache2 getDiskCache2() {
        return diskCache2;
    }



    /**
     * Main for testing
     *
     * @param args  arguments for testing
     *
     * @throws Exception  problems
     */
    public static void main(String[] args) throws Exception {
        Repository repository = new Repository(new String[] {}, 8080);
        repository.initProperties(null);
        CdmDataOutputHandler dop = new CdmDataOutputHandler(repository,
                                       "test");
        CdmManager cdmManager = new CdmManager(repository);
        String[]   types      = {
            TYPE_CDM, TYPE_GRID, TYPE_TRAJECTORY, TYPE_POINT, TYPE_RADAR,
            TYPE_CDM_GRID
        };
        for (String f : args) {
            System.err.println("file:" + f);
            for (String type : types) {
                boolean ok      = cdmManager.hasSuffixForType(f, type, false);
                boolean exclude = cdmManager.hasSuffixForType(f, type, true);
                if ( !ok && !exclude) {
                    System.err.println("\t" + type + ": " + "unknown");
                } else {
                    System.err.println("\t" + type + ": " + "ok? " + ok
                                       + " exclude:" + exclude);
                }
            }
        }
    }





}
