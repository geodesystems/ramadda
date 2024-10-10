/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.harvester;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.FileWrapper;
import org.ramadda.util.PatternHolder;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.geo.Place;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.lang.reflect.*;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;


/**
 * A harvester that looks at the local server file system
 *
 *
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class PatternHarvester extends Harvester /*implements EntryInitializer*/ {



    /** attribute id */
    public static final String ATTR_TYPE = "type";

    public static final String ATTR_LASTGROUPTYPE = "lastgrouptype";    


    /** attribute id */
    public static final String ATTR_DATEFORMAT = "dateformat";

    /** _more_ */
    public static final String ATTR_IGNORE_ERRORS = "ignore_errors";

    /** attribute id */
    public static final String ATTR_FILEPATTERN = "filepattern";

    /** attribute id */
    public static final String ATTR_NOTREE = "notree";

    /** _more_ */
    public static final String ATTR_TOPPATTERN = "toppattern";

    /** attribute id */
    public static final String ATTR_NOTFILEPATTERN = "notfilepattern";
    public static final String ATTR_SKIPTIMECHECK = "skiptimecheck";    

    public static final String ATTR_SIZELIMIT = "sizelimit";
    public static final String ATTR_MAXLEVEL = "maxlevel";

    /** attribute id */
    public static final String ATTR_MOVETOSTORAGE = "movetostorage";

    public static final String ATTR_DELETE_WHEN_FILE_SIZE_DIFFERS = "delete_when_file_size_differs";


    public static final String ATTR_PUSHGEO = "pushgeo";    

    /** _more_ */
    private static final int FILE_CHANGED_TIME_THRESHOLD_MS = 30 * 1000;

    /** _more_ */
    private String dateFormat = "yyyyMMdd_HHmm";

    /** _more_ */
    private List<SimpleDateFormat> sdf;
    //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");


    /** _more_ */
    private List<String> patternNames = new ArrayList<String>();


    /** _more_ */
    private String filePatternString = ".*";

    /** _more_ */
    private String topPatternString = "";


    private String lastGroupType = "";

    /** _more_ */
    private boolean ignoreErrors = false;

    /** _more_ */
    private boolean noTree = false;


    /** _more_ */
    private List<PatternHolder> filePattern;

    /** _more_ */
    private Pattern topPattern;

    /** _more_ */
    private String notfilePatternString = "";

    private boolean skipTimeCheck = false;


    /** _more_ */
    private List<PatternHolder> notfilePattern;


    private double sizeLimit = -1;

    private int maxLevel = -1;

    private boolean pushGeo = false;

    private boolean makeThumbnails = true;

    /** _more_ */
    private boolean moveToStorage = false;

    /** _more_ */
    private boolean deleteWhenFileSizeDiffers = false;

    /** _more_ */
    private List<HarvesterFile> dirs;

    /** _more_ */
    private HashSet<FileWrapper> dirMap = new HashSet<FileWrapper>();

    /** _more_ */
    private HashSet seenFiles = new HashSet();

    /** _more_ */
    private List<String[]> idList;

    /** _more_ */
    private int entryCnt = 0;

    private int nonUniqueCnt = 0;    

    private int skipCnt = 0;


    /** _more_ */
    private int newEntryCnt = 0;

    private int totalAddedEntries = 0;    




    /** _more_ */
    private long lastRunTime = 0;

    /**
     * ctor
     *
     * @param repository _more_
     * @param id harvester id
     *
     * @throws Exception _more_
     */
    public PatternHarvester(Repository repository, String id)
	throws Exception {
        super(repository, id);
        if (groupTemplate.length() == 0) {
            groupTemplate = "${dirgroup}";
        }
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public PatternHarvester(Repository repository, Element element)
	throws Exception {
        super(repository, element);
        if (groupTemplate.length() == 0) {
            groupTemplate = "${dirgroup}";
        }
        init();
    }



    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    private boolean haveProcessedFile(String f) {
        if (seenFiles.contains(f)) {
            return true;
        }

        return false;
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    private void putProcessedFile(String f) {
        //Limit the size to 10000
        if (seenFiles.size() > 10000) {
            seenFiles = new HashSet();
        }
        seenFiles.add(f);
    }

    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    protected void init(Element element) throws Exception {
        super.init(element);

        moveToStorage = XmlUtil.getAttribute(element, ATTR_MOVETOSTORAGE, moveToStorage);
        deleteWhenFileSizeDiffers = XmlUtil.getAttribute(element, ATTR_DELETE_WHEN_FILE_SIZE_DIFFERS,
							 false);

        filePatternString = XmlUtil.getAttribute(element, ATTR_FILEPATTERN,
						 filePatternString);

        ignoreErrors = XmlUtil.getAttribute(element, ATTR_IGNORE_ERRORS,
                                            ignoreErrors);
        noTree = XmlUtil.getAttribute(element, ATTR_NOTREE, noTree);

        topPatternString = XmlUtil.getAttribute(element, ATTR_TOPPATTERN,
						topPatternString);
        lastGroupType = XmlUtil.getAttribute(element, ATTR_LASTGROUPTYPE,
					     lastGroupType);	

    
        notfilePatternString = XmlUtil.getAttribute(element,
						    ATTR_NOTFILEPATTERN, notfilePatternString);
        skipTimeCheck = XmlUtil.getAttribute(element,
					     ATTR_SKIPTIMECHECK, skipTimeCheck);	
        sizeLimit = XmlUtil.getAttribute(element,
					 ATTR_SIZELIMIT, sizeLimit);
        pushGeo = XmlUtil.getAttribute(element,
					 ATTR_PUSHGEO, pushGeo);
        makeThumbnails = XmlUtil.getAttribute(element,
					 ATTR_MAKETHUMBNAILS, makeThumbnails);		
	maxLevel = XmlUtil.getAttribute(element,ATTR_MAXLEVEL, maxLevel);	

        filePattern    = null;
        topPattern     = null;
        notfilePattern = null;
        sdf            = null;
        init();
        dateFormat = XmlUtil.getAttribute(element, ATTR_DATEFORMAT,
                                          dateFormat);
        sdf = null;

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "Server File System";
    }

    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        element.setAttribute(ATTR_FILEPATTERN, filePatternString);
        element.setAttribute(ATTR_TOPPATTERN, topPatternString);
        element.setAttribute(ATTR_LASTGROUPTYPE, lastGroupType);	
        element.setAttribute(ATTR_IGNORE_ERRORS, "" + ignoreErrors);
        element.setAttribute(ATTR_NOTREE, "" + noTree);
        element.setAttribute(ATTR_NOTFILEPATTERN, notfilePatternString);
        element.setAttribute(ATTR_SKIPTIMECHECK, skipTimeCheck+"");	
        element.setAttribute(ATTR_SIZELIMIT, sizeLimit+"");
        element.setAttribute(ATTR_PUSHGEO, pushGeo+"");
        element.setAttribute(ATTR_MAKETHUMBNAILS, makeThumbnails+"");		
        element.setAttribute(ATTR_MAXLEVEL, maxLevel+"");	
        element.setAttribute(ATTR_MOVETOSTORAGE, "" + moveToStorage);
	element.setAttribute(ATTR_DELETE_WHEN_FILE_SIZE_DIFFERS, "" + deleteWhenFileSizeDiffers);	

        element.setAttribute(ATTR_DATEFORMAT, dateFormat);

    }

    @Override
    public String getDefaultTypeHandler() throws Exception {
	return TypeHandler.TYPE_FINDMATCH;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);

        //Reset the seen files
        seenFiles   = new HashSet();

        lastRunTime = 0;
        filePatternString = request.getUnsafeString(ATTR_FILEPATTERN,
						    filePatternString);
        filePattern = null;

        topPatternString = request.getUnsafeString(ATTR_TOPPATTERN,
						   topPatternString);
        topPattern   = null;

        lastGroupType = request.getString(ATTR_LASTGROUPTYPE,
					  lastGroupType);

        ignoreErrors = request.get(ATTR_IGNORE_ERRORS, false);
        noTree       = request.get(ATTR_NOTREE, false);

        skipTimeCheck = request.get(ATTR_SKIPTIMECHECK,false);
        notfilePatternString = request.getUnsafeString(ATTR_NOTFILEPATTERN,
						       notfilePatternString).trim();
        notfilePattern = null;

        sizeLimit = request.get(ATTR_SIZELIMIT, sizeLimit);
	maxLevel = request.get(ATTR_MAXLEVEL, maxLevel);	
        pushGeo = request.get(ATTR_PUSHGEO, false);
        makeThumbnails = request.get(ATTR_MAKETHUMBNAILS, false);	


        sdf            = null;
        init();
        dateFormat = request.getUnsafeString(ATTR_DATEFORMAT, dateFormat);


        if (request.exists(ATTR_MOVETOSTORAGE)) {
            moveToStorage = request.get(ATTR_MOVETOSTORAGE, moveToStorage);
        } else {
            moveToStorage = false;
        }

        if (request.exists(ATTR_DELETE_WHEN_FILE_SIZE_DIFFERS)) {
            deleteWhenFileSizeDiffers = request.get(ATTR_DELETE_WHEN_FILE_SIZE_DIFFERS, deleteWhenFileSizeDiffers);
        } else {
            deleteWhenFileSizeDiffers = false;
        }


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void createEditForm(Request request, StringBuffer sb)
	throws Exception {

        super.createEditForm(request, sb);
        String            extraLabel     = "";
        String            fileFieldExtra = "";

        List<FileWrapper> rootDirs       = getRootDirs();
        for (FileWrapper rootDir : rootDirs) {
            if (!rootDir.exists() && !getStorageManager().isS3(rootDir.toString()) &&
		!rootDir.toString().startsWith("#")) {
                extraLabel = HU.br()
		    + HU.span(
			      msg("Directory does not exist"),
			      HU.cssClass(
					  CSS_CLASS_REQUIRED_LABEL));
                fileFieldExtra = HU.cssClass(CSS_CLASS_REQUIRED);

                break;
            } else if (!getStorageManager().isLocalFileOk(rootDir)
		       && !getStorageManager().isS3(rootDir.toString())) {
                String adminLink =
                    HU.href(getPageHandler().makeHtdocsUrl("/userguide/admin.html#filesystemaccess"),
			    msg("More information"), " target=_HELP");
                extraLabel =
                    HU.br()
                    + HU
		    .span(msg(
			      "You need to add this directory to the file system access list"), HU
			  .cssClass(CSS_CLASS_REQUIRED_LABEL)) + HU.space(2) + adminLink;
                fileFieldExtra = HU.cssClass(CSS_CLASS_REQUIRED);

                break;
            }
        }

        if (rootDirs.size() == 0) {
            extraLabel = HU.br()
		+ HU.span(
			  msg("Required"),
			  HU.cssClass(CSS_CLASS_REQUIRED_LABEL));
            fileFieldExtra = HU.cssClass(CSS_CLASS_REQUIRED);

        }

        sb.append(HU.colspan(formHeader("Look for files"), 2));

        StringBuffer inputText = new StringBuffer();
        for (FileWrapper rootDir : rootDirs) {
            String path = rootDir.toString();
            path = path.replace("\\", "/");
            inputText.append(path);
            inputText.append("\n");
        }

        sb.append(
		  HU.formEntry(
			       msgLabel("Under directories"),
			       HU.textArea(
					   ATTR_ROOTDIR, inputText.toString(), 5, 60,
					   fileFieldExtra.toString()) + extraLabel));


        sb.append(HU.formEntry("","Only harvest the top-level files that match this pattern"));
        sb.append(HU.formEntry(msgLabel("Top directory pattern"),
			       HU.input(ATTR_TOPPATTERN,
					topPatternString,
					HU.SIZE_60)));

        sb.append(HU.formEntry("","Only harvest the descendent files that match one or more of these patterns"));
        sb.append(HU.formEntryTop(msgLabel("File Patterns"),
				  HU.textArea(ATTR_FILEPATTERN,
					      filePatternString,
					      3,60)));
				  

        sb.append(HU.formEntry("","Skip any file that matches any of these patterns"));
        sb.append(HU.formEntryTop(msgLabel("Exclude files that match"),
				  HU.textArea(ATTR_NOTFILEPATTERN,
					      notfilePatternString,
					      3,60)));
        sb.append(HU.formEntryTop("",
				  HU.labeledCheckbox(ATTR_SKIPTIMECHECK,
						     "true",skipTimeCheck,"Skip file time check")));

        sb.append(HU.formEntry(msgLabel("Size limit"),
			       HU.input(ATTR_SIZELIMIT,
					sizeLimit+"",
					HU.SIZE_5) +" (MB)"));
        sb.append(HU.formEntry(msgLabel("Max level"),
			       HU.hbox(HU.input(ATTR_MAXLEVEL,  maxLevel+"", HU.SIZE_5),
				       "How far down the directory hierarchy does the harvester go<br>-1 -&gt; no limit<br>0 -&gt; just files under the main directories<br>1 -&gt; files under sub-directories<br>etc.")));

        sb.append(HU.formEntry("",
			       HU.labeledCheckbox(ATTR_IGNORE_ERRORS,
					   "true",
						  ignoreErrors,"Ignore errors")));

        sb.append(HU.colspan(formHeader("Then create an entry with"),2));
        addBaseGroupSelect(ATTR_BASEGROUP, sb);
	String folderHelp =
                    HU.href(getPageHandler().makeHtdocsUrl("/userguide/harvesters.html#entry"),
			    msg("Help"), " target=_HELP");


        sb.append(HU.formEntry(msgLabel("Folder template"),
			       HU.input(ATTR_GROUPTEMPLATE,
					groupTemplate, HU.SIZE_60) +HU.space(2) + folderHelp));
        sb.append(
		  HU.formEntry(
			       "",
			       HU.labeledCheckbox(ATTR_NOTREE, "true", noTree,
						  "Don't make entry hierarchy from directory tree")));

        sb.append(HU.formEntry(msgLabel("Name template"),
			       HU.input(ATTR_NAMETEMPLATE,
					nameTemplate, HU.SIZE_60)));
        sb.append(HU.formEntry(msgLabel("Description template"),
			       HU.input(ATTR_DESCTEMPLATE,
					descTemplate, HU.SIZE_60)));

        sb.append(HU.formEntry(msgLabel("Default Entry type"),
			       makeEntryTypeSelector(request,
						     getTypeHandler())));

        sb.append(HU.formEntry(msgLabel("Last Group Type"),
			       getRepository().makeTypeSelect(Utils.makeListFromValues(new TwoFacedObject("Default","")),
							      request, ATTR_LASTGROUPTYPE,HU.style("max-width:200px;"),false, lastGroupType,
							      false, null,true)));


	getEntryManager().makeTypePatternsInput(request, ATTR_TYPEPATTERNS,sb,typePatterns);


        sb.append(HU.formEntry(msgLabel("Date format"),
			       HU.input(ATTR_DATEFORMAT,
					dateFormat, HU.SIZE_60)));


        String moveNote =HU.div("Note: This will move the files from their current location to RAMADDA's storage directory",
				HU.cssClass("ramadda-callout ramadda-callout-info"));
	sb.append(HU.formEntry("",moveNote));
        sb.append(HU.formEntry("",
			       HU.labeledCheckbox(ATTR_MOVETOSTORAGE,
					   "true",
						  moveToStorage,"Move file to storage")));

        String deleteNote =HU.div("Note: If the file has already been harvested but has since changed it's size then delete the existing entry and re-harvest",
				  HU.cssClass("ramadda-callout ramadda-callout-info"));
	sb.append(HU.formEntry("",deleteNote));
        sb.append(HU.formEntry("",
			       HU.labeledCheckbox(ATTR_DELETE_WHEN_FILE_SIZE_DIFFERS,
						  "true",
						  deleteWhenFileSizeDiffers,"Delete matching entry and re-add when file size differs")));



	List<String> mtds = new ArrayList<String>();
	mtds.add(HU.labeledCheckbox(ATTR_ADDMETADATA, "true",getAddMetadata(),"Add full"));
	mtds.add(HU.labeledCheckbox(ATTR_ADDSHORTMETADATA, "true", getAddShortMetadata(), "Just spatial/temporal"));
	mtds.add(HU.labeledCheckbox(ATTR_MAKETHUMBNAILS, "true", makeThumbnails, "Make thumbnails"));
	mtds.add(HU.labeledCheckbox(ATTR_PUSHGEO, "true", pushGeo, "Set geo from parent"));	
	HU.formEntry(sb, msgLabel("Metadata"),  Utils.join(mtds, HU.space(4)));


	addAliasesEditForm(request, sb);



        sb.append(HU.formEntry(msgLabel("Owner"),
			       HU.input(ATTR_USER,
					(getUserName() != null)
					? getUserName().trim()
					: "", HU.SIZE_30)));


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandler _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeEntryTypeSelector(Request request,
                                        TypeHandler typeHandler)
	throws Exception {
        return getPageHandler().makeFileTypeSelector(request, typeHandler,
						     false);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private List<SimpleDateFormat> getSDF() {
        if (sdf == null) {
            sdf = new ArrayList<SimpleDateFormat>();
            if ((dateFormat != null) && (dateFormat.length() > 0)) {
                for (String tok :
			 (List<String>) Utils.split(dateFormat, ",", true,
						    true)) {
                    sdf.add(new SimpleDateFormat(tok));
                }
            } else {
                sdf.add(new SimpleDateFormat("yyyyMMdd_HHmm"));
            }
            for (SimpleDateFormat format : sdf) {
                format.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
            }
        }

        return sdf;

    }


    /**
     * _more_
     */
    private void init() {

        if ((notfilePattern == null) && (notfilePatternString.length() > 0)) {
	    notfilePattern = PatternHolder.parseLines(notfilePatternString);
        }

        if ((topPattern == null) && (topPatternString.length() > 0)) {
            topPattern = Pattern.compile(topPatternString);
        }

        if ((filePattern == null) && (filePatternString != null)
	    && (filePatternString.length() > 0)) {

            patternNames = new ArrayList<String>();
            String pattern = Utils.extractPatternNames(filePatternString,
						       patternNames);
            filePattern = PatternHolder.parseLines(pattern);
        }
    }





    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        String dirMsg = "";
        if (dirs != null) {
            if (dirs.size() == 0) {
                dirMsg = "No directories found<br>";
            } else {
                List<HarvesterFile> dirsToUse = dirs;
                dirMsg = dirsToUse.size() + " directories";
                String suffix = "";
                if (dirsToUse.size() > 50) {
                    ArrayList<HarvesterFile> subset =
                        new ArrayList<HarvesterFile>();
                    while (subset.size() < 50) {
                        subset.add(dirsToUse.get(subset.size()));
                    }
                    dirsToUse = subset;
                    suffix = "<b>... and " + (dirs.size() - 50) + " more</b>";
                }
                StringBuffer dirBlock = new StringBuffer();
                dirBlock.append(HU.insetDiv(Utils.wrap(dirsToUse,"<div>","</div>"), 0, 20, 0, 0));
                dirBlock.append(suffix);
                dirMsg = HU.makeShowHideBlock(dirMsg,
					      dirBlock.toString(), false);
            }
        }

	List<String> entryMsg = new ArrayList<String>();
	String msg = "";
        if (entryCnt > 0) {
            long   dt      = ((getActive()
                               ? System.currentTimeMillis()
                               : endTime) - startTime) / 1000;
            String timeMsg = "";

            if (dt < 60) {
                timeMsg = dt + " seconds ";
            } else {
                double ePerM = (entryCnt / ((double) dt / 60.0));
                ePerM = (int) ePerM;
                timeMsg = ((int) (100 * dt / 60.0)) / 100.0 + " minutes "
		    + ePerM + " entries/minute";
            }
	    msg +=  "found " + entryCnt + Utils.plural(entryCnt," potential file") + ".";
	    msg += " " + newEntryCnt + Utils.plural(newEntryCnt," new file")+".";
	    if(nonUniqueCnt>0) {
		msg += " " + nonUniqueCnt+" " + Utils.plural(nonUniqueCnt," file") +" already added.";
	    }
	}
	if(skipCnt>0) {
	    msg+=" " + skipCnt+" " + Utils.plural(skipCnt,"file")+" skipped.";
	}
	if(msg.length()>0) {
            entryMsg.add(msg);
	}


	if(totalAddedEntries>0) {
	    entryMsg.add("Created " + totalAddedEntries +(totalAddedEntries==1?" new entry":" new entries"));
	}
	entryMsg.addAll(otherMsgs);


        List<FileWrapper> rootDirs = getRootDirs();


	String cs = "";
	if(getActive()) {
	    cs = currentStatus;
	    if(cs.length()>0) {
		cs = HU.b("Current status:") + HU.div(cs,HU.style("margin-left:10px;"));
	    }
	} else {
	    if(dirMsg.length()>0 && nonUniqueCnt == 0 && entryCnt==0 && status.length()==0 && entryMsg.size()==0) {
		cs = "No new files found";
	    }
	}


        return "Directory:" + Utils.join( rootDirs,"<br>") + "<br>"
	    + dirMsg + Utils.join(entryMsg,"<br>") + HU.div(status.toString()) + cs;
    }

    /**
     * _more_
     *
     * @param dir _more_
     */
    private void removeDir(HarvesterFile dir) {
        dirs.remove(dir);
        dirMap.remove(dir.getFile());
    }

    /**
     * _more_
     *
     * @param dir _more_
     * @param rootDir _more_
     *
     * @return _more_
     */
    private HarvesterFile addDir(FileWrapper dir, FileWrapper rootDir) {
        HarvesterFile fileInfo = new HarvesterFile(dir, rootDir, true);
        dirs.add(fileInfo);
        dirMap.add(dir);
        return fileInfo;
    }

    /**
     * _more_
     *
     * @param dir _more_
     *
     * @return _more_
     */
    private boolean hasDir(FileWrapper dir) {
        return dirMap.contains(dir);
    }




    /**
     * _more_
     *
     *
     * @param timestamp _more_
     * @throws Exception _more_
     */
    @Override
    protected void runInner(int timestamp) throws Exception {
        logHarvesterInfo("******************* Starting ****************");
        if ( !canContinueRunning(timestamp)) {
            logHarvesterInfo("stopping in runInner");
            return;
        }

	skipCnt  = 0;
        entryCnt    = 0;
	nonUniqueCnt  = 0;
        newEntryCnt = 0;
	totalAddedEntries=0;
	otherMsgs = new ArrayList<String>();
	dirMap = new HashSet<FileWrapper>();
        status = new StringBuffer("Looking for initial directory listing");
        long tt1 = System.currentTimeMillis();
        dirs = new ArrayList<HarvesterFile>();

        //Call init so we get the filePattern, etc.
        init();


        Hashtable<String, Entry> entriesMap = new Hashtable<String, Entry>();
        Entry  baseGroup = getBaseGroup();
	if(baseGroup!=null) entriesMap.put("*",baseGroup);

        List<FileWrapper>        rootDirs   = getRootDirs();
        for (FileWrapper rootDir : rootDirs) {
            logHarvesterInfo("Looking for initial directory listing:"
                             + rootDir);
            if ( !rootDir.exists()) {
                logHarvesterInfo("Root directory does not exist:" + rootDir);
            }
            dirs.add(new HarvesterFile(rootDir, rootDir, true));
            dirs.addAll(collectDirs(rootDir, topPattern, timestamp));
            if ( !canContinueRunning(timestamp)) {
                return;
            }
        }


        logHarvesterInfo("Found " + dirs.size()
                         + " directories under top-level dir");

        long tt2 = System.currentTimeMillis();
        status = new StringBuffer("");
        //        System.err.println("took:" + (tt2 - tt1) + " to find initial dirs:"
        //                           + dirs.size());

        for (HarvesterFile dir : dirs) {
            dirMap.add(dir.getFile());
        }

        int cnt = 0;


        while (canContinueRunning(timestamp)) {
            long t1 = System.currentTimeMillis();
            logHarvesterInfo("Start scanning");
            printTab = "\t";
	    resetStatus();
            harvestEntries((cnt == 0), timestamp, entriesMap);
            printTab = "";
            logHarvesterInfo("Done scanning");
            lastRunTime = System.currentTimeMillis();
            long t2 = System.currentTimeMillis();
            cnt++;
            //            System.err.println("found:" + entries.size() + " files in:"
            //                               + (t2 - t1) + "ms");
            if ( !getMonitor()) {
                logHarvesterInfo("Ran one time only. Exiting loop");
                break;
            }

            status.append("Done. Sleeping until: "); 
            logHarvesterInfo("Sleeping for " + getSleepMinutes()
                             + " minutes");
            doPause();
            status = new StringBuffer();
        }
        logHarvesterInfo("***********  Done running **************");
    }


	


    /**
     * _more_
     *
     * @param rootDir _more_
     * @param topDirPattern _more_
     * @param timestamp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<HarvesterFile> collectDirs(final FileWrapper rootDir,
                                           final Pattern topDirPattern,
                                           final int timestamp)
	throws Exception {
	final PatternHarvester thisHarvester = this;
        final List<HarvesterFile> dirs       = new ArrayList();
	final boolean hasLevel = thisHarvester.maxLevel>=0;
	//	System.err.println("ViewFile max level:" +thisHarvester.maxLevel);
        FileWrapper.FileViewer    fileViewer = new FileWrapper.FileViewer() {
		@Override
		public int viewFile(int level, FileWrapper f,FileWrapper[] siblings) throws Exception {
		    if (!f.isDirectory()) {
			return DO_CONTINUE;
		    }			
		    //		    System.err.println("\tlevel:" + level +" file:" + f);

		    if(hasLevel && level>=thisHarvester.maxLevel)   {
			return DO_DONTRECURSE;
		    }
		    if ( !canContinueRunning(timestamp)) {
			return DO_STOP;
		    }

		    if (f.getName().startsWith(".")) {
			return DO_DONTRECURSE;
		    }
		    if ( !okToRecurse(f)) {
			return DO_DONTRECURSE;
		    }
		    dirs.add(new HarvesterFile(f, rootDir, true));
		    status = new StringBuffer(
					      "Looking for initial directory listing<br>Found:"
					      + dirs.size() + " directories");
		    for(int i=dirs.size()-10;i<dirs.size();i++) {
			if(i<0) continue;
			status.append("<br>" + dirs.get(i));
		    }
		    if (dirs.size() > 100) {
			logHarvesterInfo("Collected " + dirs.size()
					 + " dirs");
		    }
		    return DO_CONTINUE;
		}
	    };
        FileWrapper.walkDirectory(rootDir, fileViewer);
	//	System.err.println("DIRS:" + dirs);
        return dirs;
    }


    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean okToRecurse(FileWrapper f) throws Exception {
        if (topPattern != null) {
            List<FileWrapper> rootDirs   = getRootDirs();
            FileWrapper       parentFile = f.getParentFile();
            for (FileWrapper rootDir : rootDirs) {
                if (parentFile.equals(rootDir)) {
                    Matcher matcher = topPattern.matcher(f.getName());
                    if ( !matcher.find()) {
                        //                        System.err.println("No match:" + f);
                        return false;
                    } else {
                        //                        System.err.println("Match:" + f);
                    }
                } else {
                    //                    System.err.println("Not parent:" + parentFile + " " + rootDir);
                }
            }
        }

        return HarvesterFile.okToRecurse(f, this);
    }


    /**
     * _more_
     *
     * @param firstTime _more_
     * @param timestamp _more_
     * @param entriesMap _more_
     *
     *
     * @throws Exception _more_
     */
    private void harvestEntries(boolean firstTime, int timestamp,
                                Hashtable<String, Entry> entriesMap)
	throws Exception {

        long t1 = System.currentTimeMillis();
        //Initialize the list of entry ids for post-processing
        idList = new ArrayList<String[]>();

        List<Entry>         needToAdd = new ArrayList<Entry>();
        List<HarvesterFile> tmpDirs   = new ArrayList<HarvesterFile>(dirs);
        entryCnt    = 0;
	skipCnt = 0;
	nonUniqueCnt = 0;
        newEntryCnt = 0;
	totalAddedEntries = 0;

        boolean checkIfDirHasChanged = true;

        //For now lets always look at each dir even if it hasn't changed
        //It seems as though sometimes files come in or have been changed
        //and we skip them then we miss them the next time through
        checkIfDirHasChanged = true;

        boolean replaceIfFileChanged = true;
        boolean replaceInPlace       = true;

        Request request              = getRequest();
        //Iterate by size because we can add new dirs to the list
	boolean anyChanged = false;
        for (int fileIdx = 0; fileIdx < tmpDirs.size(); fileIdx++) {
            printTab = "\t";
            HarvesterFile dirInfo = tmpDirs.get(fileIdx);

            if (!dirInfo.exists()) {
                logHarvesterInfo("Directory:" + dirInfo.getFile()   + " * does not exist *");
                removeDir(dirInfo);
                continue;
            }
            boolean directoryChanged = dirInfo.hasChanged();
            if (checkIfDirHasChanged && !firstTime && !directoryChanged) {
		logHarvesterInfo("Directory:"  + dirInfo.getFile() +" has not changed");
                continue;
            }

	    anyChanged = true;
            FileWrapper[] files = dirInfo.getFile().doListFiles();
            dirInfo.clearAddedFiles();
            if ((files == null) || (files.length == 0)) {
                logHarvesterInfo("Directory:" + dirInfo.getFile() + " * no files *");
                continue;
            }
	    //	    System.err.println("dir:"+ dirInfo);

            logHarvesterInfo("Directory:" + dirInfo.getFile() + "  found " + files.length + " files");
            printTab = "\t\t";
            files    = FileWrapper.sortFilesOnName(files);
            for (FileWrapper f : files) {
		//		System.err.println("FILE:" +f.getName() +" level:" + f.getLevel());
		if(maxLevel>0 && f.getLevel()>this.maxLevel)   {
		    //		    System.err.println("TOO DEEP:" + f.getName());
		    //		    continue;
		}

		
                if (f.isDirectory()) {
		    continue;
		    /*

                    //If this is a directory then check if we already have it 
                    //in the list. If not then add it to the main list and the local list
                    if ( !hasDir(f)) {
                        if (this.okToRecurse(f)) {
                            logHarvesterInfo("New directory:" + f);
                            tmpDirs.add(addDir(f, dirInfo.getRootDir()));
                        }
                    }
                    continue;
		    */
                }
		//		System.err.println("\tfile:" + f);

		//Check if it matches the pattern, etc
		if(!isFileOk(f)) {
		    //		    System.err.println("SKIP:" + f);
		    skipCnt++;
		    continue;
		}

		//Check if it has changed recently
                long fileTime = f.lastModified();
                //time diff threshold = 1 minute
                long now = new Date().getTime();
                if (!skipTimeCheck && (now - fileTime) < FILE_CHANGED_TIME_THRESHOLD_MS) {
		    skipCnt++;
		    int diff = (int)((now-fileTime)/1000.0);
		    String message = "Skipping recently modified file: " + f
			+ " date:" + new Date(fileTime);
		    String htmlMessage=message+" ready in " + (FILE_CHANGED_TIME_THRESHOLD_MS/1000-diff)  +" seconds";
		    otherMsgs.add(htmlMessage);
                    logHarvesterInfo(message);
                    //Reset the state that gets set and checked in hasChanged so we can return to this dir
                    dirInfo.reset();
                    continue;
                }

                Entry entry = null;
                try {
                    entry = processFile(dirInfo, f, entriesMap);
                } catch (Exception exc) {
                    appendError("Error processing file:" + f);
                    logHarvesterError("Error creating entry:" + f, exc);
                    if ( !ignoreErrors) {
                        throw (Exception) LogUtil.getInnerException(exc);
                    }
                    appendError(getStack(exc));
                    continue;
                }
                if (entry == null) {
		    //                    logHarvesterInfo("No entry created for file: " + f);
                    continue;
                }


                entryCnt++;
		//		System.err.println(entryCnt +" " + f);
                if (getTestMode()) {
                    if (entryCnt >= getTestCount()) {
                        return;
                    }

                    continue;
                }
                List<Entry> nonUnique = new ArrayList<Entry>();
                List<Entry> uniqueEntries =
                    getEntryManager().getUniqueEntries(
						       new Utils.TypedList<Entry>(entry), nonUnique);
		nonUniqueCnt+= nonUnique.size();
                for (Entry found : nonUnique) {
                    String existingId = (String) found.getTransientProperty(
									    "existingEntryId");
                    if (replaceIfFileChanged && (existingId != null)) {
                        Entry existing = getEntryManager().getEntry(request,
								    existingId);
                        if (existing != null) {
                            //                      System.err.println("existing:" + existing.getResource() +" " + existing.getResource().getFileSizeRaw() +" found:" +  found.getResource().getFileSizeRaw());

			    if (deleteWhenFileSizeDiffers &&
				existing.getResource().getFileSizeRaw()
				!= found.getResource().getFileSizeRaw()) {
                                uniqueEntries.add(found);
                                getEntryManager().deleteEntry(request,
							      existing);
                                logHarvesterInfo("File sizes are different. replacing entry:" + existing);
                                if (replaceInPlace) {
                                    found.setId(existing.getId());
                                }
                                continue;
                            }
                        } else {
                            logHarvesterInfo("Entry already exists:"
                                             + found.getResource());
                        }
                    }
                }
                newEntryCnt += uniqueEntries.size();
                needToAdd.addAll(uniqueEntries);
                for (Entry newEntry : uniqueEntries) {
                    logHarvesterInfo("creating new entry:" + newEntry.getResource());
                    dirInfo.addFile(newEntry.getResource().getPath());
                }
                if (needToAdd.size() > 999) {
                    addEntries(needToAdd, timestamp, entriesMap);
                    needToAdd = new ArrayList<Entry>();
                }
            }
            if ( !canContinueRunning(timestamp)) {
                logHarvesterInfo("stopping harvest");

                return;
            }
	}

	if(!anyChanged) {
	    status.append("No directories changed<br>");
	}

        if (needToAdd.size() > 0) {
            addEntries(needToAdd, timestamp, entriesMap);
        }

	getEntryManager().parentageChanged(getBaseGroup());
        if ( !canContinueRunning(timestamp)) {
            return;
        }



        currentStatus = "Done processing.";
	if(idList.size()>0) {
	    currentStatus+=" Calling convertIdsFromImport on " + idList.size()+" new entries";
	}
        for (String[] tuple : idList) {
            String newId    = tuple[0];
            Entry  newEntry = getEntryManager().getEntry(request, newId);
            if (newEntry == null) {
                continue;
            }
            if (newEntry.getTypeHandler().convertIdsFromImport(newEntry,
							       idList)) {
                getEntryManager().updateEntry(getRequest(), newEntry);
            }
        }
        idList = null;

    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param timestamp _more_
     * @param entriesMap _more_
     *
     * @throws Exception _more_
     */
    private void addEntries(List<Entry> entries, int timestamp,
                            Hashtable<String, Entry> entriesMap)
	throws Exception {
        if (getTestMode() || (entries.size() == 0)) {
            return;
        }
        List<Entry> entriesToAdd = new ArrayList<Entry>();
        status = new StringBuffer();
        status.append("Initializing " + entries.size() + " new "
                      + ((entries.size() > 1)
                         ? "entries"
                         : "entry") + "<br>");
        int     cnt     = 0;
        Request request = getRequest();
        for (Entry newEntry : entries) {
            String originalId =
                (String) newEntry.getProperty(ATTR_ORIGINALID);
            if ( !canContinueRunning(timestamp)) {
                return;
            }
            try {
                newEntry.getTypeHandler().initializeEntryFromHarvester(
								       request, newEntry, true);
            } catch (Exception exc) {
                appendError("Error processing file:"
                            + newEntry.getResource().getPath());
                logHarvesterInfo("Error initializing:"
                                 + newEntry.getResource().getPath());
                if ( !ignoreErrors) {
                    throw (Exception) LogUtil.getInnerException(exc);
                }
		appendError(getStack(exc));
                continue;
            }
            //System.err.println("createEntry:" + newEntry);
            if (originalId != null) {
                entriesMap.put(originalId, newEntry);
            }

	    if(originalId!=null)
		idList.add(new String[] {originalId, newEntry.getId()});
            entriesToAdd.add(newEntry);
            cnt++;
            currentStatus = "Initialized " + cnt + " of " + entries.size()
		+ " entries";
        }
        currentStatus = "Done initializing entries";
        if (getAddMetadata() || getAddShortMetadata()) {
            currentStatus = "Adding metadata";
            int count = 0;
            for (Entry entry : entriesToAdd) {
                List<Entry> tmpList = new ArrayList<Entry>();
                tmpList.add(entry);
                count++;
		long t1= System.currentTimeMillis();
		Request myRequest = getRequest();
		myRequest.put(ATTR_MAKETHUMBNAILS,""+makeThumbnails);
                getEntryManager().addInitialMetadata(myRequest, tmpList, getAddMetadata(),
						     getAddShortMetadata());
		long t2= System.currentTimeMillis();
		//		System.err.println("addInitialMetadata:" + entry +" time:" + (t2-t1));
                currentStatus = "Added metadata for " + count + " entries. "
		    + (entriesToAdd.size() - count)
		    + " more entries to process";
		if ( !canContinueRunning(timestamp)) {
		    return;
		}
            }
	}
        currentStatus = "";
        status        = new StringBuffer();
        logHarvesterInfo("Inserting " + entriesToAdd.size() + " new entries");
        currentStatus="Inserting entries";
        getEntryManager().addNewEntries(getRequest(), entriesToAdd);
	totalAddedEntries+=entriesToAdd.size();
        currentStatus = "Done inserting " + entriesToAdd.size()
                      + " entries";
    }




    /**
     * _more_
     *
     * @param entry _more_
     */
    public void initEntry(Entry entry) {
	try {
	    if (getAddMetadata() || getAddShortMetadata()) {
		if(!entry.hasLocationDefined(getRequest())) {
		    //see if the group name is a county, city, state, etc
		    Place place;
		    String name =entry.getName();
		    /*
		    a bit of a hack for when we have something like:
		    colorado
		         boulder county
		    */
		    place = GeoUtils.getLocationFromAddress(GeoUtils.PREFIX_STATE+name,null);
		    //		    System.err.println("name:" + name +" p:" + place);
		    if(place==null) {
			//			System.err.println("Name:" + name +" parent:" +entry.getParentEntry());
			if(name.toLowerCase().indexOf("county")>=0 && entry.getParentEntry()!=null) {
			    String parentName  = entry.getParentEntry().getName();
			    place = GeoUtils.getLocationFromAddress(GeoUtils.PREFIX_COUNTY+name+","+parentName,null);
			    //			    System.err.println("trying:" + GeoUtils.PREFIX_COUNTY+name+","+parentName+ " " + place);
			}
		    }
		    //		System.err.println("initnewgroup:" + entry +" " + place);
		    if(place!=null) {
			entry.setLocation(place.getLatitude(), place.getLongitude());
		    }
		}
	    }

	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }


    private EntryInitializer groupInitializer;
    private EntryInitializer getGroupInitializer() {
	final PatternHarvester theHarvester = this;
	if(groupInitializer==null)
	    groupInitializer =
		new EntryInitializer() {
		    @Override
		    public void initEntry(Entry entry) {
			//System.err.println("\tNEW GROUP:" + entry);
			theHarvester.initEntry(entry);
		    }
		};
	return groupInitializer;
    }

    private Hashtable<String,File> getFilesMap(File file) {
	Hashtable<String,File> filesMap = null;
	if(!file.isDirectory()) file = file.getParentFile();
	for(File f: file.listFiles()) {
	    if(f.getName().startsWith(".metadata_")) {
		if(filesMap==null)
		    filesMap = new Hashtable<String,File>();		    
		String name = f.getName().substring(".metadata_".length());
		filesMap.put(name, f);
	    }
	}
	return filesMap;
    }


    /**
     * _more_
     *
     * @param parentFile _more_
     * @param parentGroup _more_
     * @param dirToks _more_
     * @param makeGroup _more_
     * @param entriesMap _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getDirNames(FileWrapper parentFile, Entry parentGroup,
                               List<String> dirToks, boolean makeGroup,
                               Hashtable<String, Entry> entriesMap)
	throws Exception {
        Request request = getRequest();
        //        if(dirToks.size()==0) return parentFile.toString();
        List names = new ArrayList();
        for (int i = 0; i < dirToks.size(); i++) {
            String filename = (String) dirToks.get(i);
            FileWrapper file = FileWrapper.createFileWrapper(parentFile + "/"
							     + filename);
            if ( !file.exists()) {
                file = FileWrapper.createFileWrapper(parentFile + "/"
						     + filename.replaceAll(" ", "_"));
            }
            Entry template =
                getEntryManager().getTemplateEntry(file.getFile(),
						   entriesMap,getFilesMap(file.getFile()));
            String name = ((template != null)
                           ? template.getName()
                           : filename);
            if ( !Utils.stringDefined(name)) {
                name = filename;
            }
	    name = getName(name,true);
            if (makeGroup && (parentGroup != null)) {
                Entry group = getEntryManager().findEntryFromName(request,parentGroup, name,false,null,null,getGroupInitializer());
                if ((group == null) && (name.indexOf("_") >= 0)) {
                    String blankName = name.replaceAll("_", " ");
                    group = getEntryManager().findEntryFromName(request,parentGroup, blankName,false,null,null,getGroupInitializer());
                    if (group != null) {
                        name = blankName;
                    }
                }

                if (group == null) {
		    boolean lastDir = i==dirToks.size()-1;
                    String groupType = lastDir?getLastGroupType():TypeHandler.TYPE_GROUP;
		    if(!Utils.stringDefined(groupType))groupType = TypeHandler.TYPE_GROUP;
                    if (template != null) {
                        groupType = template.getType();
                    }
                    final FileWrapper dirFile     = file;
		    final PatternHarvester theHarvester = this;
                    EntryInitializer  initializer = new EntryInitializer() {
			    @Override
			    public void initEntry(Entry entry) {
				theHarvester.initEntry(entry);
			    }

			    /**
			     *  This returns the file for the metadata attachment for the entry
			     *  Look for .<attachment file name>
			     */
			    @Override
			    public File getMetadataFile(Entry entry,
							String fileArg) {
				File f = new File(dirFile.toString()
						  + File.separator + "." + fileArg);

				return f;
			    }
			};

                    group = getEntryManager().makeNewGroup(request,parentGroup, name,
							   getUser(), template, groupType, initializer);


                    String originalId = null;
                    if (template != null) {
                        originalId =
                            (String) template.getProperty(ATTR_ORIGINALID);
                    }
                    if (originalId != null) {
                        group.putProperty(ATTR_ORIGINALID, originalId);
                    }
                    //System.err.println("createGroup:" + group);

                    if (originalId != null) {
                        entriesMap.put(originalId, group);
                    }
                    idList.add(new String[] { group.getId(), originalId });

                    //                    System.err.println ("New group:" + group.getName() +" ID: " + group.getProperty(ATTR_ORIGINALID));
                }
                parentGroup = group;
            }
            names.add(name);
            parentFile = file;
        }

        return Utils.join(names,Entry.PATHDELIMITER);
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Date parseDate(String value) throws Exception {
        Exception lastException = null;
        for (SimpleDateFormat sdf : getSDF()) {
            try {
                return sdf.parse(value);
            } catch (Exception exc) {
                lastException = exc;
            }
        }
        if (lastException != null) {
            throw lastException;
        }

        return null;
    }



    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {

    }

    /**
     * _more_
     *
     *
     * @param fileInfo _more_
     * @param f _more_
     * @param entriesMap _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry processFile(HarvesterFile fileInfo, FileWrapper f,
                             Hashtable<String, Entry> entriesMap)
	throws Exception {
	//        logHarvesterInfo("processFile:" + f);
        Request request = getRequest();
        Matcher matcher = null;
        return harvestFile(fileInfo, f, matcher, entriesMap);
    }


    private boolean isFileOk(FileWrapper f) {
        String filePath = f.toString();
        String fileName = f.getName();
        //check if its a hidden file
        boolean isPlaceholder = fileName.equals(FILE_PLACEHOLDER);
        boolean isEntryXml    = isEntryXml(fileName);
        if ( !isPlaceholder) {
            if (f.getName().startsWith(".")) {
                logHarvesterInfo("File is hidden file:" + f);
                return false;
            }
        }

        if (fileName.equals("ramadda.properties")
	    || fileName.equals(".this.ramadda.xml")) {
            return false;
        }
        filePath = filePath.replace("\\", "/");

        if (filePattern != null && filePattern.size()>0) {
	    if(!PatternHolder.checkPatterns(filePattern,filePath)) {
                debug("file:<i>" + filePath + "</i> does not match pattern");
                logHarvesterInfo("file:" + filePath + " does not match pattern");
                return false;
            }
        }
	if(sizeLimit>=0) {
	    if(sizeLimit>f.length()) {
                logHarvesterInfo("file:" + filePath + " over size limit:" + f.length());
		return false;
	    }
	}


        if (notfilePattern != null && notfilePattern.size()>0) {
	    if(PatternHolder.checkPatterns(notfilePattern,filePath)) {
		logHarvesterInfo("excluding file because it matches the NOT pattern:"
				 + filePath);
		return false;
            }
        }

        debug("file:<i>" + filePath + "</i> matches pattern");
	return true;
    }

    /**
     * _more_
     *
     * @param fileName _more_
     *
     * @return _more_
     */
    private boolean isEntryXml(String fileName) {
        return fileName.endsWith(".entry.xml")
	    || fileName.endsWith(".ramadda.xml");
    }


    /**
     * _more_
     *
     * @param fileInfo _more_
     * @param f _more_
     * @param matcher _more_
     * @param entriesMap _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry harvestFile(HarvesterFile fileInfo, FileWrapper fileWrapper,
                             Matcher matcher,
                             Hashtable<String, Entry> entriesMap)
	throws Exception {
        Request request = getRequest();
	final boolean debug = false;
        Entry  baseGroup = getBaseGroup();
	String filePath      = fileWrapper.toString();
        filePath = filePath.replace("\\", "/");
        String fileName   = fileWrapper.getName();
	if(debug)    System.err.println("**** harvestFile:\n\tbaseGroup:" + baseGroup +"\n\tfile:" + fileWrapper);
        boolean isPlaceholder = fileName.equals(FILE_PLACEHOLDER);
        TypeHandler typeHandler      = getTypeHandler(TypeHandler.TYPE_FINDMATCH);
        TypeHandler typeHandlerToUse = null;
        boolean     isEntryXml       = isEntryXml(filePath);
        Entry       templateEntry    = null;
	Hashtable<String,File> filesMap = getFilesMap(fileWrapper.getFile());

        if (isEntryXml) {
            templateEntry = getEntryManager().parseEntryXml(fileWrapper.getFile(),
							    EntryManager.INTERNAL.YES,
							    EntryManager.TEMPLATE.NO,
							    entriesMap,filesMap).get(0);
        } else {
            templateEntry = getEntryManager().getTemplateEntry(fileWrapper.getFile(), entriesMap,filesMap);
        }

        if (templateEntry != null) {
            typeHandlerToUse = templateEntry.getTypeHandler();
        }

        if (typeHandlerToUse == null) {
	    typeHandlerToUse = getEntryManager().findTypeFromPatterns(typePatterns, filePath);
	}


        if ((typeHandlerToUse == null)
	    && typeHandler.getType().equals(TypeHandler.TYPE_FINDMATCH)) {
	    typeHandlerToUse = getEntryManager().findDefaultTypeHandler(filePath);
        }

        if (typeHandlerToUse == null) {
            if ( !typeHandler.getType().equals(TypeHandler.TYPE_FINDMATCH)) {
                typeHandlerToUse = typeHandler;
            } else {
                typeHandlerToUse =
                    getRepository().getTypeHandler(TypeHandler.TYPE_FILE);
            }
        }

        String dirPath     = fileWrapper.getParentFile().toString();
	File theFile= new File(dirPath);
	filesMap = getFilesMap(theFile);
        Entry dirTemplateEntry =
            getEntryManager().getTemplateEntry(theFile, entriesMap,filesMap);

	if(debug)    System.err.println("\tdirPath 1:" + dirPath);
        dirPath =
            dirPath.substring(fileInfo.getRootDir().toString().length()).replace("\\", "/");
	if(debug)    System.err.println("\tdirPath 2:" + dirPath);

        String dirGroup  = null;
        if (!noTree) {
	    List<String> dirToks = Utils.split(dirPath, "/", true, true);
            dirGroup = getDirNames(fileInfo.getRootDir(), baseGroup, dirToks,
                                   !getTestMode()
                                   && (groupTemplate.indexOf("${dirgroup}")
                                       >= 0), entriesMap);
            dirGroup = dirGroup.replace("\\", "/");
	    if(debug)    System.err.println("\tdirGroup:" + dirGroup);
        }


        Hashtable entryValues       = new Hashtable();
        Date      fromDate  = null;
        Date      toDate    = null;
        String    tag       = tagTemplate;
        String    groupName = groupTemplate;
        String    name      = nameTemplate;
        String    desc      = descTemplate;

        //        System.err.println("pattern names:" + patternNames);
        for (int dataIdx = 0;
	     (matcher != null) && (dataIdx < patternNames.size());
	     dataIdx++) {
            String dataName = patternNames.get(dataIdx);
            if ( !Utils.stringDefined(dataName)) {
                continue;
            }
            Object value = matcher.group(dataIdx + 1);
            if (dataName.equals("fromdate")) {
                value = fromDate = parseDate((String) value);
            } else if (dataName.equals("todate")) {
                value = toDate = parseDate((String) value);
            } else {
                value = typeHandlerToUse.convert(dataName, (String) value);
                groupName = groupName.replace("${" + dataName + "}",
					      value.toString());
                name = name.replace("${" + dataName + "}", value.toString());
                desc = desc.replace("${" + dataName + "}", value.toString());
                entryValues.put(dataName, value);
            }
        }

        Object[] values = typeHandlerToUse.makeEntryValues(entryValues);
        String   ext    = IO.getFileExtension(filePath);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        tag = tag.replace("${extension}", ext);

        Date   createDate = new Date(fileWrapper.lastModified());

        if (fromDate == null) {
            fromDate = toDate;
        }
        if (toDate == null) {
            toDate = fromDate;
        }

        if (fromDate == null) {
            //Don't try to pull the date from the filename for now
            //            fromDate = Utils.extractDate(applyMacros(name, createDate, fromDate, toDate, filename));
        }
        if (toDate == null) {
            toDate = fromDate;
        }

        if (fromDate == null) {
            fromDate = createDate;
        }
        if (toDate == null) {
            toDate = createDate;
        }

        if (templateEntry != null) {
            if (templateEntry.hasDate()) {
                fromDate = new Date(templateEntry.getStartDate());
                toDate   = new Date(templateEntry.getEndDate());
            }
        }

        if (dirGroup != null) {
            groupName = groupName.replace("${dirgroup}", dirGroup);
            groupName = applyMacros(groupName, createDate, fromDate, toDate,
                                    fileName);
        }
        name = applyMacros(name, createDate, fromDate, toDate, fileName);
        desc = applyMacros(desc, createDate, fromDate, toDate, fileName);
        desc = desc.replace("${name}", name);

        if (templateEntry != null) {
            if (Utils.stringDefined(templateEntry.getName())) {
                name = templateEntry.getName();
            }
            if (Utils.stringDefined(templateEntry.getDescription())) {
                desc = templateEntry.getDescription();
            }
            groupName =
                templateEntry.getTypeHandler().applyTemplate(request,templateEntry,
							     groupName);
        }

	name = getName(name,false);

	if(debug)    System.err.println("\tgroup name:" + groupName);

        if (getTestMode()) {
            debug("\tname: " + name + "\n\tgroup:" + groupName
                  + "\n\tfromdate:" + getDateHandler().formatDate(fromDate));
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    debug("\tvalue: " + values[i]);
                }
            }

            return null;
        }

        boolean                createIfNeeded = !getTestMode();
        final PatternHarvester theHarvester   = this;
        Entry                  group;
	groupName = getName(groupName,true);
	if(noTree) {
	    group =  baseGroup;
	    if(debug)    System.err.println("\tusing base group:" + group);
	} else {
	    group= getEntryManager().findEntryFromName(request,baseGroup,
						       groupName, createIfNeeded, getLastGroupType(),
						       dirTemplateEntry,
						       getGroupInitializer());
	    if(debug)
		System.err.println("\tgroup from findEntry:" + group);
	}

        if (group == null) {
            logHarvesterInfo("Could not create group:" + groupName);
            return null;
        }

        //If its just a placeholder then all we need to do is create the group and return
        if (isPlaceholder) {
	    if(debug)    System.err.println("\twas a placeholder");
            return null;
        }
        String tmpName = fileWrapper.getName().toLowerCase();
        if (tmpName.equals("readme") || tmpName.equals("readme.txt")) {
            if (group.getDescription().length() > 0) {
                return null;
            }
            group.setDescription(IO.readContents(fileWrapper.toString(), ""));
            getEntryManager().updateEntry(request, group);

            return null;
        }

        if (tmpName.equals("thumbs.sb")) {
            return null;
        }
        if (tmpName.endsWith(".thm")) {
            return null;
        }


        //        System.err.println("Harvested file:" + f);
        Entry entry = templateEntry;
	if(templateEntry!=null)
            getRepository().getMetadataManager().initNewEntry(request,templateEntry, null);	    

        if (entry == null) {
            entry = typeHandlerToUse.createEntry(getRepository().getGUID());
        } else {
            //            System.err.println("\tcreated entry from entry.xml template");
        }

        Resource resource;
        if (moveToStorage) {
            File fromFile;
	    if(fileWrapper.isRemoteFile()) {
		File tmp = getStorageManager().getTmpFile(fileName);
		System.err.println("copying:" +fileWrapper);
		fileWrapper.copyFileTo(tmp);
		System.err.println("done copying");
		fromFile = tmp;
	    } else {
		fromFile = fileWrapper.getFile();
	    }
	    //	    System.err.println("FILE:" + fromFile.exists()+ "  "+ fromFile);
            File newFile = getStorageManager().moveToStorage(
							     null, fromFile,
							     getStorageManager().getStorageFileName(
												    getStorageManager().getOriginalFilename(fromFile.getName())));
            resource = new Resource(newFile.toString(),
                                    Resource.TYPE_STOREDFILE);
        } else {
	    if(getStorageManager().isS3(filePath)) {
		resource = new Resource(filePath, Resource.TYPE_S3,fileWrapper.length());
	    } else {
		resource = new Resource(filePath, Resource.TYPE_FILE);
	    }
        }
        entry.setUser(getUser());

        Date now = new Date();
        if (isEntryXml) {
            if ( !entry.hasCreateDate()) {
                entry.setCreateDate(now.getTime());
            }
        } else {
            if (getGenerateMd5()) {
                resource.setMd5(IOUtil.getMd5(resource.getPath()));
            }
            //We used to use the file create date as the entry create and change date. This is wrong so we now use the current time
            Date changeDate = now;
            long createTime = now.getTime();
            if (entry.hasCreateDate()) {
                createTime = entry.getCreateDate();
            }
            entry.initEntry(name, desc, group, getUser(), resource, "",
                            Entry.DEFAULT_ORDER, createTime,
                            changeDate.getTime(), fromDate.getTime(),
                            toDate.getTime(), values);

	    getEntryManager().parentageChanged(group);
        }

	if(pushGeo && !entry.isGeoreferenced(request)) {
	    Entry ancestor = group;
	    while(ancestor!=null) {
		if(ancestor.isGeoreferenced(request)) break;
		ancestor = ancestor.getParentEntry();
	    }
	    if(ancestor!=null) {
		entry.setNorth(ancestor.getNorth(request));
		entry.setWest(ancestor.getWest(request));		
		entry.setSouth(ancestor.getSouth(request));
		entry.setEast(ancestor.getEast(request));		
	    }
	}


        entry.setParentEntry(group);

        //If it is an image then we create a thumbnail for it in the JpegMetadataHandler
        //else we check if there is a .thm file
        if ( !Utils.isImage(resource.getPath())) {
            File thumbnail = null;
            File tmp;
            tmp = new File(IO.stripExtension(resource.getPath())
                           + ".thm");
            if (tmp.exists()) {
                thumbnail = tmp;
            }
            if (thumbnail == null) {
                tmp = new File(IO.stripExtension(resource.getPath())
                               + ".THM");
                if (tmp.exists()) {
                    thumbnail = tmp;
                }
            }

            if (thumbnail != null) {
                String jpegFile = IO.stripExtension(thumbnail.getName())
		    + ".jpg";
                String newThumbFile =
                    getStorageManager().copyToEntryDir(entry, thumbnail,
						       jpegFile).getName();
                getMetadataManager().addMetadata(request,entry,
						 new Metadata(getRepository().getGUID(),
							      entry.getId(),
							      getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL),
							      false, newThumbFile, null, null, null,
							      null));

            }
        }

        return initializeNewEntry(fileInfo, fileWrapper, entry);

    }

    /**
     * _more_
     *
     * @param fileInfo _more_
     * @param originalFile _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public Entry initializeNewEntry(HarvesterFile fileInfo,
                                    FileWrapper originalFile, Entry entry) {
        return entry;
    }

    /**
     * _more_
     */
    public void clearCache() {
        lastRunTime = 0;
        super.clearCache();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getLastGroupType() {
	return lastGroupType;
    }

}
    
