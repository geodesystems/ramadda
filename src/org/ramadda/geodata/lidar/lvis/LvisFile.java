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

package org.ramadda.geodata.lidar.lvis;


import org.ramadda.data.point.PointFile;


import org.ramadda.util.IO;
import org.ramadda.data.record.*;
import org.ramadda.data.record.filter.*;


import org.ramadda.geodata.lidar.*;

import java.io.*;


/**
 * Class description
 *
 *
 */
public class LvisFile extends LidarFile {

    enum FileType {
        LCE, LGE, LGW, TEXT
    }

    /** _more_ */
    FileType fileType;


    /** _more_ */
    static final double VERSION_1_2 = 1.2;

    /** _more_ */
    static final double VERSION_1_3 = 1.3;

    /** _more_ */
    static final double VERSION_1_4 = 1.4;


    /** _more_ */
    double version = VERSION_1_2;


    /** _more_ */
    boolean running = false;


    /**
     * _more_
     */
    public LvisFile() {}


    /**
     * ctor
     *
     *
     * @param filename lvis data file
     *
     * @throws Exception On badness
     *
     * @throws IOException _more_
     */
    public LvisFile(IO.Path path) throws IOException {
        super(path);
        findFileType(path.getPath());
        findVersion(path.getPath());
    }


    /**
     * _more_
     *
     * @param action _more_
     *
     * @return _more_
     */
    public boolean isCapable(String action) {
        if (action.equals(ACTION_TIME)) {
            return true;
        }

        return super.isCapable(action);
    }


    /**
     * _more_
     *
     * @param that _more_
     *
     * @return _more_
     */
    public boolean sameDataType(LidarFile that) {
        if ( !super.sameDataType(that)) {
            return false;
        }
        LvisFile lvisFile = (LvisFile) that;

        //        System.err.println (this.fileType + " " +  lvisFile.fileType +" " +  this.version + " " +  lvisFile.version); 
        return (this.fileType == lvisFile.fileType)
               && (this.version == lvisFile.version);
    }



