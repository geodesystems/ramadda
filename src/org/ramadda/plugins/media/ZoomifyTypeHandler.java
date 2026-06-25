/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;

import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class ZoomifyTypeHandler extends GenericTypeHandler implements WikiTagHandler {
    public ZoomifyTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }

    @Override
    public synchronized void initializeNewEntry(Request request, final Entry theEntry,NewType newType)
	throws Exception {
        super.initializeNewEntry(request, theEntry, newType);
        if ( !theEntry.isFile()) {
            return;
        }
        final String slicer = getRepository().getScriptPath("ramadda.image.slicer");
        if (slicer == null) {
            return;
        }

	ActionManager.Action action = new ActionManager.Action() {
		@Override
		public void run(Object actionId) throws Exception {
		    processImage(request, theEntry, actionId,this);
		};
		@Override
		public void setRunning(boolean running) {
		    super.setRunning(running);
		}
	    };
	Object actionId = getActionManager().runAction(action,"Process Zoom Images","",theEntry);
	StringBuilder sb = new StringBuilder();
	String url = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&output=json";
        getPageHandler().entrySectionOpen(request, theEntry, sb, "Zoomable Image Processing");
	boolean canCancel = true;
	String cancelUrl = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&" + ARG_CANCEL+"=true";
	sb.append(HU.div("",HU.attrs("id",actionId+"_cancel")));
	HU.div(sb,"",HU.attrs("class","ramadda-action-results", "id",actionId.toString()));
	sb.append(HU.importJS(getHtdocsUrl("/actions.js")));
	HU.script(sb,HU.call("new RamaddaActionManager",
			     HU.squote(actionId),
			     HU.squote(url),
			     JU.map("cancelUrl",JU.quote(cancelUrl))));
        getPageHandler().entrySectionClose(request, theEntry, sb);
	Result result = getEntryManager().makeEntryEditResult(request, theEntry, "Zoom Image Processing", sb);
	request.setOverrideResult(result);
    }

    private void processImage(Request request, Entry entry, Object actionId,ActionManager.Action action) throws Exception {
        String slicer = getRepository().getScriptPath("ramadda.image.slicer");
	getRepository().addScriptPath("sh");
        File entryDir  = getStorageManager().getEntryDir(entry.getId(), true);
        File imagesDir = new File(entryDir, "images");
        imagesDir.mkdir();
        List<String> commands = new ArrayList<String>();

	Utils.add(commands,
		  "sh", slicer,
		  "-v2",
		  "-i", entry.getResource().getPath(),
		  "-o", imagesDir.toString(),
		  "-e", "jpg",
		  "-w", getRepository().getProperty("ramadda.zoomify.width","1024"),
		  "-s", "200",
		  "-p",
		  getRepository().getProperty("ramadda.zoomify.arguments",
					      "-limit memory 512MiB -limit map 1GiB -limit disk 20GiB -auto-orient  -limit thread 1 -strip -quality 85")
		  );


	getLogManager().logSpecial("Zoomify: calling: " + Utils.join(Utils.quoteSpace(commands)," "));
	ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
	pb.redirectErrorStream(true);
	String tmpDir = getStorageManager().makeTempDir("imagemagic",false).toString();
	pb.environment().put("TMPDIR", tmpDir);
	pb.environment().put("MAGICK_TEMPORARY_PATH", tmpDir);
	pb.environment().put("MAGICK_THREAD_LIMIT", "1");
	pb.environment().put("MAGICK_SLICER_VMEM_KB","2500000");
        pb.redirectErrorStream(true);
	Process process = pb.start();
	action.setProcess(process);
	processImage(request, entry, actionId,action,process);
    }

    private void processImage(Request request, Entry entry, Object actionId,ActionManager.Action action,
			      Process process) throws Exception {
	long startTime = System.currentTimeMillis();
	StringBuilder output = new StringBuilder();
	List<String> lines = new ArrayList<String>();
	getLogManager().logSpecial("Zoomify: creating image tiles for:" + entry.getName());
	lines.add("Creating image tiles for: " + entry.getName());
	getActionManager().setActionMessage(actionId,Utils.join(lines,""));
	String line="";
	boolean inError = false;
	try (BufferedReader br = new BufferedReader(
						    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
	    while ((line = br.readLine()) != null) {
		//		System.err.println("line:" + line);
		if ( !getActionManager().getActionOk(actionId)) {
		    if(process!=null) {
			Utils.destroy(process);
			action.setProcess(null);
			process=null;
		    }
		    break;
		}

		if(!stringDefined(line)) {
		    continue;
		}
		line = formatProgress(line);
		//		ucar.unidata.util.Misc.sleepSeconds(5);
		if(line==null) continue;
		if(line.matches(".*(error/|no images defined|unable to open).*")) {
		    inError=true;
		    if(process!=null) {
			Utils.destroy(process);
			action.setProcess(null);
			process=null;
		    }
		    break;
		}

		if(line.indexOf("Finished")>=0 || line.indexOf("sliced")>=0) continue;
		line = line.replaceAll("/.*/images_files","...images_files");
		lines.add(line);
		if(lines.size()>150) {
		    while(lines.size()>75) {
			lines.remove(0);
		    }
		}
		long currentTime = System.currentTimeMillis();
		double time = Utils.millisToMinutes(currentTime-startTime);
		String heading = "Results: Running for ";
		if(time<1) {
		    int seconds = ((int)Utils.millisToSeconds(currentTime-startTime));
		    heading += seconds+(seconds==1?" second":" seconds");
		} else   {
		    heading  += (Utils.decimals(time,1)) +" minutes\n";
		}		    
		//for now don't set the time as it only appears on a new line
		heading = "Image Tiling Results";
		String msg = Utils.join(lines,"\n");
		getActionManager().setActionMessage(actionId,  heading,msg);
		if (output.length() < 100_000) {
		    output.append(line).append('\n');
		}
		//		getLogManager().logSpecial("Zoomify slicer: " + line);
	    }
	}
	
	action.setProcess(null);
	if(process!=null) {
	    int exitCode= process.waitFor();
	    if (exitCode != 0 ) inError=true;
	}
	if ( !getActionManager().getActionOk(actionId)) {
	    return;
	}

	String result = output.toString();
	if (inError) {
	    getActionManager().setContinueHtml(actionId,"Error:" + line);
	    getLogManager().logSpecial("Zoomify: error processing:" + entry+"\n"+line);
	    return;
	}

	getActionManager().setContinueHtml(actionId,
					   "Finished processing. "+
					   HU.href(getEntryManager().getEntryURL(request, entry),
						   "View " + entry.getName()));
	//	getLogManager().logSpecial("Zoomify: after reading results: "+   getRepository().getAdmin().appendMemory());
    }

    private static final Pattern PROGRESS_PATTERN1 =
	Pattern.compile("PROGRESS\\s+(\\w+)\\s+level=(\\d+)\\s+total=(\\d+)\\s+width=(\\d+)\\s+height=(\\d+)");

    private static final Pattern PROGRESS_PATTERN2 =
	Pattern.compile("PROGRESS\\s+(\\w+)\\s+.*");

    private String formatProgress(String line) {
	try {
	    Matcher m = PROGRESS_PATTERN1.matcher(line);
	    if (!m.find()) {
		m = PROGRESS_PATTERN2.matcher(line);
		if (m.find()) {
		    String phase = m.group(1);
		    if (phase.equals("normalize")) {
			return  "Normalizing the source image";
		    }
		}
		if(line.indexOf("PROGRESS")<0) {
		    return line;
		}
		return null;
	    }

	    String phase = m.group(1);
	    int level = Integer.parseInt(m.group(2));
	    int total = Integer.parseInt(m.group(3));
	    int width = Integer.parseInt(m.group(4));
	    int height = Integer.parseInt(m.group(5));	    

	    // Convert to 1-based numbering for display
	    int displayLevel = level + 1;



	    if (phase.equals("resize")) {
		return String.format(
				     "Preparing zoom level %d of %d W:%d H:%d",
				     displayLevel, total, width,height);
	    }

	    if (phase.equals("slice")) {
		return String.format(
				     "Creating tiles for zoom level %d of %d W:%d  H:%d",
				     displayLevel, total, width,height);
	    }

	    if (phase.equals("sliced")) {
		return String.format(
				     "Finished zoom level %d of %d",
				     displayLevel, total);
	    }
	    return null;
	} catch(Exception exc){
	    getLogManager().logError("processing magickslicer line:" + line,exc);
	    System.err.println("Error:" +line+" " + exc);
	    return line;
	}
    }


    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {
        if ( !tag.equals("zoomify") && !tag.equals("zoomable")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuilder sb     = new StringBuilder();
	String id = getWikiManager().makeZoomifyLayout(request, entry,sb,props);
	List<String> jsonProps =  getWikiManager().getZoomifyProperties(request, entry,props);	
        Utils.add(jsonProps, "id", JU.quote(id));
	HU.script(sb, "new RamaddaZoomableImage(" + HU.comma(JU.map(jsonProps),HU.quote(id))+");\n");
        return sb.toString();
    }

    @Override
    public void getWikiTags(List<WikiTag> tags, Entry entry) {
	tags.add(new WikiTag("zoomify_collection","Zoomify collection"));
    }

    @Override
    public void initTags(Hashtable<String, WikiTagHandler> tagHandlers) {
	tagHandlers.put("zoomify_collection",this);
    }

    @Override
    public void addTagDefinition(List<String>  tags) {
    }

    @Override
    public String handleTag(WikiUtil wikiUtil, Request request,
                            Entry originalEntry, Entry entry, String theTag,
                            Hashtable props, String remainder) throws Exception {
	List<Entry> children =
	    getEntryUtil().getEntriesOfType(getWikiManager().getEntries(request, wikiUtil,
									originalEntry, entry, props),
					    getType());

	if(children.size()==0) {
	    return "No zoomable images";
	}
	//ramadda.image.slicer
        StringBuilder sb     = new StringBuilder();
	getWikiManager().initZoomifyImports(request,sb);
	String id = getWikiManager().makeZoomifyLayout(request, children.get(0),sb,props);
	List<String> jsonProps =  getWikiManager().getZoomifyProperties(request, children.get(0),props);	
        Utils.add(jsonProps, "id", JU.quote(id));
	HU.script(sb, "new RamaddaZoomableImage(" + HU.comma(JU.map(jsonProps),HU.quote(id))+");\n");
        return sb.toString();
    }

}
