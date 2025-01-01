/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.publisher;


import org.ramadda.repository.client.InteractiveRepositoryClient;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.ui.DateTimePicker;
import org.ramadda.util.HttpFormEntry;
//import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;
import ucar.visad.display.Animation;

import visad.DateTime;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;

import java.io.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.*;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


/**
 * @author IDV development team
 */
@SuppressWarnings("unchecked")
public class RamaddaPublisher extends ucar.unidata.idv.publish
    .IdvPublisher implements org.ramadda.repository.Constants {


    /** _more_ */
    private DateTimePicker fromDateFld;

    /** _more_ */
    private DateTimePicker toDateFld;


    /** _more_ */
    private JTextField nameFld;

    /** _more_ */
    private JTextField tagFld;

    /** _more_ */
    private JTextArea descFld;

    /** _more_ */
    private JTextField contentsNameFld;

    /** _more_ */
    private JTextArea contentsDescFld;

    /** _more_ */
    private JTextField northFld;

    /** _more_ */
    private JTextField southFld;

    /** _more_ */
    private JTextField eastFld;

    /** _more_ */
    private JTextField westFld;

    /** _more_ */
    private JCheckBox doBundleCbx =
        new JCheckBox("Publish bundle and attach image", true);

    /** _more_ */
    private JCheckBox doThumbnailCbx =
        new JCheckBox("Attach image as thumbnail", true);

    /** _more_ */
    private JCheckBox doImageEntryCbx = new JCheckBox("Add image as entry",
                                            false);

    /** _more_ */
    private JCheckBox doZidvCbx = new JCheckBox("Save as ZIDV file", false);

    /** _more_ */
    private JCheckBox uploadZidvDataCbx = new JCheckBox("Upload ZIDV Data",
                                              false);

    /** _more_ */
    private JCheckBox uploadZidvBundleCbx =
        new JCheckBox("Upload ZIDV Bundle", true);

    /** _more_ */
    private JCheckBox myAddAssociationCbx = new JCheckBox("", false);


    /** _more_ */
    private List comps;


    /** _more_ */
    private InteractiveRepositoryClient repositoryClient;


    /** _more_ */
    private boolean isImport;

    /**
     * _more_
     */
    public RamaddaPublisher() {}



    /**
     * Create the object
     *
     * @param idv The idv
     * @param element _more_
     */
    public RamaddaPublisher(IntegratedDataViewer idv, Element element) {
        super(idv, element);
        repositoryClient = new InteractiveRepositoryClient();

	/*
	  We used to enable the below setting but it is breaking things in the IDV so for now comment this out
        //Some SSL connections are failing so we turn this off
        //See  -http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
        System.setProperty("jsse.enableSNIExtension", "false");
	*/
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public boolean identifiedBy(String url) {
        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        if (repositoryClient != null) {
            return repositoryClient.getName();
        }

        return super.getName();
    }




    /**
     * What is the name of this publisher
     *
     * @return The name
     */
    public String getTypeName() {
        return "RAMADDA repository";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean configurexxx() {
        if (repositoryClient == null) {
            repositoryClient = new InteractiveRepositoryClient();
        }

        return repositoryClient.showConfigDialog();
    }


    /**
     * _more_
     */
    public void configure() {
        if (repositoryClient == null) {
            repositoryClient = new InteractiveRepositoryClient();
        }
        repositoryClient.showConfigDialog();
    }

    /**
     * Do the configuration
     *
     * @return Configuration ok
     */
    public boolean doInitNew() {
        if (repositoryClient == null) {
            repositoryClient = new InteractiveRepositoryClient();
        }

        return repositoryClient.showConfigDialog();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isConfigured() {
        if (repositoryClient == null) {
            repositoryClient = new InteractiveRepositoryClient();
        }

        return repositoryClient.doConnect();
    }





    /**
     * _more_
     *
     * @param root _more_
     * @param file _more_
     * @param entryId _more_
     * @param parentId _more_
     * @param name _more_
     * @param desc _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element addEntry(Element root, String file, String entryId,
                             String parentId, String name, String desc)
            throws Exception {
        String fromDate = formatDate(fromDateFld.getDate());
        String toDate   = formatDate(toDateFld.getDate());
        List tags = StringUtil.split(tagFld.getText().trim(), ",", true,
                                     true);
        List attrs = Misc.toList(new String[] {
            ATTR_ID, entryId, ATTR_FILE, IOUtil.getFileTail(file),
            ATTR_PARENT, parentId, ATTR_TYPE, "guess", ATTR_NAME, name,
            ATTR_DESCRIPTION, desc, ATTR_FROMDATE, fromDate, ATTR_TODATE,
            toDate
        });


        checkAndAdd(attrs, ATTR_NORTH, northFld);
        checkAndAdd(attrs, ATTR_SOUTH, southFld);
        checkAndAdd(attrs, ATTR_EAST, eastFld);
        checkAndAdd(attrs, ATTR_WEST, westFld);
        //Create the entry node
        Element node = XmlUtil.create(TAG_ENTRY, root, attrs);
        repositoryClient.addTags(node, tags);



        for (int i = 0; i < myDataSourcesCbx.size(); i++) {
            if (((JCheckBox) myDataSourcesCbx.get(i)).isSelected()) {
                String id = (String) myDataSourcesIds.get(i);
                repositoryClient.addAssociation(root, id, entryId,
                        "uses data");
            }
        }

        return node;
    }


    /**
     * _more_
     */
    private void doMakeContents() {
        if (fromDateFld != null) {
            return;
        }
        //        dialog = new JDialog("Publish to RAMADDA",true);

        comps = new ArrayList();
        Date now = new Date();
        fromDateFld = new DateTimePicker(now);
        toDateFld   = new DateTimePicker(now);
        nameFld     = new JTextField("", 30);
        tagFld      = new JTextField("", 30);
        tagFld.setToolTipText("Comma separated tag values");
        descFld         = new JTextArea("", 5, 30);
        contentsNameFld = new JTextField("", 30);
        contentsDescFld = new JTextArea("", 5, 30);
        northFld        = new JTextField("", 5);
        southFld        = new JTextField("", 5);
        eastFld         = new JTextField("", 5);
        westFld         = new JTextField("", 5);
        JComponent treeComp = repositoryClient.getTreeComponent();
        if (repositoryClient.getDefaultGroupId() != null) {
            treeComp = new JLabel(repositoryClient.getDefaultGroupName());
        }

        Insets i = new Insets(1, 1, 1, 1);
        JComponent bboxComp = GuiUtils.vbox(
                                  GuiUtils.wrap(GuiUtils.inset(northFld, i)),
                                  GuiUtils.hbox(
                                      GuiUtils.inset(westFld, i),
                                      GuiUtils.inset(
                                          eastFld, i)), GuiUtils.wrap(
                                              GuiUtils.inset(southFld, i)));



        Dimension dim = new Dimension(200, 50);
        JScrollPane descScroller = GuiUtils.makeScrollPane(descFld,
                                       (int) dim.getWidth(),
                                       (int) dim.getHeight());
        descScroller.setPreferredSize(dim);
        JComponent dateComp = GuiUtils.hbox(fromDateFld, toDateFld);
        if (isImport) {
            comps = Misc.toList(new Component[] {
                GuiUtils.rLabel("Parent Folder:"),
                treeComp, });

        } else {
            comps = Misc.toList(new Component[] {
                GuiUtils.rLabel("Name:"), nameFld,
                GuiUtils.top(GuiUtils.rLabel("Description:")), descScroller,
                GuiUtils.rLabel("Tags:"),
                GuiUtils.centerRight(tagFld, new JLabel(" (optional)")),
                GuiUtils.rLabel("Parent Folder:"), treeComp,
                GuiUtils.top(GuiUtils.rLabel("Date Range:")), dateComp,
                GuiUtils.rLabel("Lat/Lon Box:"), GuiUtils.left(bboxComp)
            });
        }
    }


    /** _more_ */
    private String lastBundleFile;

    /** _more_ */
    private String lastBundleId;

    /** _more_ */
    private boolean dialogOk;

    /** _more_ */
    private JDialog dialog;

    /** _more_ */
    private List myDataSources;

    /** _more_ */
    private List myDataSourcesCbx;

    /** _more_ */
    private List myDataSourcesIds;

    /**
     * _more_
     *
     * @param contentFile _more_
     * @param fromViewManager _more_
     *
     * @return _more_
     */
    @Override
    public String publishContent(String contentFile,
                                 ViewManager fromViewManager) {

        if ( !isConfigured()) {
            return null;
        }


        //Uncomment to test publishing an import
        //        contentFile = "/Users/jeffmc/testloop.zip";

        try {
            boolean isBundle = ((contentFile == null)
                                ? false
                                : getIdv().getArgsManager().isBundleFile(
                                    contentFile));

            boolean isZidv = ((contentFile == null)
                              ? false
                              : getIdv().getArgsManager().isZidvFile(
                                  contentFile));


            isImport = ((contentFile == null)
                        ? false
                        : contentFile.endsWith(".zip"));


            doMakeContents();
            List      myComps           = new ArrayList(comps);

            JCheckBox addAssociationCbx = null;
            List      topComps          = new ArrayList();


            boolean   isImage           = false;
            if ((contentFile != null) && !isBundle && !isImport) {
                topComps.add(GuiUtils.rLabel("File:"));
                JComponent extra;
                if (ImageUtils.isImage(contentFile)) {
                    isImage = true;
                    extra   = GuiUtils.hbox(doThumbnailCbx, doImageEntryCbx);
                } else {
                    extra = GuiUtils.filler(1, 1);
                }

                if (isImage) {
                    doBundleCbx.setText("Publish bundle and attach image");
                } else {
                    doBundleCbx.setSelected(false);
                    doBundleCbx.setText("Also publish bundle");
                }
                doBundleCbx.setToolTipText(
                    "<html>Also publish the bundle</html>");
                topComps.add(
                    GuiUtils.left(
                        GuiUtils.hbox(
                            new JLabel(IOUtil.getFileTail(contentFile)),
                            GuiUtils.filler(10, 5),
                            GuiUtils.hbox(doBundleCbx, extra), doZidvCbx)));
                if (lastBundleFile != null) {
                    addAssociationCbx = myAddAssociationCbx;
                    addAssociationCbx.setText(
                        "Add association with last bundle: "
                        + IOUtil.getFileTail(lastBundleFile));
                    topComps.add(GuiUtils.filler());
                    topComps.add(addAssociationCbx);
                }
            }


            if (isZidv) {
                topComps.add(GuiUtils.filler());
                topComps.add(GuiUtils.left(GuiUtils.hbox(uploadZidvDataCbx,
                        uploadZidvBundleCbx)));
            }

            int numTopComps = topComps.size();
            topComps.addAll(myComps);

            myDataSources    = new ArrayList();
            myDataSourcesCbx = new ArrayList();
            myDataSourcesIds = new ArrayList();

            List notMineDataSources = new ArrayList();
            List dataSources        = getIdv().getDataSources();
            for (int i = 0; i < dataSources.size(); i++) {
                DataSource dataSource = (DataSource) dataSources.get(i);
                String ramaddaId =
                    (String) dataSource.getProperty("ramadda.id");
                String ramaddaHost =
                    (String) dataSource.getProperty("ramadda.host");
                if ((ramaddaId == null) || (ramaddaHost == null)) {
                    notMineDataSources.add(dataSource);

                    continue;
                }
                if ( !Misc.equals(ramaddaHost,
                                  repositoryClient.getHostname())) {
                    notMineDataSources.add(dataSource);

                    continue;
                }
                myDataSources.add(dataSource);
                myDataSourcesCbx.add(new JCheckBox(dataSource.toString(),
                        false));
                myDataSourcesIds.add(ramaddaId);
            }


            if (myDataSourcesCbx.size() > 0) {
                topComps.add(GuiUtils.rLabel("Make associations to:"));
                topComps.add(GuiUtils.left(GuiUtils.vbox(myDataSourcesCbx)));
            }


            GuiUtils.tmpInsets = GuiUtils.INSETS_5;
            double[] wts = {
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            };
            wts[numTopComps / 2 + 1] = 0.2;
            wts[numTopComps / 2 + 3] = 1.0;


            JComponent contents = GuiUtils.doLayout(topComps, 2,
                                      GuiUtils.WT_NY, wts);


            //Get one from the list
            if (fromViewManager == null) {
                List viewManagers = getIdv().getVMManager().getViewManagers();
                if (viewManagers.size() == 1) {
                    fromViewManager = (ViewManager) viewManagers.get(0);
                }
            }


            if (fromViewManager != null) {
                if ((fromViewManager instanceof MapViewManager)) {
                    MapViewManager   mvm = (MapViewManager) fromViewManager;
                    NavigatedDisplay navDisplay = mvm.getNavigatedDisplay();
                    Rectangle2D.Double bbox = navDisplay.getLatLonBox(false,
                                                  false);
                    if (bbox != null) {
                        southFld.setText(
                            getIdv().getDisplayConventions().formatLatLon(
                                Math.max(-90, bbox.getY())));
                        northFld.setText(
                            getIdv().getDisplayConventions().formatLatLon(
                                Math.min(
                                    90, (bbox.getY() + bbox.getHeight()))));
                        westFld.setText(
                            getIdv().getDisplayConventions().formatLatLon(
                                Math.max(
                                    -180,
                                    Misc.normalizeLongitude(bbox.getX()))));
                        eastFld.setText(
                            getIdv().getDisplayConventions().formatLatLon(
                                Math.min(
                                    180,
                                    Misc.normalizeLongitude(
                                        bbox.getX() + bbox.getWidth()))));
                    }
                }
                Animation anim = fromViewManager.getAnimation();
                if (anim != null) {
                    DateTime[] dttms = anim.getTimes();
                    if ((dttms != null) && (dttms.length > 0)) {
                        fromDateFld.setDate(Util.makeDate(dttms[0]));
                        toDateFld.setDate(
                            Util.makeDate(dttms[dttms.length - 1]));
                    }

                }
            }
            if (contentFile != null) {
                nameFld.setText(
                    IOUtil.stripExtension(IOUtil.getFileTail(contentFile)));
            } else {
                nameFld.setText("");
            }


            dialogOk = false;
            String parentId = null;
            while (true) {
                while (true) {
                    if ( !GuiUtils.showOkCancelDialog(null,
                            "Publish to RAMADDA", contents, null)) {
                        return null;
                    }
                    parentId = repositoryClient.getSelectedGroup();
                    if (parentId == null) {
                        LogUtil.userMessage(
                            "You must select a parent folder");
                    } else {
                        break;
                    }
                }

                List<HttpFormEntry> entries = new ArrayList<HttpFormEntry>();
                repositoryClient.addUrlArgs(entries);


                GuiUtils.ProgressDialog dialog =
                    new GuiUtils.ProgressDialog("Publishing to RAMADDA");
                String bundleFile = null;
                if (isImport) {
                    FileInputStream fos = new FileInputStream(contentFile);
                    entries.add(HttpFormEntry.hidden("group", parentId));
                    entries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                            IOUtil.readBytes(fos)));
                    IOUtil.close(fos);
                } else {

                    List<String> files         = new ArrayList<String>();
                    List<String> zipEntryNames = new ArrayList<String>();

                    if (isBundle) {
                        bundleFile  = contentFile;
                        contentFile = null;
                        files.add(bundleFile);
                        zipEntryNames.add(IOUtil.getFileTail(bundleFile));
                    } else if (doBundleCbx.isSelected()) {
                        String tmpFile = contentFile;
                        if (tmpFile == null) {
                            tmpFile = "publish.xidv";
                        }
                        bundleFile =
                            getIdv().getObjectStore().getTmpFile(
                                IOUtil.stripExtension(
                                    IOUtil.getFileTail(
                                        tmpFile)) + (doZidvCbx.isSelected()
                                ? ".zidv"
                                : ".xidv"));
                        getIdv().getPersistenceManager().doSave(bundleFile);
                        files.add(bundleFile);
                        zipEntryNames.add(IOUtil.getFileTail(bundleFile));
                        if (contentFile != null) {
                            files.add(contentFile);
                            zipEntryNames.add(
                                IOUtil.getFileTail(contentFile));
                        }
                    } else if (contentFile != null) {
                        files.add(contentFile);
                        zipEntryNames.add(IOUtil.getFileTail(contentFile));
                    }



                    String   fromDate = formatDate(fromDateFld.getDate());
                    String   toDate   = formatDate(toDateFld.getDate());
                    int      cnt      = 0;


                    Document doc      = XmlUtil.makeDocument();
                    //Create the top level node
                    Element root = XmlUtil.create(doc, TAG_ENTRIES);
                    List tags = StringUtil.split(tagFld.getText().trim(),
                                    ",", true, true);

                    String bundleId  = (cnt++) + "";
                    String contentId = (cnt++) + "";
                    //                    String mainFile;

                    if (bundleFile != null) {
                        //                        mainFile = bundleFile;
                    } else {
                        //                        mainFile    = contentFile;
                        //                        contentFile = null;
                        //                        contentId   = mainId;
                    }


                    String zidvFile = (isZidv
                                       ? bundleFile
                                       : null);
                    if (isZidv && !uploadZidvBundleCbx.isSelected()) {
                        bundleFile = null;
                        //                        mainFile   = null;
                    }

                    List    attrs;
                    Element node    = null;

                    String  theName = nameFld.getText().trim();
                    String  desc    = descFld.getText().trim();
                    if (contentFile != null) {
                        if (doImageEntryCbx.isSelected()) {
                            node = addEntry(root, contentFile, contentId,
                                            parentId, theName, desc);
                            theName = "IDV Bundle for " + theName;
                            desc    = "";
                        }
                    }
                    if (bundleFile != null) {
                        node = addEntry(root, bundleFile, bundleId, parentId,
                                        theName, desc);
                        if (contentFile != null) {
                            if (doImageEntryCbx.isSelected()) {
                                repositoryClient.addAssociation(root,
                                        contentId, bundleId, "uses bundle");
                            }
                        }
                    }
                    System.err.println("isImage:" + isImage + " "
                                       + contentFile + " " + (node != null)
                                       + " " + doThumbnailCbx.isSelected());

                    if ((contentFile != null) && (node != null)) {
                        if (isImage && doThumbnailCbx.isSelected()) {
                            repositoryClient.addThumbnail(node,
                                    IOUtil.getFileTail(contentFile));
                        } else {
                            //                            repositoryClient.addAttachment(node, IOUtil.getFileTail(contentFile));
                        }
                        if (isImage && doThumbnailCbx.isSelected()) {
                            Image image = ImageIO.read(
                                              new BufferedInputStream(
                                                  new FileInputStream(
                                                      contentFile)));
                            if (image != null) {
                                System.err.println("image size before:"
                                        + image.getWidth(null) + " "
                                        + image.getHeight(null));
                                image = ImageUtils.resize(image, 75, -1);
                                System.err.println("image size after:"
                                        + image.getWidth(null) + " "
                                        + image.getHeight(null));
                                String filename =
                                    "thumb_"
                                    + IOUtil.getFileTail(contentFile);
                                String tmpFile =
                                    getIdv().getObjectStore().getTmpFile(
                                        filename);
                                ImageUtils.writeImageToFile(image, tmpFile);
                                repositoryClient.addThumbnail(node, filename);
                                zipEntryNames.add(filename);
                                files.add(tmpFile);
                            }
                        }
                    }


                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ZipOutputStream       zos = new ZipOutputStream(bos);
                    for (int i = 0; i < files.size(); i++) {
                        String file = files.get(i);
                        String name = zipEntryNames.get(i);
                        if (file == null) {
                            continue;
                        }
                        zos.putNextEntry(new ZipEntry(name));
                        byte[] bytes =
                            IOUtil.readBytes(new FileInputStream(file));
                        zos.write(bytes, 0, bytes.length);
                        zos.closeEntry();
                    }


                    if ((zidvFile != null) && isZidv
                            && uploadZidvDataCbx.isSelected()) {
                        ZipInputStream zin = new ZipInputStream(
                                                 new FileInputStream(
                                                     new File(zidvFile)));
                        ZipEntry ze;
                        SimpleDateFormat sdf =
                            new SimpleDateFormat(
                                DataSource.DATAPATH_DATE_FORMAT);
                        while ((ze = zin.getNextEntry()) != null) {
                            String entryName = ze.getName();
                            String dateString =
                                StringUtil.findPattern(entryName,
                                    "(" + DataSource.DATAPATH_DATE_PATTERN
                                    + ")");
                            Date dttm = null;
                            if (dateString != null) {
                                dttm = sdf.parse(dateString);
                            }
                            if (getIdv().getArgsManager().isBundleFile(
                                    entryName)) {
                                continue;
                            }
                            dialog.setText("Adding " + entryName);
                            zos.putNextEntry(new ZipEntry(entryName));
                            byte[] bytes = IOUtil.readBytes(zin, null, false);
                            zos.write(bytes, 0, bytes.length);
                            zos.closeEntry();
                            String id = (cnt++) + "";
                            attrs = Misc.toList(new String[] {
                                ATTR_ID, id, ATTR_FILE, entryName,
                                ATTR_PARENT, parentId, "type.guess", "true",
                                //ATTR_TYPE, TYPE_FILE, 
                                ATTR_NAME, entryName
                            });
                            if (dttm != null) {
                                attrs.addAll(Misc.newList(ATTR_FROMDATE,
                                        formatDate(dttm), ATTR_TODATE,
                                        formatDate(dttm)));
                            }
                            node = XmlUtil.create(TAG_ENTRY, root, attrs);
                        }
                    }



                    if ((addAssociationCbx != null)
                            && addAssociationCbx.isSelected()) {
                        repositoryClient.addAssociation(root, lastBundleId,
                                contentId, "generated product");
                    }


                    String xml = XmlUtil.toString(root);
                    //                System.out.println(xml);

                    zos.putNextEntry(new ZipEntry("entries.xml"));
                    byte[] bytes = xml.getBytes();
                    zos.write(bytes, 0, bytes.length);
                    zos.closeEntry();
                    zos.close();
                    bos.close();



                    entries.add(new HttpFormEntry(ARG_FILE, "entries.zip",
                            bos.toByteArray()));

                }



                dialog.setText("Posting to RAMADDA");
                String[] result = repositoryClient.doPost(
                                      repositoryClient.URL_ENTRY_XMLCREATE,
                                      entries);
                dialog.dispose();
                if (result[0] != null) {
                    LogUtil.userErrorMessage("Error publishing:\n"
                                             + result[0]);

                    return null;
                }

                //                System.out.println("results:" + result[1]);
                Element response = XmlUtil.getRoot(result[1]);

                if (repositoryClient.responseOk(response)) {
                    String url = "";
                    Element firstResult = XmlUtil.findChild(response,
                                              TAG_ENTRY);
                    String entryId = null;
                    if (firstResult != null) {
                        entryId = XmlUtil.getAttribute(firstResult, ATTR_ID);
                        url = repositoryClient.absoluteUrl(
                            repositoryClient.getUrlBase()
                            + "/entry/show?entryid=" + entryId);
                    }

                    if (bundleFile != null) {
                        lastBundleId   = entryId;
                        lastBundleFile = bundleFile;
                    }
                    LogUtil.userMessage("Publication was successful. URL:\n"
                                        + url);

                    return url;
                }
                String body = XmlUtil.getChildText(response).trim();
                LogUtil.userErrorMessage("Error publishing:" + body);
            }
        } catch (Exception exc) {
            LogUtil.logException("Publishing", exc);
        }

        return null;
    }

    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    private String formatDate(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(d);
    }


    /**
     * _more_
     *
     * @param attrs _more_
     * @param attr _more_
     * @param fld _more_
     */
    private void checkAndAdd(List attrs, String attr, JTextField fld) {
        String v = fld.getText().trim();
        if (v.length() > 0) {
            attrs.add(attr);
            attrs.add(v);
        }
    }


    /**
     * _more_
     *
     * @param tag _more_
     * @param image _more_
     */
    public void publishIslImage(Element tag, Image image) {}

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        //        System.err.println("Publisher.tostring:" + getName() +" super: " + super.getName());
        if ((repositoryClient != null) && repositoryClient.hasSession()) {
            //            System.err.println("\t if 1 " + super.toString() + "  (connected)");
            return getName() + "  (connected)";
        }

        return getName();
    }

    /**
     * _more_
     *
     */
    public void doPublish() {
        publishContent(null, null);
    }



    /**
     *  Set the RepositoryClient property.
     *
     *  @param value The new value for RepositoryClient
     */
    public void setRepositoryClient(InteractiveRepositoryClient value) {
        repositoryClient = value;
    }

    /**
     *  Get the RepositoryClient property.
     *
     *  @return The RepositoryClient
     */
    public InteractiveRepositoryClient getRepositoryClient() {
        return repositoryClient;
    }






}
