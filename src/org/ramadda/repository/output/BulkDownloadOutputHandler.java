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

package org.ramadda.repository.output;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Link;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.auth.AuthorizationMethod;
import org.ramadda.repository.harvester.Harvester;
import org.ramadda.repository.util.SelectInfo;


import org.ramadda.util.HtmlUtils;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;



/**
 * Produces a shell script to download files
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class BulkDownloadOutputHandler extends OutputHandler {


    /** The  output type */
    public static final OutputType OUTPUT_CURL =
        new OutputType("Bulk Download Script", "bulk.curl",
                       OutputType.TYPE_FILE, "", ICON_FETCH);

    /** _more_ */
    public static final OutputType OUTPUT_WGET =
        new OutputType("Bulk Download Script", "bulk.wget",
                       OutputType.TYPE_FILE, "", ICON_FETCH);


    /** _more_ */
    public static final String ARG_RECURSE = "recurse";

    /** _more_ */
    public static final String ARG_INCLUDEPARENT = "includeparent";

    /** _more_ */
    public static final String ARG_OVERWRITE = "overwrite";

    /** _more_ */
    public static final String ARG_OUTPUTS = "outputs";

    /** _more_ */
    public static final String ARG_COMMAND = "command";

    /** _more_ */
    public static final String COMMAND_WGET = "wget";

    /** _more_ */
    public static final String COMMAND_CURL = "curl";

    /**
     * Create a wget output handler
     *
     * @param repository  the repository
     * @param element     the XML definition
     * @throws Exception  problem creating the handler
     */
    public BulkDownloadOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_CURL);
    }




    /**
     * Get the authorization method
     *
     * @param request  the request
     *
     * @return  the authorization method
     */
    public AuthorizationMethod getAuthorizationMethod(Request request) {
        return AuthorizationMethod.AUTH_HTTP;
    }

    /**
     * Get the entry links
     *
     * @param request  the Request
     * @param state    the State
     * @param links    the list of links to add to
     *
     * @throws Exception  problem generating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.entry != null) {
            if (state.entry.getResource().isUrl()
                    || getAccessManager().canDownload(request, state.entry)) {
                links.add(
                    makeLink(
                        request, state.entry, OUTPUT_CURL,
                        "/" + IOUtil.stripExtension(state.entry.getName())
                        + "_download.sh"));
            }
        } else {
            boolean ok = false;
            for (Entry child : state.getAllEntries()) {
                //For now add the bulk download link to any folder entry, even if it doesn't have file children
                ok = true;
                if (ok) {
                    break;
                }
                /*
                if (child.getResource().isUrl()
                        || getAccessManager().canDownload(request, child)) {
                    ok = true;

                    break;
                }
                    */
            }



            if (ok) {
                //Maybe don't put this for the top level entries. 
                //Somebody will invariably come along and try to fetch everything
                if (state.group != null) {
                    if ( !state.group.isTopEntry()) {
                        links.add(
                            makeLink(
                                request, state.group, OUTPUT_CURL,
                                "/"
                                + IOUtil.stripExtension(
                                    state.group.getName()) + "_download.sh"));
                    }
                } else {
                    links.add(makeLink(request, state.group, OUTPUT_CURL));
                }
            }
        }
    }




    /**
     * Output the entry
     *
     * @param request   the request
     * @param outputType  the output type
     * @param entry     the entry
     *
     * @return the Result
     *
     * @throws Exception on badness
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        request.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                  + "_download.sh");

        return outputGroup(request, outputType, null, new ArrayList<Entry>(),
                           (List<Entry>) Misc.newList(entry));
    }


    /**
     * Output a group of entries
     *
     * @param request    the Request
     * @param outputType the output type
     * @param group      the group (may be null)
     * @param subGroups  the list of subgroups (may be null)
     * @param entries    the list of entries
     *
     * @return  the result
     *
     * @throws Exception  problem creating the script
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {

        //For the download get all children entries
        if ( !request.defined(ARG_MAX)) {
            request.put(ARG_MAX, "20000");
        }
        if ((group != null) && group.isDummy()) {
            request.setReturnFilename("Search_Results_download.sh");
        }

        StringBuilder sb = new StringBuilder();
        subGroups.addAll(entries);
        boolean recurse   = request.get(ARG_RECURSE, true);
        boolean overwrite = request.get(ARG_OVERWRITE, false);
        process(request, sb, group, subGroups, recurse, overwrite,
                new HashSet<String>());

        return new Result("", sb, getMimeType(OUTPUT_CURL));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param group _more_
     * @param entries _more_
     * @param recurse _more_
     * @param overwrite _more_
     * @param seen _more_
     *
     * @throws Exception _more_
     */
    public void process(Request request, StringBuilder sb, Entry group,
                        List<Entry> entries, boolean recurse,
                        boolean overwrite, HashSet<String> seen)
            throws Exception {


        List<List<String>> outputPairs         =
            new ArrayList<List<String>>();
        boolean            includeGroupOutputs = false;
        for (String pair :
                StringUtil.split(request.getString(ARG_OUTPUTS, ""), ",",
                                 true, true)) {
            outputPairs.add(StringUtil.splitUpTo(pair, ":", 2));
            String outputId = outputPairs.get(outputPairs.size()
                                  - 1).get(0).toString();
            if (outputId.equals(XmlOutputHandler.OUTPUT_XMLENTRY.getId())) {
                includeGroupOutputs = true;
            }
        }

        CurlCommand command = new CurlCommand(request);


        if (request.get(ARG_INCLUDEPARENT, false)) {
            writeGroupScript(request, group, sb, command, outputPairs,
                             includeGroupOutputs);
        }

        process(request, sb, group, entries, recurse, overwrite, command,
                outputPairs, includeGroupOutputs, seen);
        if (request.get(ARG_INCLUDEPARENT, false)) {
            sb.append(cmd("cd .."));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param group _more_
     * @param entries _more_
     * @param recurse _more_
     * @param overwrite _more_
     * @param command _more_
     * @param outputPairs _more_
     * @param includeGroupOutputs _more_
     * @param seen _more_
     *
     * @throws Exception _more_
     */
    public void process(Request request, StringBuilder sb, Entry group,
                        List<Entry> entries, boolean recurse,
                        boolean overwrite, CurlCommand command,
                        List<List<String>> outputPairs,
                        boolean includeGroupOutputs, HashSet<String> seen)
            throws Exception {

        HashSet seenFiles = new HashSet();
        for (Entry entry : entries) {
            if (seen.contains(entry.getId())) {
                continue;
            }
            //If this is a real entry (as opposed to the results of a search or selection) then check for entries like the virtual group
            if ( !group.isDummy()) {
                if ( !Misc.equals(entry.getParentEntryId(), group.getId())) {
                    continue;
                }
            }
            seen.add(entry.getId());
            if (getEntryManager().isSynthEntry(entry.getId())) {
                continue;
            }
            boolean wroteEntryXml = false;

            if (entry.isGroup()) {
                if ( !recurse) {
                    continue;
                }
                List<Entry> subEntries =
                    getEntryManager().getChildrenAll(request, entry,
                        new SelectInfo(null, 20000));

                if (includeGroupOutputs || (subEntries.size() > 0)) {
                    wroteEntryXml = true;
                    writeGroupScript(request, entry, sb, command,
                                     outputPairs, includeGroupOutputs);
                    process(request, sb, entry, subEntries, recurse,
                            overwrite, command, outputPairs,
                            includeGroupOutputs, seen);
                    sb.append(cmd("cd .."));
                }
            }

            String destFile = entry.getName();
            if (entry.getResource().isFile()
                    && getAccessManager().canDownload(request, entry)) {
                String tail = getStorageManager().getFileTail(entry);
                int    cnt  = 1;
                destFile = tail;
                //Handle duplicate file names
                while (seenFiles.contains(destFile)) {
                    destFile = "v" + (cnt++) + "_" + tail;
                }
                seenFiles.add(destFile);
                String path = request.getAbsoluteUrl(
                                  getEntryManager().getEntryResourceUrl(
                                      request, entry));

                path = HtmlUtils.urlEncodeSpace(path);
                String tmpFile = destFile + ".tmp";
                if ( !overwrite) {
                    sb.append("if ! test -e " + qt(destFile) + " ; then \n");
                }

                long size = entry.getResource().getFileSize();

                sb.append(cmd("echo "
                              + qt("downloading " + destFile + " ("
                                   + formatFileLength(size) + ")")));

                sb.append(cmd("touch " + qt(tmpFile)));

                command.append(sb, tmpFile, path);
                sb.append("if [[ $? != 0 ]] ; then\n");
                sb.append(cmd("echo" + " "
                              + qt("file download failed for " + destFile)));
                sb.append("exit $?\n");
                sb.append("fi\n");
                sb.append(cmd("mv " + qt(tmpFile) + " " + qt(destFile)));
                if ( !overwrite) {
                    sb.append("else\n");
                    sb.append(cmd("echo "
                                  + qt("File " + destFile
                                       + " already exists")));
                    sb.append("fi\n");
                }
            }

            for (List<String> pair : outputPairs) {
                String output = pair.get(0);
                String suffix = output;
                if (pair.size() > 1) {
                    suffix = pair.get(1);
                }
                String extraUrl = HtmlUtils.url(
                                      getEntryManager().getFullEntryShowUrl(
                                          request), ARG_ENTRYID,
                                              entry.getId(), ARG_OUTPUT,
                                              output);
                String destOutputFile = destFile + "." + suffix;

                if (output.equals(XmlOutputHandler.OUTPUT_XMLENTRY.getId())) {
                    if (wroteEntryXml) {
                        continue;
                    }
                    destOutputFile = "." + destFile + ".ramadda.xml";
                    appendDownloadMetadata(request, entry, sb, command);
                }


                sb.append(cmd("echo " + qt("downloading " + destOutputFile)));
                command.append(sb, destOutputFile, extraUrl);
            }


        }

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param command _more_
     * @param outputPairs _more_
     * @param includeGroupOutputs _more_
     *
     * @throws Exception _more_
     */
    private void writeGroupScript(Request request, Entry entry,
                                  StringBuilder sb, CurlCommand command,
                                  List<List<String>> outputPairs,
                                  boolean includeGroupOutputs)
            throws Exception {
        String dirName = IOUtil.cleanFileName(entry.getName());
        if (dirName.length() == 0) {
            dirName = entry.getId();
        }
        sb.append("if ! test -e " + qt(dirName) + " ; then \n");
        sb.append(cmd("mkdir " + qt(dirName)));
        sb.append("fi\n");
        sb.append(cmd("cd " + qt(dirName)));
        if (includeGroupOutputs) {
            //Make a .placeholder file so we force the harvest of the directory
            sb.append(cmd("touch " + qt(Harvester.FILE_PLACEHOLDER)));
            for (List<String> pair : outputPairs) {
                String output = pair.get(0);
                String suffix = output;
                if (pair.size() > 1) {
                    suffix = pair.get(1);
                }
                String destFile = "." + dirName;
                String extraUrl = HtmlUtils.url(
                                      getEntryManager().getFullEntryShowUrl(
                                          request), ARG_ENTRYID,
                                              entry.getId(), ARG_OUTPUT,
                                              output);
                String destOutputFile = destFile + "." + suffix;

                String message        = "downloading " + destOutputFile;
                if (output.equals(XmlOutputHandler.OUTPUT_XMLENTRY.getId())) {
                    destOutputFile = ".this.ramadda.xml";
                    appendDownloadMetadata(request, entry, sb, command);
                    message = "downloading metadata for " + dirName;
                }
                sb.append(cmd("echo " + qt(message)));

                command.append(sb, destOutputFile, extraUrl);
            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param command _more_
     *
     * @throws Exception _more_
     */
    private void appendDownloadMetadata(Request request, Entry entry,
                                        StringBuilder sb, CurlCommand command)
            throws Exception {
        for (String[] pair :
                getMetadataManager().getFilelUrls(request, entry)) {
            command.append(sb, "." + pair[0],
                           request.getAbsoluteUrl(pair[1]));
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private static String cmd(String s) {
        return s + ";\n";
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private static String qt(String s) {
        return "\"" + s + "\"";
    }

    /**
     * Get the MIME type for this output handler
     *
     * @param output  the output type
     *
     * @return  the MIME type
     */
    public String getMimeType(OutputType output) {
        return "application/x-sh";
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Aug 15, '14
     * @author         Enter your name here...
     */
    public static final class CurlCommand {

        /** _more_ */
        String command;

        /** _more_ */
        String args;

        /** _more_ */
        String outputArg;

        /**
         * _more_
         *
         * @param request _more_
         */
        public CurlCommand(Request request) {
            command   = request.getString(ARG_COMMAND, COMMAND_CURL);
            args      = command.equals(COMMAND_WGET)
                        ? ""
                        : " --progress-bar -k ";
            outputArg = command.equals(COMMAND_WGET)
                        ? "-O"
                        : command.equals(COMMAND_CURL)
                          ? "-o "
                          : "";
        }

        /**
         * _more_
         *
         * @param sb _more_
         * @param filename _more_
         * @param url _more_
         */
        public void append(StringBuilder sb, String filename, String url) {
            sb.append(cmd(command + args + " " + outputArg + " "
                          + qt(filename) + " " + qt(url)));
        }


    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public int getMaxEntryCount() {
        return 20000;
    }


}
