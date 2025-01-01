/*
* Copyright (c) 2008-2025 Geode Systems LLC
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

package org.ramadda.bio.image;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.repository.type.*;



import org.ramadda.service.Service;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.Utils;



import org.w3c.dom.*;

import ucar.nc2.units.DateUnit;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;


import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author Jeff McWhirter
 */
public class OmeTiffTypeHandler extends GenericTypeHandler {

    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception On badness
     */
    public OmeTiffTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }


    /*
<Experiment ID="urn:lsid:loci.wisc.edu:Experiment:OWS348" Type="PGIDocumentation">
      <Description>4 Cell Embryo</Description>
      <ExperimenterRef ID="urn:lsid:loci.wisc.edu:Experimenter:116"/>
   </Experiment>
   <Experimenter Email="mnasim@wisc.edu" FirstName="Maimoon" ID="urn:lsid:loci.wisc.edu:Experimenter:116" Institution="" LastName="Nasim"/>
   <Instrument ID="urn:lsid:loci.wisc.edu:Instrument:OWS1">
      <Microscope Manufacturer="Nikon" Model="Eclipse TE300" SerialNumber="U629762" Type="Inverted"/>
      <LightSource ID="urn:lsid:loci.wisc.edu:LightSource:OWS1" Manufacturer="Spectral Physics" Model="Tsunami 5W" SerialNumber="2123">
         <Laser LaserMedium="TiSapphire" Type="SolidState"/>
      </LightSource>
      <Detector ID="urn:lsid:loci.wisc.edu:Detector:OWS1" Manufacturer="Hamamatzu" Model="H7422" Type="PMT"/>
      <Detector ID="urn:lsid:loci.wisc.edu:Detector:OWS2" Manufacturer="Bio-Rad" Model="1024TLD" Type="Photodiode"/>
      <Objective CalibratedMagnification="100.0" Correction="PlanApo" ID="urn:lsid:loci.wisc.edu:Objective:OWS2" Immersion="Oil" LensNA="1.4" Manufacturer="Nikon" Model="S Fluor" NominalMagnification="100.0" SerialNumber="044989" WorkingDistance="0.13"/>
   </Instrument>

    */

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param service _more_
     * @param output _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void handleServiceResults(Request request, Entry entry,
                                     Service service, ServiceOutput output)
            throws Exception {
        super.handleServiceResults(request, entry, service, output);

        List<Entry> entries = output.getEntries();
        if (entries.size() == 0) {
            return;
        }
        String filename = entries.get(0).getFile().toString();
        if ( !filename.endsWith(".xml")) {
            return;
        }
        String xml = IOUtil.readContents(filename, getClass(), "");
        //Look for the xml
        int idx = xml.indexOf("<?xml");
        if (idx > 0) {
            xml = xml.substring(idx);
        }

        //        System.err.println("XML:\n" + xml);

        try {
            processXml(request,xml, entry);
        } catch (Exception exc) {
            getLogManager().logError("OmeTiff:" + exc);
        }
    }

    /**
     * _more_
     *
     * @param xml _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void processXml(Request request,String xml, Entry entry) throws Exception {


        Element root         = root = XmlUtil.getRoot(xml);
        Element experiment   = XmlUtil.findChild(root, "Experiment");
        Element experimenter = XmlUtil.findChild(root, "Experimenter");

        if (experiment != null) {
            Element ref = XmlUtil.findChild(experiment, "ExperimenterRef");
            getMetadataManager().addMetadata(request,
                entry,
                new Metadata(
                    getRepository().getGUID(), entry.getId(),
                    getMetadataManager().findType("bio_ome_experiment"), false,
                    XmlUtil.getAttribute(experiment, "Type", ""),
                    XmlUtil.getGrandChildText(experiment, "Description"),
                    XmlUtil.getAttribute(experiment, "ID", ""), ((ref != null)
                    ? XmlUtil.getAttribute(ref, "ID", "")
                    : ""), null));
            //            type,description,experimenter,id
        }

        if (experiment != null) {
            Element ref = XmlUtil.findChild(experiment, "ExperimenterRef");
            Hashtable<Integer, String> mapToExtra = new Hashtable<Integer,
                                                        String>();

            //For extra metadata we put it into a hash
            mapToExtra.put(5, XmlUtil.getAttribute(experimenter, "ID", ""));
            getMetadataManager().addMetadata(request,
                entry,
                new Metadata(
                    getRepository().getGUID(), entry.getId(),
                    getMetadataManager().findType("bio_ome_experimenter"), false,
                    XmlUtil.getAttribute(experimenter, "FirstName", ""),
                    XmlUtil.getAttribute(experimenter, "LastName", ""),
                    XmlUtil.getAttribute(experimenter, "Email", ""),
                    XmlUtil.getAttribute(experimenter, "Institution", ""),
                    Metadata.mapToExtra(mapToExtra)));
            //first,last,email,inst,id
        }

        //   <Experimenter Email="mnasim@wisc.edu" FirstName="Maimoon" ID="urn:lsid:loci.wisc.edu:Experimenter:116" Institution="" LastName="Nasim"/>

    }


}
