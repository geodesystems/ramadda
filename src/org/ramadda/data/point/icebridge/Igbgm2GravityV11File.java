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
public class Igbgm2GravityV11File extends org.ramadda.data.point.text
    .TextFile {

    /**
     * _more_
     */
    public Igbgm2GravityV11File() {}

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @throws java.io.IOException _more_
     */
    public Igbgm2GravityV11File(String filename) throws java.io.IOException {
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
        return new Igbgm2GravityV11Record(this);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, Igbgm2GravityV11File.class);
    }


    //generated record class


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Feb 28, '15
     * @author         Enter your name here...
     */
    public static class Igbgm2GravityV11Record extends org.ramadda.data.point
        .PointRecord {

        /** _more_ */
        public static final int ATTR_FIRST =
            org.ramadda.data.point.PointRecord.ATTR_LAST;

        /** _more_ */
        public static final List<RecordField> FIELDS =
            new ArrayList<RecordField>();

        /** _more_ */
        public static final int ATTR_YEAR = ATTR_FIRST + 1;

        /** _more_ */
        public static final RecordField RECORDATTR_YEAR;

        /** _more_ */
        public static final int ATTR_DAYOFYEAR = ATTR_FIRST + 2;

        /** _more_ */
        public static final RecordField RECORDATTR_DAYOFYEAR;

        /** _more_ */
        public static final int ATTR_SECONDOFDAY = ATTR_FIRST + 3;

        /** _more_ */
        public static final RecordField RECORDATTR_SECONDOFDAY;

        /** _more_ */
        public static final int ATTR_LONGITUDE = ATTR_FIRST + 4;

        /** _more_ */
        public static final RecordField RECORDATTR_LONGITUDE;

        /** _more_ */
        public static final int ATTR_LATITUDE = ATTR_FIRST + 5;

        /** _more_ */
        public static final RecordField RECORDATTR_LATITUDE;

        /** _more_ */
        public static final int ATTR_AIRCRAFTHEIGHT = ATTR_FIRST + 6;

        /** _more_ */
        public static final RecordField RECORDATTR_AIRCRAFTHEIGHT;

        /** _more_ */
        public static final int ATTR_FREEAIRGRAVITYDISTURBANCE = ATTR_FIRST
                                                                 + 7;

        /** _more_ */
        public static final RecordField RECORDATTR_FREEAIRGRAVITYDISTURBANCE;

        /** _more_ */
        public static final int ATTR_LAST = ATTR_FIRST + 8;


        static {
            FIELDS.add(RECORDATTR_YEAR = new RecordField("year", "year", "",
                    ATTR_YEAR, "", "int", "int", 0, SEARCHABLE_NO,
                    CHARTABLE_NO));
            RECORDATTR_YEAR.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((Igbgm2GravityV11Record) record).year;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((Igbgm2GravityV11Record) record).year;
                }
            });
            FIELDS.add(RECORDATTR_DAYOFYEAR = new RecordField("dayOfYear",
                    "dayOfYear", "", ATTR_DAYOFYEAR, "", "int", "int", 0,
                    SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_DAYOFYEAR.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((Igbgm2GravityV11Record) record)
                        .dayOfYear;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((Igbgm2GravityV11Record) record).dayOfYear;
                }
            });
            FIELDS.add(RECORDATTR_SECONDOFDAY =
                new RecordField("secondOfDay", "secondOfDay", "",
                                ATTR_SECONDOFDAY, "", "int", "int", 0,
                                SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_SECONDOFDAY.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((Igbgm2GravityV11Record) record)
                        .secondOfDay;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((Igbgm2GravityV11Record) record).secondOfDay;
                }
            });
            FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude",
                    "longitude", "", ATTR_LONGITUDE, "degrees", "double",
                    "double", 0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((Igbgm2GravityV11Record) record)
                        .longitude;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((Igbgm2GravityV11Record) record).longitude;
                }
            });
            FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude",
                    "latitude", "", ATTR_LATITUDE, "degrees", "double",
                    "double", 0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((Igbgm2GravityV11Record) record)
                        .latitude;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((Igbgm2GravityV11Record) record).latitude;
                }
            });
            FIELDS.add(RECORDATTR_AIRCRAFTHEIGHT =
                new RecordField("aircraftHeight", "aircraftHeight", "",
                                ATTR_AIRCRAFTHEIGHT, "m", "double", "double",
                                0, SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_AIRCRAFTHEIGHT.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((Igbgm2GravityV11Record) record)
                        .aircraftHeight;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((Igbgm2GravityV11Record) record)
                        .aircraftHeight;
                }
            });
            FIELDS.add(RECORDATTR_FREEAIRGRAVITYDISTURBANCE =
                new RecordField("freeAirGravityDisturbance",
                                "freeAirGravityDisturbance", "",
                                ATTR_FREEAIRGRAVITYDISTURBANCE, "mGal",
                                "double", "double", 0, SEARCHABLE_NO,
                                CHARTABLE_YES));
            RECORDATTR_FREEAIRGRAVITYDISTURBANCE.setValueGetter(
                new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((Igbgm2GravityV11Record) record)
                        .freeAirGravityDisturbance;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((Igbgm2GravityV11Record) record)
                        .freeAirGravityDisturbance;
                }
            });

        }


        /** _more_ */
        int year;

        /** _more_ */
        int dayOfYear;

        /** _more_ */
        int secondOfDay;

        /** _more_ */
        double longitude;

        /** _more_ */
        double latitude;

        /** _more_ */
        double aircraftHeight;

        /** _more_ */
        double freeAirGravityDisturbance;


        /**
         * _more_
         *
         * @param that _more_
         */
        public Igbgm2GravityV11Record(Igbgm2GravityV11Record that) {
            super(that);
            this.year                      = that.year;
            this.dayOfYear                 = that.dayOfYear;
            this.secondOfDay               = that.secondOfDay;
            this.longitude                 = that.longitude;
            this.latitude                  = that.latitude;
            this.aircraftHeight            = that.aircraftHeight;
            this.freeAirGravityDisturbance = that.freeAirGravityDisturbance;


        }



        /**
         * _more_
         *
         * @param file _more_
         */
        public Igbgm2GravityV11Record(RecordFile file) {
            super(file);
        }



        /**
         * _more_
         *
         * @param file _more_
         * @param bigEndian _more_
         */
        public Igbgm2GravityV11Record(RecordFile file, boolean bigEndian) {
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
            if ( !(object instanceof Igbgm2GravityV11Record)) {
                return false;
            }
            Igbgm2GravityV11Record that = (Igbgm2GravityV11Record) object;
            if (this.year != that.year) {
                System.err.println("bad year");

                return false;
            }
            if (this.dayOfYear != that.dayOfYear) {
                System.err.println("bad dayOfYear");

                return false;
            }
            if (this.secondOfDay != that.secondOfDay) {
                System.err.println("bad secondOfDay");

                return false;
            }
            if (this.longitude != that.longitude) {
                System.err.println("bad longitude");

                return false;
            }
            if (this.latitude != that.latitude) {
                System.err.println("bad latitude");

                return false;
            }
            if (this.aircraftHeight != that.aircraftHeight) {
                System.err.println("bad aircraftHeight");

                return false;
            }
            if (this.freeAirGravityDisturbance
                    != that.freeAirGravityDisturbance) {
                System.err.println("bad freeAirGravityDisturbance");

                return false;
            }

            return true;
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
            if (attrId == ATTR_YEAR) {
                return year;
            }
            if (attrId == ATTR_DAYOFYEAR) {
                return dayOfYear;
            }
            if (attrId == ATTR_SECONDOFDAY) {
                return secondOfDay;
            }
            if (attrId == ATTR_LONGITUDE) {
                return longitude;
            }
            if (attrId == ATTR_LATITUDE) {
                return latitude;
            }
            if (attrId == ATTR_AIRCRAFTHEIGHT) {
                return aircraftHeight;
            }
            if (attrId == ATTR_FREEAIRGRAVITYDISTURBANCE) {
                return freeAirGravityDisturbance;
            }

            return super.getValue(attrId);

        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getRecordSize() {
            return super.getRecordSize() + 44;
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
            year = (int) Double.parseDouble(toks[fieldCnt++]);
            dayOfYear = (int) Double.parseDouble(toks[fieldCnt++]);
            secondOfDay = (int) Double.parseDouble(toks[fieldCnt++]);
            longitude = (double) Double.parseDouble(toks[fieldCnt++]);
            latitude = (double) Double.parseDouble(toks[fieldCnt++]);
            aircraftHeight = (double) Double.parseDouble(toks[fieldCnt++]);
            freeAirGravityDisturbance =
                (double) Double.parseDouble(toks[fieldCnt++]);


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
            printWriter.print(year);
            printWriter.print(delimiter);
            printWriter.print(dayOfYear);
            printWriter.print(delimiter);
            printWriter.print(secondOfDay);
            printWriter.print(delimiter);
            printWriter.print(longitude);
            printWriter.print(delimiter);
            printWriter.print(latitude);
            printWriter.print(delimiter);
            printWriter.print(aircraftHeight);
            printWriter.print(delimiter);
            printWriter.print(freeAirGravityDisturbance);
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
            pw.print(year);
            myCnt++;
            pw.print(',');
            pw.print(dayOfYear);
            myCnt++;
            pw.print(',');
            pw.print(secondOfDay);
            myCnt++;
            pw.print(',');
            pw.print(longitude);
            myCnt++;
            pw.print(',');
            pw.print(latitude);
            myCnt++;
            pw.print(',');
            pw.print(aircraftHeight);
            myCnt++;
            pw.print(',');
            pw.print(freeAirGravityDisturbance);
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
            RECORDATTR_YEAR.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_DAYOFYEAR.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_SECONDOFDAY.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_LONGITUDE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_LATITUDE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_AIRCRAFTHEIGHT.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_FREEAIRGRAVITYDISTURBANCE.printCsvHeader(visitInfo,
                    pw);
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
            buff.append(" year: " + year + " \n");
            buff.append(" dayOfYear: " + dayOfYear + " \n");
            buff.append(" secondOfDay: " + secondOfDay + " \n");
            buff.append(" longitude: " + longitude + " \n");
            buff.append(" latitude: " + latitude + " \n");
            buff.append(" aircraftHeight: " + aircraftHeight + " \n");
            buff.append(" freeAirGravityDisturbance: "
                        + freeAirGravityDisturbance + " \n");

        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getYear() {
            return year;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setYear(int newValue) {
            year = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getDayOfYear() {
            return dayOfYear;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setDayOfYear(int newValue) {
            dayOfYear = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getSecondOfDay() {
            return secondOfDay;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setSecondOfDay(int newValue) {
            secondOfDay = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getLongitude() {
            return longitude;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setLongitude(double newValue) {
            longitude = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getLatitude() {
            return latitude;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setLatitude(double newValue) {
            latitude = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getAircraftHeight() {
            return aircraftHeight;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setAircraftHeight(double newValue) {
            aircraftHeight = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getFreeAirGravityDisturbance() {
            return freeAirGravityDisturbance;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setFreeAirGravityDisturbance(double newValue) {
            freeAirGravityDisturbance = newValue;
        }



    }

}
