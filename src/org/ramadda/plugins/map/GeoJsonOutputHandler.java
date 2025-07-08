/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;

import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.GeoJson;
import org.ramadda.util.geo.KmlUtil;
import org.ramadda.util.geo.Point;
import org.ramadda.util.seesv.Seesv;

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

import org.json.*;
import org.w3c.dom.*;

import java.awt.Color;

import java.io.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class GeoJsonOutputHandler extends OutputHandler {

    private static final GeoJson GJ = null;

    /** Map output type */
    public static final OutputType OUTPUT_GEOJSON_TABLE =
        new OutputType("Map Table", "geojsontable", OutputType.TYPE_VIEW, "",
                       ICON_TABLE);

    /** Map output type */
    public static final OutputType OUTPUT_GEOJSONCSV =
        new OutputType("GeoJson CSV", "geojsoncsv", OutputType.TYPE_VIEW|OutputType.TYPE_SERVICE, "",
                       ICON_CSV);

    /**  */
    public static final OutputType OUTPUT_GEOJSON_REDUCE =
        new OutputType("Reduce GeoJson", "geojsonreduce",
                       OutputType.TYPE_VIEW|OutputType.TYPE_SERVICE, "", ICON_MAP);

    /**  */
    public static final OutputType OUTPUT_GEOJSON_SUBSET =
        new OutputType("Subset GeoJson", "geojsonsubset",
                       OutputType.TYPE_VIEW|OutputType.TYPE_SERVICE, "", ICON_MAP);    

    /**  */
    public static final OutputType OUTPUT_GEOJSON_FILTER =
        new OutputType("Filter GeoJson", "geojsonfilter",
                       OutputType.TYPE_VIEW|OutputType.TYPE_SERVICE, "", ICON_MAP);

    /**  */
    public static final OutputType OUTPUT_EDITABLE_TOKML =
        new OutputType("Convert to KML", "editable.kml",
                       OutputType.TYPE_VIEW|OutputType.TYPE_SERVICE, "", ICON_KML);

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
        addType(OUTPUT_GEOJSON_SUBSET);	
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
            if (state.getEntry().getTypeHandler().isType("geo_geojson")) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_GEOJSON_TABLE));
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_GEOJSON_SUBSET));
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_GEOJSON_FILTER));		
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_GEOJSON_REDUCE));
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_GEOJSONCSV));

            }
            if (state.getEntry().getTypeHandler().isType(
                    "geo_editable_json")) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_EDITABLE_TOKML));
            }
        }

    }

    /**
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param children _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {

        if (outputType.equals(OUTPUT_EDITABLE_TOKML)) {
            return outputEditableToKml(request, group);
        }

        return null;
    }

    private Result makeStream(Request request, Entry entry, InputStream is) throws Exception {
	return request.returnStream(getStorageManager().getOriginalFilename(entry.getResource().getPath()),
				    GJ.DOWNLOAD_MIMETYPE,is);	    
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
    public Result outputEntry(final Request request,
			      OutputType outputType,
                              Entry entry)
            throws Exception {

        if (outputType.equals(OUTPUT_GEOJSON_FILTER)) {
            if ( !request.exists("geojson_filter")) {
                StringBuilder sb = new StringBuilder();
                getPageHandler().entrySectionOpen(request, entry, sb,
                        "Geojson Filter");
		String formId = HU.getUniqueId("form_");
		sb.append(HU.form(getRepository().URL_ENTRY_SHOW.toString(), HU.attrs("id",formId)));
                sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
                sb.append(HU.hidden(ARG_OUTPUT, OUTPUT_GEOJSON_FILTER.toString()));
                sb.append(HU.formTable());
		String names = (String) entry.getValue(request,GeoJsonTypeHandler.IDX_COLUMNS);
		List<String> properties = null;
		if(stringDefined(names)) properties = Utils.split(names,",",true,true);
		if(properties==null) {
		    properties = (List<String>) entry.getTransientProperty("geojsonproperties");
		    if(properties==null) {
			properties = GJ.getProperties(entry.getResource().getPath());
			entry.putTransientProperty("geojsonproperties",properties);
		    }
		}
                String tableUrl = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
						   ARG_OUTPUT, OUTPUT_GEOJSON_TABLE.toString());

		String help =HU.href(tableUrl,"View Table",HU.attrs("target","_help"))+HU.space(2)+
		    msg("Note: Values can be regular expression");		    
		HU.formEntry(sb,"",help);

		HU.formEntry(sb,"",HU.labeledCheckbox("matchall","true",true,"Match All"));
		for(int i=1;i<=3;i++) {
		    if(i==2)
			properties.add(0,"");
		    HU.formEntry(sb, msgLabel("Property " + i),
				 HU.select("geojson_property"+i,properties,
					   request.getString("geojson_property"+i, "")) +HU.space(2) +
				 HU.b("Value " +i+":") +HU.space(1) +
				 HU.input("geojson_value"+i,
					  request.getString("geojson_value" +i, ""),
					  HU.attrs("size","80",
						   "onkeydown","HtmlUtils.preventSubmitOnEnter(event)")
					  ));
		}

		HU.formEntry(sb, "", HU.submit("Subset","geojson_filter"));
                sb.append(HU.formTableClose());
                sb.append(HU.formClose());
		addUrlShowingForm(sb, null, formId,
				  "null",
				  null, "includeCopyArgs","false");
                getPageHandler().entrySectionClose(request, entry, sb);

                return new Result("", sb);
            }

	    InputStream is =IO.pipeIt(new IO.PipedThing(){
		    public void run(OutputStream os) {
			PrintStream           pw  = new PrintStream(os);
			try {
			    List<String> props=new ArrayList<String>();
			    for(int i=1;true;i++) {
				String prop =    request.getString("geojson_property"+i, null);
				if(prop==null) break;
				props.add(prop);
				props.add(request.getString("geojson_value"+i, ""));
			    }
			    GJ.geojsonSubsetByProperty(entry.getResource().getPath(), pw,request.get("matchall",false),props);
			} catch(Exception exc) {
			    getLogManager().logError("Filtering geojson:" + entry,exc);
			    pw.println("Filtering geojson:" + exc);
			    return;
			    //			    throw new RuntimeException(exc);
			}
		    }});
            return makeStream(request, entry,is);	    
        }

        if (outputType.equals(OUTPUT_GEOJSON_SUBSET)) {
            if ( !request.exists("geojson_subset")) {
                StringBuilder sb = new StringBuilder();
                getPageHandler().entrySectionOpen(request, entry, sb,
                        "Geojson Subset");
                sb.append(HU.form(getRepository().URL_ENTRY_SHOW.toString()));
                sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
                sb.append(HU.hidden(ARG_OUTPUT,
                                           OUTPUT_GEOJSON_SUBSET.toString()));
                sb.append(HU.formTable());
		entry.getTypeHandler().addAreaWidget(request,  entry.getParentEntry(),entry, sb, null);

		sb.append(HU.labeledCheckbox("intersects","true",false,"Intersects"));

                StringBuffer buttons =
                    new StringBuffer(HU.submit("Subset",
                        "geojson_subset"));
                sb.append(HU.formTableClose());
                sb.append(buttons);
                sb.append(HU.formClose());
                getPageHandler().entrySectionClose(request, entry, sb);

                return new Result("", sb);
            }

	    final boolean intersects = request.get("intersects",false);
	    final Bounds bounds = new Bounds(GJ.parse(request.getString("area_north","")),
				       GJ.parse(request.getString("area_west","")),
				       GJ.parse(request.getString("area_south","")),
				       GJ.parse(request.getString("area_east","")));
	    JSONObject obj = GJ.read(entry.getResource().getPath());
	    obj =GJ.subset(obj,bounds,intersects);
            request.setReturnFilename(
                getStorageManager().getOriginalFilename(
                    entry.getResource().getPath()));
            return new Result("", new StringBuilder(obj.toString()), GJ.DOWNLOAD_MIMETYPE);
        }

        if (outputType.equals(OUTPUT_GEOJSON_REDUCE)) {
            String geoJson =
                getStorageManager().readFile(entry.getResource().getPath());
            geoJson = geoJson.replaceAll("\\.(\\d\\d\\d\\d\\d\\d)[\\d]+",
                                         ".$1");
            request.setReturnFilename(
                getStorageManager().getOriginalFilename(
                    entry.getResource().getPath()));

            Result result = new Result("", new StringBuilder(geoJson),
                                       GJ.DOWNLOAD_MIMETYPE);

            return result;

        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream           pw  = new PrintStream(bos);
        GJ.geojsonFileToCsv(entry.getResource().getPath(), pw, null);
        pw.close();
        StringBuilder sb = new StringBuilder(new String(bos.toByteArray()));
        if (outputType.equals(OUTPUT_GEOJSONCSV)) {
            Result result = new Result("", sb, "text/csv");
            result.setReturnFilename(IOUtil.stripExtension(entry.getName())
                                     + ".csv");

            return result;
        }

        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        String[]              args = new String[] { "-plaintable" };

        Seesv csvUtil = new Seesv(args, new BufferedOutputStream(bos2),
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

    /**
     *
     * @param request _more_
     * @param entry _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputEditableToKml(Request request, Entry entry)
            throws Exception {

        String json =
            getStorageManager().readFile(entry.getResource().getPath());
        JSONArray array  = new JSONArray(json);
        Element   root   = KmlUtil.kml(getName());
        Element   doc    = KmlUtil.document(root, entry.getName(), true);
        Element   folder = KmlUtil.folder(doc, entry.getName(), true);

        for (int i = 0; i < array.length(); i++) {
            JSONObject  feature = array.getJSONObject(i);
            String      type    = feature.optString("type", "");
            JSONObject  style   = JsonUtil.readObject(feature, "style");
            JSONArray   jpoints = JsonUtil.readArray(feature, "points");
            List<Point> points  = new ArrayList<Point>();
            Bounds      b       = new Bounds();
            for (int ptIdx = 0; ptIdx < jpoints.length(); ptIdx++) {
                JSONObject jpoint = jpoints.getJSONObject(ptIdx);
                Point p = new Point(jpoint.getDouble("latitude"),
                                    jpoint.getDouble("longitude"));
                b.expand(p);
                points.add(p);
            }
            if (type.equals("image")) {
                String url = style.getString("imageUrl");
                if ( !url.startsWith("http")) {
                    url = request.getAbsoluteUrl(url);
                }
                Element ele = KmlUtil.groundOverlay(folder, "image", "", url,
                                  b.getNorth(), b.getSouth(), b.getEast(),
                                  b.getWest(), true);

            } else if (type.equals("line") || type.equals("polyline")
                       || type.equals("freehand")) {
                KmlUtil.placemark(
                    folder, "line", "", Point.getCoordinates(points),
                    Utils.decodeColor(
                        style.optString("strokeColor", (String) null),
                        Color.blue), style.optInt("strokeWidth", 1));
            } else if (type.equals("point")) {
                KmlUtil.placemark(folder, "", "",
                                  points.get(0).getLatitude(),
                                  points.get(0).getLongitude(), 0, null);
            } else if (type.equals("label")) {
                String label  = style.optString("label", "");
                String _label = label;
                if (_label.length() > 10) {
                    _label = _label.substring(0, 9) + "...";
                }
                KmlUtil.placemark(folder, _label, label,
                                  points.get(0).getLatitude(),
                                  points.get(0).getLongitude(), 0, null);
            }
        }

        String xml = XmlUtil.toString(root, true);
        String returnFile =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry))
            + ".kml";
        request.setReturnFilename(returnFile);

        Result result = new Result("", new StringBuilder(xml),
                                   KmlOutputHandler.MIME_KML);

        return result;
    }

}
