/**
* Copyright (c) 2008-2015 Geode Systems LLC
* This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file 
* ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
*/

package gov.noaa.esrl.psd.repository.data.model;


import java.util.regex.Matcher;

import org.ramadda.geodata.model.ClimateModelFileTypeHandler;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;


/**
 * ClimateModelFileTypeHandler to handle NOAA/PSL climate model files
 */
public class NOAAClimateModelFileTypeHandler extends ClimateModelFileTypeHandler {

    /** type identifier */
    public final static String TYPE_NOAA_FACTS_CLIMATE_MODELFILE =
        "noaa_facts_climate_modelfile";

    /**
     * Create a new NOAAClimateModelFileTypeHandler
     *
     * @param repository   the Repository
     * @param entryNode    the XML definition
     *
     * @throws Exception  problem creating the handler
     */
    public NOAAClimateModelFileTypeHandler(Repository repository,
                                           Element entryNode)
            throws Exception {
        super(repository, entryNode, FILE_REGEX);
    }

    /**
     * Initialize the entry
     *
     *
     * @param request _more_
     * @param entry the Entry
     *
     * @throws Exception  problems during initialization
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry, NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        Object[] values = getEntryValues(entry);
        if ((values[1] != null) && !values[1].toString().isEmpty()) {
            //System.err.println("already have  values set");
            return;
        }
        if (pattern == null) {
            return;
        }
        //System.err.println("no values set");
        String filepath = entry.getFile().toString();
        // strip off entry.das in case it's an opendap link from RAMADDA
        filepath = filepath.replaceAll("/entry.das", "");
        String filename = IOUtil.getFileTail(filepath);
        // Filename looks like  var_model_scenario_ens??_<date>.nc
        Matcher m = pattern.matcher(filename);
        if ( !m.find()) {
            System.err.println("no match");
            return;
        }
        String var        = m.group(1);
        String model      = m.group(2);
        String experiment = m.group(3);
        String member     = m.group(4);
        String date       = m.group(6);
        String frequency  = "Monthly";
        if (filepath.indexOf("Daily") >= 0) {
            frequency = "Daily";
        }

        /*
     <column name="collection_id" type="string"  label="Collection ID" showinhtml="false" showinform="false"/>
     <column name="model" type="enumerationplus"  label="Model"  showinhtml="true" xxxxvalues="file:/org/ramadda/data/model/models.txt"/>
     <column name="experiment" type="enumerationplus"  label="Experiment" xxxxvalues="file:/org/ramadda/data/model/experiments.txt" showinhtml="true" />
     <column name="ensemble" type="string"  label="Ensemble"/>
     <column name="variable" type="enumerationplus"  label="Variable"  xxxxxvalues="file:/org/ramadda/data/model/vars.txt"/>
        */

        int idx = 1;
        values[idx++] = model;
        values[idx++] = experiment;
        values[idx++] = member;
        values[idx++] = var;

    }
}
