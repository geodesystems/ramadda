/**
Copyright (c) 2008-2023 Geode Systems LLC
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


    /** _more_ */
    public static final int COL_PATH = COL++;

    /** _more_ */
    public static final int COL_AGE = COL++;

    /** _more_ */
    public static final int COL_INCLUDES = COL++;

    /** _more_ */
    public static final int COL_EXCLUDES = COL++;

    /** _more_ */
    public static final int COL_DIRECTORY_TYPE = COL++;

    public static final int COL_DATE_OFFSET = COL++;


    /** _more_ */
    public static final int COL_DATE_PATTERNS = COL++;


    /** _more_ */
    public static final int COL_NAMES = COL++;



    /** _more_ */
    private File rootDir;

    /** _more_ */
    private List<String> includes;

    /** _more_ */
    private List<String> excludes;

    private String directoryType;

    private String datePatterns;


    /** _more_ */
    private List<String> names;

    /** _more_ */
    private double ageLimit = -1;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public LocalFileInfo(Repository repository, Entry entry)
            throws Exception {
        Object[] values = entry.getValues();
        if (values == null) {
            return;
        }

	directoryType = (String) values[COL_DIRECTORY_TYPE];
	if(!Utils.stringDefined(directoryType)) directoryType=null;

	datePatterns = (String) values[COL_DATE_PATTERNS];

        names    = get(values, COL_NAMES);
        includes = get(values, COL_INCLUDES);
        excludes = get(values, COL_EXCLUDES);
        ageLimit = ((Double) values[COL_AGE]).doubleValue();
	String rootPath = (String) values[0];
	if(!Utils.stringDefined(rootPath)) {
	    return;
	}
	rootDir  = new File(rootPath);
        checkMe(repository);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param rootDir _more_
     *
     * @throws Exception _more_
     */
    public LocalFileInfo(Repository repository, File rootDir)
            throws Exception {
        this(repository, rootDir, null, null, null, -1);

    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param rootDir _more_
     * @param includes _more_
     * @param excludes _more_
     * @param names _more_
     * @param ageLimit _more_
     *
     * @throws Exception _more_
     */
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


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    private void checkMe(Repository repository) throws Exception {
        if ( !rootDir.exists()) {
	    return;
	    //	    throw new RepositoryUtil.MissingEntryException("Could not find entry: " + rootDir);
        }
        repository.getStorageManager().checkLocalFile(rootDir);
    }

    /**
     * _more_
     *
     * @param values _more_
     * @param idx _more_
     *
     * @return _more_
     */
    public static List<String> get(Object[] values, int idx) {
        if (values[idx] == null) {
            return new ArrayList<String>();
        }

        return (List<String>) Utils.split(values[idx], "\n", true, true);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDefined() {
        return rootDir != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getIncludes() {
        return includes;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getExcludes() {
        return excludes;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public File getRootDir() {
        return rootDir;
    }

    /**
     * _more_
     *
     * @return _more_
     */
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
