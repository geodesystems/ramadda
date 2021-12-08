/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;



import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.Point;
import org.ramadda.util.geo.GeoJson;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.text.CsvUtil;

import org.ramadda.util.Utils;
import org.ramadda.util.geo.KmlUtil;
import org.json.*;
import org.w3c.dom.*;

import ucar.unidata.gis.shapefile.EsriShapefile;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;

import ucar.unidata.xml.XmlUtil;


import java.awt.Color;
import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 *
 *
 */
public class GeoJsonOutputHandler extends OutputHandler {


    /** Map output type */
    public static final OutputType OUTPUT_GEOJSON_TABLE =
        new OutputType("Map Table", "geojsontable", OutputType.TYPE_VIEW, "",
                       ICON_TABLE);

    /** Map output type */
    public static final OutputType OUTPUT_GEOJSONCSV =
        new OutputType("GeoJson CSV", "geojsoncsv", OutputType.TYPE_FILE, "",
                       ICON_CSV);

    public static final OutputType OUTPUT_GEOJSON_REDUCE =
        new OutputType("Reduce GeoJson", "geojsonreduce", OutputType.TYPE_VIEW, "",
                       ICON_MAP);

    public static final OutputType OUTPUT_GEOJSON_FILTER =
        new OutputType("Filter GeoJson", "geojsonfilter", OutputType.TYPE_VIEW, "",
                       ICON_MAP);    

    public static final OutputType OUTPUT_EDITABLE_TOKML =
        new OutputType("Convert to KML", "editable.kml", OutputType.TYPE_VIEW,
                       "", ICON_KML);


