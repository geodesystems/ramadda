/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.ogc;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.TwoFacedObject;

import java.awt.geom.*;

import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

public class EsriServiceImporter extends ImportHandler {

    public static final String TAG_SERVICES = "services";

    public static final String TAG_COPYRIGHT_TEXT = "copyrightText";

    public static final String TAG_FOLDERS = "folders";

    public static final String TAG_URL = "url";

    public static final String TAG_DESCRIPTION = "description";

    public static final String TAG_SERVICEDESCRIPTION = "serviceDescription";

    public static final String TAG_LAYERS = "layers";

    public static final String TAG_FULLEXTENT = "fullExtent";

    public static final String TAG_SPATIALREFERENCE = "spatialReference";

    public static final String TAG_WKID = "wkid";

    public static final String TAG_LATEST_WKID = "latestWkid";

    public static final String TYPE_ESRI = "esriservice";

    public EsriServiceImporter(Repository repository) {
        super(repository);
    }

    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("Esri Rest Service Import",
                                           TYPE_ESRI));
    }

    @Override
    public Result handleUrlRequest(final Request request,
                                   Repository repository,
                                   final String theUrl,
                                   final Entry parentEntry)
            throws Exception {
        if ( !request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_ESRI)) {
            return null;
        }

        ActionManager.Action action = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                try {
                    String       url     = theUrl;
                    StringBuffer sb      = new StringBuffer();
                    List<Entry>  entries = new ArrayList<Entry>();
                    String[]     toks    = url.split("\\?");
                    url = toks[0];
                    if (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }

                    //Make a top-level entry
                    String topName = new URL(url).getHost()
                                     + " rest services";
                    Entry topEntry = makeEntry(request, parentEntry,
                                         "type_esri_restserver", topName,
                                         url);
                    entries.add(topEntry);

                    try {
                        processServiceList(request, topEntry, actionId,
                                           entries, url, url);
                    } catch (Exception exc) {
                        exc.printStackTrace();

                        throw exc;
                    }

                    if ( !okToContinue(actionId, entries)) {
                        return;
                    }
                    for (Entry entry : entries) {
                        entry.setUser(request.getUser());
                    }
                    getEntryManager().addNewEntries(request, entries);
                    getActionManager().setContinueHtml(
                        actionId,
                        entries.size() + " entries created" + HtmlUtils.br()
                        + HtmlUtils.href(
                            request.makeUrl(
                                getRepository().URL_ENTRY_SHOW, ARG_ENTRYID,
                                topEntry.getId()), "Continue"));

                } catch (Exception exc) {
                    getActionManager().setContinueHtml(actionId,
                            "Error:" + exc);
                }
            }
        };

        return getActionManager().doAction(request, action,
                                           "Importing the services", "",
                                           parentEntry);

    }

    private JSONTokener getTokenizer(String url) throws Exception {
        url = url + "?f=pjson";
        try {
            String      json = IOUtil.readContents(url.toString(),
                                   getClass());
            JSONTokener tokenizer = new JSONTokener(json);

            return tokenizer;
        } catch (Exception exc) {
            System.err.println("Error fetching url:" + url);

            return null;
        }
    }

    private void processServiceList(Request request, Entry parentEntry,
                                    Object actionId, List<Entry> entries,
                                    String baseUrl, String serviceUrl)
            throws Exception {

        if ( !okToContinue(actionId, entries)) {
            return;
        }

        System.err.println("EsriServiceImporter: url: " + serviceUrl);
        JSONTokener tokenizer = getTokenizer(serviceUrl);
        if (tokenizer == null) {
            return;
        }
        JSONObject obj = new JSONObject(tokenizer);

        if ( !okToContinue(actionId, entries)) {
            return;
        }

        if (obj.has(TAG_FOLDERS)) {
            JSONArray folders = obj.getJSONArray(TAG_FOLDERS);
            for (int i = 0; i < folders.length(); i++) {
                if ( !okToContinue(actionId, entries)) {
                    return;
                }

                String folder = folders.getString(i);
                //System.err.println("EsriServiceImporter: making folder:" + folder);
                String url = baseUrl + "/" + folder;
                Entry folderEntry = makeEntry(request, parentEntry,
                                        "type_esri_restfolder",
                                        getNameFromId(folder), url);
                entries.add(folderEntry);
                processServiceList(request, folderEntry, actionId, entries,
                                   baseUrl, url);
            }
        }

        if (obj.has(TAG_SERVICES)) {
            JSONArray services = obj.getJSONArray(TAG_SERVICES);
            for (int i = 0; i < services.length(); i++) {
                JSONObject service = services.getJSONObject(i);
                if ( !okToContinue(actionId, entries)) {
                    return;
                }
                processService(request, parentEntry, actionId, entries,
                               baseUrl, service);
            }
        }
    }

    private Entry makeEntry(Request request, Entry parentEntry, String type,
                            String name, String url)
            throws Exception {
        Entry entry = getRepository().getTypeHandler(type).createEntry(
                          getRepository().getGUID());
        Date now = new Date();
        entry.setResource(new Resource(url, Resource.TYPE_URL));
        entry.setCreateDate(now.getTime());
        entry.setChangeDate(now.getTime());
        entry.setStartDate(now.getTime());
        entry.setEndDate(now.getTime());
        entry.setName(name);
        entry.setParentEntryId(parentEntry.getId());

        return entry;

    }

    private boolean okToContinue(Object actionId, List<Entry> entries) {
        if ( !getRepository().getActionManager().getActionOk(actionId)) {
            return false;
        }

        if (entries.size() > 0) {
            getActionManager().setActionMessage(actionId,
                    "Processed:" + entries.size() + " entries. Last URL:"
                    + entries.get(entries.size()
                                  - 1).getResource().getPath());
        }

        return true;
    }

    private String getNameFromId(String id) {
        String[] toks = id.split("/");

        return toks[toks.length - 1];
    }

    private void processService(Request request, Entry parentEntry,
                                Object actionId, List<Entry> entries,
                                String baseUrl, JSONObject service)
            throws Exception {
        String id   = service.getString("name");
        String type = service.getString("type");
        String url  = null;

        //        http://services.nationalmap.gov/arcgis/rest/services?f=pjson
        if (service.has(TAG_URL)) {
            url = service.getString(TAG_URL);
        } else {
            url = baseUrl + "/" + id + "/" + type;
        }
        //        System.err.println("EsriServiceImporter.processService:" + url);

        JSONObject   obj         = new JSONObject(getTokenizer(url));
        String       name        = null;
        StringBuffer description = new StringBuffer();
        if (obj.has(TAG_SERVICEDESCRIPTION)) {
            description.append(obj.getString(TAG_SERVICEDESCRIPTION));
        }
        if ( !Utils.stringDefined(name)) {
            name = getNameFromId(id);
        }
        if (obj.has(TAG_DESCRIPTION)) {
            if (description.length() != 0) {
                description.append("\n");
            }
            description.append(obj.getString(TAG_DESCRIPTION).trim());
        }
        if (obj.has(TAG_SERVICEDESCRIPTION)) {
            if (description.length() != 0) {
                description.append("\n");
            }
            description.append(obj.getString(TAG_SERVICEDESCRIPTION).trim());
        }

        if (name.length() > Entry.MAX_NAME_LENGTH) {
            name = name.substring(0, 195) + "...";
        }

        String entryType = "type_esri_restservice";
        if (type.equals("FeatureServer")) {
            entryType = "type_esri_featureserver";
        } else if (type.equals("MapServer")) {
            entryType = "type_esri_mapserver";
        } else if (type.equals("ImageServer")) {
            entryType = "type_esri_imageserver";
        } else if (type.equals("GPServer")) {
            entryType = "type_esri_gpserver";
        } else if (type.equals("GeometryServer")) {
            entryType = "type_esri_geometryserver";
        }

        Entry entry = makeEntry(request, parentEntry, entryType, name, url);
        entry.setDescription(description.toString());
        Object[] values = entry.getTypeHandler().getEntryValues(entry);
        String   wkid   = processBounds(request, entry, obj);

        values[0] = id;
        if (obj.has(TAG_COPYRIGHT_TEXT)) {
            values[1] = obj.get(TAG_COPYRIGHT_TEXT) + "";
        }
        values[2] = wkid;

        entries.add(entry);

        if (obj.has(TAG_LAYERS)) {
            JSONArray layers = obj.getJSONArray(TAG_LAYERS);
            for (int i = 0; i < layers.length(); i++) {
                JSONObject layer = layers.getJSONObject(i);
                if ( !okToContinue(actionId, entries)) {
                    return;
                }
                processLayer(request, entry, actionId, entries, url, layer);
            }

        }

    }

    private void processLayer(Request request, Entry parentEntry,
                              Object actionId, List<Entry> entries,
                              String baseUrl, JSONObject layer)
            throws Exception {
        String id  = layer.get("id") + "";
        String url = baseUrl + "/" + id;
        System.err.println("URL:" + url);
        JSONObject obj  = new JSONObject(getTokenizer(url));

        String     name = obj.getString("name");
        String     type = obj.getString("type");
        String     desc = obj.getString("description");

        System.err.println("layer:" + name + " " + type);
        Entry entry = makeEntry(request, parentEntry, "type_esri_layer",
                                name, url);
        entry.setDescription(desc);
        Object[] values = entry.getTypeHandler().getEntryValues(entry);
        values[0] = id;
        values[1] = type;
        if (obj.has(TAG_COPYRIGHT_TEXT)) {
            values[2] = obj.get(TAG_COPYRIGHT_TEXT) + "";
        }
        String wkid = processBounds(request, entry, obj);

        if (obj.has("fields") && !obj.isNull("fields")) {
            JSONArray     fields = obj.getJSONArray("fields");
            StringBuilder sb     = new StringBuilder();
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                sb.append(field.getString("name"));
                sb.append("\n");
            }
            values[3] = sb.toString();
        }

        entries.add(entry);
    }

    private double[] getBounds(JSONObject extent, String wkid)
            throws Exception {
        double                         xmin = extent.getDouble("xmin");
        double                         xmax = extent.getDouble("xmax");
        double                         ymin = extent.getDouble("ymin");
        double                         ymax = extent.getDouble("ymax");
        com.jhlabs.map.proj.Projection proj = null;

        wkid = wkid.trim();
        if (wkid.equals("4326") || wkid.equals("4269") || wkid.equals("4269")
                || wkid.equals("4617")) {}
        else if (proj == null) {
            try {
                proj = com.jhlabs.map.proj.ProjectionFactory
                    .getNamedPROJ4CoordinateSystem(wkid);
                if (proj != null) {
                    //                    System.err.println("Found projection:" + wkid);
                } else {
                    System.err.println("* could not find projection:" + wkid);
                }
            } catch (Exception exc) {
                System.err.println("Error making projection:" + wkid + " "
                                   + exc);

                return null;
            }
            if (proj == null) {
                return null;
            }
        }

        String msg = "coords:" + xmin + " " + ymax + " " + xmax + " " + ymin;
        if (proj != null) {
            Point2D.Double dst = new Point2D.Double(0, 0);
            Point2D.Double src = new Point2D.Double(xmin, ymax);
            dst  = proj.inverseTransform(src, dst);
            xmin = dst.getX();
            ymax = dst.getY();
            src  = new Point2D.Double(xmax, ymin);
            dst  = proj.inverseTransform(src, dst);
            xmax = dst.getX();
            ymin = dst.getY();
        }
        double[] b = new double[] { ymax, xmin, ymin, xmax };
        if ((Math.abs(b[0]) < 360) && (Math.abs(b[1]) < 360)
                && (Math.abs(b[2]) < 360) && (Math.abs(b[3]) < 360)) {
            return b;
        }

        System.err.println("** bad bounds  - projection: " + wkid + " "
                           + b[0] + " " + b[1] + " " + b[2] + " " + b[3]);
        System.err.println(msg);

        return null;
    }

    public static void main(String[] args) throws Exception {
        com.jhlabs.map.proj.Projection proj =
            com.jhlabs.map.proj.ProjectionFactory
                .getNamedPROJ4CoordinateSystem(args[0]);

        if (proj == null) {
            System.err.println("no proj");

            return;
        }
        Point2D.Double dst = new Point2D.Double(0, 0);
        Point2D.Double src = new Point2D.Double(Double.parseDouble(args[1]),
                                 Double.parseDouble(args[2]));
        dst = proj.inverseTransform(src, dst);
        System.err.println("dst:" + dst.getX() + " " + dst.getY());
    }

    private String processBounds(Request request, Entry entry, JSONObject obj)
            throws Exception {
        String wkid = null;
        if (obj.has(TAG_FULLEXTENT)) {
            JSONObject extent = obj.getJSONObject(TAG_FULLEXTENT);
            if (extent.has(TAG_SPATIALREFERENCE)) {
                JSONObject spatialReference =
                    extent.getJSONObject(TAG_SPATIALREFERENCE);
                if (spatialReference.has(TAG_WKID)) {
                    wkid = spatialReference.get(TAG_WKID) + "";
                    if (spatialReference.has(TAG_LATEST_WKID)) {
                        wkid = spatialReference.get(TAG_LATEST_WKID) + "";
                    }
                    double[] bounds = getBounds(extent, wkid);
                    if ((bounds != null) && (Math.abs(bounds[0]) < 360)
                            && (Math.abs(bounds[1]) < 360)
                            && (Math.abs(bounds[2]) < 360)
                            && (Math.abs(bounds[3]) < 360)) {
                        entry.setNorth(bounds[0]);
                        entry.setWest(bounds[1]);
                        entry.setSouth(bounds[2]);
                        entry.setEast(bounds[3]);
                    }
                }
            }
        }

        return wkid;
    }

}
