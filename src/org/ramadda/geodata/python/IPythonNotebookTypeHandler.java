/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.python;


import org.json.*;



import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;
import org.ramadda.util.Utils;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.ImageUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.ProcessRunner;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;


import ucar.unidata.util.StringUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 */

public class IPythonNotebookTypeHandler extends GenericTypeHandler {
    public static final String ARG_NOTEBOOKIFRAME= "notebookiframe";



    /**
     * @param repository   the Repository
     * @param node         the defining Element
     * @throws Exception   problems
     */
    public IPythonNotebookTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
	if(!isNew(newType)) return;
	try {
	    initializeNewEntryInner(request, entry);
	} catch(Exception exc) {
	    exc.printStackTrace();
	    getSessionManager().addSessionMessage(request,"Error initializing notebook:" +  exc); 
	    
	}
    }


    private void initializeNewEntryInner(Request request, Entry entry)
	throws Exception {
	extractThumbnail(request, entry);
    }

    private boolean extractThumbnail(Request request, Entry entry) throws Exception {
	JSONArray cells = getCells(entry);
        for (int i = 0; i < cells.length(); i++) {
            JSONObject    cell     = cells.getJSONObject(i);
            String        cellType = cell.getString("cell_type");
            if (!cell.has("outputs")) continue;
	    JSONArray outputs = cell.getJSONArray("outputs");
	    for (int outputIdx = 0; outputIdx < outputs.length();  outputIdx++) {
		JSONObject output = outputs.getJSONObject(outputIdx);
		String[] image  =getImage(output);
		if(image==null) {
		    if(output.has("data")) {
			JSONObject data = output.getJSONObject("data");
			image  =getImage(data);
		    }
		}
		if(image==null) continue;
		BufferedImage bufferedImage = ImageUtils.readBase64(image[0],image[1]);
		File tmpFile = getStorageManager().getTmpFile(request,"thumbnail." + image[0]);
		ImageUtils.writeImageToFile(bufferedImage, tmpFile);
		String fileName = getStorageManager().copyToEntryDir(entry,
								     tmpFile).getName();
		Metadata thumbnailMetadata =
		    new Metadata(getRepository().getGUID(), entry.getId(),
				 getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL), false,
				 fileName, null, null, null, null);
		getMetadataManager().addMetadata(request,entry, thumbnailMetadata);
		return true;
	    }
	}
	return false;
    }


    private String[]getImage(JSONObject obj) {
	String imageString=null;
	String imageType = "png";
	if (obj.has("png")) {
	    imageString=obj.getString("png");
	} else if (obj.has("image/png")) {
	    imageString=obj.getString("image/png");
	} else if (obj.has("jpg")) {
	    imageString=obj.getString("jpg");
	    imageType = "jpg";
	} else if (obj.has("text/html")) {
	    JSONArray a = obj.getJSONArray("text/html");
	    for(int i=0;i<a.length() && imageString==null;i++ ) {
		String html = a.getString(i);
		//	data:image/png;base64,
		String s=StringUtil.findPattern(html,".*(data:image[^\"]+)\".*");
		if(s!=null) {
		    int idx = s.indexOf(",");
		    if(idx>=0) {
			imageString=s.substring(idx);
			s = s.substring(0,idx);
			if(s.matches("(?i).*(jpg|jpeg)")) imageType="jpg";
		    }
		}
	    }
	} else if (obj.has("jpg")) {
	} 
	if(imageString==null) return null;
	return new String[]{imageType,imageString};

    }

    @Override
    public boolean applyEditCommand(Request request,Entry entry, String command,String...args) throws Exception {
	if(!command.equals("addthumbnail")) return super.applyEditCommand(request, entry, command,args);
	List<String> urls = new ArrayList<String>();
	getMetadataManager().getThumbnailUrls(request, entry, urls);
	if (urls.size() > 0) return false;
	addThumbnail(request, entry,true,-1);
	return true;
    }

    @Override
    public boolean addThumbnail(Request request, Entry entry, boolean deleteExisting, int width) throws Exception {
	getRepository().getMetadataManager().addThumbnail(request,entry,deleteExisting,width);
	return extractThumbnail(request,entry);
    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        if (tag.equals("notebook")) {
            return getHtmlDisplayInner(request, entry);
        }
        if (tag.equals(ARG_NOTEBOOKIFRAME)) {
	    StringBuilder sb     = new StringBuilder();
	    if(Utils.getProperty(props,"showTitle",true)) {
		getPageHandler().entrySectionOpen(request, entry, sb,null,true);
	    }
	    String height= Utils.getProperty(props,"height","800");
	    String url = HU.url(getEntryManager().getEntryUrl(request, entry),ARG_NOTEBOOKIFRAME,"true");
	    HU.open(sb,"iframe","class","ramadda-iframe-progress","src",url,"width","100%","height",height,"frameborder","0");
	    HU.close(sb,"iframe");
	    if(Utils.getProperty(props,"showTitle",true))
		getPageHandler().entrySectionClose(request, entry, sb,true);
	    return sb.toString();
	}


        return super.getWikiInclude(wikiUtil, request, originalEntry, entry,
                                    tag, props);
    }


    @Override
    public String getCorpus(Request request, Entry entry, CorpusType corpusType) throws Exception {
	JSONArray cells = getCells(entry);
	StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length(); i++) {
            JSONObject    cell     = cells.getJSONObject(i);
            String        cellType = cell.getString("cell_type");
	    if (cellType.equals("code")) {
		sb.append("The following is python code\n");
	    } else if (cellType.equals("markdown")) {
		sb.append("The following is markdown code\n");
	    } 
            if (cell.has("source")) {
                readLines(cell, "source", sb);
		sb.append("\n");
            } else if (cell.has("input")) {
                readLines(cell, "input", sb);
		sb.append("\n");
            }

	    //Don't do the outputs for now
            if (false && cell.has("outputs")) {
                JSONArray outputs = cell.getJSONArray("outputs");
		for (int outputIdx = 0; outputIdx < outputs.length();  outputIdx++) {
		   JSONObject output = outputs.getJSONObject(outputIdx);
		   String     type   = output.getString("output_type");
		   if (output.has("text")) {
		       try {
			   sb.append("The following is the output of the above code\n");
			   JSONArray text = output.getJSONArray("text");
			   readLines(text, sb);
		       } catch (Exception exc) {
		       }
		   }
	       }
	    }
	}
	return sb.toString();
    }

    @Override
    public Result getHtmlDisplay(Request request, Entry entry) throws Exception {
	if(!request.get(ARG_NOTEBOOKIFRAME,false)) return super.getHtmlDisplay(request,entry);
	StringBuilder sb = new StringBuilder();
	sb.append(getHtmlDisplayInner(request, entry));
	Result result = new Result("",sb);
	result.setShouldDecorate(false);
	return result;
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
    private String getHtmlDisplayInner(Request request, Entry entry)
            throws Exception {
        String jupyterPath =  getRepository().getScriptPath("ramadda.jupyter.path");
        if (jupyterPath != null) {
            //            System.err.println(jupyterPath);
            return renderNotebookWithJupyter(request, entry, jupyterPath);
        } else {
            return renderNotebook(request, entry);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String renderNotebookWithJupyter(Request request, Entry entry,
                                             String path)
            throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(path);
        commands.add("nbconvert");
        commands.add("--to");
        commands.add("html");
        commands.add("--stdout");
        commands.add(getStorageManager().getEntryFile(entry).toString());
        ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
        pb.redirectErrorStream(true);
        Process     process = pb.start();
        InputStream is      = process.getInputStream();
        String      html    = new String(IOUtil.readBytes(is));
        html = html.replaceAll(
            "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js\"></script>",
            "");
        html = html.replaceAll(
            "\\[NbConvertApp\\] Converting notebook .* to html", "");

        return html;
    }


    private JSONArray  getCells(Entry entry) throws Exception {
        String json = getStorageManager().readSystemResource(entry.getFile());
        JSONObject obj   = new JSONObject(new JSONTokener(json));
        JSONArray  cells = null;
        if (obj.has("cells")) {
            cells = obj.getJSONArray("cells");
        } else if (obj.has("worksheets")) {
            JSONArray  worksheets = obj.getJSONArray("worksheets");
            JSONObject worksheet  = worksheets.getJSONObject(0);
            cells = worksheet.getJSONArray("cells");
        }
	return cells;
    }


    private String renderNotebook(Request request, Entry entry)
            throws Exception {


        StringBuilder sb = new StringBuilder();
        HtmlUtils.importJS(sb, getRepository().getFileUrl("/lib/require.js"));

        sb.append(
            HtmlUtils.cssLink(
                "https://cdn.pydata.org/bokeh/release/bokeh-0.12.9.min.css"));
        sb.append(
            HtmlUtils.cssLink(
                "https://cdn.pydata.org/bokeh/release/bokeh-widgets-0.12.9.min.css"));
        sb.append(
            HtmlUtils.cssLink(
                "https://cdn.pydata.org/bokeh/release/bokeh-tables-0.12.9.min.css"));

        HtmlUtils.importJS(
            sb, "https://cdn.pydata.org/bokeh/release/bokeh-0.12.9.min.js");
        HtmlUtils.importJS(
            sb,
            "https://cdn.pydata.org/bokeh/release/bokeh-widgets-0.12.9.min.js");
        HtmlUtils.importJS(
            sb,
            "https://cdn.pydata.org/bokeh/release/bokeh-tables-0.12.9.min.js");


        sb.append(
            HtmlUtils.cssLink(
                getRepository().getFileUrl("/python/python.css")));
        sb.append(
            HtmlUtils.importJS(
                getRepository().getHtdocsUrl("/lib/prettify/prettify.js")));
        sb.append(
            HtmlUtils.cssLink(
                getRepository().getHtdocsUrl("/lib/prettify/prettify.css")));


	sb.append(HU.importCss(".notebook-container {margin-top:5px;padding: 15px;background-color: #fff;min-height: 0; -webkit-box-shadow: 0px 0px 12px 1px rgba(87, 87, 87, 0.2); box-shadow: 0px 0px 12px 1px rgba(87, 87, 87, 0.2);}"));


        //        getPageHandler().entrySectionOpen(request, entry, sb, "IPython Notebook", true);
	sb.append("<div class='notebook-container'>");
        sb.append("<table width=100%>");
	JSONArray cells = getCells(entry);
        int index = 0;
        for (int i = 0; i < cells.length(); i++) {
            JSONObject    cell     = cells.getJSONObject(i);
            String        cellType = cell.getString("cell_type");
            String        input    = null;

            StringBuilder ssb      = new StringBuilder();
            if (cell.has("source")) {
                readLines(cell, "source", ssb);
                input = ssb.toString();
            } else if (cell.has("input")) {
                readLines(cell, "input", ssb);
                input = ssb.toString();
            }
            if (input != null) {
                boolean showTag = true;
                if (cellType.equals("code")) {
                    input = HtmlUtils.pre(input,
                                          HtmlUtils.cssClass("prettyprint"));
                } else if (cellType.equals("markdown")) {
                    //TODO: process markdown
                    input   = processMarkdown(input);
                    showTag = false;
                } else {
                    input = HtmlUtils.pre(input, "");
                }
                sb.append("<tr valign=top>");
                if (showTag) {
                    index++;
                    sb.append(
                        HtmlUtils.td(
                            HtmlUtils.div(
                                "In&nbsp;[" + (index) + "]:",
                                HtmlUtils.cssClass(
                                    "ipynb-input-label")), "align=right width=10%"));
                    sb.append(
                        HtmlUtils.td(
                            HtmlUtils.div(
                                input,
                                HtmlUtils.cssClass(
                                    "ipynb-input")), " width=90% "));
                } else {
                    sb.append(HtmlUtils.td("", "align=right width=10%"));
                    sb.append(
                        HtmlUtils.td(
                            HtmlUtils.div(
                                input,
                                HtmlUtils.cssClass(
                                    "xxx-ipynb-input")), " width=90% "));
                }
                sb.append("</tr>\n");
            }

            if (cell.has("outputs")) {
                JSONArray outputs = cell.getJSONArray("outputs");
                for (int outputIdx = 0; outputIdx < outputs.length();
                        outputIdx++) {
                    JSONObject output = outputs.getJSONObject(outputIdx);
                    String     type   = output.getString("output_type");
                    if (output.has("text")) {
                        processText(request, output, sb, index);
                    }
                    if (output.has("png")) {
                        processPng(request, output, sb, index);
                    }		    
                }
            }
        }
        sb.append("</table>");
	sb.append("</div>");
        //        getPageHandler().entrySectionClose(request, entry, sb);
        sb.append(HtmlUtils.script("prettyPrint();"));

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String processMarkdownLine(String input) throws Exception {
        int          index = input.indexOf("**");
        StringBuffer buff  = new StringBuffer();
        while (index >= 0) {
            buff.append(input.substring(0, index));
            input = input.substring(index + 2, input.length());
            int index2 = input.indexOf("**");
            if (index2 < 0) {
                break;
            }
            buff.append("<strong>");
            buff.append(input.substring(0, index2));
            buff.append("</strong>");
            input = input.substring(index2 + 2, input.length());
            index = input.indexOf("**");
        }
        buff.append(input);
        input = buff.toString();
        String regexp = "\\[([^\\]]+)\\(([^\\)]+\\))";
        regexp = "\\[([^\\]]*)\\]\\s*\\(([^\\)]+)\\)";
        input  = input.replaceAll(regexp, "<a href=\"$2\">$1</a>");

        input = input.replaceAll("([^\"]+)(https?:[^\\s]+)",
                                 "$1<a href=\"$2\">$2</a>");


        return input;
    }

    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String processMarkdown(String input) throws Exception {
        List<String> lines = StringUtil.split(input, "\n", false, false);
        StringBuffer sb    = new StringBuffer();
        boolean      inOl  = false;
        boolean      inUl  = false;
        for (String line : lines) {
            if (line.startsWith("* ")) {
                line = line.substring(1, line.length());
                if ( !inUl) {
                    sb.append("<ul>\n");
                }
                sb.append("<li> ");
                sb.append(processMarkdownLine(line));
                sb.append("\n");
                inUl = true;

                continue;
            }
            if (line.matches("^\\d+\\. .*")) {
                line = line.substring(line.indexOf(".") + 1, line.length());
                if ( !inOl) {
                    sb.append("<ol>\n");
                }
                sb.append("<li> ");
                sb.append(processMarkdownLine(line));
                sb.append("\n");
                inOl = true;

                continue;

            }
            if (inUl) {
                sb.append("</ul>\n");
                inUl = false;
            }
            if (inOl) {
                sb.append("</ol>\n");
                inOl = false;
            }
            if (line.trim().length() == 0) {
                sb.append("<p>\n");

                continue;
            }
            if (line.startsWith("# ")) {
                sb.append(HtmlUtils.h1(processMarkdownLine(line.substring(2,
                        line.length()))));
            } else if (line.startsWith("## ")) {
                sb.append(HtmlUtils.h2(processMarkdownLine(line.substring(3,
                        line.length()))));
            } else if (line.startsWith("### ")) {
                sb.append(HtmlUtils.h3(processMarkdownLine(line.substring(4,
                        line.length()))));
            } else {
                sb.append(processMarkdownLine(line));
            }
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param obj _more_
     * @param name _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public static void readLines(JSONObject obj, String name, Appendable sb)
            throws Exception {
        try {
            readLines(obj.getJSONArray(name), sb);
        } catch (Exception exc) {
            sb.append(obj.getString(name));
        }
    }


    /**
     * _more_
     *
     * @param lines _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public static void readLines(JSONArray lines, Appendable sb)
            throws Exception {
        for (int lineIdx = 0; lineIdx < lines.length(); lineIdx++) {
            String line = lines.getString(lineIdx);
            sb.append(line);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param s _more_
     * @param i _more_
     *
     * @throws Exception _more_
     */
    private void writeOutput(Request request, Appendable sb, String s, int i)
            throws Exception {
        sb.append("<tr valign=top>");
        sb.append(HtmlUtils.td(HtmlUtils.div(((i >= 0)
                ? "Out [" + i + "]:"
                : ""), HtmlUtils.cssClass(
                    "ipynb-output-label")), "align=right"));
        sb.append(
            HtmlUtils.td(
                HtmlUtils.div(s, HtmlUtils.cssClass("ipynb-output")), ""));
        sb.append("</tr>\n");
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param obj _more_
     * @param sb _more_
     * @param i _more_
     *
     * @throws Exception _more_
     */
    private void processText(Request request, JSONObject obj, Appendable sb,
                             int i)
            throws Exception {
        StringBuilder osb = new StringBuilder();
        try {
            JSONArray text = obj.getJSONArray("text");
            readLines(text, osb);
        } catch (Exception exc) {
            osb.append(obj.getString("text"));
        }
        String s = osb.toString();
        s = "<pre style=\"border:0px;\">"
            + s.replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "</pre>";
        writeOutput(request, sb, s, -1);

    }

    private void processPng(Request request, JSONObject obj, Appendable sb,
                             int i)
            throws Exception {
        StringBuilder osb = new StringBuilder();
	String png = obj.getString("png");
	osb.append("<img src=\"data:image/png;base64,");
	osb.append(png);
	osb.append("\">");
        String s = osb.toString();
        writeOutput(request, sb, s, -1);

    }
    
    /**
     * _more_
     *
     * @param request _more_
     * @param data _more_
     * @param sb _more_
     * @param i _more_
     *
     * @throws Exception _more_
     */
    private void processData(Request request, JSONObject data, Appendable sb,
                             int i)
            throws Exception {
        boolean hasImage = false;
        if (data.has("image/png")) {
            String png = data.getString("image/png");
            String img = "<img src=\"data:image/png;\nbase64," + png
                         + "\">\n";
            writeOutput(request, sb, img, i);
            hasImage = true;
        }


        if (data.has("text/html")) {
            JSONArray lines = data.getJSONArray("text/html");
            if (lines.length() == 0) {
                return;
            }
            StringBuilder osb = new StringBuilder();
            readLines(lines, osb);
            writeOutput(request, sb, osb.toString(), i);
        } else if (data.has("text/plain") && !hasImage) {
            JSONArray lines = data.getJSONArray("text/plain");
            if (lines.length() == 0) {
                return;
            }
            StringBuilder osb = new StringBuilder();
            readLines(lines, osb);
            String s = "<pre style=\"border:0px;\">"
                       + osb.toString().replaceAll("<",
                           "&lt;").replaceAll(">", "&gt;") + "</pre>";
            writeOutput(request, sb, s, i);
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        String       line  = "hellothere [foo](bar) xxxxx [foo](bar) yyy";
        int          index = line.indexOf("**");
        StringBuffer buff  = new StringBuffer();
        while (index >= 0) {
            buff.append(line.substring(0, index));
            line = line.substring(index + 2, line.length());
            int index2 = line.indexOf("**");
            if (index2 < 0) {
                break;
            }
            buff.append("<strong>");
            buff.append(line.substring(0, index2));
            buff.append("</strong>");
            line  = line.substring(index2 + 2, line.length());
            index = line.indexOf("**");
        }
        buff.append(line);


        line = buff.toString();

        String regexp = "\\[([^\\]]+)\\(([^\\)]+\\))";
        regexp = "\\[([^\\]]*)\\]\\s*\\(([^\\)]+)\\)";
        line   = line.replaceAll(regexp, " <a href=\"$2\">$1</a> ");


        String input =
            "asdsd <a href=\"http://foo.bar\">foo.bar</a> http://foo.bar ";
        input = input.replaceAll("([^\"]+)(https?:[^\\s]+)",
                                 "$1<a href=\"$2\">$2</a>");
        System.err.println(input);
    }

}
