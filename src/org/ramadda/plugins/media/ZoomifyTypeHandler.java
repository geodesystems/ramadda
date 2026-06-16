/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;

import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.*;

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
    public synchronized void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
        super.initializeNewEntry(request, entry, newType);
        if ( !entry.isFile()) {
            return;
        }
        String slicer = getRepository().getScriptPath("ramadda.image.slicer");
        if (slicer == null) {
            return;
        }
	getRepository().addScriptPath("sh");
        File entryDir  = getStorageManager().getEntryDir(entry.getId(), true);
        File imagesDir = new File(entryDir, "images");
        imagesDir.mkdir();
        List<String> commands = new ArrayList<String>();

	Utils.add(commands,
		  "sh", slicer,
		  "-i", entry.getResource().getPath(),
		  "-o", imagesDir.toString(),
		  "-e", "jpg",
		  "-w", "512",
		  "-s", "200",
		  "-p",
		  "-limit memory 128MiB -limit map 256MiB -limit disk 4GiB -limit thread 1 -strip -quality 85"
		  //		  "-limit memory 512MiB -limit map 1GiB -limit thread 1 -strip -quality 85"
		  );


	getLogManager().logSpecial("Zoomify: calling: " + Utils.join(commands," "));

	ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
	pb.redirectErrorStream(true);
	//	pb.environment().put("MAGICK_TEMPORARY_PATH", "/mnt/ramadda/tmp");
	pb.environment().put("MAGICK_THREAD_LIMIT", "1");
	//        Utils.add(commands, "sh", slicer, "-i", entry.getResource().getPath(), "-o", imagesDir.toString())
	//        ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
        pb.redirectErrorStream(true);
	getLogManager().logSpecial("Zoomify: creating image tiles for:" + entry.getName());
	Process process = pb.start();
	StringBuilder output = new StringBuilder();
	try (BufferedReader br = new BufferedReader(
						    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
	    String line;
	    while ((line = br.readLine()) != null) {
		if (output.length() < 100_000) {
		    output.append(line).append('\n');
		}
		if(stringDefined(line)) {
		    getLogManager().logSpecial("Zoomify slicer: " + line);
		}
	    }
	}
	
	int exitCode = process.waitFor();
	String result = output.toString();
	if (exitCode != 0) {
	    throw new IOException("Image slicer failed, exit code=" + exitCode
				  + "\nOutput:\n" + result);
	}
	getLogManager().logSpecial("Zoomify: after reading results: "+
				   getRepository().getAdmin().appendMemory());
        if (result.indexOf("unable to open image")<0 && result.trim().length() > 0) {
            throw new IllegalArgumentException("Error running image slicer:"
					       + result);
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
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	HU.script(sb, "new RamaddaZoomableImage(" + HU.comma(JsonUtil.map(jsonProps),HU.quote(id))+");\n");
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
        Utils.add(jsonProps, "id", JsonUtil.quote(id));
	HU.script(sb, "new RamaddaZoomify(" + HU.comma(JsonUtil.map(jsonProps),HU.quote(id))+");\n");
        return sb.toString();
    }

}
