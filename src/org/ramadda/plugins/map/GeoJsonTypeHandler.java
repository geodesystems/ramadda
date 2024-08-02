/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;


import org.json.*;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.data.docs.ConvertibleTypeHandler;
import org.ramadda.data.docs.ConvertibleFile;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.map.MapInfo;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.GenericTypeHandler;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.GeoJson;


import org.w3c.dom.Element;


import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class GeoJsonTypeHandler extends ConvertibleTypeHandler
    implements WikiConstants {
    private static final GeoJson GJ = null;


    /** _more_ */
    private static int IDX = IDX_LAST+1;

    /** _more_ */
    public static final int IDX_COLUMNS = IDX++;



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public GeoJsonTypeHandler(Repository repository, Element node)
	throws Exception {
        super(repository, node);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
	throws Exception {
        super.initializeNewEntry(request, entry, newType);
	if(!isNew(newType)) return;
        if ( !entry.isFile()) {
            return;
        }
	List<String> names = new ArrayList<String>();
        Bounds bounds = GeoJson.getBounds(entry.getResource().toString(),names);
        if (bounds != null) {
            entry.setBounds(bounds);
        }
	entry.setValue(IDX_COLUMNS,Utils.join(names,", "));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param node _more_
     * @param files _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node,
                                       Hashtable<String, File> files)
	throws Exception {
	super.initializeEntryFromXml(request, entry, node, files);
        initializeEntryFromForm(request, entry, null, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param firstCall _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeEntryFromHarvester(Request request, Entry entry,
                                             boolean firstCall)
	throws Exception {
        super.initializeEntryFromHarvester(request, entry, firstCall);
        if (firstCall) {
            initializeEntryFromForm(request, entry, null, true);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void metadataChanged(Request request, Entry entry)
	throws Exception {
        super.metadataChanged(request, entry);
        getEntryManager().updateEntry(request, entry);
    }

    @Override
    public boolean addToMapSelector(Request request, Entry entry, Entry forEntry, MapInfo map)
            throws Exception {
        if (entry != null) {
	    String url =
		request.entryUrl(getRepository().URL_ENTRY_GET, entry).toString();
	    map.addGeoJsonUrl(
			      entry.getName(), url, true,"");
	}
        return super.addToMapSelector(request, entry, forEntry, map);
    }



    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean addToMap(Request request, Entry entry, MapInfo map)
	throws Exception {
	//        String url = getEntryManager().getEntryResourceUrl(request, entry);
	String url = getEntryManager().getEntryResourceUrl(request, entry,ARG_INLINE_DFLT,ARG_FULL_DFLT,ARG_ADDPATH_TRUE,true);

	List<String> styles = new ArrayList<String>();
	ShapefileOutputHandler.makeMapStyle(request,
					    entry,styles);
        map.addGeoJsonUrl(entry.getName(), url, true,JU.map(styles));


        return false;
    }



    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
	throws Exception {
	List<String> args = getCsvCommands(request, entry);
        return new GeoJsonRecordFile(request, getRepository(), this, entry,args,new IO.Path(getPathForEntry(request, entry,true)));
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class GeoJsonRecordFile extends ConvertibleFile {

        /** _more_ */
        private Repository repository;

	private List<String> csvCommands;
	    
        /** _more_ */
        private String dataUrl;

        /** _more_ */
        private Entry entry;

	GeoJsonTypeHandler typeHandler;

        /**
         * _more_
         *
         *
         * @param repository _more_
         * @param ctx _more_
         * @param entry _more_
         *
         * @throws IOException _more_
         */
        public GeoJsonRecordFile(Request request, Repository repository, GeoJsonTypeHandler ctx, Entry entry,List<String> args, IO.Path path)
	    throws IOException {
            super(request,  ctx, entry, args, path);
	    csvCommands = args;
	    typeHandler = ctx;
            this.repository = repository;
            this.entry      = entry;
        }


	/*
	@Override
	public List<RecordField> doMakeFields(boolean failureOk) {
	    String names = (String) entry.getValue(request,GeoJsonTypeHandler.IDX_COLUMNS);
	    if(!stringDefined(names)) return super.doMakeFields(failureOk);
	    StringBuilder fields = new StringBuilder();
	    for(String field:Utils.split(names,",",true,true)) {
		if(fields.length()>0) fields.append(",");

	    }
	    return super.doMakeFields(failureOk);
	}
	*/


        @Override
	public List<String> getCsvCommands() throws Exception {
	    List<String> commands =super.getCsvCommands();
	    if(commands.size()==0) {
		Utils.add(commands, "-geojson","true");
	    }
	    return commands;
	}


    }
}
