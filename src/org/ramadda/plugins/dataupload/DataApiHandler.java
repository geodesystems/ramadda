/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.dataupload;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.net.URL;

import java.util.Hashtable;
import java.util.List;



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
    public static final String ARG_RELAYED = "relayed";

    /**  */
    private Hashtable<String, UploadGroup> groupMap = new Hashtable<String,
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
        UploadGroup uploadGroup = groupMap.get(group);
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
            groupMap.put(group, uploadGroup);
        }

        return uploadGroup;
    }

    /**
     *
     * @param request _more_
     * @param msg _more_
     *  @return _more_
     */
    private Result error(Request request, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"code\":\"error\",\"message\":\"" + msg + "\"}");

        return new Result("", sb, JsonUtil.MIMETYPE);
    }

    /**
     *
     * @param request _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processUploadList(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
	getPageHandler().sectionOpen(request, sb, "Data Upload Files",false);
	processUploadListInner(request, sb);
	getPageHandler().sectionClose(request, sb);
        return new Result("Data Upload Files", sb);
    }
    private void  processUploadListInner(Request request, StringBuilder sb) throws Exception {	
	if(!request.isAdmin()) {
	    sb.append(getPageHandler().showDialogWarning("Only site administrators can access the data upload files"));
	    return;
	}

	String groups = getRepository().getProperty("upload.groups",null);
	if(groups==null) {
	    sb.append(getPageHandler().showDialogWarning("No upload.groups property defined"));
	    return;
	}

	for(String group:Utils.split(groups,",",true,true)) {
	    sb.append(HU.h2(group));
	    String dir  = getRepository().getProperty("upload." + group+".directory",null);
	    if(dir==null) {
		sb.append("No directory specified");
		continue;
	    }
	    sb.append("<ul>");
	    int cnt = 0;
	    for(File file: new File(dir).listFiles()) {
		sb.append("<li> ");
		sb.append(file.toString() +" " + Utils.formatFileLength(file.length()));
		cnt++;
	    }
	    sb.append("</ul>");
	    if(cnt==0) sb.append("No files");
	}

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
        StringBuffer sb    = new StringBuffer();
        String       group = request.getString(ARG_GROUP, null);
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

        /**  */
        List<String> relayServers;

        /**  */
        List<String> relayKeys;

        /**  */
        List<String> relayGroups;

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
	    if(Utils.stringDefined(key)) {
		String relayServer     = getProperty("relay.servers", null);
		if(Utils.stringDefined(relayServer)) {
		    relayServers = Utils.split(relayServer,",",true,true);
		    relayKeys       = Utils.split(getProperty("relay.apikeys", key),",",true,true);
		    relayGroups     = Utils.split(getProperty("relay.groups", group),",",true,true);
		}
	    }
        }

        /**
         *  @return _more_
         */
        private Repository getRepository() {
            return repository;
        }

        /**
         *
         * @param request _more_
         * @param msg _more_
          * @return _more_
         */
        private Result error(Request request, String msg) {
            getRepository().getLogManager().logError("DataApiHandler: "
                    + group + " " + msg);

            return DataApiHandler.this.error(request, msg);
        }


        /**
         *
         * @param prop _more_
         * @param dflt _more_
         *  @return _more_
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
         *  @return _more_
         */
        private String replace(String s) {
            return s.replace("\\n", "\n");
        }

        /**
         *
         * @param request _more_
         *  @return _more_
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
                return error(request, "No api key specified in properties");
            }

            String requestKey = request.getString(ARG_KEY, null);
            if ( !Utils.stringDefined(requestKey)) {
                return error(request, "No api key specified in url args");
            }

            if ( !key.equals(requestKey)) {
                return error(request, "api key does not match");
            }


            final String sensor = request.getString(ARG_SENSOR, null);
            if ( !Utils.stringDefined(sensor)) {
                return error(request, "No sensor argument given");
            }
            String data = request.getString(ARG_DATA, null);
            if ( !Utils.stringDefined(data)) {
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

            if (relayServers != null) {
                if (request.get(ARG_RELAYED, false)) {
                    getRepository().getLogManager().logError(
                        "DataApiHandler: " + group
                        + " circular relay detected");
                } else {
                    final String relayData = data;
                    ucar.unidata.util.Misc.run(new Runnable() {
                        public void run() {
                            relayData(sensor, relayData);
                        }
                    });
                }
            }

            return new Result("", sb, JsonUtil.MIMETYPE);
        }

        /**
         *
         * @param sensor _more_
         * @param data _more_
         */
        private void relayData(String sensor, String data) {
	    for(int i=0;i<relayServers.size();i++)  {
		String relayServer =  relayServers.get(i);
		String relayKey = relayKeys.get(i<relayKeys.size()?i:0);
		String relayGroup = relayGroups.get(i<relayGroups.size()?i:0);		
		try {
		    String url = HU.url(relayServer + "/data/upload", ARG_KEY,
					relayKey, ARG_GROUP, relayGroup,
					ARG_SENSOR, sensor, ARG_RELAYED, "true",
					ARG_DATA, data);
		    String     json = IO.doGet(new URL(url));
		    JSONObject obj  = new JSONObject(json);
		    String     code = obj.optString("code", null);
		    if ( !Utils.equals(code, "ok")) {
			getRepository().getLogManager().logInfoAndPrint(
									"DataApiHandler: " + group + " relay failed to:"
									+ relayServer + " " + relayGroup +" - "
									+ obj.optString("message", ""));
		    } else {
			getRepository().getLogManager().logInfoAndPrint(
									"DataApiHandler: " + group + " relayed to:"
									+ relayServer+" " + relayGroup);
		    }
		} catch (Exception exc) {
		    getRepository().getLogManager().logError("DataApiHandler:"
							     + group + " relay failed to:" + relayServer+" " +relayGroup, exc);
		}
	    }

        }


    }



}
