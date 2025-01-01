/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.harvester;


import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.harvester.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.util.SelectInfo;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.search.SearchProvider;
import org.ramadda.repository.type.*;
import org.ramadda.util.ChunkedAppendable;
import org.ramadda.util.FileWrapper;
import org.ramadda.util.FileInfo;

import org.ramadda.util.HtmlUtils;

import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 */
@SuppressWarnings("unchecked")
public class CommandHarvester extends Harvester {


    /** _more_ */
    public static final String TAB = "&nbsp;&nbsp;&nbsp;&nbsp;";

    /** _more_ */
    public static final String ATTR_TOKENS = "tokens";

    /** _more_ */
    public static final String ATTR_WEBHOOK = "webhook";


    /** _more_ */
    public static final String ATTR_ALLOW_CREATE = "allow_create";

    /** _more_ */
    public static final String ATTR_APITOKEN = "apitoken";



    /** _more_ */
    public static final String[] CMDS_SEARCH = { "search", "find" };

    /** _more_ */
    public static final String[] CMDS_PWD = { "pwd", "dir", "info" };

    /** _more_ */
    public static final String[] CMDS_CLEAR = { "clear" };

    /** _more_ */
    public static final String[] CMDS_HELP = { "help", "?" };


    /** _more_ */
    public static final String[] CMDS_DESC = { "desc" };

    /** _more_ */
    public static final String[] CMDS_APPEND = { "append" };

    /** _more_ */
    public static final String[] CMDS_LS = { "ls", "dir" };

    /** _more_ */
    public static final String[] CMDS_CD = { "cd", "go" };

    /** _more_ */
    public static final String[] CMDS_NEW = { "new", "create" };

    /** _more_ */
    public static final String[] CMDS_GET = { "get", "file" };

    /** _more_ */
    public static final String[] CMDS_VIEW = { "view" };


    /** _more_ */
    public static final String ARG_KEY = "ramadda_key";


    /** plain old command is a slack argument so we use ramadda_command */
    public static final String ARG_COMMAND = "ramadda_command";

    /** _more_ */
    public static final String ARG_TYPE = "ramadda_type";

    /** _more_ */
    private String tokens;

    /** _more_ */
    private String webHook;

    /** _more_ */
    private boolean allowCreate = false;


    /** _more_ */
    private String apiToken;


    /** _more_ */
    private Hashtable<String, CommandState> commandStates =
        new Hashtable<String, CommandState>();



    /** _more_ */
    public static final String[] ARGS_SINGLE = { "-help" };

