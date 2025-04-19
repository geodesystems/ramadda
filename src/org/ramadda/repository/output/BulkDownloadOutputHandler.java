/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

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
@SuppressWarnings("unchecked")
public class BulkDownloadOutputHandler extends OutputHandler {

    /** The  output type */
    public static final OutputType OUTPUT_CURL =
        new OutputType("Bulk Download Script", "bulk.curl",
                       OutputType.TYPE_OTHER, "", ICON_FETCH);

    /** _more_ */
    public static final OutputType OUTPUT_WGET =
        new OutputType("Wget Download Script", "bulk.wget",
                       OutputType.TYPE_OTHER, "", ICON_FETCH);

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

    private static String downloadsh;

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
        addType(OUTPUT_WGET);
	downloadsh = getStorageManager().readUncheckedSystemResource("/org/ramadda/repository/resources/download.sh");
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
        if ( !request.getUser().getAdmin()) {
            return;
        }

        if (state.entry != null) {
            if (state.entry.getResource().isUrl()
                    || getAccessManager().canDownload(request, state.entry)) {
                links.add(makeLink(request, state.entry, OUTPUT_CURL));
            }
        } else {
            boolean ok = false;
            for (Entry child : state.getAllEntries()) {
                //For now add the bulk download link to any folder entry, even if it doesn't have file children
                ok = true;
                if (ok) {
                    break;
                }
            }

            if (ok) {
                //Maybe don't put this for the top level entries. 
                //Somebody will invariably come along and try to fetch everything
                if (state.group != null) {
                    links.add(makeLink(request, state.group, OUTPUT_WGET));
                    if ( !state.group.isTopEntry()) {
                        links.add(
                            makeLink(
                                request, state.group, OUTPUT_CURL,
                                "/"
                                + IO.stripExtension(
                                    state.group.getName()) + "_download.sh"));
                    }
                } else {
                    links.add(makeLink(request, state.group, OUTPUT_WGET));
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
        if ( !request.getUser().getAdmin()) {
            return new Result("", new StringBuilder("Only admin"));
        }
        request.setReturnFilename("download.sh");

        return outputGroup(request, outputType, null,
                           (List<Entry>) Utils.makeListFromValues(entry));
    }

    /**
     * Output a group of entries
     *
     * @param request    the Request
     * @param outputType the output type
     * @param group      the group (may be null)
     * @param children _more_
     *
     * @return  the result
     *
     * @throws Exception  problem creating the script
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {

        if ( !request.getUser().getAdmin()) {
            return new Result("", new StringBuilder("Only admin"));
        }
        //For the download get all children entries
        if ( !request.defined(ARG_MAX)) {
            request.put(ARG_MAX, "20000");
        }
        boolean wget = outputType.equals(OUTPUT_WGET);
        request.setReturnFilename("download.sh");

        StringBuilder sb        = new StringBuilder();
        boolean       recurse   = request.get(ARG_RECURSE, true);
        boolean       overwrite = request.get(ARG_OVERWRITE, false);
        process(request, sb, group, children, recurse, overwrite,
                new HashSet<String>(), wget);

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
     * @param wget _more_
     *
     * @throws Exception _more_
     */
    public void process(Request request, StringBuilder sb, Entry group,
                        List<Entry> entries, boolean recurse,
                        boolean overwrite, HashSet<String> seen, boolean wget)
            throws Exception {

        List<List<String>> outputPairs         =
            new ArrayList<List<String>>();
        boolean            includeGroupOutputs = false;
        for (String pair :
                Utils.split(request.getString(ARG_OUTPUTS, "xml.xmlentry"),
                            ",", true, true)) {
            outputPairs.add(Utils.splitUpTo(pair, ":", 2));
            String outputId = outputPairs.get(outputPairs.size()
                                  - 1).get(0).toString();
            if (outputId.equals(XmlOutputHandler.OUTPUT_XMLENTRY.getId())) {
                includeGroupOutputs = true;
            }
        }

        CurlCommand command = new CurlCommand(request, wget);
        command.init(request, sb);

        if (request.get(ARG_INCLUDEPARENT, true)) {
            writeGroupScript(request, group, sb, command, outputPairs,
                             includeGroupOutputs, wget);
        }

        process(request, sb, group, entries, recurse, overwrite, command,
                outputPairs, includeGroupOutputs, seen, wget);
        if (request.get(ARG_INCLUDEPARENT, true)) {
            sb.append(cmd("cd .."));
        }
    }

    private String sanitize(String s) {
	return s.replace("$","_dollar_");
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
     * @param wget _more_
     *
     * @throws Exception _more_
     */
    public void process(Request request, StringBuilder sb, Entry group,
                        List<Entry> entries, boolean recurse,
                        boolean overwrite, CurlCommand command,
                        List<List<String>> outputPairs,
                        boolean includeGroupOutputs, HashSet<String> seen,
                        boolean wget)
            throws Exception {

        HashSet seenFiles = new HashSet();
        for (Entry entry : entries) {
            if (seen.contains(entry.getId())) {
                continue;
            }
            //If this is a real entry (as opposed to the results of a search or selection) then check for entries like the virtual group
            if ( !group.isDummy()) {
                if ( !Utils.equals(entry.getParentEntryId(), group.getId())) {
                    continue;
                }
            }
            seen.add(entry.getId());
            if ( !wget) {
                if (getEntryManager().isSynthEntry(entry.getId())) {
                    continue;
                }
            }
            boolean wroteEntryXml = false;

            if (entry.isGroup()) {
                if ( !recurse) {
                    continue;
                }
                List<Entry> subEntries =
                    getEntryManager().getChildrenAll(request, entry,
						     new SelectInfo(request, entry, null, 20000,false));
                if (includeGroupOutputs || (subEntries.size() > 0)) {
                    wroteEntryXml = true;
                    writeGroupScript(request, entry, sb, command,
                                     outputPairs, includeGroupOutputs, wget);
                    process(request, sb, entry, subEntries, recurse,
                            overwrite, command, outputPairs,
                            includeGroupOutputs, seen, wget);
                    sb.append(cmd("cd .."));
                }
            }

            String destFile = sanitize(entry.getName());
            if (entry.getResource().isFile()
                    && getAccessManager().canDownload(request, entry)) {
                String tail = sanitize(getStorageManager().getFileTail(entry));
		//		System.err.println("DEST:" + destFile +" tail:" + tail);
                int    cnt  = 1;
                destFile = tail;
                //Handle duplicate file names
                while (seenFiles.contains(destFile)) {
                    destFile = "v" + (cnt++) + "_" + tail;
                }
                seenFiles.add(destFile);
                String path =
                    "${ROOT}"
                    + getEntryManager().getEntryResourceUrl(request, entry);

                path = HtmlUtils.urlEncodeSpace(path);
                if ( !overwrite) {
		    //                    sb.append("if ! test -e " + qt(destFile) + " ; then \n");
                }

                long size = entry.getResource().getFileSize(true);
                String msg = "downloading " + destFile + " ("
                             + formatFileLength(size) + ")";
                command.download(sb, msg, destFile, path,overwrite?-1:size);
                if ( !overwrite) {
		    /*
                    sb.append("else\n");
                    sb.append(cmd("\techo "
                                  + qt("File " + destFile
                                       + " already exists")));
                    sb.append("fi\n");
		    */
                }
            }

            for (List<String> pair : outputPairs) {
                String output = pair.get(0);
                String suffix = output;
                if (pair.size() > 1) {
                    suffix = pair.get(1);
                }
                String entryShowUrl =
                    request.makeUrl(getRepository().URL_ENTRY_SHOW);
                String extraUrl = "${ROOT}"
                                  + HtmlUtils.url(entryShowUrl, ARG_ENTRYID,
                                      entry.getId(), ARG_OUTPUT, output);
                String destOutputFile = destFile + "." + suffix;

                if (output.equals(XmlOutputHandler.OUTPUT_XMLENTRY.getId())) {
                    if (wroteEntryXml) {
                        continue;
                    }
                    destOutputFile = "." + destFile + ".ramadda.xml";
                    appendDownloadMetadata(request, entry, sb, command);
                }
                command.download(sb, "downloading " + destOutputFile,
                                 destOutputFile, extraUrl,-1);
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
     * @param wget _more_
     *
     * @throws Exception _more_
     */
    private void writeGroupScript(Request request, Entry entry,
                                  StringBuilder sb, CurlCommand command,
                                  List<List<String>> outputPairs,
                                  boolean includeGroupOutputs, boolean wget)
            throws Exception {
        String dirName = sanitize(IO.cleanFileName(entry.getName()));
        if (dirName.length() == 0) {
            dirName = entry.getId();
        }
        sb.append(mkdir(dirName));
        sb.append(line());
        sb.append(cmd("cd " + qt(dirName)));
        if (includeGroupOutputs) {
            //Make a .placeholder file so we force the harvest of the directory
            //            if ( !wget) {
            sb.append(cmd("touch " + qt(Harvester.FILE_PLACEHOLDER)));
            //            }
            for (List<String> pair : outputPairs) {
                String output = pair.get(0);
                String suffix = output;
                if (pair.size() > 1) {
                    suffix = pair.get(1);
                }
                String destFile = "." + dirName;
                String entryShowUrl =
                    request.makeUrl(getRepository().URL_ENTRY_SHOW);
                String extraUrl = "${ROOT}"
                                  + HtmlUtils.url(entryShowUrl, ARG_ENTRYID,
                                      entry.getId(), ARG_OUTPUT, output);
                String destOutputFile = destFile + "." + suffix;

                String message        = "downloading " + destOutputFile;
                if (output.equals(XmlOutputHandler.OUTPUT_XMLENTRY.getId())) {
                    destOutputFile = ".this.ramadda.xml";
                    appendDownloadMetadata(request, entry, sb, command);
                    message = "downloading metadata for " + dirName;
                }
                command.download(sb, message, destOutputFile, extraUrl,-1);
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
        for (String[] triple :
                getMetadataManager().getFilelUrls(request, entry)) {
	    //Use the full name
            command.download(sb, "downloading metadata " + triple[0],
                             ".metadata_" + triple[2], "${ROOT}" + triple[1],-1);
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
     *
     * @param s _more_
     *
     * @return _more_
     */
    private static String comment(String s) {
        return "#" + s + "\n";
    }

    /**
     *
     * @return _more_
     */
    private static String line() {
        return comment(
            "--------------------------------------------------------------------");
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
     *
     * @param dir _more_
     *
     * @return _more_
     */
    private static String mkdir(String dir) {
        return cmd("makedir " + qt(dir));
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
         * @param wget _more_
         */
        public CurlCommand(Request request, boolean wget) {
            command   = request.getString(ARG_COMMAND, wget
                    ? COMMAND_WGET
                    : COMMAND_CURL);
            args      = command.equals(COMMAND_WGET)
                        ? " -q "
                        : " --progress-bar -k -C - ";
            outputArg = command.equals(COMMAND_WGET)
                        ? " -O "
                        : command.equals(COMMAND_CURL)
                          ? "-o "
                          : "";
            if (command.equals(COMMAND_WGET)) {
                command = "\"" + command + "  --no-check-certificate \"";
            }
        }

        /**
         * _more_
         *
         *
         * @param request _more_
         * @param sb _more_
         */
        public void init(Request request, StringBuilder sb) {
	    sb.append("#!/bin/sh\n");
	    sb.append("#this is generated by RAMADDA\n");
	    sb.append("#\n");
            sb.append(cmd("export DOWNLOAD_COMMAND=" + command));
            sb.append(cmd("export ROOT=\"" + request.getAbsoluteUrl("")
                          + "\""));
	    sb.append(downloadsh.replace("${downloadargs}",args +" " + outputArg));
        }

        /**
         * _more_
         *
         * @param sb _more_
         * @param msg _more_
         * @param filename _more_
         * @param url _more_
         */
        public void download(StringBuilder sb, String msg, String filename,
                             String url, long size) {
            sb.append(cmd("download " + qt(msg) + " " + qt(filename) + " "
                          + qt(url)+" " + size));
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