    /**
     * Create a MapOutputHandler
     *
     *
     * @param repository  the repository
     * @param element     the Element
     * @throws Exception  problem generating handler
     */
    public GeoJsonOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GEOJSON_TABLE);
        addType(OUTPUT_GEOJSON_FILTER);	
        addType(OUTPUT_GEOJSON_REDUCE);	
        addType(OUTPUT_GEOJSONCSV);
	addType(OUTPUT_EDITABLE_TOKML);
    }



    /**
     * Get the entry links
     *
     * @param request  the Request
     * @param state    the repository State
     * @param links    the links
     *
     * @throws Exception  problem creating links
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.getEntry() != null) {
	    if(state.getEntry().getTypeHandler().isType("geo_geojson")) {
		links.add(makeLink(request, state.getEntry(),
				   OUTPUT_GEOJSON_TABLE));
		links.add(makeLink(request, state.getEntry(),
				   OUTPUT_GEOJSON_FILTER));	    
		links.add(makeLink(request, state.getEntry(),
				   OUTPUT_GEOJSON_REDUCE));	    
		links.add(makeLink(request, state.getEntry(), OUTPUT_GEOJSONCSV));

	    }
	    if (state.getEntry().getTypeHandler().isType("geo_editable_json")) {
		links.add(makeLink(request, state.getEntry(), OUTPUT_EDITABLE_TOKML));
	    }
	}

    }



    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> subGroups,
                              List<Entry> entries)
            throws Exception {

        if (outputType.equals(OUTPUT_EDITABLE_TOKML)) {
            return outputEditableToKml(request, group);
        }	

	return null;
    }


    /**
     * Output the entry
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


        if (outputType.equals(OUTPUT_GEOJSON_FILTER)) {
	    if(!request.exists("geojson_subset")) {
		StringBuilder sb = new StringBuilder();
		getPageHandler().entrySectionOpen(request, entry, sb, "Geojson Filter");
		sb.append(HtmlUtils.formTable());
		sb.append(HtmlUtils.form(getRepository().URL_ENTRY_SHOW.toString()));
		sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
		sb.append(HtmlUtils.hidden(ARG_OUTPUT,
					   OUTPUT_GEOJSON_FILTER.toString()));
		sb.append(
			  HtmlUtils.formEntry(
					      msgLabel("Property"),
					      HU.input("geojson_property",request.getString("geojson_property",""))));
		sb.append(
			  HtmlUtils.formEntry(
					      msgLabel("Value"),
					      HU.input("geojson_value",
						       request.getString("geojson_value",""))));
		StringBuffer buttons =
		    new StringBuffer(HtmlUtils.submit("Subset",
						      "geojson_subset"));
		sb.append(HtmlUtils.formEntry("", buttons.toString()));
		sb.append(HtmlUtils.formClose());
		sb.append(HtmlUtils.formTableClose());
		getPageHandler().entrySectionClose(request, entry, sb);
		return new Result("", sb);
	    }

	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    PrintStream           pw  = new PrintStream(bos);
	    GeoJson.geojsonSubsetByProperty(entry.getResource().getPath(), pw,
					 request.getString("geojson_property",""),
					 request.getString("geojson_value",""));

	    pw.close();
	    StringBuilder sb = new StringBuilder(new String(bos.toByteArray()));	    
	    request.setReturnFilename(getStorageManager().getOriginalFilename(entry.getResource().getPath()));
            Result result = new Result("", sb, "application/json");
	    return result;

	}


        if (outputType.equals(OUTPUT_GEOJSON_REDUCE)) {
	    String geoJson = getStorageManager().readFile(entry.getResource().getPath());
	    geoJson  = geoJson.replaceAll("\\.(\\d\\d\\d\\d\\d\\d)[\\d]+", ".$1");
	    request.setReturnFilename(getStorageManager().getOriginalFilename(entry.getResource().getPath()));

            Result result = new Result("", new StringBuilder(geoJson), "application/json");
	    return result;

	}	


        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream           pw  = new PrintStream(bos);
        GeoJson.geojsonFileToCsv(entry.getResource().getPath(), pw, null);
        pw.close();
        StringBuilder sb = new StringBuilder(new String(bos.toByteArray()));




        if (outputType.equals(OUTPUT_GEOJSONCSV)) {
            Result result = new Result("", sb, "text/csv");
            result.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                     + ".csv");

            return result;
        }

        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        String[]              args = new String[] { "-table" };
	
        CsvUtil csvUtil = new CsvUtil(args, new BufferedOutputStream(bos2),
                                      null);

	csvUtil.setInteractive(true);
        csvUtil.setInputStream(
            new ByteArrayInputStream(sb.toString().getBytes()));
        csvUtil.run(null);
        sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "Map Table");
        sb.append("\n");
        sb.append(new String(bos2.toByteArray()));
        sb.append("\n");
        getPageHandler().entrySectionClose(request, entry, sb);
        sb.append("\n");

        return new Result("", sb);
    }

    private Result outputEditableToKml(Request request, Entry entry)
            throws Exception {

	String json  = getStorageManager().readFile(entry.getResource().getPath());
        JSONArray array     = new JSONArray(json);
        Element                root       = KmlUtil.kml(getName());
        Element                doc = KmlUtil.document(root, entry.getName(),  true);
        Element folder = KmlUtil.folder(doc, entry.getName(), true);

        for (int i = 0; i < array.length(); i++) {
            JSONObject feature = array.getJSONObject(i);
	    String type = feature.optString("type","");
	    JSONObject style  = Json.readObject(feature,"style");
	    JSONArray jpoints  = Json.readArray(feature,"points");		
	    List<Point> points= new ArrayList<Point>();
	    Bounds b = new Bounds();
	    for (int ptIdx = 0; ptIdx < jpoints.length(); ptIdx++) {
		JSONObject jpoint = jpoints.getJSONObject(ptIdx);
		Point p = new Point(jpoint.getDouble("latitude"),jpoint.getDouble("longitude"));
		b.expand(p);
		points.add(p);
	    }
	    if(type.equals("image")) {
		String url = style.getString("imageUrl");
		if(!url.startsWith("http")) {
		    url = request.getAbsoluteUrl(url);
		}
		Element  ele = KmlUtil.groundOverlay(folder, "image","",url,
						     b.getNorth(),b.getSouth(),b.getEast(),b.getWest(),true);

	    } else if(type.equals("line") || type.equals("polyline") || type.equals("freehand")) {
		KmlUtil.placemark(folder,"line","",Point.getCoordinates(points),
				  Utils.decodeColor(style.optString("strokeColor",(String)null),Color.blue), style.optInt("strokeWidth",1));
	    } else if(type.equals("point")) {
		KmlUtil.placemark(folder, "",
				  "",points.get(0).getLatitude(),
				  points.get(0).getLongitude(),	0,null);
	    } else if(type.equals("label")) {
		String label = style.optString("label","");
		String _label = label;
		if(_label.length()>10) _label = _label.substring(0,9)+"...";
		KmlUtil.placemark(folder, _label,
				  label,points.get(0).getLatitude(),
				  points.get(0).getLongitude(),	0,null);
	    }
	}



	String       xml  = XmlUtil.toString(root, true);
        String returnFile =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry))
            + ".kml";
	request.setReturnFilename(returnFile);
				  
	Result result = new Result("", new StringBuilder(xml),KmlOutputHandler.MIME_KML);
        return result;
    }
    



}
