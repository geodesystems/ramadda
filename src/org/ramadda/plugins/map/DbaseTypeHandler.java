/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.map;

import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;

import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.map.MapInfo;

import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.GenericTypeHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.*;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.Element;

import ucar.unidata.gis.shapefile.DbaseData;
import ucar.unidata.gis.shapefile.DbaseFile;

import ucar.unidata.io.BeLeDataInputStream;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * TODO: this seems to fail with a number of different dbase files
 * Also - need to check the RecordFile to see if its making and storing the fields OK
 */
public class DbaseTypeHandler extends PointTypeHandler implements WikiConstants {

    public static final String PROP_FIELDS = "fields";

    public static final String PROP_POINTFIELDS = "pointfields";

    private TTLCache<String, DbaseFile> cache;

    public DbaseTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        //Create the cache with a 1 minute TTL
        cache = new TTLCache<String, DbaseFile>(60 * 1000,
                             "Dbase File Cache");
    }

    public boolean shouldProcessResource(Request request, Entry entry) {
        return false;
    }

    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new DbaseRecordFile(request, entry,
                                   new IO.Path(entry.getResource().getPath()), null);
    }

    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        if ( !newEntry) {
            return;
        }
        if ( !entry.isFile()) {
            return;
        }
        DbaseRecordFile recordFile = new DbaseRecordFile(request, entry,
							 new IO.Path(entry.getResource().getPath()),
							 getDbf(entry));
        String props = recordFile.getEntryFieldsProperties();
        getEntryValues(entry)[IDX_PROPERTIES] = props;
    }

    private DbaseFile getDbf(Entry entry) throws Exception {
        synchronized (cache) {
            DbaseFile dbf = cache.get(entry.getId());
            if (dbf == null) {
                dbf = new DbaseFile(
                    new BeLeDataInputStream(
                        getStorageManager().getInputStream(
                            entry.getResource().getPath())));
                dbf.loadHeader();
                dbf.loadData();
                cache.put(entry.getId(), dbf);
            }

            return dbf;
        }
    }

    private ShapefileOutputHandler getOutputHandler() throws Exception {
        return (ShapefileOutputHandler) getRepository().getOutputHandler(
            ShapefileOutputHandler.class);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Dec 8, '18
     * @author         Enter your name here...
     */
    public class DbaseRecordFile extends CsvFile {

        Request request;

        Entry entry;

        DbaseFile dbf;

        Hashtable props;

        String fields;

        public DbaseRecordFile(Request request, Entry entry, IO.Path path,
                               DbaseFile dbf)
                throws Exception {
            super(path);
            //Get the properties from the entry
            props        = getRecordProperties(entry);
            this.request = request;
            this.entry   = entry;
            this.dbf     = dbf;
            fields       = (String) props.get(PROP_FIELDS);
            if (fields == null) {
                getEntryFieldsProperties();
            }
        }

        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
            if (dbf == null) {
                dbf = getDbf(entry);
            }
            List<DbaseDataWrapper> datum =
                getOutputHandler().getDatum(request, entry, dbf);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dbf.getNumRecords(); i++) {
                int colCnt = 0;
                for (DbaseDataWrapper dbd : datum) {
                    String value;
                    if (dbd.getType() == DbaseData.TYPE_NUMERIC) {
                        value = "" + dbd.getDouble(i);
                    } else {
                        value = "" + dbd.getData(i);
                        value = value.trim();
                    }
                    if (colCnt++ > 0) {
                        sb.append(",");
                    }
                    sb.append(Seesv.cleanColumnValue(value));
                }
                sb.append("\n");
            }
            ByteArrayInputStream bais =
                new ByteArrayInputStream(sb.toString().getBytes());

            return bais;
        }

        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            putProperty(PROP_SKIPLINES, "0");
            super.prepareToVisit(visitInfo);
            String fields = makeFields();
            putProperty(PROP_FIELDS, fields);

            return visitInfo;
        }

        private String makeFields() throws Exception {
            List<String> fields = new ArrayList<String>();
            if (dbf == null) {
                dbf = getDbf(entry);
            }
            if (dbf == null) {
                return makeFields(fields);
            }
            List<DbaseDataWrapper> fieldDatum =
                getOutputHandler().getDatum(request, entry, dbf);
            for (DbaseDataWrapper dbd : fieldDatum) {
                String type = "string";
                if (dbd.getType() == DbaseData.TYPE_NUMERIC) {
                    type = "double";
                }
                fields.add(makeField(dbd.getName(), attrType(type)));
            }

            return makeFields(fields);
        }

        @Override
        public List<RecordField> doMakeFields(boolean failureOk) {
            try {
                if (fields == null) {
                    getEntryFieldsProperties();
                }
                putProperty(PROP_FIELDS, fields);

                return super.doMakeFields(failureOk);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        public String getEntryFieldsProperties() throws Exception {
            fields = makeFields();

            return "#fields for data access. do not change\n" + PROP_FIELDS
                   + "=" + fields + "\n";
        }

    }

}
