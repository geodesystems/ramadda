/*
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

package org.ramadda.data.point.icebridge;


import org.ramadda.data.point.PointFile;

import org.ramadda.data.record.*;

import java.io.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;



/** This is generated code from generate.tcl. Do not edit it! */
public class McordsIrmcr2File extends org.ramadda.data.point.text.TextFile {

    /**
     * _more_
     */
    public McordsIrmcr2File() {}

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @throws java.io.IOException _more_
     */
    public McordsIrmcr2File(String filename) throws java.io.IOException {
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
        return new McordsIrmcr2Record(this);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        PointFile.test(args, McordsIrmcr2File.class);
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    @Override
    public int getSkipLines(VisitInfo visitInfo) {
        return 1;
    }


    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    @Override
    public boolean isCapable(String action) {
        if (action.equals(ACTION_MAPINCHART)) {
            return true;
        }

        return super.isCapable(action);
    }


    //generated record class


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...    
     */
    public static class McordsIrmcr2Record extends org.ramadda.data.point
        .PointRecord {

        /** _more_          */
        public static final int ATTR_FIRST =
            org.ramadda.data.point.PointRecord.ATTR_LAST;

        /** _more_          */
        public static final List<RecordField> FIELDS =
            new ArrayList<RecordField>();

        /** _more_          */
        public static final int ATTR_LATITUDE = ATTR_FIRST + 1;

        /** _more_          */
        public static final RecordField RECORDATTR_LATITUDE;

        /** _more_          */
        public static final int ATTR_LONGITUDE = ATTR_FIRST + 2;

        /** _more_          */
        public static final RecordField RECORDATTR_LONGITUDE;

        /** _more_          */
        public static final int ATTR_TIME = ATTR_FIRST + 3;

        /** _more_          */
        public static final RecordField RECORDATTR_TIME;

        /** _more_          */
        public static final int ATTR_THICKNESS = ATTR_FIRST + 4;

        /** _more_          */
        public static final RecordField RECORDATTR_THICKNESS;

        /** _more_          */
        public static final int ATTR_ALTITUDE = ATTR_FIRST + 5;

        /** _more_          */
        public static final RecordField RECORDATTR_ALTITUDE;

        /** _more_          */
        public static final int ATTR_FRAME = ATTR_FIRST + 6;

        /** _more_          */
        public static final RecordField RECORDATTR_FRAME;

        /** _more_          */
        public static final int ATTR_BOTTOM = ATTR_FIRST + 7;

        /** _more_          */
        public static final RecordField RECORDATTR_BOTTOM;

        /** _more_          */
        public static final int ATTR_SURFACE = ATTR_FIRST + 8;

        /** _more_          */
        public static final RecordField RECORDATTR_SURFACE;

        /** _more_          */
        public static final int ATTR_QUALITY = ATTR_FIRST + 9;

        /** _more_          */
        public static final RecordField RECORDATTR_QUALITY;

        /** _more_          */
        public static final int ATTR_LAST = ATTR_FIRST + 10;


        static {
            FIELDS.add(RECORDATTR_LATITUDE = new RecordField("latitude",
                    "latitude", "", ATTR_LATITUDE, "", "double", "double", 0,
                    SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_LATITUDE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).latitude;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).latitude;
                }
            });
            FIELDS.add(RECORDATTR_LONGITUDE = new RecordField("longitude",
                    "longitude", "", ATTR_LONGITUDE, "", "double", "double",
                    0, SEARCHABLE_NO, CHARTABLE_NO));
            RECORDATTR_LONGITUDE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).longitude;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).longitude;
                }
            });
            FIELDS.add(RECORDATTR_TIME = new RecordField("time", "time", "",
                    ATTR_TIME, "", "double", "double", 0, SEARCHABLE_NO,
                    CHARTABLE_NO));
            RECORDATTR_TIME.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).time;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).time;
                }
            });
            FIELDS.add(RECORDATTR_THICKNESS = new RecordField("thickness",
                    "thickness", "", ATTR_THICKNESS, "", "double", "double",
                    0, SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_THICKNESS.setMissingValue(-9999.0);
            RECORDATTR_THICKNESS.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).thickness;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).thickness;
                }
            });
            FIELDS.add(RECORDATTR_ALTITUDE = new RecordField("altitude",
                    "altitude", "", ATTR_ALTITUDE, "", "double", "double", 0,
                    SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_ALTITUDE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).altitude;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).altitude;
                }
            });
            FIELDS.add(RECORDATTR_FRAME = new RecordField("frame", "frame",
                    "", ATTR_FRAME, "", "int", "int", 0, SEARCHABLE_NO,
                    CHARTABLE_NO));
            RECORDATTR_FRAME.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).frame;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).frame;
                }
            });
            FIELDS.add(RECORDATTR_BOTTOM = new RecordField("bottom",
                    "bottom", "", ATTR_BOTTOM, "m", "double", "double", 0,
                    SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_BOTTOM.setMissingValue(-9999.0);
            RECORDATTR_BOTTOM.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).bottom;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).bottom;
                }
            });
            FIELDS.add(RECORDATTR_SURFACE = new RecordField("surface",
                    "surface", "", ATTR_SURFACE, "m", "double", "double", 0,
                    SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_SURFACE.setMissingValue(-9999.0);
            RECORDATTR_SURFACE.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).surface;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).surface;
                }
            });
            FIELDS.add(RECORDATTR_QUALITY = new RecordField("quality",
                    "quality", "", ATTR_QUALITY, "", "int", "int", 0,
                    SEARCHABLE_NO, CHARTABLE_YES));
            RECORDATTR_QUALITY.setValueGetter(new ValueGetter() {
                public double getValue(Record record, RecordField field,
                                       VisitInfo visitInfo) {
                    return (double) ((McordsIrmcr2Record) record).quality;
                }
                public String getStringValue(Record record,
                                             RecordField field,
                                             VisitInfo visitInfo) {
                    return "" + ((McordsIrmcr2Record) record).quality;
                }
            });

        }


        /** _more_          */
        double time;

        /** _more_          */
        double thickness;

        /** _more_          */
        int frame;

        /** _more_          */
        double bottom;

        /** _more_          */
        double surface;

        /** _more_          */
        int quality;


        /**
         * _more_
         *
         * @param that _more_
         */
        public McordsIrmcr2Record(McordsIrmcr2Record that) {
            super(that);
            this.latitude  = that.latitude;
            this.longitude = that.longitude;
            this.time      = that.time;
            this.thickness = that.thickness;
            this.altitude  = that.altitude;
            this.frame     = that.frame;
            this.bottom    = that.bottom;
            this.surface   = that.surface;
            this.quality   = that.quality;


        }



        /**
         * _more_
         *
         * @param file _more_
         */
        public McordsIrmcr2Record(RecordFile file) {
            super(file);
        }



        /**
         * _more_
         *
         * @param file _more_
         * @param bigEndian _more_
         */
        public McordsIrmcr2Record(RecordFile file, boolean bigEndian) {
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
            if ( !(object instanceof McordsIrmcr2Record)) {
                return false;
            }
            McordsIrmcr2Record that = (McordsIrmcr2Record) object;
            if (this.latitude != that.latitude) {
                System.err.println("bad latitude");

                return false;
            }
            if (this.longitude != that.longitude) {
                System.err.println("bad longitude");

                return false;
            }
            if (this.time != that.time) {
                System.err.println("bad time");

                return false;
            }
            if (this.thickness != that.thickness) {
                System.err.println("bad thickness");

                return false;
            }
            if (this.altitude != that.altitude) {
                System.err.println("bad altitude");

                return false;
            }
            if (this.frame != that.frame) {
                System.err.println("bad frame");

                return false;
            }
            if (this.bottom != that.bottom) {
                System.err.println("bad bottom");

                return false;
            }
            if (this.surface != that.surface) {
                System.err.println("bad surface");

                return false;
            }
            if (this.quality != that.quality) {
                System.err.println("bad quality");

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
            if (attrId == ATTR_LATITUDE) {
                return latitude;
            }
            if (attrId == ATTR_LONGITUDE) {
                return longitude;
            }
            if (attrId == ATTR_TIME) {
                return time;
            }
            if (attrId == ATTR_THICKNESS) {
                return thickness;
            }
            if (attrId == ATTR_ALTITUDE) {
                return altitude;
            }
            if (attrId == ATTR_FRAME) {
                return frame;
            }
            if (attrId == ATTR_BOTTOM) {
                return bottom;
            }
            if (attrId == ATTR_SURFACE) {
                return surface;
            }
            if (attrId == ATTR_QUALITY) {
                return quality;
            }

            return super.getValue(attrId);

        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getRecordSize() {
            return super.getRecordSize() + 64;
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
            String[] toks     = line.split(",");
            int      fieldCnt = 0;
            latitude  = (double) Double.parseDouble(toks[fieldCnt++]);
            longitude = (double) Double.parseDouble(toks[fieldCnt++]);
            time      = (double) Double.parseDouble(toks[fieldCnt++]);
            thickness = (double) Double.parseDouble(toks[fieldCnt++]);
            if (isMissingValue(RECORDATTR_THICKNESS, thickness)) {
                thickness = Double.NaN;
            }
            altitude = (double) Double.parseDouble(toks[fieldCnt++]);
            frame    = (int) Double.parseDouble(toks[fieldCnt++]);
            bottom   = (double) Double.parseDouble(toks[fieldCnt++]);
            if (isMissingValue(RECORDATTR_BOTTOM, bottom)) {
                bottom = Double.NaN;
            }
            surface = (double) Double.parseDouble(toks[fieldCnt++]);
            if (isMissingValue(RECORDATTR_SURFACE, surface)) {
                surface = Double.NaN;
            }
            quality = (int) Double.parseDouble(toks[fieldCnt++]);


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
            String      delimiter   = ",";
            PrintWriter printWriter = recordIO.getPrintWriter();
            printWriter.print(latitude);
            printWriter.print(delimiter);
            printWriter.print(longitude);
            printWriter.print(delimiter);
            printWriter.print(time);
            printWriter.print(delimiter);
            printWriter.print(thickness);
            printWriter.print(delimiter);
            printWriter.print(altitude);
            printWriter.print(delimiter);
            printWriter.print(frame);
            printWriter.print(delimiter);
            printWriter.print(bottom);
            printWriter.print(delimiter);
            printWriter.print(surface);
            printWriter.print(delimiter);
            printWriter.print(quality);
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
            pw.print(getStringValue(RECORDATTR_LATITUDE, latitude));
            myCnt++;
            pw.print(',');
            pw.print(getStringValue(RECORDATTR_LONGITUDE, longitude));
            myCnt++;
            pw.print(',');
            pw.print(getStringValue(RECORDATTR_TIME, time));
            myCnt++;
            pw.print(',');
            pw.print(getStringValue(RECORDATTR_THICKNESS, thickness));
            myCnt++;
            pw.print(',');
            pw.print(getStringValue(RECORDATTR_ALTITUDE, altitude));
            myCnt++;
            pw.print(',');
            pw.print(getStringValue(RECORDATTR_FRAME, frame));
            myCnt++;
            pw.print(',');
            pw.print(getStringValue(RECORDATTR_BOTTOM, bottom));
            myCnt++;
            pw.print(',');
            pw.print(getStringValue(RECORDATTR_SURFACE, surface));
            myCnt++;
            pw.print(',');
            pw.print(getStringValue(RECORDATTR_QUALITY, quality));
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
            RECORDATTR_LATITUDE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_LONGITUDE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_TIME.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_THICKNESS.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_ALTITUDE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_FRAME.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_BOTTOM.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_SURFACE.printCsvHeader(visitInfo, pw);
            myCnt++;
            pw.print(',');
            RECORDATTR_QUALITY.printCsvHeader(visitInfo, pw);
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
            buff.append(" latitude: " + latitude + " \n");
            buff.append(" longitude: " + longitude + " \n");
            buff.append(" time: " + time + " \n");
            buff.append(" thickness: " + thickness + " \n");
            buff.append(" altitude: " + altitude + " \n");
            buff.append(" frame: " + frame + " \n");
            buff.append(" bottom: " + bottom + " \n");
            buff.append(" surface: " + surface + " \n");
            buff.append(" quality: " + quality + " \n");

        }



        /**
         * _more_
         *
         * @return _more_
         */
        public double getTime() {
            return time;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setTime(double newValue) {
            time = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getThickness() {
            return thickness;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setThickness(double newValue) {
            thickness = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getFrame() {
            return frame;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setFrame(int newValue) {
            frame = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getBottom() {
            return bottom;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setBottom(double newValue) {
            bottom = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public double getSurface() {
            return surface;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setSurface(double newValue) {
            surface = newValue;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getQuality() {
            return quality;
        }


        /**
         * _more_
         *
         * @param newValue _more_
         */
        public void setQuality(int newValue) {
            quality = newValue;
        }



    }

}
