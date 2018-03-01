/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.data.point.icebridge;


import org.ramadda.data.point.PointFile;

import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;



/** This is generated code from generate.tcl. Do not edit it! */
public class AtmIceSSNFile extends org.ramadda.data.point.text.TextFile {

    /**
     * _more_
     */
    public AtmIceSSNFile() {}

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @throws java.io.IOException _more_
     */
    public AtmIceSSNFile(String filename) throws java.io.IOException {
        super(filename);
    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public Record doMakeRecord(VisitInfo visitInfo) {
        return new AtmIceSSNRecord(this);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, AtmIceSSNFile.class);
    }


    //generated record class


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Feb 28, '15
     * @author         Enter your name here...
     */
    public static class AtmIceSSNRecord extends org.ramadda.data.point
        .PointRecord {

        /** _more_ */
        public static final int ATTR_FIRST =
            org.ramadda.data.point.PointRecord.ATTR_LAST;

        /** _more_ */
        public static final List<RecordField> FIELDS =
            new ArrayList<RecordField>();

        /** _more_ */
        public static final int ATTR_SECONDS = ATTR_FIRST + 1;

        /** _more_ */
        public static final RecordField RECORDATTR_SECONDS;

        /** _more_ */
        public static final int ATTR_CENTERLATITUDE = ATTR_FIRST + 2;

        /** _more_ */
        public static final RecordField RECORDATTR_CENTERLATITUDE;

        /** _more_ */
        public static final int ATTR_CENTERLONGITUDE = ATTR_FIRST + 3;

        /** _more_ */
        public static final RecordField RECORDATTR_CENTERLONGITUDE;

        /** _more_ */
        public static final int ATTR_HEIGHT = ATTR_FIRST + 4;

        /** _more_ */
        public static final RecordField RECORDATTR_HEIGHT;

        /** _more_ */
        public static final int ATTR_SOUTHTONORTHSLOPE = ATTR_FIRST + 5;

        /** _more_ */
        public static final RecordField RECORDATTR_SOUTHTONORTHSLOPE;

        /** _more_ */
        public static final int ATTR_WESTTOEASTSLOPE = ATTR_FIRST + 6;

        /** _more_ */
        public static final RecordField RECORDATTR_WESTTOEASTSLOPE;

        /** _more_ */
        public static final int ATTR_RMSFIT = ATTR_FIRST + 7;

        /** _more_ */
        public static final RecordField RECORDATTR_RMSFIT;

        /** _more_ */
        public static final int ATTR_NUMBEROFPOINTSUSED = ATTR_FIRST + 8;

        /** _more_ */
        public static final RecordField RECORDATTR_NUMBEROFPOINTSUSED;

        /** _more_ */
        public static final int ATTR_NUMBEROFPOINTSEDITED = ATTR_FIRST + 9;

        /** _more_ */
        public static final RecordField RECORDATTR_NUMBEROFPOINTSEDITED;

        /** _more_ */
        public static final int ATTR_DISTANCEFROMTRAJECTORY = ATTR_FIRST + 10;

        /** _more_ */
        public static final RecordField RECORDATTR_DISTANCEFROMTRAJECTORY;

        /** _more_ */
        public static final int ATTR_TRACKIDENTIFIER = ATTR_FIRST + 11;

        /** _more_ */
        public static final RecordField RECORDATTR_TRACKIDENTIFIER;

        /** _more_ */
        public static final int ATTR_LAST = ATTR_FIRST + 12;


        static {
            FIELDS.add(RECORDATTR_SECONDS = new RecordField("seconds",
                    "seconds", "", ATTR_SECONDS, "", "double", "double", 0,
                    SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_SECONDS.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record).seconds;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).seconds;
                }
            });
            FIELDS.add(RECORDATTR_CENTERLATITUDE =
                new RecordField("centerLatitude", "centerLatitude", "",
                                ATTR_CENTERLATITUDE, "", "double", "double",
                                0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_CENTERLATITUDE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record).centerLatitude;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).centerLatitude;
                }
            });
            FIELDS.add(RECORDATTR_CENTERLONGITUDE =
                new RecordField("centerLongitude", "centerLongitude", "",
                                ATTR_CENTERLONGITUDE, "", "double", "double",
                                0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_CENTERLONGITUDE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record)
                        .centerLongitude;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).centerLongitude;
                }
            });
            FIELDS.add(RECORDATTR_HEIGHT = new RecordField("height",
                    "height", "", ATTR_HEIGHT, "", "double", "double", 0,
                    SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_HEIGHT.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record).height;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).height;
                }
            });
            FIELDS.add(RECORDATTR_SOUTHTONORTHSLOPE =
                new RecordField("southToNorthSlope", "southToNorthSlope", "",
                                ATTR_SOUTHTONORTHSLOPE, "degrees", "double",
                                "double", 0, SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_SOUTHTONORTHSLOPE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record)
                        .southToNorthSlope;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).southToNorthSlope;
                }
            });
            FIELDS.add(RECORDATTR_WESTTOEASTSLOPE =
                new RecordField("westToEastSlope", "westToEastSlope", "",
                                ATTR_WESTTOEASTSLOPE, "degrees", "double",
                                "double", 0, SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_WESTTOEASTSLOPE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record)
                        .westToEastSlope;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).westToEastSlope;
                }
            });
            FIELDS.add(RECORDATTR_RMSFIT = new RecordField("rmsFit",
                    "rmsFit", "", ATTR_RMSFIT, "", "double", "double", 0,
                    SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_RMSFIT.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record).rmsFit;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).rmsFit;
                }
            });
            FIELDS.add(RECORDATTR_NUMBEROFPOINTSUSED =
                new RecordField("numberOfPointsUsed", "numberOfPointsUsed",
                                "", ATTR_NUMBEROFPOINTSUSED, "", "int",
                                "int", 0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_NUMBEROFPOINTSUSED.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record)
                        .numberOfPointsUsed;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).numberOfPointsUsed;
                }
            });
            FIELDS.add(RECORDATTR_NUMBEROFPOINTSEDITED =
                new RecordField("numberOfPointsEdited",
                                "numberOfPointsEdited", "",
                                ATTR_NUMBEROFPOINTSEDITED, "", "int", "int",
                                0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_NUMBEROFPOINTSEDITED.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record)
                        .numberOfPointsEdited;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record)
                        .numberOfPointsEdited;
                }
            });
            FIELDS.add(RECORDATTR_DISTANCEFROMTRAJECTORY =
                new RecordField("distanceFromTrajectory",
                                "distanceFromTrajectory", "",
                                ATTR_DISTANCEFROMTRAJECTORY, "m", "double",
                                "double", 0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_DISTANCEFROMTRAJECTORY.setValueGetter(
                new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record)
                        .distanceFromTrajectory;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record)
                        .distanceFromTrajectory;
                }
            });
            FIELDS.add(RECORDATTR_TRACKIDENTIFIER =
                new RecordField("trackIdentifier", "trackIdentifier", "",
                                ATTR_TRACKIDENTIFIER, "", "int", "int", 0,
                                SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_TRACKIDENTIFIER.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((AtmIceSSNRecord) record)
                        .trackIdentifier;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((AtmIceSSNRecord) record).trackIdentifier;
                }
            });

        }


        /** _more_ */
        double seconds;

        /** _more_ */
        double centerLatitude;

        /** _more_ */
        double centerLongitude;

        /** _more_ */
        double height;

        /** _more_ */
        double southToNorthSlope;

        /** _more_ */
        double westToEastSlope;

        /** _more_ */
        double rmsFit;

        /** _more_ */
        int numberOfPointsUsed;

        /** _more_ */
        int numberOfPointsEdited;

        /** _more_ */
        double distanceFromTrajectory;

        /** _more_ */
        int trackIdentifier;


        /**
         * _more_
         *
         * @param that _more_
         */
        public AtmIceSSNRecord(AtmIceSSNRecord that) {
            super(that);
            this.seconds                = that.seconds;
            this.centerLatitude         = that.centerLatitude;
            this.centerLongitude        = that.centerLongitude;
            this.height                 = that.height;
            this.southToNorthSlope      = that.southToNorthSlope;
            this.westToEastSlope        = that.westToEastSlope;
            this.rmsFit                 = that.rmsFit;
            this.numberOfPointsUsed     = that.numberOfPointsUsed;
            this.numberOfPointsEdited   = that.numberOfPointsEdited;
            this.distanceFromTrajectory = that.distanceFromTrajectory;
            this.trackIdentifier        = that.trackIdentifier;


        }



        /**
         * _more_
         *
         * @param file _more_
         */
        public AtmIceSSNRecord(RecordFile file) {
            super(file);
        }



        /**
         * _more_
         *
         * @param file _more_
         * @param bigEndian _more_
         */
        public AtmIceSSNRecord(RecordFile file, boolean bigEndian) {
            super(file, bigEndian);
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getLastAttribute() {
            return ATTR_LAST;
        }



        /**
         * _more_
         *
         * @param object _more_
         *
         * @return _more_
         */
        public boolean equals(Object object) {
            if ( !super.equals(object)) {
                System.err.println("bad super");

                return false;
            }
            if ( !(object instanceof AtmIceSSNRecord)) {
                return false;
            }
            AtmIceSSNRecord that = (AtmIceSSNRecord) object;
            if (this.seconds != that.seconds) {
                System.err.println("bad seconds");

                return false;
            }
            if (this.centerLatitude != that.centerLatitude) {
                System.err.println("bad centerLatitude");

                return false;
            }
            if (this.centerLongitude != that.centerLongitude) {
                System.err.println("bad centerLongitude");

                return false;
            }
            if (this.height != that.height) {
                System.err.println("bad height");

                return false;
            }
            if (this.southToNorthSlope != that.southToNorthSlope) {
                System.err.println("bad southToNorthSlope");

                return false;
            }
            if (this.westToEastSlope != that.westToEastSlope) {
                System.err.println("bad westToEastSlope");

                return false;
            }
            if (this.rmsFit != that.rmsFit) {
                System.err.println("bad rmsFit");

                return false;
            }
            if (this.numberOfPointsUsed != that.numberOfPointsUsed) {
                System.err.println("bad numberOfPointsUsed");

                return false;
            }
            if (this.numberOfPointsEdited != that.numberOfPointsEdited) {
                System.err.println("bad numberOfPointsEdited");

                return false;
            }
            if (this.distanceFromTrajectory != that.distanceFromTrajectory) {
                System.err.println("bad distanceFromTrajectory");

                return false;
            }
            if (this.trackIdentifier != that.trackIdentifier) {
                System.err.println("bad trackIdentifier");

                return false;
            }

            return true;
        }




        //overwrite the getLatitude/getLongitude methods

        /**
         * _more_
         *
         * @return _more_
         */
        public double getLatitude() {
            return centerLatitude;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getLongitude() {
            return org.ramadda.util.GeoUtils.normalizeLongitude(
                centerLongitude);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getAltitude() {
            return height;
        }


        /**
         * _more_
         *
         * @param fields _more_
         */
        protected void addFields(List<RecordField> fields) {
            super.addFields(fields);
            fields.addAll(FIELDS);
        }



        /**
         * _more_
         *
         * @param attrId _more_
         *
         * @return _more_
         */
        public double getValue(int attrId) {
            if (attrId == ATTR_SECONDS) {
                return seconds;
            }
            if (attrId == ATTR_CENTERLATITUDE) {
                return centerLatitude;
            }
            if (attrId == ATTR_CENTERLONGITUDE) {
                return centerLongitude;
            }
            if (attrId == ATTR_HEIGHT) {
                return height;
            }
            if (attrId == ATTR_SOUTHTONORTHSLOPE) {
                return southToNorthSlope;
            }
            if (attrId == ATTR_WESTTOEASTSLOPE) {
                return westToEastSlope;
            }
            if (attrId == ATTR_RMSFIT) {
                return rmsFit;
            }
            if (attrId == ATTR_NUMBEROFPOINTSUSED) {
                return numberOfPointsUsed;
            }
            if (attrId == ATTR_NUMBEROFPOINTSEDITED) {
                return numberOfPointsEdited;
            }
            if (attrId == ATTR_DISTANCEFROMTRAJECTORY) {
                return distanceFromTrajectory;
            }
            if (attrId == ATTR_TRACKIDENTIFIER) {
                return trackIdentifier;
            }

            return super.getValue(attrId);

        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getRecordSize() {
            return super.getRecordSize() + 76;
        }



        /**
         * _more_
         *
         * @param recordIO _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public ReadStatus read(RecordIO recordIO) throws Exception {
            ReadStatus status = ReadStatus.OK;
            String     line   = recordIO.readLine();
            if (line == null) {
                return ReadStatus.EOF;
            }
            line = line.trim();
            if (line.length() == 0) {
                return status;
            }
            String[] toks     = line.split(" +");
            int      fieldCnt = 0;
            seconds = (double) Double.parseDouble(toks[fieldCnt++]);
            centerLatitude = (double) Double.parseDouble(toks[fieldCnt++]);
            centerLongitude = (double) Double.parseDouble(toks[fieldCnt++]);
            height = (double) Double.parseDouble(toks[fieldCnt++]);
            southToNorthSlope = (double) Double.parseDouble(toks[fieldCnt++]);
            westToEastSlope = (double) Double.parseDouble(toks[fieldCnt++]);
            rmsFit = (double) Double.parseDouble(toks[fieldCnt++]);
            numberOfPointsUsed = (int) Double.parseDouble(toks[fieldCnt++]);
            numberOfPointsEdited = (int) Double.parseDouble(toks[fieldCnt++]);
            distanceFromTrajectory =
                (double) Double.parseDouble(toks[fieldCnt++]);
            trackIdentifier = (int) Double.parseDouble(toks[fieldCnt++]);


            return status;
        }



        /**
         * _more_
         *
         * @param recordIO _more_
         *
         * @throws IOException _more_
         */
        public void write(RecordIO recordIO) throws IOException {
            String      delimiter   = " ";
            PrintWriter printWriter = recordIO.getPrintWriter();
            printWriter.print(seconds);
            printWriter.print(delimiter);
            printWriter.print(centerLatitude);
            printWriter.print(delimiter);
            printWriter.print(centerLongitude);
            printWriter.print(delimiter);
            printWriter.print(height);
            printWriter.print(delimiter);
            printWriter.print(southToNorthSlope);
            printWriter.print(delimiter);
            printWriter.print(westToEastSlope);
            printWriter.print(delimiter);
            printWriter.print(rmsFit);
            printWriter.print(delimiter);
            printWriter.print(numberOfPointsUsed);
            printWriter.print(delimiter);
            printWriter.print(numberOfPointsEdited);
            printWriter.print(delimiter);
            printWriter.print(distanceFromTrajectory);
            printWriter.print(delimiter);
            printWriter.print(trackIdentifier);
            printWriter.print("\n");

        }



        /**
         * _more_
         *
         * @param visitInfo _more_
         * @param pw _more_
         *
         * @return _more_
         */
        public int doPrintCsv(VisitInfo visitInfo, PrintWriter pw) {
            boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR,
                                        false);
            int superCnt = super.doPrintCsv(visitInfo, pw);
            int myCnt    = 0;
            if (superCnt > 0) {
                pw.print(',');
            }
            pw.print(seconds);
            myCnt++;
            pw.print(',');
            pw.print(centerLatitude);
            myCnt++;
            pw.print(',');
            pw.print(centerLongitude);
            myCnt++;
            pw.print(',');
            pw.print(height);
            myCnt++;
            pw.print(',');
            pw.print(southToNorthSlope);
            myCnt++;
            pw.print(',');
            pw.print(westToEastSlope);
            myCnt++;
            pw.print(',');
            pw.print(rmsFit);
            myCnt++;
            pw.print(',');
            pw.print(numberOfPointsUsed);
            myCnt++;
            pw.print(',');
            pw.print(numberOfPointsEdited);
            myCnt++;
            pw.print(',');
            pw.print(distanceFromTrajectory);
            myCnt++;
            pw.print(',');
            pw.print(trackIdentifier);
            myCnt++;

            return myCnt + superCnt;

        }



        /**
         * _more_
         *
         * @param visitInfo _more_
         * @param pw _more_
         *
         * @return _more_
         */
        public int doPrintCsvHeader(VisitInfo visitInfo, PrintWriter pw) {
            int superCnt = super.doPrintCsvHeader(visitInfo, pw);
            int myCnt    = 0;
            boolean includeVector = visitInfo.getProperty(PROP_INCLUDEVECTOR,
                                        false);
            if (superCnt > 0) {
                pw.print(',');
            }
            RECORDATTR_SECONDS.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_CENTERLATITUDE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_CENTERLONGITUDE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_HEIGHT.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_SOUTHTONORTHSLOPE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_WESTTOEASTSLOPE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_RMSFIT.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_NUMBEROFPOINTSUSED.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_NUMBEROFPOINTSEDITED.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_DISTANCEFROMTRAJECTORY.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_TRACKIDENTIFIER.printCsvHeader(visitInfo, pw);
            myCnt++;

            return myCnt + superCnt;

        }



        /**
         * _more_
         *
         * @param buff _more_
         *
         * @throws Exception _more_
         */
        public void print(Appendable buff) throws Exception {
            super.print(buff);
            buff.append(" seconds: " + seconds + " \n");
            buff.append(" centerLatitude: " + centerLatitude + " \n");
            buff.append(" centerLongitude: " + centerLongitude + " \n");
            buff.append(" height: " + height + " \n");
            buff.append(" southToNorthSlope: " + southToNorthSlope + " \n");
            buff.append(" westToEastSlope: " + westToEastSlope + " \n");
            buff.append(" rmsFit: " + rmsFit + " \n");
            buff.append(" numberOfPointsUsed: " + numberOfPointsUsed + " \n");
            buff.append(" numberOfPointsEdited: " + numberOfPointsEdited
                        + " \n");
            buff.append(" distanceFromTrajectory: " + distanceFromTrajectory
                        + " \n");
            buff.append(" trackIdentifier: " + trackIdentifier + " \n");

        }



        /**
         * _more_
         *
         * @return _more_
         */
        public double getSeconds() {
            return seconds;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setSeconds(double newValue) {
            seconds = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getCenterLatitude() {
            return centerLatitude;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setCenterLatitude(double newValue) {
            centerLatitude = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getCenterLongitude() {
            return centerLongitude;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setCenterLongitude(double newValue) {
            centerLongitude = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getHeight() {
            return height;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setHeight(double newValue) {
            height = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getSouthToNorthSlope() {
            return southToNorthSlope;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setSouthToNorthSlope(double newValue) {
            southToNorthSlope = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getWestToEastSlope() {
            return westToEastSlope;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setWestToEastSlope(double newValue) {
            westToEastSlope = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getRmsFit() {
            return rmsFit;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setRmsFit(double newValue) {
            rmsFit = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getNumberOfPointsUsed() {
            return numberOfPointsUsed;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setNumberOfPointsUsed(int newValue) {
            numberOfPointsUsed = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getNumberOfPointsEdited() {
            return numberOfPointsEdited;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setNumberOfPointsEdited(int newValue) {
            numberOfPointsEdited = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getDistanceFromTrajectory() {
            return distanceFromTrajectory;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setDistanceFromTrajectory(double newValue) {
            distanceFromTrajectory = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getTrackIdentifier() {
            return trackIdentifier;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setTrackIdentifier(int newValue) {
            trackIdentifier = newValue;
        }



    }

}
