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

package org.ramadda.repository.harvester;


import org.ramadda.util.FileInfo;


import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;


/**
 * Class FileInfo holds information about a file or directory
 *
 *
 */
public class HarvesterFile extends FileInfo {

    /** _more_ */
    private File rootDir;


    /** _more_ */
    private List addedFiles;

    /**
     * ctor
     *
     * @param f the file
     */
    public HarvesterFile(File f) {
        super(f);

    }

    /**
     * ctor
     *
     * @param f the file
     * @param rootDir _more_
     * @param isDir is file a directory
     */
    public HarvesterFile(File f, File rootDir, boolean isDir) {
        super(f, isDir);
        this.rootDir = rootDir;
    }


    /**
     * _more_
     */
    public void clearAddedFiles() {
        addedFiles = null;
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public void addFile(Object f) {
        if (addedFiles == null) {
            addedFiles = new ArrayList();
        }
        addedFiles.add(f);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        String s   = getFile().toString();
        List   tmp = addedFiles;
        if ((tmp != null) && (tmp.size() > 0)) {
            String fileBlock;
            if (tmp.size() > 50) {
                return s + " Added " + tmp.size() + " files";
            }
            fileBlock = HtmlUtils.insetDiv("Files:<br>"
                                           + StringUtil.join("<br>", tmp), 0,
                                               10, 0, 0);

            return HtmlUtils.makeShowHideBlock(s, fileBlock, false);
        }

        return s;
    }


    /**
     * _more_
     *
     * @param dir _more_
     * @param harvester _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean okToRecurse(File dir, Harvester harvester)
            throws Exception {
        //check for a ramadda.properties file. 
        File propFile = new File(IOUtil.joinDir(dir, "ramadda.properties"));
        if (propFile.exists()) {
            harvester.logHarvesterInfo("Checking properties file:"
                                       + propFile);
            Properties      properties = new Properties();
            FileInputStream fis        = new FileInputStream(propFile);
            properties.load(fis);
            IOUtil.close(fis);
            String ok = (String) properties.get("harvester.ok");
            if ((ok != null) && ok.trim().equals("false")) {
                harvester.logHarvesterInfo("Skipping directory:" + dir);

                return false;
            }
            harvester.logHarvesterInfo("Not Skipping directory:" + ok);
        }

        return true;
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
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        File rootDir = new File(".");
        if ( !rootDir.exists()) {
            rootDir = new File("");
        }
        if (args.length > 0) {
            rootDir = new File(args[0]);
        }
        final List<FileInfo> dirs       = new ArrayList();
        final int[]          cnt        = { 0 };
        IOUtil.FileViewer    fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                cnt[0]++;
                if (cnt[0] % 1000 == 0) {
                    System.err.print(".");
                }
                if (f.isDirectory()) {
                    dirs.add(new HarvesterFile(f, f, true));
                    //    if(dirs.size()%1000==0) System.err.print(".");
                }

                return DO_CONTINUE;
            }
        };

        long tt1 = System.currentTimeMillis();
        IOUtil.walkDirectory(rootDir, fileViewer);
        long tt2 = System.currentTimeMillis();
        //        System.err.println("found:" + dirs.size() + " in:" + (tt2 - tt1)
        //                           + " looked at:" + cnt[0]);

        while (true) {
            long t1 = System.currentTimeMillis();
            for (FileInfo fileInfo : dirs) {
                long oldTime = fileInfo.getTime();
                if (fileInfo.hasChanged()) {
                    //                    System.err.println("Changed:" + fileInfo);
                    File[] files = fileInfo.getFile().listFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].lastModified() > oldTime) {
                            //                            System.err.println("    " + files[i].getName());
                        }

                    }


                }
            }
            long t2 = System.currentTimeMillis();
            //            System.err.println ("Time:" + (t2-t1));
            Misc.sleep(5000);
        }
    }

}
