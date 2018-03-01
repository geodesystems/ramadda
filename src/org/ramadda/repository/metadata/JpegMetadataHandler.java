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

package org.ramadda.repository.metadata;


import com.drew.imaging.jpeg.*;
import com.drew.lang.*;

import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.metadata.iptc.IptcDirectory;

import org.ramadda.repository.*;


import org.ramadda.util.Utils;

import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.awt.Image;
import java.awt.image.*;

import java.io.File;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * A class for handling JPEG Metadata
 *
 * @author RAMADDA Development Team
 */
public class JpegMetadataHandler extends MetadataHandler {

    /** Camera Direction type */
    public static final String TYPE_CAMERA_DIRECTION = "camera.direction";


    /**
     * Construct a new instance for the repository
     *
     * @param repository  the repository
     *
     * @throws Exception  problems
     */
    public JpegMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }


    /**
     * Get the initial metadata
     *
     * @param request  the request
     * @param entry  the entry
     * @param metadataList the metadata list
     * @param extra  extra stuff
     * @param shortForm  true for shortform
     */
    public void getInitialMetadata(Request request, Entry entry,
                                   List<Metadata> metadataList,
                                   Hashtable extra, boolean shortForm) {

        if (shortForm) {
            return;
        }

        if ( !entry.getResource().isImage()) {
            return;
        }

        String path = entry.getResource().getPath();
        try {
            Image image = Utils.readImage(path);
            if (image == null) {
                System.err.print("JpegMetadataHandler: image is null:"
                                 + entry.getResource());

                return;
            }
            //            Image newImage = ImageUtils.resize(image, 300, -1);
            Image newImage = image.getScaledInstance(300, -1,
                                 Image.SCALE_FAST);
            ImageUtils.waitOnImage(newImage);
            newImage = ImageUtils.toBufferedImage(newImage,
                    BufferedImage.TYPE_INT_RGB);

            String thumbFile = IOUtil.stripExtension(entry.getName())
                               + "_thumb.";
            if (path.toLowerCase().endsWith("gif")) {
                thumbFile += "gif";
            } else {
                thumbFile += "jpg";
            }


            File f = getStorageManager().getTmpFile(request, thumbFile);
            ImageUtils.writeImageToFile(newImage, f);


            String fileName = getStorageManager().copyToEntryDir(entry,
                                  f).getName();
            Metadata thumbnailMetadata =
                new Metadata(getRepository().getGUID(), entry.getId(),
                             ContentMetadataHandler.TYPE_THUMBNAIL, false,
                             fileName, null, null, null, null);

            metadataList.add(thumbnailMetadata);

        } catch (Exception exc) {
            getLogManager().logError("JpgeMetadataHandler", exc);

            return;
        }


        if ( !(path.toLowerCase().endsWith(".jpg")
                || path.toLowerCase().endsWith(".jpeg"))) {
            return;
        }
        try {
            File jpegFile = new File(path);
            com.drew.metadata.Metadata metadata =
                JpegMetadataReader.readMetadata(jpegFile);
            com.drew.metadata.Directory exifDir =
                metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            com.drew.metadata.Directory gpsDir =
                metadata.getFirstDirectoryOfType(GpsDirectory.class);
            com.drew.metadata.Directory iptcDir =
                metadata.getFirstDirectoryOfType(IptcDirectory.class);

            if (exifDir != null) {
                //This tells ramadda that something was added
                if (exifDir.containsTag(
                        ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                    Date dttm =
                        exifDir.getDate(
                            ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                    if (dttm != null) {
                        //                        System.err.println("JpegMetadataHandler: setting date:" + dttm);
                        entry.setStartDate(dttm.getTime());
                        entry.setEndDate(dttm.getTime());
                        extra.put("1", "");
                    }
                }
            } else {
                //                System.err.println("no exif");
            }

            if (iptcDir != null) {
                // Get caption and make it the description if the user didn't add one
                if (iptcDir.containsTag(IptcDirectory.TAG_CAPTION)) {
                    String caption =
                        iptcDir.getString(IptcDirectory.TAG_CAPTION);
                    if ((caption != null)
                            && entry.getDescription().isEmpty()) {
                        entry.setDescription(caption);
                        //This tells ramadda that something was added
                        extra.put("1", "");
                    }
                }

            }

            if (gpsDir != null) {
                if (gpsDir.containsTag(GpsDirectory.TAG_IMG_DIRECTION)) {
                    Metadata dirMetadata =
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(), TYPE_CAMERA_DIRECTION,
                                     DFLT_INHERITED,
                                     "" + getValue(gpsDir,
                                         GpsDirectory
                                             .TAG_IMG_DIRECTION), Metadata
                                                 .DFLT_ATTR, Metadata
                                                 .DFLT_ATTR, Metadata
                                                 .DFLT_ATTR, Metadata
                                                 .DFLT_EXTRA);

                    metadataList.add(dirMetadata);
                }

                if (gpsDir.containsTag(GpsDirectory.TAG_LATITUDE)) {
                    double latitude = getValue(gpsDir,
                                          GpsDirectory.TAG_LATITUDE);
                    double longitude = getValue(gpsDir,
                                           GpsDirectory.TAG_LONGITUDE);
                    String lonRef =
                        gpsDir.getString(GpsDirectory.TAG_LONGITUDE_REF);
                    String latRef =
                        gpsDir.getString(GpsDirectory.TAG_LATITUDE_REF);
                    if ((lonRef != null) && lonRef.equalsIgnoreCase("W")) {
                        longitude = -longitude;
                    }
                    if ((latRef != null) && latRef.equalsIgnoreCase("S")) {
                        latitude = -latitude;
                    }
                    double altitude =
                        (gpsDir.containsTag(GpsDirectory.TAG_ALTITUDE)
                         ? getValue(gpsDir, GpsDirectory.TAG_ALTITUDE)
                         : 0);
                    try {
                        int altRef =
                            gpsDir.getInt(GpsDirectory.TAG_ALTITUDE_REF);
                        if (altRef > 0) {
                            altitude = -altitude;
                        }
                    } catch (MetadataException mde) {
                        // means that the tag didn't exist
                        // with version 2.5.0 of metadata extractor could move to 
                        // getInteger which will return null instead of throw exception
                    }
                    entry.setLocation(latitude, longitude, altitude);
                }
            }

            //This tells ramadda that something was added
            extra.put("1", "");
        } catch (Throwable thr) {
            System.err.println("err:" + thr);
            thr.printStackTrace();
            getRepository().getLogManager().logError("Processing jpg:"
                    + path, thr);
        }

    }


    /**
     * Get the value of a tag as a double
     *
     * @param dir  the directory
     * @param tag  the tag
     *
     * @return  the double value
     *
     * @throws Exception  couldn't create the double
     */
    private double getValue(Directory dir, int tag) throws Exception {

        boolean debug = ((tag == GpsDirectory.TAG_LATITUDE)
                         || (tag == GpsDirectory.TAG_LONGITUDE));
        try {
            Rational[] comps = dir.getRationalArray(tag);
            if ((comps != null) && (comps.length == 3)) {
                int    deg    = comps[0].intValue();
                float  min    = comps[1].floatValue();
                float  sec    = comps[2].floatValue();
                double result = deg + min / 60 + sec / 3600;

                return result;
            }
        } catch (Exception exc) {
            //Ignore this
        }

        return dir.getDouble(tag);
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        //        String str = "40:00:40.200000000004366";


        int cnt = 0;
        for (String path : args) {
            Image image = Utils.readImage(path);
            System.err.println("before:" + image.getWidth(null) + " "
                               + image.getHeight(null));
            Image newImage = ImageUtils.resize(image, 100, -1);
            //The waitOnImage was blocking so just check the width and sleep for a few milliseconds
            ImageUtils.waitOnImage(newImage);
            System.err.println("width:" + newImage.getWidth(null));
            ImageUtils.writeImageToFile(newImage,
                                        new File("thumb_" + cnt + ".jpg"));
            cnt++;
        }

    }


}
