/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.astro;


import nom.tam.fits.*;
import nom.tam.util.*;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;


import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WmsUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;

import java.awt.Toolkit;
import java.awt.image.*;

import java.io.*;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 *
 *
 */
@SuppressWarnings({"unchecked","deprecation"})
public class FitsOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String ARG_FITS_HDU = "fits.hdu";

    /** _more_ */
    public static final String ARG_FITS_SUBSET = "fits.hdu";

    /** _more_ */
    public static final OutputType OUTPUT_VIEWER =
        new OutputType("FITS Viewer", "fits.viewer", OutputType.TYPE_VIEW,
                       "", "/astro/fits.gif");


    /** _more_ */
    public static final OutputType OUTPUT_INFO = new OutputType("FITS Info",
                                                     "fits.info",
                                                     OutputType.TYPE_VIEW,
                                                     "", "/astro/fits.gif");

    /** _more_ */
    public static final OutputType OUTPUT_IMAGE =
        new OutputType("FITS Image", "fits.image", OutputType.TYPE_VIEW, "",
                       "/fits/fits.gif");


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public FitsOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        Header.setLongStringsEnabled(true);
        addType(OUTPUT_INFO);
        addType(OUTPUT_VIEWER);
        addType(OUTPUT_IMAGE);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.entry != null) {
            if (state.entry.getTypeHandler().isType("fits_data")) {
                links.add(makeLink(request, state.entry, OUTPUT_INFO));
                links.add(makeLink(request, state.entry, OUTPUT_VIEWER));
                //                links.add(makeLink(request, state.entry, OUTPUT_IMAGE));
            }
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        if (outputType.equals(OUTPUT_VIEWER)) {
            return outputEntryViewer(request, entry);
        }
        if (outputType.equals(OUTPUT_IMAGE)) {
            return outputEntryImage(request, entry);
        }
        if (request.exists(ARG_FITS_SUBSET)) {
            return outputEntrySubset(request, entry);
        }

        return outputEntryInfo(request, entry);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntrySubset(Request request, Entry entry)
            throws Exception {
        Fits    fits = new Fits(entry.getFile());
        HashSet hdus = new HashSet();
        for (String hdu :
                (List<String>) request.get(ARG_FITS_HDU, new ArrayList())) {
            hdus.add(hdu);
        }
        OutputStream os = request.getHttpServletResponse().getOutputStream();

        String filename =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry));
        filename = IOUtil.stripExtension(filename);
        request.setReturnFilename(filename + "_subset.fits");
        Result result = new Result();
        result.setNeedToWrite(false);
        BufferedDataOutputStream bdos = new BufferedDataOutputStream(os);

        for (int headerIdx = 0; headerIdx < fits.getNumberOfHDUs(); headerIdx++) {
            if ( !hdus.contains("" + headerIdx)) {
                continue;
            }
            BasicHDU hdu = fits.getHDU(headerIdx);
            hdu.write(bdos);
        }
        bdos.close();
        os.close();

        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntryViewer(Request request, Entry entry)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        String getFileUrl = getEntryManager().getEntryResourceUrl(request,  entry);
        //TODO: set the path right
        sb.append(
            "<applet archive=\"/repository/fits/fits1.3.jar\" code=\"eap.fitsbrowser.BrowserApplet\" width=700 height=700 ><param name=\"FILE\" value=\""
            + getFileUrl
            + "\">Your browser is ignoring the applet tag</applet>");

        return new Result("", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntryImage(Request request, Entry entry)
            throws Exception {
        int  hduIndex  = request.get(ARG_FITS_HDU, -1);
        File imageFile = outputImage(request, entry.getFile(), hduIndex);
        if (imageFile == null) {
            return new Result("Error: no image found");
        }

        return new Result("",
                          getStorageManager().getFileInputStream(imageFile),
                          getRepository().getMimeTypeFromSuffix("png"));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param fitsFile _more_
     * @param hduIndex _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File outputImage(Request request, File fitsFile, int hduIndex)
            throws Exception {
        System.err.println("file:" + fitsFile + " " + fitsFile.exists());
        Fits     fits     = new Fits(fitsFile);
        ImageHDU imageHdu = null;
        if (hduIndex >= 0) {
            BasicHDU hdu = fits.getHDU(hduIndex);
            if ( !(hdu instanceof ImageHDU)) {
                throw new IllegalArgumentException("Bad HDU:" + hduIndex);
            }
            imageHdu = (ImageHDU) hdu;
        } else {
            for (int hduIdx = 0; hduIdx < fits.getNumberOfHDUs(); hduIdx++) {
                BasicHDU hdu = fits.getHDU(hduIdx);
                if (hdu instanceof ImageHDU) {
                    imageHdu = (ImageHDU) hdu;

                    break;
                }
            }
        }

        if (imageHdu == null) {
            System.err.println("no image hdu");

            return null;
        }

        Image image = makeImage(request, imageHdu);
        if (image == null) {
            System.err.println("no image");

            return null;
        }
        File imageFile = getStorageManager().getTmpFile(request,
                             "fitsimage.png");
        ImageUtils.writeImageToFile(image, imageFile);

        return imageFile;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param hdu _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Image makeImage(Request request, ImageHDU hdu) throws Exception {
        int[] axes = hdu.getAxes();
        //TODO: How to handle 1D data
        if ((axes == null) || (axes.length <= 1)) {
            return null;
        }

        int    width  = axes[0];
        int    height = axes[1];
        Object fData  = hdu.getData().getData();
        if (fData == null) {
            throw new IllegalArgumentException("No HDU Data");
        }
        if ( !fData.getClass().isArray()) {
            throw new IllegalArgumentException("Unknown HDU Data type: "
                    + fData.getClass().getName());
        }

        double   min   = Double.MAX_VALUE;
        double   max   = Double.MIN_VALUE;
        double[] range = buildRange(fData)[0];
        for (double value : range) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        int[] pixels = new int[range.length];
        //        System.err.println ("range:" + min +" " + max + " #pixels: " + pixels.length);
        for (int i = 0; i < pixels.length; i++) {
            double value   = range[i];
            double percent = ((max == min)
                              ? 0.5
                              : (value - min) / (max - min));

            int    c       = (int) (percent * 255);
            //            if(value>0) 
            //                System.err.println ("value:" + value+" %:" + percent +" c:" + c);
            //            Color  c       = getColor(table, percent);
            //            int pixelValue =  ((0xff << 24) | (c.getRed() << 16) | (c.getGreen() << 8)
            //                               | c.getBlue());
            int pixelValue = ((0xff << 24) | (c << 16) | (c << 8) | c);

            pixels[i] = pixelValue;
        }

        Image image = Toolkit.getDefaultToolkit().createImage(
                          new MemoryImageSource(
                              width, height, pixels, 0, width));


        return image;

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntryInfo(Request request, Entry entry)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        sb.append(request.form(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtils.submit("Subset", ARG_FITS_SUBSET));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_OUTPUT, OUTPUT_INFO.getId()));

        Fits fits = new Fits(entry.getFile());
        for (int hduIdx = 0; hduIdx < fits.getNumberOfHDUs(); hduIdx++) {
            BasicHDU            hdu       = fits.getHDU(hduIdx);
            nom.tam.fits.Header header    = hdu.getHeader();
            StringBuffer        subSB     = new StringBuffer();

            String              hduType   = "N/A";
            TableData           tableData = null;
            String              hduLink   = "";


            if (hdu instanceof AsciiTableHDU) {
                hduType   = "Ascii Table";
                tableData = (TableData) hdu.getData();
            } else if (hdu instanceof ImageHDU) {
                hduType = "Image";
                ImageHDU imageHdu = (ImageHDU) hdu;
                int[]    axes     = imageHdu.getAxes();
                if ((axes != null) && (axes.length > 1)) {
                    String imageUrl =
                        HtmlUtils.url(
                            getRepository().URL_ENTRY_SHOW + "/"
                            + IOUtil.stripExtension(entry.getName())
                            + ".png", ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
                                      OUTPUT_IMAGE.toString(), ARG_FITS_HDU,
                                      "" + hduIdx);

                    hduLink = HtmlUtils.href(imageUrl, msg("View Image"))
                              + HtmlUtils.br();
                }
            } else if (hdu instanceof BinaryTableHDU) {
                hduType   = "Binary Table";
                tableData = (TableData) hdu.getData();

            }
            if ((tableData != null) && (tableData.getNRows() < 5000)) {
                StringBuffer tableSB = new StringBuffer();
                tableSB.append("<div style=\"margin-left:25px;\">");
                tableSB.append("<table cellspacing=2 cellpadding=2>");
                if (header.getStringValue("TTYPE1") != null) {
                    tableSB.append("<tr>");
                    for (int colIdx = 0; colIdx < tableData.getNCols();
                            colIdx++) {
                        String colName = header.getStringValue("TTYPE"
                                             + (colIdx + 1));
                        if (colName == null) {
                            colName = "&nbsp;";
                        }
                        tableSB.append("<td align=center><b>" + colName
                                       + "</td>");
                    }
                    tableSB.append("</tr>");
                }
                for (int rowIdx = 0; rowIdx < tableData.getNRows();
                        rowIdx++) {
                    Object[] row = tableData.getRow(rowIdx);
                    tableSB.append("<tr align=right>");
                    for (Object item : row) {
                        tableSB.append("<td>");
                        tableSB.append(getRowItem(item));
                        tableSB.append("</td>");
                    }
                    tableSB.append("</tr>");
                }
                tableSB.append("</table>");
                tableSB.append("</div>");
                subSB.append(HtmlUtils.makeShowHideBlock("Data",
                        tableSB.toString(), false));
            }


            subSB.append("<div style=\"margin-left:25px;\">");
            subSB.append(hduLink);
            subSB.append("<table>");
            int numCards = header.getNumberOfCards();
            for (int cardIdx = 0; cardIdx < numCards; cardIdx++) {
                String card = header.getCard(cardIdx);
                card = card.trim();
                if (card.length() == 0) {
                    continue;
                }
                List<String> toks = StringUtil.splitUpTo(card, "=", 2);
                subSB.append("<tr>");
                //Look for an '=' in the comment
                if ((toks.size() == 1)
                        || (toks.get(0).trim().indexOf(" ") >= 0)) {
                    if (card.startsWith("/")) {
                        card = card.substring(1);
                    }
                    subSB.append("<td colspan=3><i>"
                                 + HtmlUtils.entityEncode(card)
                                 + "</i></td>");
                } else {
                    String key     = toks.get(0).trim();
                    String comment = "";
                    String value   = toks.get(1);
                    int    idx;
                    if (value.startsWith("'")) {
                        idx     = value.indexOf("'", 1);
                        comment = value.substring(idx + 1).trim();
                        value   = value.substring(1, idx);
                        if (comment.startsWith("/")) {
                            comment = comment.substring(1);
                        }
                    } else {
                        idx = value.indexOf("/");
                        if (idx >= 0) {
                            comment = value.substring(idx + 1).trim();
                            value   = value.substring(0, idx).trim();
                        }
                    }

                    subSB.append("<td><b>" + HtmlUtils.entityEncode(key)
                                 + "</b></td><td>"
                                 + HtmlUtils.entityEncode(value)
                                 + "</td><td><i>"
                                 + HtmlUtils.entityEncode(comment)
                                 + "</i></td></tr>");
                }
                subSB.append("</tr>");
            }
            subSB.append("</table>");
            subSB.append("</div>");
            String label = HtmlUtils.checkbox(ARG_FITS_HDU, "" + hduIdx,
                               true) + " " + hduType;
            sb.append(HtmlUtils.makeShowHideBlock(label, subSB.toString(),
                    false));
        }
        sb.append(HtmlUtils.formClose());

        return new Result("", sb);


    }

    /**
     * _more_
     *
     * @param item _more_
     *
     * @return _more_
     */
    private String getRowItem(Object item) {
        try {
            int length = Array.getLength(item);
            if (length > 0) {
                return Array.get(item, 0).toString();
            }

            return item.toString();
        } catch (Exception exc) {
            return item.toString();
        }
    }




    //The below code was taken from the visad.data.fits.FitsAdapter code

    /**
     * _more_
     *
     * @param data _more_
     * @param list _more_
     * @param offset _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int decompose(Object data, double[] list, int offset)
            throws Exception {
        Class component = data.getClass().getComponentType();
        if (component == null) {
            return offset;
        }

        if ( !component.isArray()) {
            return copyArray(data, list, offset);
        }

        int len = Array.getLength(data);
        for (int i = len - 1; i >= 0; i--) {
            offset = decompose(Array.get(data, i), list, offset);
        }

        return offset;
    }




    /**
     * _more_
     *
     * @param data _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private double[][] buildRange(Object data) throws Exception {
        int      len    = get1DLength(data);

        double[] values = new double[len];

        int      offset = decompose(data, values, 0);
        while (offset < len) {
            values[offset++] = Double.NaN;
        }

        double[][] range = new double[1][];
        range[0] = values;

        return range;
    }




    /**
     * _more_
     *
     * @param data _more_
     * @param list _more_
     * @param offset _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int copyArray(Object data, double[] list, int offset)
            throws Exception {
        if (data instanceof byte[]) {
            byte[] bl = (byte[]) data;
            for (int i = 0; i < bl.length; i++) {
                int val = ((bl[i] >= 0)
                           ? bl[i]
                           : (((int) Byte.MAX_VALUE + 1) * 2 + (int) bl[i]));
                list[offset++] = (double) val;
            }
        } else if (data instanceof short[]) {
            short[] sl = (short[]) data;
            for (int i = 0; i < sl.length; i++) {
                int val = ((sl[i] >= 0)
                           ? sl[i]
                           : ((Short.MAX_VALUE + 1) * 2) - sl[i]);
                list[offset++] = (double) val;
            }
        } else if (data instanceof int[]) {
            int[] il = (int[]) data;
            for (int i = 0; i < il.length; i++) {
                list[offset++] = (double) il[i];
            }
        } else if (data instanceof long[]) {
            long[] ll = (long[]) data;
            for (int i = 0; i < ll.length; i++) {
                list[offset++] = (double) ll[i];
            }
        } else if (data instanceof float[]) {
            float[] fl = (float[]) data;
            for (int i = 0; i < fl.length; i++) {
                list[offset++] = (double) fl[i];
            }
        } else if (data instanceof double[]) {
            double[] dl = (double[]) data;
            for (int i = 0; i < dl.length; i++) {
                list[offset++] = dl[i];
            }
        } else {
            throw new IllegalArgumentException("type '"
                    + data.getClass().getName() + "' not handled");
        }

        return offset;
    }


    /**
     * _more_
     *
     * @param data _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int get1DLength(Object data) throws Exception {
        if ( !data.getClass().isArray()) {
            return 1;
        }

        int len   = Array.getLength(data);

        int total = 0;
        for (int i = 0; i < len; i++) {
            total += get1DLength(Array.get(data, i));
        }

        return total;
    }




}
