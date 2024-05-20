/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.gps;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.User;
import org.ramadda.repository.type.*;
import org.ramadda.util.MailUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;


import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.io.File;

import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class OpusTypeHandler extends SolutionTypeHandler {

    /** _more_ */
    public static final String TYPE_OPUS = "gps_solution_opus";

    /** _more_ */
    public static final String PROP_OPUS_MAIL_URL = "gps.opus.mail.url";

    /** _more_ */
    public static final String PROP_OPUS_MAIL_USER = "gps.opus.mail.user";


    /** _more_ */
    private boolean monitoringOpus = false;

    /** _more_ */
    private String opusUser;

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public OpusTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        final String opusMailUrl =
            getRepository().getProperty(PROP_OPUS_MAIL_URL, (String) null);
        opusUser = getRepository().getProperty(PROP_OPUS_MAIL_USER,
                (String) null);
        if (opusMailUrl != null) {
            if (opusUser == null) {
                getLogManager().logInfoAndPrint(
                    "OPUS:  error: No user id defined by property: "
                    + PROP_OPUS_MAIL_USER);

                return;
            }
            //Start up in 10 seconds
            Misc.runInABit(10000, new Runnable() {
                public void run() {
                    //Make sure we have a user
                    try {
                        User user = getUserManager().findUser(opusUser);
                        if (user == null) {
                            getLogManager().logInfoAndPrint(
                                "OPUS: could not find user:" + opusUser);

                            return;
                        }
                    } catch (Exception exc) {
                        getLogManager().logError("OPUS: could not find user:"
                                + opusUser, exc);

                        return;
                    }
                    //                        getLogManager().logInfoAndPrint ("OPUS:  monitoring email "+ opusMailUrl);
                    getLogManager().logInfoAndPrint(
                        "OPUS: monitoring OPUS email");
                    monitorOpusEmail(opusMailUrl);
                }
            });
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        monitoringOpus = false;
    }


    /**
     * _more_
     *
     * @param opusMailUrl _more_
     */
    private void monitorOpusEmail(String opusMailUrl) {
        monitoringOpus = true;
        int  errorCnt = 0;
        long time     = System.currentTimeMillis();
        while (monitoringOpus) {
            if (errorCnt > 5) {
                getLogManager().logError(
                    "OPUS: Opus email monitoring failed", null);

                return;
            }
            try {
                //                System.err.println ("calling checkfor opusmail");
                //                System.err.println ("OPUS: checking mbox");
                if ( !checkForOpusEmail(opusMailUrl)) {
                    errorCnt++;
                } else {
                    errorCnt = 0;
                }
                //                System.err.println ("OPUS: done checking mbox");
                //Sleep for  5 minutes
                Misc.sleepSeconds(60 * 5);
            } catch (Exception exc) {
                errorCnt++;
                //                if(errorCnt>5) {
                exc.printStackTrace();
                getLogManager().logError(
                    "OPUS: Opus email monitoring failed", exc);

                return;
                //                }
            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception On badness
     */
    @Override
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);
        initializeOpusEntry(entry);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception On badness
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        initializeOpusEntry(entry);
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void initializeOpusEntry(Entry entry) throws Exception {
        String opus = new String(
                          IOUtil.readBytes(
                              getStorageManager().getFileInputStream(
                                  entry.getFile())));
        /*
      LAT:   40 6 46.56819      0.003(m)        40 6 46.58791      0.003(m)
    E LON:  253 35  8.56821      0.010(m)       253 35  8.52089      0.010(m)
    W LON:  106 24 51.43179      0.010(m)       106 24 51.47911      0.010(m)
   EL HGT:         2275.608(m)   0.009(m)              2274.768(m)   0.009(m)
Northing (Y) [meters]     4441227.340           391737.791
Easting (X)  [meters]      379359.228           836346.070

  X:     -1380903.608(m)   0.015(m)          -1380904.391(m)   0.015(m)
        Y:     -4687187.453(m)   0.012(m)          -4687186.144(m)   0.012(m)
        Z:      4089011.143(m)   0.010(m)           4089011.067(m)   0.010(m)
         */
        //        List<String> 
        String   eol    = "($|\\r?\\n)";

        Object[] values = entry.getTypeHandler().getEntryValues(entry);
        String coordPattern =
            "[^\\r\\n]+\\s+(-?[0-9\\.]+)\\s*\\(m\\)\\s*[0-9\\.]+\\s*\\(m\\)"
            + eol;


        //REF FRAME: NAD_83(2011)(EPOCH:2010.0000)              IGS08 (EPOCH:2012.4407)

        String[] refFramePatterns = { "REF\\s*FRAME:\\s[^\\r\\n]+\\)([^\\(]+\\([^\\)]+\\))\\s*"
                                      + eol,
                                      "REF\\s*FRAME:\\s([^\\r\\n]+)\\s*"
                                      + eol, };
        for (String refFramePattern : refFramePatterns) {
            String v = StringUtil.findPattern(opus, refFramePattern);
            if (v != null) {
                values[IDX_REFERENCE_FRAME] = v.trim();

                break;
            }
        }

        int[] indices = { IDX_UTM_X, IDX_UTM_Y, IDX_X, IDX_Y, IDX_Z };
        String[] patterns = { "Northing\\s*\\(Y\\)\\s*\\[meters\\]\\s*([-\\.\\d]+)\\s+",
                              "Easting\\s*\\(X\\)\\s*\\[meters\\]\\s*([-\\.\\d]+)\\s+",
                              "X:" + coordPattern, "Y:" + coordPattern,
                              "Z:" + coordPattern };
        for (int i = 0; i < patterns.length; i++) {
            String value = StringUtil.findPattern(opus, patterns[i]);
            if (value != null) {
                values[indices[i]] = Double.parseDouble(value);
            } else {
                System.err.println("no match: " + patterns[i]);
            }
        }
        String latLine = StringUtil.findPattern(opus, "LAT: *([^\n]+)\n");
        String lonLine = StringUtil.findPattern(opus,
                             "E\\s*LON: *([^\n]+)\n");
        String heightLine = StringUtil.findPattern(opus,
                                "HGT: *([^\\(]+)\\(");
        double altitude = 0.0;
        if (heightLine != null) {
            //            System.err.println ("hgt: " + heightLine);
            altitude = Double.parseDouble(heightLine.trim());
        }
        if ((latLine != null) && (lonLine != null)) {
            List<String> latToks = StringUtil.split(latLine.trim(), " ",
                                       true, true);
            List<String> lonToks = StringUtil.split(lonLine.trim(), " ",
                                       true, true);

            int indexOffset = 0;
            if (latToks.size() > 4) {
                indexOffset = 4;
            }

            String latString = latToks.get(indexOffset + 0) + ":"
                               + latToks.get(indexOffset + 1) + ":"
                               + latToks.get(indexOffset + 2);
            String lonString = lonToks.get(indexOffset + 0) + ":"
                               + lonToks.get(indexOffset + 1) + ":"
                               + lonToks.get(indexOffset + 2);

            //            System.err.println ("lat:" + latString);
            //            System.err.println ("lon:" + lonString);
            double lat = GeoUtils.decodeLatLon(latString);
            double lon =
                Misc.normalizeLongitude(GeoUtils.decodeLatLon(lonString));
            //            System.err.println ("lat: " + lat + " " + lon +" alt:" + altitude);
            entry.setLocation(lat, lon, altitude);
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        String eol = "($|\\r?\\n)";
        String opus =
            " REF FRAME: NAD_83(2011)(EPOCH:2010.0000)              IGS08 (EPOCH:2012.4407)\nsdfsfsd";
        opus = " REF FRAME: IGS08 (EPOCH:2012.4903)\nsdfsdfds";
        String[] refFramePatterns = { "REF\\s*FRAME:\\s[^\\r\\n]+\\)([^\\(]+\\([^\\)]+\\))\\s*"
                                      + eol,
                                      "REF\\s*FRAME:\\s([^\\r\\n]+)\\s*"
                                      + eol, };
        for (String refFramePattern : refFramePatterns) {
            String v = StringUtil.findPattern(opus, refFramePattern);
            if (v != null) {
                System.err.println(v.trim());

                break;
            }
        }
        if (true) {
            return;
        }



        for (String s :
                new String[] {
                    "         X:      1594210.831(m)   0.004(m)           1594209.866(m)   0.004(m)\nfoobar",
                    "         X:      1241435.191(m)   0.018(m)\nzzzzz" }) {
            String pattern =
                "X:[^\\r\\n]+\\s+([0-9\\.]+)\\s*\\(m\\)\\s*[0-9\\.]+\\s*\\(m\\)($|\\r?\\n)";
            System.err.println(StringUtil.findPattern(s, pattern));
        }
    }


    /**
     * _more_
     *
     * @param opusMailUrl _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean checkForOpusEmail(String opusMailUrl) throws Exception {
        Properties props = System.getProperties();
        try {
            Session session = Session.getDefaultInstance(props);
            URLName urlName = new URLName(opusMailUrl);
            Store   store   = session.getStore(urlName);
            if ( !store.isConnected()) {
                store.connect();
            }
            Folder folder = store.getFolder("Inbox");
            if ((folder == null) || !folder.exists()) {
                getLogManager().logError("OPUS: Invalid folder");

                return false;
            }

            folder.open(Folder.READ_WRITE);
            Message[] messages = folder.getMessages();
            for (int i = 0; i < messages.length; i++) {
                String subject = messages[i].getSubject();
                if (subject.indexOf("OPUS solution") < 0) {
                    System.err.println("OPUS: skipping: " + subject);

                    //                    messages[i].setFlag(Flags.Flag.DELETED, true);
                    continue;
                }
                System.err.println("OPUS: subject:" + subject);
                Object       content = messages[i].getContent();
                StringBuffer sb      = new StringBuffer();
                MailUtil.extractText(content, sb);
                GpsOutputHandler gpsOutputHandler =
                    (GpsOutputHandler) getRepository().getOutputHandler(
                        GpsOutputHandler.OUTPUT_GPS_TORINEX);
                //                System.err.println("deleting:" +  subject);
                //                messages[i].setFlag(Flags.Flag.DELETED, true);
                //                if(true) continue;

                StringBuffer msgBuff = new StringBuffer();
                Request tmpRequest   =
                    getRepository().getTmpRequest(opusUser);
                Entry newEntry = gpsOutputHandler.processAddOpus(tmpRequest,
                                     sb.toString(), msgBuff);
                if (newEntry == null) {
                    System.err.println(
                        "OPUS: Unable to process OPUS message:" + msgBuff);
                    getLogManager().logError(
                        "OPUS: Unable to process OPUS message:" + msgBuff);
                } else {
                    //                    monitoringOpus = false;
                    System.err.println("OPUS: added opus. deleting email: "
                                       + newEntry.getId());
                    messages[i].setFlag(Flags.Flag.DELETED, true);
                }
            }
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return true;
    }




}
