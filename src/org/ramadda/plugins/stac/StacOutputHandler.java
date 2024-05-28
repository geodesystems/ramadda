/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.stac;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import ucar.unidata.util.IOUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.*;
import org.w3c.dom.Element;


/**
 * This class is a singleton and gets instantiated by the Repository at
 * start up time. It's presence is made known to the Repository
 * through the outputhandler.xml file
 *
 */
public class StacOutputHandler extends OutputHandler {

    public static final String STAC_VERSION  = "1.0.0";

    /** 
	Stac output type. This class can handle any number of output types
	The icon is in the htdocs/stac directory. Any other http resources
	can be put there. If there is an index.html then that will show
	up in RAMADDA's help listing
    */
    public static final OutputType OUTPUT_STAC =
        new OutputType("STAC Catalog", "stac", OutputType.TYPE_FEEDS, "",
                       "/stac/stac.png");



    /**
     * Create a StacOutputHandler
     *
     * @param repository  the repository
     * @param element     the XML Element
     * @throws Exception  problem generating handler
     */
    public StacOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
	//add the output type 
        addType(OUTPUT_STAC);
    }


    /**
     * This gets called anytime an entry is displayed and the links are added to the
     * OUTPUT_STAC output URL. Many output handlers are only applicable to certain
     * entries but the Stac output is applicable to all
     *
     * @param request  the Request
     * @param state    the repository State
     * @param links    the links
     *
     * @throws Exception  problem creating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
	links.add(makeLink(request, state.getEntry(), OUTPUT_STAC));
    }

    /**
     *
     * This gets called when the entry is a group entry, i.e., it contains a set of
     * children entries
     *
     * @param request This holds all request parameters
     * @param outputType The OUTPUT_STAC type
     * @param group The parent group
     * @param children Its children
     * @return The Result class holds the JSON
     *
     * @throws Exception On badness
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
	return outputStac(request, group,children);
    }


    /**
     * Output the entry. This gets called when the entry has no children entries
     *
     * @param request      the Request
     * @param outputType   the type of output
     * @param entry        the Entry to output
     *
     * @return  the Result
     *
     * @throws Exception  problem outputting entry
     */
    @Override
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
	return outputStac(request, entry,new ArrayList<Entry>());
    }

    /**
     * This creates the STAC json catalog for the given entries
     */
    public Result outputStac(Request request,  Entry entry,List<Entry> children)
	throws Exception {
	StringBuilder sb = new StringBuilder();
	List<Metadata> metadataList;
	
	List<String> topProps = new ArrayList<String>();
	
	Utils.add(topProps,"stac_version",quote(STAC_VERSION),
		  "type",quote(isItem(entry)?"Collection":"Catalog"),
		  "id",quote(entry.getId()));
	//Get the snippet instead of the description because the
	//description can contain all sorts of HTML, wiki text, etc
	String description = entry.getSnippet();
	if(!Utils.stringDefined(description)) description = "STAC Item - " + entry.getName();
	Utils.add(topProps,"description",quote(description.trim()));
	Utils.add(topProps,"stac_extensions",JsonUtil.list());
	Utils.add(topProps,"title",quote(entry.getName()));

	metadataList =  getMetadataManager().findMetadata(request, entry,
							  new String[]{ContentMetadataHandler.TYPE_LICENSE}, true);
	if ((metadataList != null) && (metadataList.size() > 0)) {
	    Utils.add(topProps,"license",quote(metadataList.get(0).getAttr1()));
	    
	}
	
	//Look for the different publisher like metadata

	List<String> providers = new ArrayList<String>();
	for(Metadata mtd: 	getMetadataManager().findMetadata(request, entry, new String[]{"thredds.publisher"}, true)) {
	    Utils.add(providers,JsonUtil.map("name",quote(mtd.getAttr1()),
					     "url",quote(mtd.getAttr4()),
					     "roles",JsonUtil.list(quote("producer"))));
	}

	for(Metadata mtd:getMetadataManager().findMetadata(request, entry, new String[]{"metadata_publisher"}, true)) {
	    Utils.add(providers,JsonUtil.map("name",quote(mtd.getAttr1()),
					     "url",quote(mtd.getAttr3()),
					     "roles",JsonUtil.list(quote("producer"))));
	}
    

	if(providers.size()>0) {
	    Utils.add(topProps,"providers", JsonUtil.list(providers));
	}
	


	List<String> links = new ArrayList<String>();
	List<String> assets = new ArrayList<String>();

	//Add the root and self links
	links.add(getLink(request, request.getRootEntry(),"root"));
	links.add(getLink(request, entry,"self"));	
	links.add(getLink(request, entry,"describedby","text/html","Human readable dataset overview and reference",
			  getHtmlUrl(request,entry)));

	
	//Add the child links
	for(Entry child: children) {
	    links.add(getLink(request, child,JsonUtil.MIMETYPE,isItem(child)?"item":"child",
			      child.getName(),  getCatalogUrl(request,child)));
	}


	if(isItem(entry)) {
	    Utils.add(assets,"data",
		      JsonUtil.map("title",quote(entry.getName()),
				   "description",quote(description!=null?description:""),	
				   "href",
				   quote(getEntryManager().getEntryResourceUrl(request, entry,false,true,true)),
				   "type", quote(getRepository().getMimeType(request, entry)),
				   "size",""+entry.getResource().getFileSize()));
	}

	//Add the extent
	List<String> extents = new ArrayList<String>();
	if(entry.hasAreaDefined()) {
	    List<String> bbox = new ArrayList<String>();
	    Utils.add(bbox,entry.getWest(request),entry.getSouth(request),entry.getEast(request),entry.getNorth(request));
	    Utils.add(extents,"spatial",JsonUtil.map("bbox",JsonUtil.list(bbox)));
	}

	Utils.add(extents,
		  "temporal",JsonUtil.map("interval",
					  JsonUtil.list(quote(Utils.formatIso(new Date(entry.getStartDate()))),
							quote(Utils.formatIso(new Date(entry.getEndDate()))))));

	if(extents.size()>0) {
	    Utils.add(topProps, "extent",JsonUtil.map(extents));
	}

	
	List<String> keywords = new ArrayList<String>();
	for(Metadata mtd: getMetadataManager().findMetadata(request, entry,new String[]{ContentMetadataHandler.TYPE_KEYWORD,ContentMetadataHandler.TYPE_TAG,"enum_gcmdkeyword"}, true)) {
	    keywords.add(quote(mtd.getAttr1()));
	}
	if(keywords.size()>0)
	    Utils.add(topProps,"keywords",JsonUtil.list(keywords));


	Utils.add(topProps,"links",links);


	List<String> thumbs = new ArrayList<String>();
	List<String[]> thumbUrls = new ArrayList<String[]>();
	getMetadataManager().getFullThumbnailUrls(request, entry, thumbUrls);
	for(String[] tuple: thumbUrls) {
	    String url = tuple[0];
	    String _url = url.toLowerCase();
	    String title = tuple[1];	    
	    if(title==null) title = "";
	    String type = getRepository().getMimeTypeFromSuffix(_url);
	    url = request.getAbsoluteUrl(url);
	    thumbs.add(JsonUtil.map("href",quote(url),"title",quote(title),"media_type",quote(type)));
	}

	//It looks like you can only have one thumbnail
	if(thumbs.size()>0) {
	    Utils.add(assets,"thumbnail",thumbs.get(0));
	}
	    
	if(assets.size()>0) 
	    Utils.add(topProps,"assets",JsonUtil.map(assets));
    



	sb.append(JsonUtil.map(topProps));
	request.setReturnFilename("stac.json");
 	return new Result("stac.json",sb,JsonUtil.MIMETYPE);
    }


    private boolean isItem(Entry entry) {
	return entry.getResource().isFile();
    }


    /*
      Make the link section for the given entry
    */
    private String getLink(Request request, Entry entry, String rel) {
	return getLink(request, entry,rel,JsonUtil.MIMETYPE,null,getCatalogUrl(request,entry));
    }


    private String getLink(Request request, Entry entry, String rel,String type, String title, String url) {
	List<String> props = new ArrayList<String>();
	Utils.add(props,
		  "rel",quote(rel),
		  "type",quote(type),
		  "href",quote(url));
	if(Utils.stringDefined(title)) Utils.add(props,"title",quote(title));
	return JsonUtil.map(props);
	
    }    



    /**
       Get the absolute URL to the stac.json catalog for the given entry with the given rel
     */
    private String getCatalogUrl(Request request, Entry entry) {
	//The true says to include the /entry/path in the url
	String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
				      ARG_ENTRYID,true,ARG_OUTPUT,OUTPUT_STAC.toString());
	return  request.getAbsoluteUrl(url);
    }

    private String getHtmlUrl(Request request, Entry entry) {
	//The true says to include the /entry/path in the url
	String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
				      ARG_ENTRYID,true);
	return  request.getAbsoluteUrl(url);
    }

    /*
     * Utility method to quote the string for json
     */
    private String quote(String s) {
	return JsonUtil.quote(s);
    }

}
