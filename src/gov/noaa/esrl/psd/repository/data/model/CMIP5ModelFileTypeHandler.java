/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package gov.noaa.esrl.psd.repository.data.model;


import org.ramadda.geodata.model.ClimateModelFileTypeHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;

import java.util.regex.Matcher;
//import java.util.regex.Pattern;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Apr 2, '15
 * @author         Enter your name here...
 */
public class CMIP5ModelFileTypeHandler extends ClimateModelFileTypeHandler {

    //var_model_experiment_member

    /** _more_ */
    public static final String CMIP5_FILE_REGEX =
        "([^_]+)_([^_]+)_([^_]+)_([^_]+)_(r\\d+i\\d+p\\d+)(_([^_.]+))?(\\.1x1)?.nc";

    /** type identifier */
    public final static String TYPE_CMIP5_MODEL_FILE = "cmip5_model_file";


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public CMIP5ModelFileTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        //super(repository, entryNode, CMIP5_FILE_REGEX);
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry, NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        Object[] values = getEntryValues(entry);
        if ((values[1] != null) && !values[1].toString().isEmpty()) {
            //System.err.println ("already have  values set");
            return;
        }
        //System.err.println ("no values set");
        String filepath = entry.getFile().toString();
        String filename = IOUtil.getFileTail(entry.getFile().toString());
        // Filename looks like  var_mip_model_scenario_ens??_<date>.nc
        Matcher m = pattern.matcher(filename);
        if ( !m.find()) {
            System.err.println("no match for: " + filename);

            return;
        }
        String var        = m.group(1);
        String miptable   = m.group(2);
        String model      = m.group(3);
        String experiment = m.group(4);
        String member     = m.group(5);
        String date       = m.group(7);
        String frequency  = "Monthly";
        if (filepath.indexOf("Daily") >= 0) {
            frequency = "Daily";
        }


        /*
     <column name="collection_id" type="string"  label="Collection ID" showinhtml="false" showinform="false"/>
     <column name="model" type="enumerationplus"  label="Model"  showinhtml="true" />
     <column name="miptable" type="enumeration"  label="MIP Table" />
     <column name="experiment" type="enumerationplus"  label="Experiment" />
     <column name="ensemble" type="string"  label="Ensemble"/>
     <column name="variable" type="enumerationplus"  label="Variable"/>
        */

        int idx = 1;
        values[idx++] = model;
        values[idx++] = miptable;
        values[idx++] = experiment;
        values[idx++] = member;
        values[idx++] = var;

    }

}
