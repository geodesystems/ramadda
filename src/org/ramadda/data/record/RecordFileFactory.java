/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;


import org.ramadda.util.IO;
import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Fri, May 21, '10
 * @author         Enter your name here...
 */
@SuppressWarnings("unchecked")
public class RecordFileFactory {

    /** _more_ */
    private List<RecordFile> prototypes = new ArrayList<RecordFile>();

    /**
     * _more_
     */
    public RecordFileFactory() {}

    /**
     * _more_
     *
     * @param classListFile _more_
     *
     * @throws Exception _more_
     */
    public RecordFileFactory(String classListFile) throws Exception {
        addPrototypes(classListFile);
    }

    /**
     * _more_
     *
     * @param file _more_
     */
    public void addPrototype(RecordFile file) {
        prototypes.add(file);
    }

    /**
     * _more_
     *
     * @param classListFile _more_
     *
     * @throws Exception _more_
     */
    public void addPrototypes(String classListFile) throws Exception {
        //        System.err.println ("file:" + classListFile);
        for (String line :
                StringUtil.split(IOUtil.readContents(classListFile,
                    getClass()), "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            //            System.err.println ("line:" + line);
            Class c = Misc.findClass(line);
            addPrototype((RecordFile) c.getDeclaredConstructor().newInstance());
        }
    }


    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordFile doMakeRecordFile(String path) throws Exception {
        return doMakeRecordFile(path, null, null);
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordFile doMakeRecordFile(String path, Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        for (RecordFile f : prototypes) {
            if (f.canLoad(path)) {
                //                System.err.println("loading " +  f.getClass().getName());
                return f.cloneMe(new IO.Path(path), properties, requestProperties);
            }
        }

        throw new IllegalArgumentException("Unknown file type:" + path);
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canLoad(String path) throws Exception {
        for (RecordFile f : prototypes) {
            if (f.canLoad(path)) {
                return true;
            }
        }

        return false;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public void test(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            final int[]         cnt     = { 0 };
            RecordFile          file    = doMakeRecordFile(args[i]);

            final RecordVisitor visitor = new RecordVisitor() {
                public boolean visitRecord(RecordFile file,
                                           VisitInfo visitInfo,
                                           BaseRecord record) {
                    cnt[0]++;

                    return true;
                }
            };

            file.visit(visitor, new VisitInfo(), null);
            System.err.println(args[i] + " #points:" + cnt[0]);
        }

    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        new RecordFileFactory().test(args);
    }



}
