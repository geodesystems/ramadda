/**
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.projects.rdx;


import org.apache.commons.dbcp2.BasicDataSource;

import org.ramadda.plugins.phone.TwilioApiHandler;





import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * Handles the monitoring of the instrument status db and provides a html page of the status
 *
 * The file api.xml specifies the /path to method mapping
 */
public class RdxApiHandler extends RepositoryManager implements RequestHandler {

    /**
     *     ctor
     *
     *     @param repository the repository
     *
     *     @throws Exception on badness
     */
    public RdxApiHandler(Repository repository) throws Exception {
        super(repository);
        int delayToStart = 10;
        //Start running in a bit
        Misc.run(new Runnable() {
            public void run() {
                Misc.sleepSeconds(delayToStart);
                runCheckInstruments();
            }
        });
        Misc.run(new Runnable() {
            public void run() {
                Misc.sleepSeconds(delayToStart);
                runCheckNotifications();
            }
        });
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sun, May 24, '20
     * @author         Enter your name here...    
     */
    public static class Notifications {

        /** _more_          */
        public static final int MINUTES_EMAIL = 60;

        /** _more_          */
        public static final int MINUTES_TEXT = 60 * 11;


        /** _more_          */
        public static final String TYPE_EMAIL = "email";

        /** _more_          */
        public static final String TYPE_TEXT = "text";

        /** _more_          */
        public static final String TABLE = "rdx_notifications";

        /** _more_          */
        public static final String COLUMN_ENTRY_ID = "entry_id";

        /** _more_          */
        public static final String COLUMN_EVENT_TYPE = "event_type";

        /** _more_          */
        public static final String COLUMN_DATE = "date";
    }



    /**
     * Check the instrument status
     */
    public void runCheckInstruments() {
        int pause = getRepository().getProperty("rdx.check.interval", 60);
        //TODO: how many errors until we stop?
        while (true) {
            try {
                log("Checking instruments");
                checkInstruments();
            } catch (Exception exc) {
                log("Error:" + exc);
                exc.printStackTrace();
            }
            Misc.sleepSeconds(pause);
        }
    }


    /**
     * _more_
     */
    public void runCheckNotifications() {
        //Check every 15 minutes
        while (true) {
            Misc.sleepSeconds(60 * 15);
            try {
                checkNotifications();
            } catch (Exception exc) {
                log("Error:" + exc);
                exc.printStackTrace();
            }
        }
    }

    /**
     * _more_
     *
     * @param msg _more_
     */
    private void log(String msg) {
        System.err.println("RDX:" + msg);
    }


    /**
     * Get the db connection to the instrument status database
     *
     * @return db connection
     *
     * @throws Exception _more_
     */
    private Connection getConnection() throws Exception {
        return getDatabaseManager().getExternalConnection("rdx", "db");
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Instrument> readInstruments() throws Exception {
        Connection       connection  = getConnection();
        List<Instrument> instruments = new ArrayList<Instrument>();
        Statement stmt =
            SqlUtil.select(connection, "*",
                           Misc.newList(TableInstrumentStatus.TABLE),
                           Clause.and(new ArrayList<Clause>()), "", 100);
        SqlUtil.Iterator iter = new SqlUtil.Iterator(stmt);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            instruments.add(new Instrument(this, results));
        }
        stmt.close();
        connection.close();

        return instruments;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void checkNotifications() throws Exception {
        Request tmpRequest = getRepository().getTmpRequest();
        Statement stmt = getDatabaseManager().select("*",
                             Misc.newList(Notifications.TABLE), null);
        try {
            Date             now  = new Date();
            SqlUtil.Iterator iter = new SqlUtil.Iterator(stmt);
            ResultSet        results;
            while ((results = iter.getNext()) != null) {
                Date date = getDatabaseManager().getTimestamp(results,
                                Notifications.COLUMN_DATE, true);
                String type =
                    results.getString(Notifications.COLUMN_EVENT_TYPE);
                //Check the time
                long minutesDiff = (now.getTime() - date.getTime()) / 1000
                                   / 60;
                if (type.equals(Notifications.TYPE_EMAIL)) {
                    if (minutesDiff < Notifications.MINUTES_EMAIL) {
                        continue;
                    }
                } else {
                    if (minutesDiff < Notifications.MINUTES_TEXT) {
                        continue;
                    }
                }
                String entryId =
                    results.getString(Notifications.COLUMN_ENTRY_ID);

                Entry entry = getEntryManager().getEntry(tmpRequest, entryId);
                if (entry == null) {
                    log("checkNotifications: Could not find entry:"
                        + entryId);
                    continue;
                }

                String url = tmpRequest.getAbsoluteUrl(
						       tmpRequest.entryUrl(
									   getRepository().URL_ENTRY_SHOW, entry));
                String instrumentId =
                    (String) entry.getValue(
                        RdxInstrumentTypeHandler.IDX_INSTRUMENT_ID);
                String msg;
		boolean networkUp = (boolean) entry.getValue(RdxInstrumentTypeHandler.IDX_NETWORK_UP);

		//TODO: make status message 
                if (type.equals(Notifications.TYPE_EMAIL)) {
		    msg = "Network for station:" + instrumentId
			+ " is down\n" + url;
		} else {
		    msg = "Network for station:" + instrumentId
			+ " is down\n" + url;
		}
                try {
                    sendNotification(tmpRequest, entry, instrumentId, type, msg);
                } catch (Exception exc) {
                    System.err.println(
                        "RdxApiHandler: Error sending notification:" + exc);
                    exc.printStackTrace();
                }
            }
        } finally {
            SqlUtil.close(stmt);
        }
    }


    public Result processTest(Request request) throws Exception {

	getDatabaseManager().update(TableInstrumentStatus.TABLE,  TableInstrumentStatus.COLUMN_INSTRUMENT_ID, request.getString("instrument_id", "radiometer1"),
				  new String[]{TableInstrumentStatus.COLUMN_NETWORK_IS_UP},
				  new Object[]{new Integer(0)});

	return processStatus(request);
    }


    /**
     * Handle the /rdx/status request
     *
     * @param request The request
     *
     * @return The result
     *
     * @throws Exception _more_
     */
    public Result processStatus(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtils.sectionOpen(null, false));
        HtmlUtils.sectionTitle(sb, "Instrument Status");
        sb.append(HtmlUtils.formTable());
        List<Instrument> instruments = readInstruments();
	String id = request.getString("instrument_id");
        if (instruments.size() > 0) {
            sb.append(HtmlUtils.row(HtmlUtils.headerCols(new Object[] {
                "Instrument ID",
                "Network Up", "Data Down", "Last Network Connection",
                "Last Data Time" })));
        }
        for (Instrument instrument : instruments) {
	    String label = HtmlUtils.href(getRepository().getUrlBase()+"/rdx/test?instrument_id="+instrument.id, instrument.id);

            sb.append(HtmlUtils.row(HtmlUtils.cols(new Object[] {
			    label, instrument.networkIsUp
                               ? "true"
                               : "false", instrument.dataDown,
                                          instrument.lastNetworkConnection,
                instrument.lastDataTime })));
        }
        sb.append(HtmlUtils.formTableClose());
        if (instruments.size() == 0) {
            sb.append("No instruments found");
            addExampleInstruments();
        }
        sb.append(HtmlUtils.sectionClose());

        return new Result("", sb);
    }

    /**
     * Check the instruments
     *
     * @throws Exception on badness
     */
    private void checkInstruments() throws Exception {
        List<Instrument> instruments = readInstruments();
        for (Instrument instrument : instruments) {
            checkInstrument(instrument);
        }
    }



    /**
     * _more_
     *
     * @param instrument _more_
     *
     * @throws Exception _more_
     */
    private void checkInstrument(Instrument instrument) throws Exception {
        //Find the station entries
        Request tmpRequest = getRepository().getTmpRequest();
        tmpRequest.put("type", "rdx_instrument");
	String id = instrument.id;
        tmpRequest.put("search.rdx_instrument.instrument_id", id);
        List[]      result  = getEntryManager().getEntries(tmpRequest);
        List<Entry> entries = new ArrayList<Entry>();
        entries.addAll((List<Entry>) result[0]);
        entries.addAll((List<Entry>) result[1]);
        if (entries.size() == 0) {
            log("checkInstrument: Could not find instrument: " +instrument.id);
            return;
        }

        Entry   entry        = entries.get(0);
        boolean instrumentOk = true;
        boolean changed      = false;
	//TODO: determine when the instrument is bad
	System.err.println("entry:" + entry);

	System.err.println("network:"+ entry.getValue(RdxInstrumentTypeHandler.IDX_NETWORK_UP));

        if ((boolean) entry.getValue(RdxInstrumentTypeHandler.IDX_NETWORK_UP)
                != instrument.networkIsUp) {
            changed = true;
            entry.setValue(RdxInstrumentTypeHandler.IDX_NETWORK_UP,
                           instrument.networkIsUp);
            if ( !instrument.networkIsUp) {
                instrumentOk = false;
            }
        }

        if ((int) entry.getValue(RdxInstrumentTypeHandler.IDX_DATA_DOWN)
                != instrument.dataDown) {
            changed = true;
            entry.setValue(RdxInstrumentTypeHandler.IDX_DATA_DOWN,
                           instrument.dataDown);
        }

        if ( !changed) {
            return;
        }


        System.err.println("changed:" + entry);
        getEntryManager().updateEntry(tmpRequest, entry);
        if (instrumentOk) {
            deleteNotification(entry);

            return;
        }

        String insert = SqlUtil.makeInsert(Notifications.TABLE,
                                           new String[] {
                                               Notifications.COLUMN_ENTRY_ID,
                Notifications.COLUMN_EVENT_TYPE, Notifications.COLUMN_DATE });
        getDatabaseManager().executeInsert(insert,
                                           new Object[] { entry.getId(),
                Notifications.TYPE_EMAIL, new Date() });



    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void deleteNotification(Entry entry) throws Exception {
        getDatabaseManager().delete(Notifications.TABLE,
                                    Clause.eq(Notifications.COLUMN_ENTRY_ID,
                                        entry.getId()));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param instrumentId _more_
     * @param msg _more_
     *
     * @throws Exception _more_
     */
    private void sendNotification(Request request, Entry entry,
                                  String instrumentId, String type, String msg)
            throws Exception {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        boolean weekend = (cal.get(cal.DAY_OF_WEEK) == cal.SUNDAY)
                          || (cal.get(cal.DAY_OF_WEEK) == cal.SATURDAY);

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                "rdx_notification", true);
        if ((metadataList == null) || (metadataList.size() == 0)) {
            System.err.println("RdxApiHandler: no notifications found");

            return;
        }

        for (Metadata metadata : metadataList) {
            String when = metadata.getAttr(5);
            if (when.equals("weekend") && !weekend) {
                continue;
            }
            boolean enabled = Misc.equals(metadata.getAttr1(), "true");
            if ( !enabled) {
                continue;
            }
            String name  = metadata.getAttr2();
            String email = Utils.trim(metadata.getAttr3());
            String phone = Utils.trim(metadata.getAttr4());
            log("notification:" + name	+ " email:" + email + " phone:" + phone);
	    if(type.equals(Notifications.TYPE_EMAIL)) {
		if (email.length() > 0) {
		    if ( !getRepository().getMailManager().isEmailCapable()) {
			System.err.println(
					   "RdxApiHandler: Error: Email is not enabled");
			
			continue;
		    }
		    System.err.println(
				       "RdxApiHandler: Sending site status email:" + email);
		    getRepository().getMailManager().sendEmail(email,
							       "Instrument status:" + instrumentId, msg, true);
		}
	    } else {
		phone = phone.replaceAll("-", "").replaceAll(" ","");
		if (phone.length() > 0) {
		    TwilioApiHandler twilio =
			(TwilioApiHandler) getRepository().getApiManager()
                        .getApiHandler("twilio");
		    if ((twilio == null) || !twilio.sendingEnabled()) {
			log("Error: SMS is not enabled");
			continue;
		    }
		    log("Sending site status sms:" + phone);
		    twilio.sendTextMessage(null, phone, msg);
		}
	    }
	}
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void addExampleInstruments() throws Exception {
        String insert = SqlUtil.makeInsert(TableInstrumentStatus.TABLE,
                                           new String[] {
            TableInstrumentStatus.COLUMN_INSTRUMENT_ID,
            TableInstrumentStatus.COLUMN_TYPE,
            TableInstrumentStatus.COLUMN_LAST_NETWORK_CONNECTION,
            TableInstrumentStatus.COLUMN_LAST_DATA_TIME,
            TableInstrumentStatus.COLUMN_NETWORK_IS_UP,
            TableInstrumentStatus.COLUMN_DATA_DOWN
        });

        for (String line :
                StringUtil.split(
                    getRepository().getResource(
                        "/org/ramadda/projects/rdx/instruments.txt"), "\n",
                            true, true)) {
            List<String> toks = StringUtil.split(line, ",");
            getDatabaseManager().executeInsert(insert, new Object[] {
                toks.get(0), toks.get(1), Utils.parseDate(toks.get(2)),
                Utils.parseDate(toks.get(3)), new Integer(toks.get(4)),
                new Integer(toks.get(5))
            });
        }
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Sat, May 23, '20
     * @author         Enter your name here...
     */
    public static class TableInstrumentStatus {

        /** _more_ */
        public static final String TABLE = "rdx_test_instrument_status";

        /** _more_ */
        public static final String COLUMN_INSTRUMENT_ID = "instrument_id";

        /** _more_ */
        public static final String COLUMN_TYPE = "type";

        /** _more_ */
        public static final String COLUMN_LAST_NETWORK_CONNECTION =
            "last_network_connection";

        /** _more_ */
        public static final String COLUMN_LAST_DATA_TIME = "last_data_time";

        /** _more_ */
        public static final String COLUMN_NETWORK_IS_UP = "network_is_up";

        /** _more_ */
        public static final String COLUMN_DATA_DOWN = "data_down";
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, May 23, '20
     * @author         Enter your name here...
     */
    private static class Instrument {

        /** _more_ */
        String id;

        /** _more_ */
        int dataDown;

        /** _more_ */
        boolean networkIsUp;

        /** _more_ */
        Date lastNetworkConnection;

        /** _more_ */
        Date lastDataTime;

        /**
         * _more_
         *
         * @param api _more_
         * @param results _more_
         *
         * @throws Exception _more_
         */
        public Instrument(RdxApiHandler api, ResultSet results)
                throws Exception {
            id = results.getString(TableInstrumentStatus.COLUMN_INSTRUMENT_ID);
            networkIsUp = 1 == results.getInt(
                TableInstrumentStatus.COLUMN_NETWORK_IS_UP);
            dataDown = results.getInt(TableInstrumentStatus.COLUMN_DATA_DOWN);
            //TODO: read datetime differently
            lastNetworkConnection =
                api.getDatabaseManager().getTimestamp(results,
                    TableInstrumentStatus.COLUMN_LAST_NETWORK_CONNECTION,
                    true);

            lastDataTime = api.getDatabaseManager().getTimestamp(results,
                    TableInstrumentStatus.COLUMN_LAST_DATA_TIME, true);
        }



    }


}