    /** _more_ */
    public static final String[] ARGS_PAIR = { "-entry", "-provider" };


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception On badness
     */
    public CommandHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
        setActiveOnStart(true);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badness
     */
    public CommandHarvester(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String getCommandTypeName() {
        return "Command";

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getMessageLimit() {
        return 100000;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getNoop(CommandRequest request) throws Exception {
        return new Result("", Constants.MIME_TEXT);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return "Command Harvester";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result getUsage(CommandRequest request, String msg)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        appendUsageMessage(request, sb);
        msg = encodeWarning(msg, sb.toString());

        return message(msg);
    }

    /**
     * _more_
     *
     * @param title _more_
     * @param s _more_
     *
     * @return _more_
     */
    public String encodeWarning(String title, String s) {
        if (Utils.stringDefined(title)) {
            return title + "\n" + s;
        }

        return s;
    }


    /**
     * _more_
     *
     * @param title _more_
     * @param s _more_
     *
     * @return _more_
     */
    public String encodeInfo(String title, String s) {
        if (Utils.stringDefined(title)) {
            return title + "\n" + s;
        }

        return s;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param line _more_
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public void formatCommandHelp(CommandRequest request, Appendable sb,
                                  String line, String... args)
            throws Exception {
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                tmp.append("&nbsp;&nbsp;&nbsp;");
            } else {
                tmp.append(" ");
            }
            tmp.append(args[i]);
        }


        StringBuilder lineSB = new StringBuilder();
        String        cmd    = request.getCommand();
        if ( !Utils.stringDefined(cmd)) {
            cmd = "/ramadda";
        }
        lineSB.append(TAB);
        lineSB.append("<i>");
        lineSB.append(cmd);
        lineSB.append(" ");
        lineSB.append(line);
        lineSB.append("</i>");

        sb.append("<tr>");


        sb.append("<td>");
        sb.append(lineSB);
        sb.append("</td>");

        if (tmp.length() > 0) {
            sb.append("<td>");
            sb.append(tmp);
            sb.append("</td>");
        }
        sb.append("</tr>");
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getHelpUrl() {
        return null;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception On badness
     */
    public void appendUsageMessage(CommandRequest request, Appendable sb)
            throws Exception {


        String user = (request.getFrom() != null)
                      ? request.getFrom()
                      : getCommandTypeName() + " user";
        sb.append("Hello " + user + ". ");
        sb.append("Welcome to "
                  + makeLink(request, getRepository().getUrlBase(),
                             "RAMADDA") + ".");
        String help = getHelpUrl();
        if (help != null) {
            sb.append("  ");
            sb.append(makeLink(request, help, "View help") + ".");
        }
        sb.append("\n");
        String searchUrl =
            request.getRequest().makeUrl(getSearchManager().URL_SEARCH_FORM)
            + "?show_providers=true";
        String searchHref =
            makeLink(request, searchUrl,
                     "View all "
                     + getRepository().getSearchManager().getSearchProviders()
                         .size() + " sources");
        sb.append(HtmlUtils.b("Search") + "\n");
        sb.append("<table>");
        formatCommandHelp(request, sb,
                          "search  -&lt;source id&gt; search terms",
                          searchHref);
        formatCommandHelp(request, sb, "search  -atlassian codegeist",
                          "Search Atlassian Confluence site");
        formatCommandHelp(request, sb, "search  -apache -spring bugs ",
                          "Search Apache and Spring JIRA sites");
        formatCommandHelp(request, sb,
                          "search   -wolfram -google -youtube bugs", "");
        sb.append("</table>");
        sb.append(HtmlUtils.b("Navigate") + "\n");
        sb.append("<table>");
        formatCommandHelp(request, sb, "ls", "List the entries");
        formatCommandHelp(request, sb, "pwd", "Show current directory");
        formatCommandHelp(request, sb,
                          "cd &lt;some entry number or name&gt;",
                          "Change working directory");
        formatCommandHelp(request, sb, "get", "Get the file");
        if (allowCreate) {
            formatCommandHelp(
                request, sb,
                "new (folder or post or wiki or note) Title of entry | Description  ");
        }
        formatCommandHelp(request, sb, "cd; ls;  get -entry 5");
        sb.append("</table>");
        sb.append(HtmlUtils.b("Display") + "\n");
        sb.append("<table>");
        formatCommandHelp(request, sb,
                          "view  &lt;-help&gt;  &lt;-entry #&gt; ");
        formatCommandHelp(request, sb,
                          "search -fred gdp per capita burundi; cd 1; view;");
        formatCommandHelp(
            request, sb,
            "search  -eia  annual coal production wyoming; view -entry 1 ");
        sb.append("</table>");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean getDefaultActiveOnStart() {
        return true;
    }

    /**
     * _more_
     *
     * @param arg _more_
     *
     * @return _more_
     */
    @Override
    public boolean showWidget(String arg) {
        return arg.equals(ATTR_ACTIVEONSTART);
    }


    /**
     * _more_
     *
     * @param command _more_
     * @param commands _more_
     *
     * @return _more_
     */
    public boolean isCommand(String command, String[] commands) {
        if (command == null) {
            return false;
        }
        for (String s : commands) {
            if (s.equals(command)) {
                return true;
            }
        }

        return false;
    }

    /**
     * _more_
     *
     * @param msg _more_
     */
    public void debug(String msg) {
        //        System.err.println("CommandHarvester: " + msg);
    }


    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception On badness
     */
    protected void init(Element element) throws Exception {
        super.init(element);
        tokens      = Utils.getAttributeOrTag(element, ATTR_TOKENS, tokens);
        webHook     = XmlUtil.getAttribute(element, ATTR_WEBHOOK, webHook);
        allowCreate = XmlUtil.getAttribute(element, ATTR_ALLOW_CREATE, false);
        apiToken    = XmlUtil.getAttribute(element, ATTR_APITOKEN, apiToken);
    }





    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception On badness
     */
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        if (tokens != null) {
            Element node = XmlUtil.create(ATTR_TOKENS, element);
            node.appendChild(XmlUtil.makeCDataNode(node.getOwnerDocument(),
                    tokens, false));
            //            element.setAttribute(ATTR_TOKENS, tokens);
        }
        element.setAttribute(ATTR_ALLOW_CREATE, allowCreate + "");
        if (webHook != null) {
            element.setAttribute(ATTR_WEBHOOK, webHook);
        }
        if (apiToken != null) {
            element.setAttribute(ATTR_APITOKEN, apiToken);
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception On badness
     */
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);
        tokens      = request.getString(ATTR_TOKENS, tokens);
        webHook     = request.getString(ATTR_WEBHOOK, webHook);
        allowCreate = request.get(ATTR_ALLOW_CREATE, allowCreate);
        apiToken    = request.getString(ATTR_APITOKEN, apiToken);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception On badness
     */
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        super.createEditForm(request, sb);

        addBaseGroupSelect(ATTR_BASEGROUP, sb);
        sb.append(HtmlUtils.formEntry(msgLabel("API Tokens"),
                                      HtmlUtils.textArea(ATTR_TOKENS,
                                          (tokens == null)
                                          ? ""
                                          : tokens, 4, 60) + " "
                                          + "API tokens. One per line"));

        sb.append(HtmlUtils.formEntry(msgLabel("Incoming Web Hook URL"),
                                      HtmlUtils.input(ATTR_WEBHOOK,
                                          (webHook == null)
                                          ? ""
                                          : webHook, HtmlUtils.SIZE_70)));


        sb.append(HtmlUtils.formEntry(msgLabel("API Token"),
                                      HtmlUtils.password(ATTR_APITOKEN,
                                          (getApiToken() == null)
                                          ? ""
                                          : getApiToken(), HtmlUtils
                                          .SIZE_70)));



        sb.append(
            HtmlUtils.formEntry(
                "",
                HtmlUtils.checkbox(ATTR_ALLOW_CREATE, "true", allowCreate)
                + " "
                + msgLabel(
                    "Allow creating wiki pages, notes, blog posts, etc")));

    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String getAuthToken(CommandRequest request) throws Exception {
        return request.getRequest().getString(ARG_KEY, (String) null);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String getText(CommandRequest request) throws Exception {
        return null;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String getChannelId(CommandRequest request) throws Exception {
        return null;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result handleRequest(Request request) throws Exception {
        CommandRequest cmdRequest = doMakeCommandRequest(request);

        return handleRequest(cmdRequest);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public boolean isEnabled() throws Exception {
        return Utils.stringDefined(getWebHook());
    }

    /**
     * _more_
     *
     * @param cmdRequest _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public boolean canHandle(CommandRequest cmdRequest) throws Exception {
        if ( !isEnabled()) {
            return false;
        }

        if ( !Utils.stringDefined(tokens)) {
            return false;
        }

        String tokenFromRequest = getAuthToken(cmdRequest);
        if (tokenFromRequest == null) {
            return false;
        }

        boolean ok = false;

        for (String token : Utils.split(tokens, "\n", true, true)) {
            if (Misc.equals(token, tokenFromRequest)) {
                //                debug("request token:" +tokenFromRequest +" my:" + token);
                ok = true;

                break;
            }
            String fromProp = getRepository().getProperty(token,
                                  (String) null);
            if (fromProp != null) {
                ok = true;

                break;
            }
        }

        return ok;
    }


    /**
     * _more_
     *
     * @param cmdRequest _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result handleRequest(CommandRequest cmdRequest) throws Exception {
        if ( !canHandle(cmdRequest)) {
            return null;
        }

        System.err.println("CommandHarvester.handleRequest OK request= "
                           + cmdRequest.getRequest());
        Request request         = cmdRequest.getRequest();
        String  textFromRequest = getText(cmdRequest);
        List<String> commandToks = Utils.split(textFromRequest, ";", false,
                                       false);
        if (commandToks.size() == 0) {
            commandToks.add("");
        }
        //        debug("text:" + textFromRequest);
        //        debug("command toks:" + commandToks);
        // /cd foo; ls; new wiki name|description
        Result result = null;
        //debug("request:" + request);
        for (String commandTok : commandToks) {
            //            debug("command tok:" + commandTok);
            commandTok = commandTok.trim();
            if (commandTok.length() == 0) {
                continue;
            }
            if (commandTok.startsWith("/")) {
                commandTok = commandTok.substring(1);
            }
            String       text = commandTok;
            String       cmd  = request.getString(ARG_COMMAND, (String) null);
            List<String> toks;
            if (cmd == null) {
                toks = Utils.splitUpTo(text, " ", 2);
                if (toks.size() == 0) {
                    return getUsage(cmdRequest, "No command given");
                }
                cmd = toks.get(0);
                toks.remove(0);
                if (toks.size() > 0) {
                    text = toks.get(0);
                } else {
                    text = "";
                }
            }



            //            debug("checking command:" + cmd);
            if (isCommand(cmd, CMDS_SEARCH)) {
                result = processSearch(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_LS)) {
                result = processLs(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_PWD)) {
                result = processPwd(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_CLEAR)) {
                result = processClear(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_HELP)) {
                result = getUsage(cmdRequest, "");
            } else if (isCommand(cmd, CMDS_DESC)) {
                result = processDesc(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_GET)) {
                result = processGet(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_VIEW)) {
                result = processView(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_APPEND)) {
                result = processAppend(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_NEW)) {
                if ( !allowCreate) {
                    return message(
                        "Sorry, but creating new entries is not allowed");
                }
                result = processNew(cmdRequest, text);
            } else if (isCommand(cmd, CMDS_CD)) {
                result = processCd(cmdRequest, text);
            } else {
                result = getUsage(cmdRequest, "Unknown command: " + cmd);
            }
            //Remove any default args
            request.remove(ARG_COMMAND);
            request.remove(ARG_TYPE);
        }

        //TODO: 
        if (result != null) {
            return result;
        }

        return getUsage(cmdRequest, "Unknown command: " + textFromRequest);


    }


    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result message(String msg) throws Exception {
        msg = encodeMessage(msg);

        return new Result(msg, Constants.MIME_TEXT);
    }


    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public String encodeMessage(String msg) {
        return msg;
    }



    /**
     * _more_
     *
     * @param cmdRequest _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processSearch(CommandRequest cmdRequest, String text)
            throws Exception {
        CommandHarvester.Args args = parseArgs(cmdRequest, text);
        if ( !Utils.stringDefined(args.getText())) {
            return getUsage(cmdRequest, "Need to specify search string");
        }
        if (args.isHelp()) {
            String seachUrl =
                cmdRequest.getRequest()
                    .getAbsoluteUrl(cmdRequest.getRequest()
                        .makeUrl(getSearchManager()
                            .URL_SEARCH_FORM) + "?show_providers=true");

            StringBuffer help    = new StringBuffer();
            String       command = cmdRequest.getCommand();
            help.append(
                "<i>&nbsp;&nbsp;&nbsp;/r search -<provider id> search terms</i>\n");
            help.append(
                "Or:\n<i>&nbsp;&nbsp;&nbsp;/r search -google|-flickr|-youtube|-ncbi_pubmed) search terms</i>\n");
            help.append(makeLink(cmdRequest, seachUrl, "View all providers"));
            help.append("\n");

            return message(encodeInfo("Search Help",
                                      help.toString().replaceAll("/r",
                                          command)));

        }
        Request request = cmdRequest.getRequest();

        request = request.cloneMe();
        request.put(ARG_TEXT, args.getText());

        StringBuilder message = new StringBuilder("Search results");


        String providerId = Utils.getArg("-provider", args.getArgs(), null);
        if (providerId != null) {
            request.put(SearchManager.ARG_PROVIDER, providerId);
            SearchProvider provider =
                getSearchManager().getSearchProvider(providerId);
            if (provider != null) {
                message.append(" from ");
                if (provider.getSiteUrl() != null) {
                    message.append(makeLink(cmdRequest,
                                            provider.getSiteUrl(),
                                            provider.getName()));
                } else {
                    message.append(provider.getName());
                }
            }
        } else {
            //TODO: put this in a map
            boolean didone = false;
            for (SearchProvider provider :
                    getRepository().getSearchManager().getSearchProviders()) {
                if (args.getArgs().contains("-" + provider.getId())) {
                    if ( !didone) {
                        didone = true;
                        message.append(" from ");
                    } else {
                        message.append(" and ");
                    }
                    if (provider.getSiteUrl() != null) {
                        message.append(makeLink(cmdRequest,
                                provider.getSiteUrl(), provider.getName()));
                    } else {
                        message.append(provider.getName());
                    }
                    request.putMultiples(SearchManager.ARG_PROVIDER,
                                         provider.getId());
                }
            }
        }

        request.put(ARG_MAX, cmdRequest.getSearchLimit());


        request.put("command.args", args.getArgs());

        List<Entry> entries = getSearchManager().doSearch(request, new SelectInfo(request));
        List<String> ids     = new ArrayList<String>();
        for (Entry entry : entries) {
            ids.add(entry.getId());
        }
        if (ids.size() > 0) {
            getState(cmdRequest, null, true).list = ids;
        }

        //        System.err.println("CommandHarvester: processSearch:" + pair[0]);


        return makeEntryResult(getRepository(), cmdRequest,
                               message.toString(), entries, getWebHook(),
                               false);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param url _more_
     * @param label _more_
     *
     * @return _more_
     */
    public String makeLink(CommandRequest request, String url, String label) {
        return HtmlUtils.href(request.getRequest().getAbsoluteUrl(url),
                              label);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param request _more_
     * @param message _more_
     * @param entries _more_
     * @param webHook _more_
     * @param showChildren _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result makeEntryResult(Repository repository,
                                  CommandRequest request, String message,
                                  List<Entry> entries, String webHook,
                                  boolean showChildren)
            throws Exception {
        return message("TBD");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public CommandHarvester.Args parseArgs(CommandRequest request,
                                           String text)
            throws Exception {
        List<String>          toks    = Utils.split(text, " ", true, true);
        CommandHarvester.Args args    = new CommandHarvester.Args(toks, null);
        String                entryId = null;
        StringBuilder         textSB  = new StringBuilder();
        String                lastTok = null;
        for (int i = 0; i < toks.size(); i++) {
            String tok = toks.get(i);
            if (tok.startsWith("-")) {
                if (getSearchManager().getSearchProvider(tok.substring(1))
                        != null) {
                    continue;
                }
                if (i < toks.size() - 1) {
                    i++;

                    continue;
                }
            }
            lastTok = tok;
            if (Utils.stringDefined(tok)) {
                textSB.append(tok);
                textSB.append(" ");
            }
        }

        args.setText(textSB.toString().trim());

        String tmpId = Utils.getArg("-entry", toks, (String) null);

        if (tmpId != null) {
            entryId = tmpId;
        }

        if (Utils.stringDefined(entryId)) {
            args.setEntry(getEntryFromInput(request, entryId));
        } else {
            args.setEntry(getCurrentEntry(request));
        }

        return args;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Result processDesc(CommandRequest request, String text)
            throws Exception {
        CommandHarvester.Args args  = parseArgs(request, text);
        Entry                 entry = args.getEntry();
        if (entry == null) {
            return getUsage(request, "No current entry");
        }

        String desc = "Description:" + entry.getDescription();
        if ( !Utils.stringDefined(desc)) {
            desc = "    ";
        }

        return makeEntryResult(getRepository(), request, desc, null,
                               getWebHook(), false);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Result processGet(CommandRequest request, String text)
            throws Exception {
        CommandHarvester.Args args  = parseArgs(request, text);
        Entry                 entry = args.getEntry();
        if (entry == null) {
            return getUsage(request, "No current entry");
        }
        if ( !entry.isFile()) {
            return message("RAMADDA entry not a file");
        }
        File file = new File(entry.getResource().getPath());
        //Cap at 10MB
        if (file.length() > 1000000 * 10) {
            return message("Sorry, for now too big of a file");
        }

        return sendFile(request, entry, file, getChannelId(request),
                        entry.getName(), entry.getDescription());

    }





    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result processView(final CommandRequest request, String text)
            throws Exception {

        CommandHarvester.Args args  = parseArgs(request, text);

        Entry                 entry = args.getEntry();
        if (entry == null) {
            return getUsage(request, "No current entry");
        }
        final List<FileInfo> files = new ArrayList<FileInfo>();

        ChunkedAppendable appendable =
            new ChunkedAppendable(getMessageLimit());

        entry.getTypeHandler().processCommandView(request, entry, this,
                args.getArgs(), appendable, files);

        List<StringBuilder> sbs = appendable.getBuffers();
        if (args.isHelp()) {
            String msg = sbs.get(0).toString();
            if ( !Utils.stringDefined(msg)) {
                StringBuffer sb = new StringBuffer();
                appendUsageMessage(request, sb);
                msg = sb.toString();
            }

            return message(msg);
        }

        String message = sbs.get(0).toString();
        if (Utils.stringDefined(message)) {
            postMessage(message);
        }

        Result result = null;
        if (files.size() > 0) {
            for (FileInfo file : files) {
                sendFile(request, entry, file.getFile().getFile(),
                         getChannelId(request), file.getTitle(),
                         file.getDescription());
            }

            return getNoop(request);
        }


        if (true) {
            return getNoop(request);
        }

        /*
        if ( !changedText[0] && (files.size() == 0)) {
            if (entry.getResource().isImage()) {
                File file = new File(entry.getResource().getPath());

                return sendFile(request, entry, file, getChannelId(request), entry.getName(),
                                entry.getDescription());
            }
        }
        */

        String header = "Entry: " + entry.getName() + "\n";
        for (StringBuilder sb : sbs) {
            StringBuilder msg = new StringBuilder();
            msg.append("```");
            msg.append(header);
            msg.append(sb);
            msg.append("\n```");
            result = makeEntryResult(getRepository(), request,
                                     msg.toString(), null, getWebHook(),
                                     true);
            header = "Continued...\n";
        }

        if (result == null) {
            result = message("Hmmm, nothing here");
        }

        return result;

    }


    /**
     * _more_
     *
     * @param s _more_
     * @param cnt _more_
     *
     * @return _more_
     */
    private String pad(String s, int cnt) {
        return StringUtil.padLeft(s, cnt);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Result processAppend(CommandRequest request, String text)
            throws Exception {
        Entry entry = getCurrentEntry(request);
        if (entry == null) {
            return getUsage(request, "No current entry");
        }

        getEntryManager().appendText(getRequest(), entry, text);

        return makeEntryResult(getRepository(), request, "Text appended",
                               null, getWebHook(), false);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception On badness
     */
    public void clearState(CommandRequest request) throws Exception {
        commandStates.remove(getStateKey(request));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public CommandState getState(CommandRequest request) throws Exception {
        return getState(request, null, false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param theEntry _more_
     * @param createIfNeeded _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public CommandState getState(CommandRequest request, Entry theEntry,
                                 boolean createIfNeeded)
            throws Exception {
        String       key   = getStateKey(request);
        CommandState state = commandStates.get(key);
        if ((state == null) && createIfNeeded) {
            commandStates.put(key, state = new CommandState(theEntry));
        }
        if ((state != null) && (theEntry != null)) {
            state.entry = theEntry;
        }

        return state;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Entry getCurrentEntry(CommandRequest request) throws Exception {
        CommandState state = getState(request);
        if ((state != null) && (state.entry != null)) {
            return state.entry;
        }

        return getBaseGroup();
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private List<String> getCurrentList(CommandRequest request)
            throws Exception {
        CommandState state = getState(request);
        if (state == null) {
            return null;
        }

        return state.list;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getStateKey(CommandRequest request) {
        return "";
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Result processLs(CommandRequest request, String text)
            throws Exception {
        CommandHarvester.Args args   = parseArgs(request, text);
        Entry                 parent = args.getEntry();

        List<Entry> children =
            getEntryManager().getChildren(request.getRequest(), parent);
        if (children.size() == 0) {
            return message("No children entries");
        }

        return makeEntryResult(getRepository(), request, "Listing", children,
                               getWebHook(), false);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Entry getEntryFromInput(CommandRequest request, String text)
            throws Exception {

        text = text.trim();

        //        System.err.println("getEntryFromInput:" + text);
        Entry currentEntry = getCurrentEntry(request);
        if (text.matches("\\d+")) {
            List<String> list = getCurrentList(request);
            if (list != null) {
                int index = Integer.parseInt(text) - 1;
                if ((index >= 0) && (index < list.size())) {
                    text = list.get(index);
                }
            }
        }


        //Check for an ID
        Entry entry = getEntryManager().getEntry(request.getRequest(), text);
        if (entry != null) {
            return entry;
        }


        entry = getEntryManager().getRelativeEntry(request.getRequest(),
                getBaseGroup(), currentEntry, text);
        if (entry != null) {
            return entry;
        }

        if (Utils.stringDefined(text)) {
            entry = getEntryManager().getEntryFromAlias(request.getRequest(),
                    text);
            if (entry != null) {
                return entry;
            }
        }

        //TODO: give an error

        return currentEntry;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Result processCd(CommandRequest request, String text)
            throws Exception {
        if (text.indexOf("-help") >= 0) {
            return message("/r cd <entry # or name> e.g.:\ncd 2/4; ");
        }

        Entry theEntry = null;
        if ( !Utils.stringDefined(text)) {
            theEntry = getBaseGroup();
        } else {
            theEntry = getEntryFromInput(request, text);
            //            CommandHarvester.Args args  = parseArgs(request, text);
            //            theEntry = args.getEntry();
        }

        if (theEntry == null) {
            return message("No such entry:" + text);
        }

        CommandState state = getState(request, theEntry, true);

        Result result = makeEntryResult(getRepository(), request,
                                        "Current entry:", toList(theEntry),
                                        getWebHook(), true);
        List<Entry> children = theEntry.getChildren();
        if (children != null) {
            List<String> ids = new ArrayList<String>();
            for (Entry child : children) {
                ids.add(child.getId());
            }
            state.list = ids;
        }
        //        System.err.println ("Child ids:" + state.list);

        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Result processPwd(CommandRequest request, String text)
            throws Exception {
        Entry entry = getCurrentEntry(request);

        return makeEntryResult(getRepository(), request, "Current entry:",
                               toList(entry), getWebHook(), true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Result processClear(CommandRequest request, String text)
            throws Exception {
        clearState(request);

        return processCd(request, "");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param text _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Result processNew(CommandRequest request, String text)
            throws Exception {
        Entry        parent = getCurrentEntry(request);
        StringBuffer sb     = new StringBuffer();

        List<String> toks;
        //new <type> <name>|<description>
        String type = request.getRequest().getString(ARG_TYPE, (String) null);
        if (type == null) {
            toks = Utils.splitUpTo(text, " ", 2);
            if (toks.size() == 0) {
                return getUsage(
                    request,
                    "new <folder or post or wiki or note> name|description");
            }
            type = toks.get(0);
            toks.remove(0);
            if (toks.size() > 0) {
                text = toks.get(0);
            } else {
                text = "";
            }
        }
        toks = Utils.splitUpTo(text, "|", 2);
        String name = toks.get(0);
        String desc = (toks.size() > 1)
                      ? toks.get(1)
                      : "";
        desc = desc.replace("\\n", "\n");
        String[] cmds = { "folder", "wiki", "post", "note" };
        String[] types = { TypeHandler.TYPE_GROUP, "wikipage", "blogentry",
                           "notes_note" };
        String theType = null;
        for (int i = 0; i < cmds.length; i++) {
            if (type.equals(cmds[i])) {
                theType = types[i];

                break;
            }
        }

        if (theType == null) {
            return getUsage(request,
                            "new <folder|post|wiki|note> name;description");
        }



        StringBuffer msg = new StringBuffer();
        Entry entry      = addEntry(request, parent, theType, name, desc,
                                    msg);
        if (entry == null) {
            return getUsage(request, msg.toString());
        }
        getState(request, entry, true);

        return makeEntryResult(getRepository(), request, "New entry:",
                               toList(entry), getWebHook(), true);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param type _more_
     * @param name _more_
     * @param desc _more_
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    private Entry addEntry(CommandRequest request, Entry parent, String type,
                           String name, String desc, Appendable msg)
            throws Exception {
        if ( !parent.isGroup()) {
            msg.append("ERROR: Not a folder:\n" + parent.getName());

            return null;
        }

        TypeHandler typeHandler = null;
        if (type != null) {
            typeHandler = getRepository().getTypeHandler(type);
        } else {
            typeHandler = getRepository().getTypeHandler("file");
        }
        Entry    entry  = typeHandler.createEntry(getRepository().getGUID());
        Date     date   = new Date();
        Object[] values = typeHandler.makeEntryValues(new Hashtable());
        if (type.equals("wikipage")) {
            values[0] = desc;
            desc      = "";
        }
        entry.initEntry(name, desc, parent, getUser(), new Resource(""), "",
                        Entry.DEFAULT_ORDER, date.getTime(), date.getTime(),
                        date.getTime(), date.getTime(), values);
        List<Entry> entries = (List<Entry>) Misc.newList(entry);
        getEntryManager().addNewEntries(getRequest(), entries);

        return entry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public String getEntryUrl(Request request, Entry entry) throws Exception {
        return request.getAbsoluteUrl(
            request.entryUrl(getRepository().URL_ENTRY_SHOW, entry));
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private List<Entry> toList(Entry entry) {
        List<Entry> l = new ArrayList<Entry>();
        l.add(entry);

        return l;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public CommandRequest doMakeCommandRequest(Request request)
            throws Exception {
        return new CommandRequest(request);
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Apr 3, '15
     * @author         Enter your name here...
     */
    public static class CommandRequest {

        /** _more_ */
        Request request;

        /**
         * _more_
         *
         * @param request _more_
         */
        public CommandRequest(Request request) {
            this.request = request;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String getCommand() {
            return "/ramadda";
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Request getRequest() {
            return request;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getSearchLimit() {
            return 100;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getFrom() {
            return null;
        }

    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, May 2, '15
     * @author         Enter your name here...
     */
    public static class CommandState {

        /** _more_ */
        private Entry entry;

        /** _more_ */
        private List<String> list;


        /**
         * _more_
         *
         * @param entry _more_
         */
        public CommandState(Entry entry) {
            this.entry = entry;
        }
    }




    /**
     * Send the file
     *
     *
     * @param cmdRequest _more_
     * @param entry _more_
     * @param file _more_
     * @param channel _more_
     * @param title _more_
     * @param desc _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result sendFile(CommandRequest cmdRequest, Entry entry, File file,
                           String channel, String title, String desc)
            throws Exception {
        System.err.println("No sendFile");

        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getWebHook() {
        if ( !Utils.stringDefined(webHook)) {
            return null;
        }
        String fromProp = getRepository().getProperty(webHook, (String) null);
        if (fromProp != null) {
            return fromProp;
        }

        return webHook;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getApiToken() {
        if ( !Utils.stringDefined(apiToken)) {
            return null;
        }
        String fromProp = getRepository().getProperty(apiToken,
                              (String) null);
        if (fromProp != null) {
            return fromProp;
        }

        return apiToken;

    }



    /**
     * Class description
     *
     *
     * @version        $version$, Sat, May 2, '15
     * @author         Enter your name here...
     */
    public static class Args {

        /** _more_ */
        private List<String> args;


        /** _more_ */
        private Entry entry;

        /** _more_ */
        private String text;



        /**
         * _more_
         *
         *
         * @param args _more_
         * @param entry _more_
         */
        public Args(List<String> args, Entry entry) {
            this.args  = args;
            this.entry = entry;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isHelp() {
            return args.contains("-help") || args.contains("?");
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public List<String> getArgs() {
            return args;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Entry getEntry() {
            return entry;
        }

        /**
         * _more_
         *
         * @param entry _more_
         */
        public void setEntry(Entry entry) {
            this.entry = entry;
        }



        /**
         * Set the Text property.
         *
         * @param value The new value for Text
         */
        public void setText(String value) {
            text = value;
        }

        /**
         * Get the Text property.
         *
         * @return The Text
         */
        public String getText() {
            return text;
        }



    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryHeader(CommandRequest request, Entry entry)
            throws Exception {
        String icon = request.getRequest().getAbsoluteUrl(
                          repository.getPageHandler().getIconUrl(
                              request.getRequest(), entry));

        return Utils.concatString(
            HtmlUtils.img(icon), " ",
            HtmlUtils.b(
                HtmlUtils.href(
                    getEntryUrl(request.getRequest(), entry),
                    entry.getName())), HtmlUtils.br());
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param args _more_
     * @param files _more_
     * @param rows _more_
     *
     * @throws Exception _more_
     */
    public void displayTabularData(CommandRequest request, Entry entry,
                                   List<String> args, List<FileInfo> files,
                                   List<List<Object>> rows)
            throws Exception {
        String entryHeader = getEntryHeader(request, entry);
        String header = entryHeader
                        + "<table cellspacing=0 cellpadding=5 border=0>";
        String        footer = "</table>";
        StringBuilder html   = new StringBuilder();
        html.append(header);

        int rowCnt     = 0;
        int thisRowCnt = 0;
        for (List<Object> row : rows) {
            StringBuffer rowSB = new StringBuffer("<tr>");
            for (Object value : row) {
                String s = Utils.concatString("&nbsp;", ((value == null)
                        ? ""
                        : value.toString()), "&nbsp;");
                if (rowCnt == 0) {
                    s = HtmlUtils.b(s);
                }
                HtmlUtils.td(rowSB, s, null);
            }
            rowSB.append("</td>");
            if (html.length() + rowSB.length() + footer.length()
                    > getMessageLimit()) {
                html.append(footer);
                //                System.err.println("   posting table");
                postMessage(html.toString());
                html       = new StringBuilder(header);
                thisRowCnt = 0;
            }
            rowCnt++;
            thisRowCnt++;
            html.append(rowSB);

        }
        html.append(footer);
        //        System.err.println("   posting table - end");
        postMessage(html.toString());
    }


    /**
     * _more_
     *
     * @param message _more_
     *
     * @throws Exception _more_
     */
    public void postMessage(String message) throws Exception {
        //noop
        throw new IllegalStateException(
            "CommandHarvester.postMessage not implemented");
    }




}
