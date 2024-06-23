/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.metadata;


import com.drew.imaging.jpeg.*;
import com.drew.lang.*;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.imaging.ImageMetadataReader;

import org.ramadda.repository.*;


import org.ramadda.util.Utils;
import org.ramadda.util.IO;
import org.ramadda.util.ImageUtils;



import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.awt.Image;
import java.awt.image.*;
import javax.imageio.*;

import java.io.*;

import java.util.Date;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A class for handling JPEG Metadata
 *
 * @author RAMADDA Development Team
 */
@SuppressWarnings("unchecked")
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
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Metadata getThumbnail(Request request, Entry entry,com.drew.metadata.Metadata[]mtd)
            throws Exception {

        if ( !entry.getResource().isImage()) {
            return null;
        }

	long t1= System.currentTimeMillis();
        String path  = entry.getResource().getPath();
        Image  image = ImageUtils.readImage(path);
	long t2= System.currentTimeMillis();
        if (image == null) {
            System.err.print("JpegMetadataHandler: image is null:"
                             + entry.getResource());
            return null;
        }

	if(mtd==null) mtd = new com.drew.metadata.Metadata[]{null};
	image = ImageUtils.orientImage(path,  image,mtd);

	long t3= System.currentTimeMillis();
	int newWidth = 300;
        Image scaledImage = image.getScaledInstance(newWidth, -1, Image.SCALE_FAST);
 	long t4= System.currentTimeMillis();
        ImageUtils.waitOnImage(scaledImage);
        RenderedImage finalImage = ImageUtils.toBufferedImage(scaledImage,
							      BufferedImage.TYPE_INT_RGB);

	long t5= System.currentTimeMillis();
        String thumbFile = IO.stripExtension(entry.getName()) + "_thumb.";
	String format;
        if (path.toLowerCase().endsWith("gif")) {
	    format = "gif";
        } else {
	    format =  "jpg";
        }
	thumbFile += format;
        File f = getStorageManager().getTmpFile(thumbFile);
        ImageIO.write(finalImage,format, f);
        String fileName = getStorageManager().moveToEntryDir(entry,
                              f).getName();

	long t6= System.currentTimeMillis();
	//	Utils.printTimes("thumb",t1,t2,t3,t4,t5,t6);

        return new Metadata(getRepository().getGUID(), entry.getId(),
                            getMetadataManager().findType(ContentMetadataHandler.TYPE_THUMBNAIL),
			    false,  fileName, null, null, null, null);
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

	//	System.err.println("JpegMetadataHandler.getInitialMetadata shortForm:"  + shortForm +" isImage:" +entry.getResource().isImage());
        if ( !entry.getResource().isImage()) {
            return;
        }

        String path = entry.getResource().getPath();
	//check for really big images
	File file = new File(path);
	if(file.exists() && file.length()>50*1000*1000) {
	    getLogManager().logSpecial("JpegMedataHandler:skipping metadata extraction for large image:" +entry.getName());
	    return;
	}


	com.drew.metadata.Metadata []mtd ={null};
        try {
	    if(request.get(ATTR_MAKETHUMBNAILS,true)) {
		long t1= System.currentTimeMillis();
		Metadata thumbnailMetadata = getThumbnail(request, entry,mtd);
		long t2= System.currentTimeMillis();
		//		System.err.println("getThumbnail:" + (t2-t1));
		if (thumbnailMetadata != null) {
		    metadataList.add(thumbnailMetadata);
		}
	    }
        } catch (Exception exc) {
            getLogManager().logError("JpgeMetadataHandler", exc);
            return;
        }


        if (shortForm) {
            return;
        }



        if ( !(path.toLowerCase().endsWith(".jpg")
                || path.toLowerCase().endsWith(".jpeg"))) {
            return;
        }
        try {
	    long t1= System.currentTimeMillis();
            com.drew.metadata.Metadata metadata =mtd[0];
	    if(metadata==null) {
                metadata = JpegMetadataReader.readMetadata(new File(path));
	    }
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
			//Strip out bad utf8. true->remove 0x00 byte
			caption  = Utils.removeInvalidUtf8Bytes(caption,true);
			if(stringDefined(caption)) {
			    caption = "+note\n"+ caption.trim() +"\n-note\n";
			    entry.setDescription(caption);
			}
                        //This tells ramadda that something was added
                        extra.put("1", "");
                    }
                }

            }

            if (gpsDir != null) {
                if (gpsDir.containsTag(GpsDirectory.TAG_IMG_DIRECTION)) {
                    Metadata dirMetadata =
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(), getMetadataManager().findType(TYPE_CAMERA_DIRECTION),
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

	    long t2= System.currentTimeMillis();
	    //	    System.err.println("getTags:" + (t2-t1));	    
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
            Image image = ImageUtils.readImage(path);
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
