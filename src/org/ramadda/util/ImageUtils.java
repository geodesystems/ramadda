/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import com.drew.imaging.jpeg.*;
import com.drew.lang.*;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.imaging.ImageMetadataReader;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import java.util.HashSet;
import java.awt.image.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;


/**
 * A collection of utilities
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class ImageUtils extends ucar.unidata.ui.ImageUtils {


    /**
     * _more_
     *
     * @param image _more_
     * @param top _more_
     * @param bottom _more_
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static BufferedImage crop(BufferedImage image, int top, int left,
                                     int bottom, int right) {
        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        int w           = imageWidth - right - left;
        int h           = imageHeight - top - bottom;

        //        System.err.println("iw:" + imageWidth +" w:"  + w + " " + left +" " + right);
        //        System.err.println("ih:" + imageHeight +" h:"  + h + " " + top +" " + bottom);
        return image.getSubimage(left, top, w, h);
    }



    public static BufferedImage xresizeImage(File inputFile, int targetWidth, int targetHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);
        Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage bufferedResizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        bufferedResizedImage.getGraphics().drawImage(resizedImage, 0, 0, null);
        return bufferedResizedImage;
    }


    public static BufferedImage resizeImage(File inputFile, int targetWidth,int targetHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);

	if(targetHeight<0) {
	    // Calculate the aspect ratio
	    double aspectRatio = (double) originalImage.getHeight() / (double) originalImage.getWidth();
	    // Calculate the new height based on the target width and aspect ratio
	    targetHeight = (int) (targetWidth * aspectRatio);
	}

        Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage bufferedResizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        bufferedResizedImage.getGraphics().drawImage(resizedImage, 0, 0, null);
        return bufferedResizedImage;
    }



    public static String stripImageMetadata(String file) throws Exception {
	File imageFile = new File(file);
	Image image = ImageIO.read(imageFile);
	image = orientImage(file,image,null);
	File dest = imageFile;
	String ext  =IO.getFileExtension(file).toLowerCase().replace(".","");
	ImageIO.write(	toBufferedImage(image, BufferedImage.TYPE_INT_RGB), ext, dest);
	return dest.toString();
    }

    public static Image grayscaleImage(Image image) throws Exception {
	BufferedImage gimage = new BufferedImage(image.getWidth(null), image.getHeight(null),  
						 BufferedImage.TYPE_BYTE_GRAY);  
	Graphics g = gimage.getGraphics();  
	g.drawImage(image, 0, 0, null);  
	g.dispose(); 
	ImageUtils.waitOnImage(gimage);
	return gimage;
    }



    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public static Image readImage(String file) {
        if (file == null) {
            return null;
        }
        try {
            InputStream is = Utils.getInputStream(file, Utils.class);
            if (is != null) {
                //                byte[] bytes = IOUtil.readBytes(is);
                //                return ImageIO.read(new ByteArrayInputStream(bytes));
                return ImageIO.read(is);
            }
            System.err.println("Could not read image:" + file);
        } catch (Exception exc) {
            System.err.println(exc + " getting image:  " + file);

            return null;
        }

        return null;
    }




    public static boolean isJpeg(String path) {
        return path.toLowerCase().endsWith(".jpg")
	    || path.toLowerCase().endsWith(".jpeg");
    }

    public static Image orientImage(String path, Image image,com.drew.metadata.Metadata[]mtd) throws Exception {
	long t2= System.currentTimeMillis();
        if (image == null) {
            System.err.print("Image is null");
            return null;
        }

	com.drew.metadata.Metadata metadata = null;
	File file = new File(path);
	metadata = ImageMetadataReader.readMetadata(file);
	//            metadata = JpegMetadataReader.readMetadata(jpegFile);
	if(mtd!=null)
	    mtd[0] = metadata;

	if (metadata == null) {
	    System.err.println("ImageUtils.orientImage: unable to read metadata:" + path);
	    return image;
	}
	ExifIFD0Directory exifIFD0 =
	    metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	if (exifIFD0 != null &&  exifIFD0.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
	    int orientation =
		exifIFD0.getInt(ExifIFD0Directory.TAG_ORIENTATION);
	    int rotation = 0;
	    if (orientation == 6) {
		image = rotate90(toBufferedImage(image, BufferedImage.TYPE_INT_RGB), false);
	    } else if (orientation == 3) {
		//todo
		rotation = 180;
	    } else if (orientation == 8) {
		image = rotate90(toBufferedImage(image, BufferedImage.TYPE_INT_RGB), true);
            }
        }
	return image;
    }


    public static BufferedImage readBase64(String format, String base64Image) throws Exception {
	byte[] imageBytes = Utils.decodeBase64(base64Image);
	ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
	BufferedImage image = ImageIO.read(bis);
	bis.close();
	return image;
    }


    public static void main(String[]args) throws Exception {
	BufferedImage   image = ImageIO.read(new File(args[0]));
        int width = image.getWidth();
        int height = image.getHeight();
        int index = 0;
	HashSet seen = new HashSet();
	int cnt=0;
	System.out.print("{id:'test',colors:[");
	for(int x = 0; x < width; x++) {
	    if(cnt>=256) break;
	    //            for(int y = 0; y < height; y++) {
	    int y=0;
	    Color color = new Color(image.getRGB(x,(int)(height/2)));
	    String c = color.getRed() + ","+color.getGreen() + ","+color.getBlue();
	    if(!seen.contains(c)) {
		seen.add(c);
		if(cnt++>0) System.out.print(",");
		System.out.print("\"rgb(" + c+")\"");
	    }
	} 
	System.out.println("]},");
	if(true) return;




	for(String arg:args) {
	    //	    Image image = orientImage(arg,readImage(arg),null);
	    //            writeImageToFile(image,  new File("thumb_" + arg));
	}
    }


    public static ImageInfo averageRGB(BufferedImage image) throws Exception {
	// Get image dimensions
	int width = image.getWidth();
	int height = image.getHeight();
	// Variables to store total RGB values
	long totalRed = 0, totalGreen = 0, totalBlue = 0;
	// Loop through each pixel
	ImageInfo info = new ImageInfo();
	for (int y = 0; y < height; y++) {
	    for (int x = 0; x < width; x++) {
		int pixel = image.getRGB(x, y);
		// Extract RGB values
		int red = (pixel >> 16) & 0xFF;
		int green = (pixel >> 8) & 0xFF;
		int blue = pixel & 0xFF;
		info.count(red,green,blue);
		// Add to total
		totalRed += red;
		totalGreen += green;
		totalBlue += blue;
	    }
	}
	// Calculate the average
	int numPixels = width * height;
	int avgRed = (int)(totalRed / numPixels);
	int avgGreen = (int)(totalGreen / numPixels);
	int avgBlue = (int)(totalBlue / numPixels);
	info.init(width,height,avgRed,avgGreen,avgBlue);
	return info;
    }


    public static class ImageInfo {
	public int width;
	public int height;
	public int avgRed;
	public int avgGreen;
	public int avgBlue;
	public int []redCount;
	public int []greenCount;	
	public int []blueCount;
	public ImageInfo() {
	    int length = 256;
	    redCount = new int[length];
	    greenCount = new int[length];	    
	    blueCount = new int[length];
	    for(int i=0;i<redCount.length;i++) {
		redCount[i]=0;
		greenCount[i]=0;
		blueCount[i]=0;
	    }
	}


	public ImageInfo(int width, int height, int avgRed,int avgGreen, int avgBlue){
	    this();
	    init(width,height,avgRed,avgGreen,avgBlue);
	}
	public void init(int width, int height, int avgRed,int avgGreen, int avgBlue){
	    this.width = width;
	    this.height = height;
	    this.avgRed = avgRed;
	    this.avgGreen = avgGreen;
	    this.avgBlue = avgBlue;
	}

	public void count(int red,int green,int blue) {
	    redCount[red]++;
	    greenCount[green]++;	    
	    blueCount[blue]++;
	}

    }


}



