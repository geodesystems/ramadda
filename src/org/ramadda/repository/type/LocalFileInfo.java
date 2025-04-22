/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;

import org.ramadda.repository.*;
import org.ramadda.util.Utils;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
public class LocalFileInfo {

    private static int COL=0;

    public static final int COL_PATH = COL++;

    public static final int COL_AGE = COL++;

    public static final int COL_INCLUDES = COL++;

    public static final int COL_EXCLUDES = COL++;

    public static final int COL_DIRECTORY_TYPE = COL++;

    public static final int COL_DATE_OFFSET = COL++;

    public static final int COL_DATE_PATTERNS = COL++;

    public static final int COL_NAMES = COL++;

    private File rootDir;

    private List<String> includes;

    private List<String> excludes;

    private String directoryType;

    private String datePatterns;

    private List<String> names;

    private double ageLimit = -1;

    public LocalFileInfo(Repository repository, Entry entry)
            throws Exception {
	Request request = repository.getAdminRequest();
	directoryType = entry.getStringValue(request,COL_DIRECTORY_TYPE,"");
	if(!Utils.stringDefined(directoryType)) directoryType=null;

	datePatterns = entry.getStringValue(request,COL_DATE_PATTERNS,"");

        names    = get(entry.getStringValue(request, COL_NAMES,null));
        includes = get(entry.getStringValue(request, COL_INCLUDES,null));
        excludes = get(entry.getStringValue(request, COL_EXCLUDES,null));
        ageLimit = entry.getDoubleValue(request, COL_AGE,0);
	String rootPath = entry.getStringValue(request,0,"");
	if(!Utils.stringDefined(rootPath)) {
	    return;
	}
	rootDir  = new File(rootPath);
        checkMe(repository);
    }

    public LocalFileInfo(Repository repository, File rootDir)
            throws Exception {
        this(repository, rootDir, null, null, null, -1);

    }

    public LocalFileInfo(Repository repository, File rootDir,
                         List<String> includes, List<String> excludes,
                         List<String> names, double ageLimit)
            throws Exception {
        this.rootDir  = rootDir;
        this.includes = (includes != null)
                        ? includes
                        : new ArrayList<String>();
        this.excludes = (excludes != null)
                        ? excludes
                        : new ArrayList<String>();
        this.names    = (names != null)
                        ? names
                        : new ArrayList<String>();
        this.ageLimit = ageLimit;
        checkMe(repository);
    }

    private void checkMe(Repository repository) throws Exception {
        if ( !rootDir.exists()) {
	    return;
	    //	    throw new RepositoryUtil.MissingEntryException("Could not find entry: " + rootDir);
        }
        repository.getStorageManager().checkLocalFile(rootDir);
    }

    public static List<String> get(String s) {
        if (s == null) {
            return new ArrayList<String>();
        }

        return (List<String>) Utils.split(s, "\n", true, true);
    }

    public boolean isDefined() {
        return rootDir != null;
    }

    public List<String> getNames() {
        return names;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public File getRootDir() {
        return rootDir;
    }

    public double getAgeLimit() {
        return ageLimit;
    }

    public String getDirectoryType() {
	return directoryType;
    }

    public String getDatePatterns() {
	return datePatterns;
    }    

}
