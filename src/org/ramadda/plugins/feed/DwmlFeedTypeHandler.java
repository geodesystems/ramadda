/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.feed;



import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.AtomUtil;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;

import ucar.unidata.xml.XmlUtil;


import org.w3c.dom.*;


import java.net.URL;
import java.io.*;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.TimeZone;


/**
 */
@SuppressWarnings("unchecked")
public class DwmlFeedTypeHandler extends PointTypeHandler {

    /** _more_ */
    private static int IDX = RecordTypeHandler.IDX_LAST + 1;
    private static int IDX_TIMEZONE = IDX++;


    /** _more_ */
    private TTLCache<String, Weather> forecastCache = new TTLCache<String,
	Weather>(5 * 60
		 * 1000);

    /** _more_ */
    private TTLCache<String, Weather> currentCache = new TTLCache<String,
	Weather>(5 * 60
		 * 1000);



    /** _more_ */
    private static final String URL =
        "https://forecast.weather.gov/MapClick.php?lat=${lat}&lon=${lon}&unit=0&lg=english&FcstType=dwml";


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DwmlFeedTypeHandler(Repository repository, Element entryNode)
	throws Exception {
        super(repository, entryNode);
    }


    public static boolean defined(String s) {
	return  (Utils.stringDefined(s) && !s.equals("NA"));
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
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
	throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        if ( !Utils.stringDefined(entry.getName())) {
            Weather forecast = getForecast(request,entry);
            if ((forecast != null) && (forecast.location != null)) {
                entry.setName(forecast.location);
            }
        }
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new DwmlRecordFile(request,getRepository(), this, entry);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public static class DwmlRecordFile extends CsvFile {

        /** _more_ */
        private Repository repository;

	private Request request;

        /** _more_ */
        private String dataUrl;

        /** _more_ */
        private Entry entry;

	DwmlFeedTypeHandler typeHandler;

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
        public DwmlRecordFile(Request request,Repository repository, DwmlFeedTypeHandler ctx, Entry entry)
                throws IOException {
            super(null, ctx, null);
	    this.request=request;
	    typeHandler = ctx;
            this.repository = repository;
            this.entry      = entry;
        }


	private String format(String s) {
	    if(s==null) return "NaN";
	    if(s.indexOf(",")>=0) return "\"" + s +"\"";
	    return s;
	}

        /**
         * @param buffered _more_
         *
         * @return _more_
         *
         *
         * @throws Exception _more_
         */
        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
	    Weather forecast = typeHandler.getForecast(request,entry);
	    StringBuilder sb = new StringBuilder();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	    if(forecast!=null) {
		for(Weather.Time t: forecast.times) {
		    sb.append(sdf.format(t.date));
		    sb.append(",");
		    sb.append(format(t.min));
		    sb.append(",");
		    sb.append(format(t.max));
		    sb.append(",");
		    sb.append(format(t.words));
		    sb.append(",");
		    sb.append(format(t.icon));
		    sb.append("\n");		    
		}
	    }
	    return new BufferedInputStream(new  ByteArrayInputStream(sb.toString().getBytes()));
        }


    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param getForecast _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Weather getWeather(Request request,Entry entry, boolean getForecast)
	throws Exception {
        try {
            if ( !entry.hasLocationDefined(request)) {
                return null;
            }
            String  key      = entry.getId() + "_" + entry.getChangeDate();
            Weather forecast = forecastCache.get(key);
            Weather current  = currentCache.get(key);
            if ((forecast == null) || (current == null)) {
                String url =
                    URL.replace("${lat}",
                                "" + entry.getLatitude(request)).replace("${lon}",
								  "" + entry.getLongitude(request));
                String  xml  = Utils.readUrl(url);
                Element root = XmlUtil.getRoot(xml);
                Element forecastNode =
                    XmlUtil.findElement(XmlUtil.getElements(root, "data"),
                                        "type", "forecast");
                if (forecastNode == null) {
                    return null;
                }
                forecast = new Weather(forecastNode);
                forecastCache.put(key, forecast);

                Element currentNode =
                    XmlUtil.findElement(XmlUtil.getElements(root, "data"),
                                        "type", "current observations");
                if (currentNode != null) {
                    current = new Weather(currentNode);
                    currentCache.put(key, current);
                }
            }

            return getForecast
		? forecast
		: current;
        } catch (Exception exc) {
            System.err.println("Error getting weather:" + exc);
            exc.printStackTrace();

            return null;
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Weather getForecast(Request request,Entry entry) throws Exception {
        return getWeather(request,entry, true);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Weather getCurrent(Request request,Entry entry) throws Exception {
        return getWeather(request,entry, false);
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
	throws Exception {

        StringBuilder sb = new StringBuilder();
        if ( !tag.startsWith("nws.")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        int cnt = getWikiManager().getProperty(wikiUtil, props, "count",
					       1000);
        boolean vertical = getWikiManager().getProperty(wikiUtil, props,
							"vertical", false);
        boolean showHeader = getWikiManager().getProperty(wikiUtil, props, "showHeader",
							  getWikiManager().getProperty(wikiUtil, props, "addHeader", true));							  
        boolean showDetails = getWikiManager().getProperty(wikiUtil, props,
							   "showDetails", true);
        boolean showLabel = getWikiManager().getProperty(wikiUtil, props,
							   "showLabel", true);
        boolean showHazard = getWikiManager().getProperty(wikiUtil, props,
							   "showHazard", false);		
        if (tag.equals("nws.hazards")) {
            addHazard(request, entry, sb, showHeader);
        } else if (tag.equals("nws.current")) {
            addCurrent(request, entry, sb, showHeader, vertical, showDetails,showLabel,showHazard);
        } else if (tag.equals("nws.forecast")) {
            addForecast(request, entry, sb, showHeader, cnt);
            if (showDetails) {
                addDetails(request, entry, sb, showHeader, cnt);
            }
        } else if (tag.equals("nws.details")) {
            addDetails(request, entry, sb, showHeader, cnt);
        } else if (tag.equals("nws.weather")) {
            addCurrent(request, entry, sb, showHeader, vertical, showDetails,showLabel,showHazard);
            addForecast(request, entry, sb, showHeader, cnt);
        } else if (tag.equals("nws.all")) {
            addHazard(request, entry, sb, showHeader);
            sb.append("<br>");
            addCurrent(request, entry, sb, showHeader, vertical, true,showLabel,showHazard);
            addForecast(request, entry, sb, showHeader, 1000);
            addDetails(request, entry, sb, showHeader, 1000);
        } else {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        String contents = sb.toString();
	//Don't show anything if nothing is there
	if(contents.length()==0) return "";
	sb = new StringBuilder();
	sb.append(
		  HU.cssBlock(
			      ".nws-contents {max-width:100%;padding:5px;}\n.nws-header-label {font-weight:bold;margin:5px;}\n.nws-label {font-size:80%;white-space:nowrap;max-width:90px;overflow-x:auto;}\n.nws-block {max-width:100%;display:inline-block;border: 0px red solid;  border-radius: 4px;}\n.nws-header {font-weight:bold;background:#eee; padding:5px;}\n.nws-block-hazard {border-color:#EED4D4;}\n.nws-block-hazard .nws-header {background:#EED4D4; color:#A80000}\n"));
	sb.append(contents);
	contents = sb.toString();

        String heading  = (String) props.get("heading");
        if (heading != null) {
            heading = HU.href(getEntryManager().getEntryUrl(request, entry),
                              heading);
            contents = HU.center("<b>" + heading + "</b>") + contents;
        }

        return contents;
	//        return HU.div(contents, HU.style("display:inline-block;"));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param showHeader _more_
     *
     * @throws Exception _more_
     */
    private void addHazard(Request request, Entry entry, Appendable sb,
                           boolean showHeader)
	throws Exception {
        Weather forecast = getForecast(request,entry);
        if (forecast == null) {
            sb.append("No forecast defined");
            return;
        }
        if (forecast.hazards == null) {
            return;
        }
        HU.open(sb, "div", HU.cssClass("nws-block"));
        if (showHeader) {
            HU.open(sb, "div", HU.cssClass("nws-block-hazard"));
            HU.div(sb, "Hazardous Weather Conditions",
		   HU.cssClass("nws-header"));
            HU.open(sb, "div", HU.cssClass("nws-contents"));
        }
        HU.open(sb, "div", HU.style("padding-left:5px;"));
        sb.append(HU.tag("ul"));
	String link = forecast.hazards.toString();
	link = link.replace("<a ","<a target='_wx' ");
	sb.append(link);
        sb.append("</ul>");
        HU.close(sb, "div");
        if (showHeader) {
            HU.close(sb, "div");
            HU.close(sb, "div");
        }
        HU.close(sb, "div");

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param showHeader _more_
     * @param vertical _more_
     * @param showDetails _more_
     *
     * @throws Exception _more_
     */
    private void addCurrent(Request request, Entry entry, Appendable sb,
                            boolean showHeader, boolean vertical,
                            boolean showDetails,boolean showLabel, boolean showHazard)
	throws Exception {
        Weather current = getCurrent(request,entry);
        if ((current == null) || (current.times.size() == 0)) {
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        String           timezone   = entry.getStringValue(IDX_TIMEZONE, "");
        if ( !Utils.stringDefined(timezone)) {
            timezone = getEntryUtil().getTimezone(request,entry);
        }
        if (Utils.stringDefined(timezone)) {
            dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        } else {
            dateFormat.setTimeZone(RepositoryUtil.TIMEZONE_DEFAULT);
        }
        dateFormat.applyPattern("MMM d - H:mm z");
        Weather.Time time = current.times.get(0);
	HU.open(sb, "div", HU.cssClass("nws-block"));
        if (showHeader) {
            String link = HU.href(getEntryManager().getEntryUrl(request, entry),
				  entry.getName(),
				  HU.cssClass("ramadda-clickable")+
				  HU.style("text-decoration:none;" + (showLabel?"font-size:16px;color:#135897;":""))
);
				  
            String header = showLabel?  "Current conditions at<br>":"";
	    header += link;
            HU.div(sb, header, HU.cssClass("nws-header"));
            HU.open(sb, "div", HU.cssClass("nws-contents"));
        }

	if(showHazard) {
            addHazard(request, entry, sb, false);
	}
	String blockClass=  HU.cssClass("nws-block");
	List<String> hboxes = new ArrayList<String>();
        if ( !showDetails) {
            HU.div(sb, "Now", HU.style("display:inline-block;font-weight: bold;"));
	}
	if ((time.icon != null) && !time.icon.equals("NULL")) {
	    hboxes.add(HU.img(time.icon, time.words));
	}
	String s = "";
	if (defined(time.weather)) { 
	    s+=HU.div(time.weather, "");
	}
	if (defined(time.apparent)) {
	    s+=HU.div(time.apparent + "&deg;&nbsp;F",
		      HU.style("font-size:30px; font-weight: bold;"));
	}
	if(stringDefined(s))
	    hboxes.add(s);

        if (showDetails) {
	    hboxes.add(time.getFieldsTable());
	}
	if ( !vertical) {
	    HU.open(sb, "div",HU.style("display:flex;flex-flow: row wrap;"));
	    for(String box:hboxes) {
		HU.div(sb, box, HU.style("margin-right:10px;")+HU.cssClass("nws-block"));
	    }
	    HU.close(sb, "div");
	} else {
	    for(String box:hboxes) {
		HU.div(sb, box,"");
	    }
	}


        if (showHeader) {
            HU.close(sb, "div");
        }
	HU.close(sb, "div");

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param showHeader _more_
     * @param cnt _more_
     *
     * @throws Exception _more_
     */
    private void addForecast(Request request, Entry entry, Appendable sb,
                             boolean showHeader, int cnt)
	throws Exception {
        Weather forecast = getForecast(request,entry);
        if (forecast == null) {
            sb.append("No forecast defined");

            return;
        }
        HU.open(sb, "div", HU.cssClass("nws-block"));
        if (showHeader) {
            HU.div(sb, "Extended Forecast",
		   HU.cssClass("nws-header"));
            HU.open(sb, "div", HU.cssClass("nws-contents"));
        }
        sb.append("<div style=\"width:100%;overflow-x:auto;\">\n");
        sb.append("<table border=0 cellspacing=0 cellpadding=0>\n");
        sb.append("<tr valign=bottom>");
        int tmpCnt = cnt;
        for (Weather.Time time : forecast.times) {
            if (tmpCnt-- <= 0) {
                break;
            }
            sb.append(
		      HU.td(HU.div(time.label,
				   HU.cssClass("nws-label nws-header-label")),
			    HU.attr("align","center")));
        }
        sb.append("</tr>");
        sb.append("<tr>");
        String td;
        tmpCnt = cnt;
        for (Weather.Time time : forecast.times) {
            if (tmpCnt-- <= 0) {
                break;
            }
            if (time.icon == null) {
                td = "";
            } else {
                td = HU.div(
			    HU.img(time.icon, time.words),
			    "style=\" margin:5px;  font-weight: bold;\" ");
            }
            sb.append(HU.td(td, " align=center "));
        }
        sb.append("</tr>");
        tmpCnt = cnt;
        sb.append("<tr>");
        for (Weather.Time time : forecast.times) {
            if (tmpCnt-- <= 0) {
                break;
            }
            if (time.max != null) {
                td = HU.div("High: " + time.max + "F",
			    HU.cssClass("nws-label") +
			    HU.style("color:red;"));
            } else if (time.min != null) {
                td = HU.div("Low: " + time.min + "F",
			    HU.cssClass("nws-label") +
			    HU.style("color:blue;"));
            } else {
                td = "";
            }
            sb.append(HU.td(td, " align=center "));
        }
        sb.append("</tr>");


        tmpCnt = cnt;
        sb.append("<tr valign=top >");
        for (Weather.Time time : forecast.times) {
            if (tmpCnt-- <= 0) {
                break;
            }
            if (time.weather == null) {
                td = "";
            } else {
                td = HU.div(time.weather,
			    "class=nws-label"
			    + HU.attr("title", time.weather));
            }
            sb.append(HU.td(td, " align=center "));
        }
        sb.append("</tr>");

        sb.append("</table>\n");
        sb.append("</div>");
        if (showHeader) {
            HU.close(sb, "div");
        }
        HU.close(sb, "div");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param showHeader _more_
     * @param cnt _more_
     *
     * @throws Exception _more_
     */
    private void addDetails(Request request, Entry entry, Appendable sb,
                            boolean showHeader, int cnt)
	throws Exception {
        Weather forecast = getForecast(request,entry);
        if (forecast == null) {
            sb.append("No forecast defined");

            return;
        }

        HU.open(sb, "div", HU.cssClass("nws-block"));
        if (showHeader) {
            HU.div(sb, "Detailed Forecast",
		   HU.cssClass("nws-header"));
        }

        sb.append("<table border=0 cellspacing=0 cellpadding=0>\n");
        boolean even = true;
        for (Weather.Time time : forecast.times) {
            if (cnt-- <= 0) {
                break;
            }
            String style = (even
                            ? HU.style("background-color:#eff8fd")
                            : "");
            HU.open(sb, "tr", " valign=top " + style);
            HU.td(
		  sb, HU.div(
			     time.label, HU.style(
						  "margin:5px;  font-weight: bold;")), "align=right ");
            HU.td(
		  sb,
		  HU.div(time.words, HU.style("margin:5px;")),
		  "align=left ");
            HU.close(sb, "tr");
            even = !even;
        }
        HU.close(sb, "table");
        if (Utils.stringDefined(forecast.moreWeather)) {
            sb.append(HU.href(forecast.moreWeather,
			      "More Weather@NWS"));
        }
        HU.close(sb, "div");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param times _more_
     * @param node _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void checkTimes(Request request,
                            Hashtable<String, Element> times, Element node,
                            Appendable sb)
	throws Exception {
        Element time = times.get(XmlUtil.getAttribute(node, "time-layout",
						      ""));
        if (time == null) {
            return;
        }
        times.remove(XmlUtil.getAttribute(node, "time-layout", ""));
        NodeList children = XmlUtil.getElements(time, "start-valid-time");
        sb.append("<tr>\n");
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element timeNode = (Element) children.item(childIdx);
            sb.append("<td style=\"padding:10px;\" align=center>");
            sb.append(XmlUtil.getAttribute(timeNode, "period-name", ""));
            sb.append("</td>");
        }
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Nov 16, '18
     * @author         Enter your name here...
     */
    private static class Weather {

        /** _more_ */
        String location;

        /** _more_ */
        String moreWeather;

        /** _more_ */
        StringBuilder hazards;

        /** _more_ */
        List<Time> times = new ArrayList<Time>();

        /** _more_ */
        Hashtable<String, List<Time>> keyMap = new Hashtable<String,
	    List<Time>>();

        /**
         * _more_
         *
         * @param node _more_
         *
         * @throws Exception _more_
         */
        public Weather(Element node) throws Exception {
            moreWeather = XmlUtil.getGrandChildText(node,
						    "moreWeatherInformation", (String) null);
            Element location = XmlUtil.findChild(node, "location");
            if (location != null) {
                this.location = XmlUtil.getGrandChildText(location,
							  "area-description",
							  XmlUtil.getGrandChildText(location, "description",
										    ""));
            }
            processTimes(node);
            processParams(node);
        }


        /**
         * _more_
         *
         * @param hazard _more_
         */
        public void addHazard(String hazard) {
            if (hazards == null) {
                hazards = new StringBuilder();
            } else {
                hazards.append("<br>");
            }
            hazards.append("<li>");
            hazards.append(hazard);
        }


        /**
         * _more_
         *
         * @param dataNode _more_
         *
         * @throws Exception _more_
         */
        private void processParams(Element dataNode) throws Exception {

            Element params = XmlUtil.getElement(dataNode, "parameters");
            if (params == null) {
                return;
            }
            NodeList children = XmlUtil.getElements(params);
            for (int childIdx = 0; childIdx < children.getLength();
		 childIdx++) {
                Element node = (Element) children.item(childIdx);
                String  tag  = node.getTagName();
                String  key  = XmlUtil.getAttribute(node, "time-layout", "");

                if (tag.equals("hazards")) {
                    NodeList children2 = XmlUtil.getElements(node,
							     "hazard-conditions");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        NodeList children3 = XmlUtil.getElements(child,
								 "hazard");
                        for (int j = 0; j < children3.getLength(); j++) {
                            Element child3 = (Element) children3.item(i);
                            String url = XmlUtil.getGrandChildText(child3,
								   "hazardTextURL", null);
                            if (url != null) {
                                addHazard(
					  HU.href(
						  url,
						  XmlUtil.getAttribute(
								       child3, "headline",
								       "Link"), HU.style(
											 "bold;color:#A80000;")));
                            }
                        }
                    }

                    continue;
                }

                if (key.equals("")) {
                    continue;
                }
                List<Time> times = keyMap.get(key);
                if (times == null) {
                    System.err.println("no key map:"
                                       + XmlUtil.toString(node));

                    continue;
                }
                if (tag.equals("conditions-icon")) {
                    NodeList children2 = XmlUtil.getElements(node,
							     "icon-link");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        String  icon  = XmlUtil.getChildText(child);
                        icon = icon.replace("http://forecast.weather.gov",
                                            "https://forecast.weather.gov");
                        times.get(i).icon = icon;
                    }
                } else if (tag.equals("wordedForecast")) {
                    NodeList children2 = XmlUtil.getElements(node, "text");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        times.get(i).words = XmlUtil.getChildText(child);
                    }
                } else if (tag.equals("weather")) {
                    NodeList children2 = XmlUtil.getElements(node,
							     "weather-conditions");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        String summary = XmlUtil.getAttribute(child,
							      "weather-summary",
							      (String) null);
                        if (summary != null) {
                            times.get(i).weather = summary;
                        }
                    }
                } else if (tag.equals("probability-of-precipitation")) {
                    NodeList children2 = XmlUtil.getElements(node, "value");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        times.get(i).precip = XmlUtil.getChildText(child);
                    }
                } else if (tag.equals("humidity")) {
                    NodeList children2 = XmlUtil.getElements(node, "value");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        times.get(i).humidity = XmlUtil.getChildText(child);
                    }
                } else if (tag.equals("pressure")) {
                    NodeList children2 = XmlUtil.getElements(node, "value");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        times.get(i).pressure = XmlUtil.getChildText(child);
                    }
                } else if (tag.equals("direction")) {
                    NodeList children2 = XmlUtil.getElements(node, "value");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        times.get(i).direction = XmlUtil.getChildText(child);
                    }
                } else if (tag.equals("temperature")) {
                    NodeList children2 = XmlUtil.getElements(node, "value");
                    String   type = XmlUtil.getAttribute(node, "type", "");
                    boolean  max       = type.equals("maximum");
                    boolean  min       = type.equals("minimum");
                    boolean  apparent  = type.equals("apparent");
                    boolean  dewpoint  = type.equals("dew point");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        String  value = XmlUtil.getChildText(child);
                        if (max) {
                            times.get(i).max = value;
                        } else if (min) {
                            times.get(i).min = value;
                        } else if (apparent) {
                            times.get(i).apparent = value;
                        } else if (dewpoint) {
                            times.get(i).dewpoint = value;
                        }
                    }
                } else if (tag.equals("wind-speed")) {
                    NodeList children2 = XmlUtil.getElements(node, "value");
                    String   type = XmlUtil.getAttribute(node, "type", "");
                    boolean  gust      = type.equals("gust");
                    for (int i = 0; i < children2.getLength(); i++) {
                        Element child = (Element) children2.item(i);
                        String  value = XmlUtil.getChildText(child);
                        if (gust) {
                            times.get(i).gust = value;
                        } else {
                            times.get(i).sustained = value;
                        }
                    }
                }
            }

        }


        /**
         * _more_
         *
         * @param dataNode _more_
         *
         * @throws Exception _more_
         */
        private void processTimes(Element dataNode) throws Exception {
            SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
            SimpleDateFormat sdf2 =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Hashtable<Date, Time> timeMap = new Hashtable<Date, Time>();
            NodeList timeNodes = XmlUtil.getElements(dataNode, "time-layout");
            //            System.err.println(timeNodes);
            for (int childIdx = 0; childIdx < timeNodes.getLength();
		 childIdx++) {
                Element    timeLayoutNode =
                    (Element) timeNodes.item(childIdx);
                List<Time> timesForThisKey = new ArrayList<Time>();
                String key = XmlUtil.getGrandChildText(timeLayoutNode,
						       "layout-key", "");
                keyMap.put(key, timesForThisKey);
                NodeList timesList = XmlUtil.getElements(timeLayoutNode,
							 "start-valid-time");
                for (int i = 0; i < timesList.getLength(); i++) {
                    Element timeNode = (Element) timesList.item(i);
                    Date    dttm     = null;
                    try {
                        dttm = sdf.parse(XmlUtil.getChildText(timeNode));
                    } catch (Exception exc) {
			try {
			    dttm = sdf2.parse(XmlUtil.getChildText(timeNode));
			} catch (Exception exc2) {
			}
                    }
                    Time time = dttm==null?null:timeMap.get(dttm);
                    if (time == null) {
                        time = new Time(dttm,
                                        XmlUtil.getAttribute(timeNode,
							     "period-name", ""));
                        timeMap.put(dttm, time);
                        times.add(time);
                    } else {
                        if ( !Utils.stringDefined(time.label)) {
                            time.label = XmlUtil.getAttribute(timeNode,
							      "period-name", "");
                        }
                    }
                    timesForThisKey.add(time);
                }
            }
            Collections.sort(times);
        }


        /**
         * Class description
         *
         *
         * @version        $version$, Fri, Nov 16, '18
         * @author         Enter your name here...
         */
        private class Time implements Comparable {

            /** _more_ */
            Date date;

            /** _more_ */
            String label;

            /** _more_ */
            String icon;

            /** _more_ */
            String words = "";

            /** _more_ */
            String weather;

            /** _more_ */
            String precip;

            /** _more_ */
            String max;

            /** _more_ */
            String min;

            /** _more_ */
            String apparent;

            /** _more_ */
            String dewpoint;

            /** _more_ */
            String humidity;

            /** _more_ */
            String direction;

            /** _more_ */
            String pressure;

            /** _more_ */
            String gust;

            /** _more_ */
            String sustained;



            /**
             * _more_
             *
             * @param date _more_
             * @param label _more_
             */
            public Time(Date date, String label) {
                this.date  = date;
                this.label = label;
            }



	    public String getFieldsTable() {
		StringBuilder sb = new StringBuilder();
		sb.append(HU.formTable());
		if (this.humidity != null) {
		    HU.formEntry(sb,"Humidity:", this.humidity + "%");
		}
		if (defined(this.sustained)) {
		    String gust = "";
		    if (defined(this.gust)) {
			gust = "&nbsp;G&nbsp;" + this.gust;
		    }
		    HU.formEntry(sb,"Wind&nbsp;Speed:",  this.sustained + gust + "&nbsp;" + msg("MPH"));
		}

		if (this.pressure != null) {
		    HU.formEntry(sb,"Barometer:", this.pressure + "&nbsp;in");
		}

		if (this.dewpoint != null) {
		    HU.formEntry(sb, "Dew&nbsp;Point:", this.dewpoint);
		}

		/*
		  sb.append(
		  HU.formEntry(
		  "Last&nbsp;Update:",
		  dateFormat.format(this.date).replaceAll(" ", "&nbsp;")));
		*/
		sb.append(HU.formTableClose());
		return sb.toString();
	    }



            /**
             * _more_
             *
             * @param o _more_
             *
             * @return _more_
             */
            public int compareTo(Object o) {
                Date date2 = ((Time) o).date;

                return -date2.compareTo(date);
            }

            /**
             * _more_
             *
             * @return _more_
             */
            public String toString() {
                return "time:" + label + " dttm: " + date;
            }
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String url =
            "https://forecast.weather.gov/MapClick.php?lat=40.0157&lon=-105.2792&unit=0&lg=english&FcstType=dwml";
        String xml = Utils.readUrl(url);
        System.err.println(xml);
        Element root = XmlUtil.getRoot(xml);
        Element forecastNode = XmlUtil.findElement(XmlUtil.getElements(root,
								       "data"), "type", "forecast");
        Weather forecast = new Weather(forecastNode);
    }

}
