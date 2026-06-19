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
		public void run(Object actionId) throws Exception {
		    processImage(request, theEntry, actionId);
		};
	    };
	Object actionId = getActionManager().runAction(action,"Process Zoom Images","",theEntry);
	StringBuilder sb = new StringBuilder();
	String url = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&output=json";
        getPageHandler().entrySectionOpen(request, theEntry, sb, "Zoomable Image Processing");
	sb.append(HU.span("Results",HU.attrs("id",actionId+"_heading")));
	HU.div(sb,"",HU.attrs("class","ramadda-action-results", "id",actionId.toString()));
	boolean canCancel = false;
	if(canCancel) {
	    //		String cancelUrl = getRepository().getUrlBase() +"/status?actionid=" + actionId +"&" + ARG_CANCEL+"=true";
	    //	    String cancelUrl = HU.url(extEditUrl,ARG_CANCEL,actionId.toString());
	    //	    sb.append(HU.button(HU.href(cancelUrl,LABEL_CANCEL)));
	}
	HU.script(sb,"Utils.handleActionResults('" + actionId +"','" + url+"',"+ canCancel+");\n");
        getPageHandler().entrySectionClose(request, theEntry, sb);
	Result result = getEntryManager().makeEntryEditResult(request, theEntry, "Zoom Image Processing", sb);
	request.setOverrideResult(result);
    }

    private void processImage(Request request, Entry entry, Object actionId) throws Exception {
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
		  "-w", "512",
		  "-s", "200",
		  "-p",
		  "-limit memory 128MiB -limit map 256MiB -limit disk 4GiB -limit thread 1 -strip -quality 85"
		  );

	long startTime = System.currentTimeMillis();
	getLogManager().logSpecial("Zoomify: calling: " + Utils.join(commands," "));
	ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
	pb.redirectErrorStream(true);
	String tmpDir = getStorageManager().makeTempDir("imagemagic",false).toString();
	pb.environment().put("MAGICK_TEMPORARY_PATH", tmpDir);
	pb.environment().put("TMPDIR", tmpDir);
	pb.environment().put("MAGICK_THREAD_LIMIT", "1");
        pb.redirectErrorStream(true);
	getLogManager().logSpecial("Zoomify: creating image tiles for:" + entry.getName());
	Process process = pb.start();
	StringBuilder output = new StringBuilder();
	try (BufferedReader br = new BufferedReader(
						    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
	    String line;
	    List<String> lines = new ArrayList<String>();
	    while ((line = br.readLine()) != null) {
		if(!stringDefined(line)) {
		    continue;
		}
		line = formatProgress(line);
		if(line==null) continue;
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
		heading = "Results";
		String msg = Utils.join(lines,"\n");
		getActionManager().setActionMessage(actionId,  heading,msg);
		if (output.length() < 100_000) {
		    output.append(line).append('\n');
		}
		//		getLogManager().logSpecial("Zoomify slicer: " + line);
	    }
	}
	
	int exitCode = process.waitFor();
	String result = output.toString();
	if (exitCode != 0) {
	    throw new IOException("Image slicer failed, exit code=" + exitCode
				  + "\nOutput:\n" + result);
	}
	getActionManager().setContinueHtml(actionId,
					   "Finished processing. "+
					   HU.href(getEntryManager().getEntryURL(request, entry),
						   "View " + entry.getName()));

	getLogManager().logSpecial("Zoomify: after reading results: "+
				   getRepository().getAdmin().appendMemory());
	/*
        if (result.indexOf("unable to open image")<0 && result.trim().length() > 0) {
            throw new IllegalArgumentException("Error running image slicer:"
					       + result);
        }
	*/

    }

    private static final Pattern PROGRESS_PATTERN =
	Pattern.compile("PROGRESS\\s+(\\w+)\\s+level=(\\d+)\\s+total=(\\d+)\\s+size=(\\d+)");

    public static String formatProgress(String line) {
	Matcher m = PROGRESS_PATTERN.matcher(line);
	if (!m.find()) {
	    return null;
	}

	String phase = m.group(1);
	int level = Integer.parseInt(m.group(2));
	int total = Integer.parseInt(m.group(3));
	int size = Integer.parseInt(m.group(4));

	// Convert to 1-based numbering for display
	int displayLevel = level + 1;

	if (phase.equals("resize")) {
	    return String.format(
				 "Preparing zoom level %d of %d (%d pixels)",
				 displayLevel, total, size);
	}

	if (phase.equals("slice")) {
	    return String.format(
				 "Creating tiles for zoom level %d of %d (%d pixels)",
				 displayLevel, total, size);
	}

	if (phase.equals("sliced")) {
	    return String.format(
				 "Finished zoom level %d of %d",
				 displayLevel, total);
	}

	return null;
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
