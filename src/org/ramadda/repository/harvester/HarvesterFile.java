/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.harvester;

import org.ramadda.util.FileInfo;
import org.ramadda.util.FileWrapper;
import org.ramadda.util.Utils;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;

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
 */
@SuppressWarnings("unchecked")
public class HarvesterFile extends FileInfo {
    private FileWrapper rootDir;
    private List addedFiles;

    public HarvesterFile(FileWrapper f) {
        super(f);
    }

    public HarvesterFile(File f, File rootDir, boolean isDir) {
        this(new FileWrapper.File(f), new FileWrapper.File(rootDir), isDir);
    }

    public HarvesterFile(FileWrapper f, FileWrapper rootDir, boolean isDir) {
        super(f, isDir);
        this.rootDir = rootDir;
    }
    public void clearAddedFiles() {
        addedFiles = null;
    }

    public void addFile(Object f) {
        if (addedFiles == null) {
            addedFiles = new ArrayList();
        }
        addedFiles.add(f);
    }

    public String toString() {
        String s   = getFile().toString();
        List   tmp = addedFiles;
        if ((tmp != null) && (tmp.size() > 0)) {
            String fileBlock;
            if (tmp.size() > 50) {
                return s + " Added " + tmp.size() + " files";
            }
            fileBlock = HtmlUtils.insetDiv(Utils.join(tmp,"<br>"), 0,
					   10, 0, 0);

            return HtmlUtils.makeShowHideBlock(s, fileBlock, false);
        }

        return s;
    }

    public static boolean okToRecurse(FileWrapper dir, Harvester harvester)
            throws Exception {
        //check for a ramadda.properties file. 
        File propFile = new File(IOUtil.joinDir(dir.toString(),
                            "ramadda.properties"));
        if (propFile.exists()) {
            harvester.logHarvesterInfo("Checking properties file:"
                                       + propFile);
            Properties      properties = new Properties();
            FileInputStream fis        = new FileInputStream(propFile);
            properties.load(fis);
            IO.close(fis);
            String ok = (String) properties.get("harvester.ok");
            if ((ok != null) && ok.trim().equals("false")) {
                harvester.logHarvesterInfo("Skipping directory:" + dir);

                return false;
            }
            harvester.logHarvesterInfo("Not Skipping directory:" + ok);
        }

        return true;
    }

    public FileWrapper getRootDir() {
        return rootDir;
    }

    public static void main(String[] args) throws Exception {
        FileWrapper.File rootDir = new FileWrapper.File(".");
        if ( !rootDir.exists()) {
            rootDir = new FileWrapper.File("");
        }
        if (args.length > 0) {
            rootDir = new FileWrapper.File(args[0]);
        }
        final List<FileInfo>   dirs       = new ArrayList();
        final int[]            cnt        = { 0 };
        FileWrapper.FileViewer fileViewer = new FileWrapper.FileViewer() {
            @Override
            public int viewFile(int level, FileWrapper f, FileWrapper[]siblings) throws Exception {
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
        FileWrapper.walkDirectory(rootDir, fileViewer);
        long tt2 = System.currentTimeMillis();
        //        System.err.println("found:" + dirs.size() + " in:" + (tt2 - tt1)
        //                           + " looked at:" + cnt[0]);

        while (true) {
            long t1 = System.currentTimeMillis();
            for (FileInfo fileInfo : dirs) {
                long oldTime = fileInfo.getTime();
                if (fileInfo.hasChanged()) {
                    //                    System.err.println("Changed:" + fileInfo);
                    FileWrapper[] files = fileInfo.getFile().doListFiles();
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