    /**
     * _more_
     *
     * @param filename _more_
     */
    @Override
    public void setPath(IO.Path path) {
        super.setPath(path);
        try {
            findFileType(path.getPath());
            findVersion(path.getPath());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isTrajectory() {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHtmlDescription() {
        StringBuffer sb = new StringBuffer("LVIS File<br>");
        if (version == VERSION_1_2) {
            sb.append("Version 1.2<br>");
        } else if (version == VERSION_1_3) {
            sb.append("Version 1.2<br>");
        } else {
            sb.append("Version 1.4<br>");
        }
        if (fileType.equals(FileType.LGW)) {
            sb.append("File type: LGW<br>");
        } else if (fileType.equals(FileType.LGE)) {
            sb.append("File type: LGE<br>");
        } else if (fileType.equals(FileType.LCE)) {
            sb.append("File type: LCE<br>");
        } else if (fileType.equals(FileType.TEXT)) {
            sb.append("File type: Text<br>");
        }

        return sb.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getWaveformNames() {
        if (version == VERSION_1_2) {
            return new String[] { LvisRecord.WAVEFORM_RETURN };
        } else {
            return new String[] { LvisRecord.WAVEFORM_RETURN,
                                  LvisRecord.WAVEFORM_TRANSMIT };
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean hasWaveform() {
        return fileType.equals(FileType.LGW);
    }

    /** _more_ */
    private static final String[] SUFFIXES = {
        ".lce", ".lce2", ".lce3", "lce.1.03", ".lgw", ".lgw2", ".lgw3",
        ".lgw4", ".lge", ".lge2", ".lge3", ".lge4"
    };

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public boolean canLoad(String file) {
        if (file.endsWith(".txt") && (file.indexOf("lvis") >= 0)
                && ((file.indexOf("level2") >= 0)
                    || (file.indexOf("quicklook") >= 0))) {
            return true;
        }

        return super.canLoad(file.toLowerCase(), SUFFIXES, true);
    }


    /**
     * figure out the filetype based on the filename. If
     * this cannot figure it out then throw IllegalArgumentException
     *
     * @param filename filename
     */
    private void findFileType(String filename) {
        if (filename.toLowerCase().indexOf(".lce") >= 0) {
            fileType = FileType.LCE;
        } else if (filename.toLowerCase().indexOf(".lge") >= 0) {
            fileType = FileType.LGE;
        } else if (filename.toLowerCase().indexOf(".lgw") >= 0) {
            fileType = FileType.LGW;
        } else if (filename.toLowerCase().endsWith(".txt")) {
            fileType = FileType.TEXT;
        } else {
            throw new IllegalArgumentException("Unknown file type:"
                    + filename);
        }
    }


    /**
     * Find the file version through experiment. Read a bunch of the records
     * and check that the lat/lons are valid
     *
     * @param filename data filename
     *
     * @throws IOException On badness
     */
    private void findVersion(String filename) {
        try {
            File file = new File(filename);
            filename = filename.toLowerCase();
            if (filename.endsWith(".txt")) {
                return;
            }
            if (filename.endsWith("2")) {
                version = VERSION_1_2;

                return;
            }
            if (filename.endsWith("3")) {
                version = VERSION_1_3;

                return;
            }
            if (filename.endsWith("4")) {
                version = VERSION_1_4;

                return;
            }

            //Find the version
            double[] versions     = { VERSION_1_3, VERSION_1_2 };
            boolean  foundVersion = false;
            long     size         = file.length();
            for (int versionIdx = 0; versionIdx < versions.length;
                    versionIdx++) {
                version = versions[versionIdx];
                RecordIO    recordIO = doMakeInputIO(new VisitInfo(),false);
                LidarRecord record =
                    (LidarRecord) makeRecord(new VisitInfo());
                boolean recordOk   = true;
                boolean anyNonZero = false;
                for (int i = 0;
                        (i < 1000) && (i < size / record.getRecordSize());
                        i++) {
                    record.read(recordIO);
                    if ( !record.isValidPosition()) {
                        recordOk = false;

                        break;
                    }
                    if ((record.getLatitude() != 0)
                            && (record.getLatitude()
                                == record.getLatitude())) {
                        anyNonZero = true;
                    } else if ((record.getLongitude() != 0)
                               && (record.getLongitude()
                                   == record.getLongitude())) {
                        anyNonZero = true;
                    }
                }
                if ( !anyNonZero) {
                    recordOk = false;
                }
                if (recordOk) {
                    setNumRecords(new File(getFilename()).length()
                                  / record.getRecordSize());
                }
                recordIO.close();
                if (recordOk) {
                    foundVersion = true;

                    break;
                }
            }

            if ( !foundVersion) {
                throw new IllegalArgumentException(
                    "Could not find version for:" + filename);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        if (fileType == FileType.TEXT) {
            //Most of the text have one header line but some have 2 so we'll use 2
            int skipCnt = 2;
            for (int i = 0; i < skipCnt; i++) {
                String line = visitInfo.getRecordIO().readLine();
            }
        }

        return visitInfo;
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     * @param howMany _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public boolean skip(VisitInfo visitInfo, BaseRecord record, int howMany)
            throws Exception {
        if ( !fileType.equals(FileType.TEXT)) {
            return super.skip(visitInfo, record, howMany);
        }
        BufferedReader in = visitInfo.getRecordIO().getBufferedReader();
        for (int i = 0; i < howMany; i++) {
            String line = in.readLine();
            if (line == null) {
                return false;
            }
        }

        return true;
    }





    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @return _more_
     */
    @Override
    public BaseRecord doMakeRecord(VisitInfo visitInfo) {

        if (fileType == FileType.TEXT) {
            return new LvisTextRecord(this);
        }

        if (fileType == FileType.LCE) {
            if (version == VERSION_1_2) {
                return new LceRecordV1_2(this);
            } else if (version == VERSION_1_3) {
                return new LceRecordV1_3(this);
            } else {
                throw new IllegalStateException("Unknown version:" + version);
            }
        }

        if (fileType == FileType.LGE) {
            if (version == VERSION_1_2) {
                return new LgeRecordV1_2(this);
            } else if (version == VERSION_1_3) {
                return new LgeRecordV1_3(this);
            } else {
                throw new IllegalStateException("Unknown version:" + version);
            }
        }
        if (fileType == FileType.LGW) {
            if (version == VERSION_1_2) {
                return new LgwRecordV1_2(this);
            } else if (version == VERSION_1_3) {
                return new LgwRecordV1_3(this);
            } else if (version == VERSION_1_4) {
                return new LgwRecordV1_4(this);
            } else {
                throw new IllegalStateException("Unknown version:" + version);
            }
        }

        throw new IllegalStateException("Unknown file type:" + fileType);

    }





    /**
     * _more_
     *
     * @param args _more_
     */
    public static void mainx(String[] args) {
        int skip = 0;
        for (int argIdx = 0; argIdx < args.length; argIdx++) {
            String arg = args[argIdx];
            if (arg.equals("-skip")) {
                argIdx++;
                skip = Integer.parseInt(args[argIdx]);

                continue;
            }
            try {
                long     t1       = System.currentTimeMillis();
                LvisFile lvisFile = new LvisFile(new IO.Path(arg));
                long numRecords =
                    new File(arg).length()
                    / lvisFile.makeRecord(new VisitInfo()).getRecordSize();
                final int[]   cnt      = { 0 };
                RecordVisitor metadata = new RecordVisitor() {
                    public boolean visitRecord(RecordFile file,
                            VisitInfo visitInfo, BaseRecord record) {
                        cnt[0]++;
                        if ((cnt[0] % 10000) == 0) {
                            System.err.print(".");
                        }

                        return true;
                    }
                };
                System.err.println("visiting");
                lvisFile.visit(metadata);
                long t2 = System.currentTimeMillis();
                System.err.println("time:" + (t2 - t1) / 1000.0
                                   + " # record:" + cnt[0]);
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
                exc.printStackTrace();
            }
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        PointFile.test(args, LvisFile.class);
    }


}
