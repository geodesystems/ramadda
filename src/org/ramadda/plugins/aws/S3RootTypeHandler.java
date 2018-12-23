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

package org.ramadda.plugins.aws;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.ProcessRunner;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class S3RootTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    private TTLCache<String, Entry> entryCache;


    /** _more_ */
    public static final int IDX_ROOT = 0;

    /** _more_ */
    private SimpleDateFormat displaySdf =
        new SimpleDateFormat("MMMMM dd - HH:mm");

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public S3RootTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param rootEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, Entry rootEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {

        System.err.println("S3RootTypeHandler.getSynthIds:" + synthId
                           + " parent:" + parentEntry.getName() + " root: "
                           + rootEntry.getName() + " synthId:" + synthId);

        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();

        String rootId = (String) rootEntry.getValue(IDX_ROOT);
        if ( !Utils.stringDefined(rootId)) {
            return ids;
        }

        List<BucketInfo> infos = doLs(rootId, null);

        if (synthId == null) {
            List<String> children = new ArrayList<String>();
            for (BucketInfo info : infos) {
                Entry bucketEntry = createBucketEntry(rootEntry, info);
                if (bucketEntry == null) {
                    continue;
                }
                getEntryManager().cacheSynthEntry(bucketEntry);
                ids.add(bucketEntry.getId());
            }
        } else {}
        parentEntry.setChildIds(ids);

        return ids;
    }



    /**
     * _more_
     *
     *
     * @param rootEntry _more_
     * @param info _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createBucketEntry(Entry rootEntry, BucketInfo info)
            throws Exception {
        String name = info.path;
        Date   dttm = info.dttm;
        if (dttm == null) {
            dttm = new Date();
        }
        String id = getEntryManager().createSynthId(rootEntry, info.path);
        TypeHandler bucketTypeHandler =
            getRepository().getTypeHandler("type_s3_bucket");
        Entry    bucketEntry = new Entry(id, bucketTypeHandler);
        String   desc        = "";
        Resource resource    = new Resource("s3://" + info.path);
        Object[] values      = bucketTypeHandler.makeEntryValues(null);
        bucketEntry.initEntry(name, desc, rootEntry, rootEntry.getUser(),
                              resource, "", dttm.getTime(), dttm.getTime(),
                              dttm.getTime(), dttm.getTime(), values);

        bucketEntry.setMasterTypeHandler(this);

        return bucketEntry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param rootEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry rootEntry, String id)
            throws Exception {
        List<String> toks = StringUtil.split(id, ":", true, true);

        //        Entry        bucketEntry = createBucketEntry(rootEntry, id);
        //       return bucketEntry;
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean isSynthType() {
        return true;
    }

    /**
     * _more_
     *
     * @param bucket _more_
     * @param path _more_
     *
     * @return _more_
     */
    public static String getS3Path(String bucket, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append("s3://");
        sb.append(bucket);
        if (Utils.stringDefined(path)) {
            sb.append(path);
        } else {
            sb.append("/");
        }

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param bucket _more_
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<BucketInfo> doLs(String bucket, String path)
            throws Exception {
        List<BucketInfo> results  = new ArrayList<BucketInfo>();

        List<String>     commands = getLsCommands();
        commands.add(getS3Path(bucket, path));
        String s = executeCommands(commands);
        for (String line : StringUtil.split(s, "\n", false, false)) {
            String tline = line.trim();
            if ((tline.length() == 0) || tline.startsWith("Total")) {
                continue;
            }
            //A hack to check for directory vs file
            if (line.startsWith("    ")) {
                List<String> toks = StringUtil.splitUpTo(line.trim(), " ", 2);
                String       dirName = toks.get(1);
                results.add(new BucketInfo(dirName));
            } else {
                List<String> toks = StringUtil.splitUpTo(line.trim(), " ", 4);
                Date dttm = DateUtil.parse(toks.get(0) + "'T'" + toks.get(1));
                String       size = toks.get(2);
                String       file = toks.get(3);
                results.add(new BucketInfo(file, dttm,
                                           (long) Double.parseDouble(size)));
            }

        }

        return results;
    }


    /**
     * _more_
     *
     * @param commands _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static String executeCommands(List<String> commands)
            throws Exception {
        StringWriter   outBuf            = new StringWriter();
        StringWriter   errorBuf          = new StringWriter();
        PrintWriter    stdOutPrintWriter = new PrintWriter(outBuf);
        PrintWriter    stdErrPrintWriter = new PrintWriter(errorBuf);
        ProcessBuilder pb                = new ProcessBuilder(commands);
        File           dir               = new File(".");
        pb.directory(dir);

        ProcessRunner runner = new ProcessRunner(pb, 10, stdOutPrintWriter,
                                   stdErrPrintWriter);
        int exitCode = runner.runProcess();
        if (runner.getProcessTimedOut()) {
            throw new InterruptedException("Process timed out");
        }

        return outBuf.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static List<String> getLsCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add("/usr/local/bin/aws");
        commands.add("s3");
        commands.add("ls");
        commands.add("--summarize");

        return commands;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        System.err.print(doLs("noaa-nexrad-level2", null));
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Jan 30, '18
     * @author         Enter your name here...
     */
    private static class BucketInfo {

        /** _more_ */
        Date dttm;

        /** _more_ */
        String path;

        /** _more_ */
        boolean isDir = true;

        /** _more_ */
        long size = -1;

        /**
         * _more_
         *
         * @param path _more_
         */
        public BucketInfo(String path) {
            this.dttm  = new Date();
            this.path  = path;
            this.isDir = true;
        }

        /**
         * _more_
         *
         * @param path _more_
         * @param dttm _more_
         * @param size _more_
         */
        public BucketInfo(String path, Date dttm, long size) {
            this.dttm  = dttm;
            this.path  = path;
            this.size  = size;
            this.isDir = false;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            if (isDir) {
                return "dir:" + path;
            } else {
                return "file:" + path + " " + size + " " + dttm;
            }
        }

    }

}
