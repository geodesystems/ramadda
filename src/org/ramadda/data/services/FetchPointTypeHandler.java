/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.services;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;


/**
 *
 */
public class FetchPointTypeHandler extends PointTypeHandler {

    /** _more_ */
    private static String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";

    /** _more_ */
    private static int IDX = PointTypeHandler.IDX_LAST + 1;

    /** _more_ */
    public static final int IDX_SOURCE_URL = IDX++;

    /** _more_ */
    public static final int IDX_ENABLED = IDX++;

    /** _more_ */
    public static final int IDX_HOURS = IDX++;

    /** _more_ */
    public static final int IDX_LAST_UPDATE = IDX++;

    /** _more_ */
    public static final int IDX_ADD_DATE = IDX++;

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     *
     * @throws Exception On badnes
     */
    public FetchPointTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        startMonitorThread();
    }

    /**
     * _more_
     */
    private void startMonitorThread() {
        Runnable run = new Runnable() {
            public void run() {
                doFetch();
            }
        };
        Misc.run(run);
    }

    /**
     * _more_
     */
    private void doFetch() {
        int errorCnt = 0;
        while (errorCnt < 5) {
            //Check every 30 minutes
            Misc.sleepSeconds(60 * 30);
            try {
                doFetchInner();
                errorCnt = 0;
            } catch (Exception exc) {
                System.err.println("FetchPointTypeHandler error in doFetch:"
                                   + exc);
                exc.printStackTrace();
                errorCnt++;
            }
        }
        System.err.println("FetchPointTypeHandler too many errors");
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void doFetchInner() throws Exception {
        //System.err.println("doFetch");
        Request request = getRepository().getTmpRequest();
        request.put(ARG_TYPE, "type_point_fetch");
        List<Entry> entries =
            request.getRepository().getEntryManager().getEntriesFromDb(request);
        for (Entry entry : entries) {
            fetchEntry(entry);
        }
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void fetchEntry(Entry entry) throws Exception {
        if ( !entry.getResource().isStoredFile()) {
            throw new IllegalArgumentException("Entry is not a stored file:"
                    + entry);
        }
        boolean enabled = (Boolean) entry.getValue(IDX_ENABLED);
        if ( !enabled) {
            return;
        }
        Date now        = new Date();
        Date lastUpdate = (Date) entry.getValue(IDX_LAST_UPDATE);
        if (lastUpdate != null) {
            double hours = (double) entry.getValue(IDX_HOURS);
            double hoursSince = (now.getTime() - lastUpdate.getTime()) / 1000
                                / 60 / 60.0;
            //      System.err.println("hours since:" + hoursSince +" hours:" + hours);
            if (hoursSince < hours) {
                System.err.println("\tfetch:" + entry.getName()
                                   + " not ready");

                return;
            }
        }
        boolean addDate  = (Boolean) entry.getValue(IDX_ADD_DATE);
        String  contents = readContents(entry);
        if (contents == null) {
            return;
        }
        if ( !contents.startsWith("#")) {
            if (contents.length() > 500) {
                contents = contents.substring(0, 499);
            }
            System.err.println("Fetch: received error:" + contents);
        }
        SimpleDateFormat sdf  = new SimpleDateFormat(DATE_FORMAT);
        String           dttm = "," + sdf.format(new Date());
        BufferedWriter writer = new BufferedWriter(
                                    new FileWriter(
                                        entry.getResource().getTheFile(),
                                        true));
        for (String line : StringUtil.split(contents, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            if (addDate) {
                line = line + dttm;
            }
            writer.write(line);
            writer.write("\n");
        }
        writer.close();
        entry.setValue(IDX_LAST_UPDATE, now);
        System.err.println("\tfetch:" + entry.getName() + " updated");
        getEntryManager().updateEntry(getRepository().getTmpRequest(), entry);
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
    private String readContents(Entry entry) throws Exception {
        String url = (String) entry.getValue(IDX_SOURCE_URL);
        url = url.replace("points.json", "points.csv");
        url += "&fullheader=true";
        System.err.println("U:" + url);

        return IO.readContents(url);
    }

    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
	if(!isNew(newType)) {
            super.initializeNewEntry(request, entry, newType);
            return;
        }
        File tmpFile = getStorageManager().getTmpFile("csv");
        boolean          addDate  = (Boolean) entry.getValue(IDX_ADD_DATE);
        String           contents = readContents(entry);
        StringBuilder    sb       = new StringBuilder();
        SimpleDateFormat sdf      = new SimpleDateFormat(DATE_FORMAT);
        Date             now      = new Date();
        entry.setValue(IDX_LAST_UPDATE, now);
        String dttm = sdf.format(now);
        if (addDate) {
            for (String line : StringUtil.split(contents, "\n", true, true)) {
                if (line.startsWith("#")) {
                    if (line.startsWith("#fields=")) {
                        line = line + ",fetch_date[type=date format=\""
                               + DATE_FORMAT + "\"]";
                    }
                    sb.append(line);
                    sb.append("\n");

                    continue;
                } else {
                    sb.append(line + "," + dttm + "\n");
                }
            }
            contents = sb.toString();
        }
        getStorageManager().writeFile(tmpFile, contents);
        tmpFile = getStorageManager().moveToStorage(request, tmpFile);
        Resource resource = new Resource(tmpFile, Resource.TYPE_STOREDFILE);
        entry.setResource(resource);
        super.initializeNewEntry(request, entry, newType);

    }

}
