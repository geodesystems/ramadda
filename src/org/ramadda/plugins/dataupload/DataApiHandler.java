/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.dataupload;

import org.ramadda.repository.*;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import ucar.unidata.util.IOUtil;
import java.io.*;
import java.util.Hashtable;



/**
 * Provides a top-level API to upload data into files on the server
 *
 */
public class DataApiHandler extends RepositoryManager implements RequestHandler {

    /** _more_ */
    public static final String ARG_GROUP = "group";

    /**  */
    public static final String ARG_DATA = "data";

    /**  */
    public static final String ARG_SENSOR = "sensor";

    /**  */
    public static final String ARG_KEY = "apikey";

    /**  */
    private Hashtable<String, UploadGroup> groups = new Hashtable<String,
                                                        UploadGroup>();


    /**
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public DataApiHandler(Repository repository) throws Exception {
        super(repository);
    }

    /**
     * Lookup the group. If not exists check that there is a property:
     * upload.<group>.directory. If not then return null
     *
     * @param group the group id
     * @return UploadGroup
     */
    private synchronized UploadGroup getUploadGroup(String group) {
        UploadGroup uploadGroup = groups.get(group);
        if (uploadGroup == null) {
            String dir = getRepository().getProperty("upload." + group
                             + ".directory", null);
            if (dir == null) {
                getRepository().getLogManager().logError(
                    "DataApiHandler: no upload." + group
                    + ".directory property defined");

                return null;
            }
	    dir = dir.trim();
            File file = new File(dir);
            if ( !file.exists()) {
                getRepository().getLogManager().logError(
                    "DataApiHandler: upload directory does not exist:" + dir);
                return null;
            }
            uploadGroup = new UploadGroup(getRepository(), group, file);
            groups.put(group, uploadGroup);
        }
        return uploadGroup;
    }

    /**
     *
     * @param request _more_
     * @param msg _more_
      * @return _more_
     */
    private Result error(Request request, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"code\":\"error\",\"message\":\"" + msg + "\"}");
        return new Result("", sb, JsonUtil.MIMETYPE);
    }

    /**
     * handle the request
     *
     * @param request request
     *
     * @return result
     *
     * @throws Exception on badness
     */
    public Result processUpload(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        String group = request.getString(ARG_GROUP, null);
        if ( !Utils.stringDefined(group)) {
            return error(request, "No group argument given");
        }
        UploadGroup uploadGroup = getUploadGroup(group);
        if (uploadGroup == null) {
            return error(request, "No upload group found");
        }

        return uploadGroup.processUpload(request);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Oct 20, '22
     * @author         Enter your name here...    
     */
    private class UploadGroup {

        /**  */
        private Repository repository;

        /**  */
        String group;

        /**  */
        File dir;

        /**  */
        String key;

        /**
         
         *
         * @param repository _more_
         * @param group _more_
         * @param dir _more_
         */
        UploadGroup(Repository repository, String group, File dir) {
            this.repository = repository;
            this.group      = group;
            this.dir        = dir;
            key             = getProperty("apikey", null);
        }

        /**
          * @return _more_
         */
        private Repository getRepository() {
            return repository;
        }

        /**
         *
         * @param prop _more_
         * @param dflt _more_
          * @return _more_
         */
        private String getProperty(String prop, String dflt) {
            String v = getRepository().getProperty("upload." + group + "."
                           + prop, dflt);
            if (v != null) {
                v = v.trim();
            }

            return v;
        }

        /**
         *
         * @param s _more_
          * @return _more_
         */
        private String replace(String s) {
            return s.replace("\\n", "\n");
        }

        /**
         *
         * @param request _more_
          * @return _more_
         *
         * @throws Exception _more_
         */
        private synchronized Result processUpload(Request request)
                throws Exception {
            StringBuffer sb = new StringBuffer();
	    /*
	      /data/upload?apikey=KEY&group=GROUP&sensor=SENSOR&data=1,2,3\n4,5,6
	      upload.hh.header=#This is the header for the fan data\nd1,d2,d3
	      upload.hh.directory=/data/healthyhogans/fans
	      upload.hh.file={sensor}.csv
							       */

            if (key == null) {
                getRepository().getLogManager().logError("DataApiHandler: "
                        + group + " no api key specified");

                return error(request, "No api key");

            }

            if ( !key.equals(request.getString(ARG_KEY, ""))) {
                getRepository().getLogManager().logError("DataApiHandler: "
                        + group + " api key does not match");

                return error(request, "api key does not match");
            }


            String sensor = request.getString(ARG_SENSOR, null);
            if ( !Utils.stringDefined(sensor)) {
                getRepository().getLogManager().logError("DataApiHandler: "
                        + group + " no sensor id given");

                return error(request, "No sensor argument given");
            }
            String data = request.getString(ARG_DATA, null);
            if ( !Utils.stringDefined(data)) {
                getRepository().getLogManager().logError("DataApiHandler: "
                        + group + " no data given");

                return error(request, "No data argument given");
            }
            String filename = getProperty("file",
                                          "{sensor}.csv").replace("{sensor}",
                                              sensor).replace("{group}",
                                                  group);
            File             file   = new File(IOUtil.joinDir(dir, filename));
            boolean          exists = file.exists();
            FileOutputStream fos    = new FileOutputStream(file, true);
            if ( !exists) {
                String header = getProperty("header", null);
                if (header != null) {
                    fos.write(replace(header).getBytes());
                    fos.write("\n".getBytes());
                }
            }
            data = data.trim();
            fos.write(replace(data).getBytes());
            fos.write("\n".getBytes());
            fos.close();
            sb.append("{\"code\":\"ok\",\"message\":\"data uploaded\"}");

            return new Result("", sb, JsonUtil.MIMETYPE);
        }
    }

}
