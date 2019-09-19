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

package org.ramadda.geodata.model;


import org.ramadda.geodata.cdmdata.NcmlUtil;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.service.ServiceOperand;

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Some utility methods for model processing
 */
public class ModelUtil {

    /**
     * Ctor
     */
    public ModelUtil() {}

    /**
     * Create an NcML aggregation by time and return an Entry pointing to that ncml
     *
     * @param request the request
     * @param sameEntries  Entrys to be aggregated
     * @param id an id for the file
     * @param processDir  where to put the ncml
     *
     * @return an Entry pointing to the NcML file
     *
     * @throws Exception  problemos
     */
    public static Entry aggregateEntriesByTime(Request request,
            List<Entry> sameEntries, String id, File processDir)
            throws Exception {
        Repository   repo     = request.getRepository();
        StringBuilder sb       = new StringBuilder();
        NcmlUtil     ncmlUtil = new NcmlUtil(NcmlUtil.AGG_JOINEXISTING);
        // TODO: get the time coordinate from the file
        String timeCoordinate = "time";

        NcmlUtil.openNcml(sb);
        sb.append("\n");
        sb.append(XmlUtil.openTag(NcmlUtil.TAG_AGGREGATION,
                                  XmlUtil.attrs(new String[] {
            NcmlUtil.ATTR_TYPE, NcmlUtil.AGG_JOINEXISTING,
            NcmlUtil.ATTR_DIMNAME, timeCoordinate,
            NcmlUtil.ATTR_TIMEUNITSCHANGE, "true"
        })));
        sb.append("\n");

        List<Entry> sortedChillens =
            repo.getEntryManager().getEntryUtil().sortEntriesOnName(
                sameEntries, false);

        //System.err.println("making ncml:");
        for (Entry child : sortedChillens) {
            //            System.err.println("   file:" + s);
            String s = child.getResource().getPath();
            File   f = new File(s);
            sb.append(XmlUtil
                .tag(NcmlUtil.TAG_NETCDF,
                     XmlUtil.attrs(NcmlUtil.ATTR_LOCATION,
                                   IOUtil.getURL(s,
                                       new ModelUtil().getClass())
                                           .toString(), NcmlUtil
                                           .ATTR_ENHANCE, "true"), ""));
        }

        sb.append(XmlUtil.closeTag(NcmlUtil.TAG_AGGREGATION));
        sb.append(XmlUtil.closeTag(NcmlUtil.TAG_NETCDF));
        String ncml = sb.toString();
        if (ncml.length() != 0) {
            String ncmlFileName = id + ".ncml";
            //Use the timestamp from the files to make the ncml file name based on the input files
            File outFile = new File(IOUtil.joinDir(processDir, ncmlFileName));
            try {
                IOUtil.writeFile(outFile, ncml);
            } catch (IOException ioe) {
                System.err.println("Couldn't write out NcML file");

                return null;
            }
            //TypeHandler myHandler = repo.getTypeHandler("cdm_grid", true);
            TypeHandler myHandler   = sortedChillens.get(0).getTypeHandler();
            Entry       outputEntry = myHandler.createEntry(repo.getGUID());
            outputEntry.setDate(new Date().getTime());
            outputEntry.setName(ncmlFileName);
            Resource resource = new Resource(outFile,
                                             Resource.TYPE_LOCAL_FILE);
            outputEntry.setResource(resource);
            outputEntry.setId(
                repo.getEntryManager().getProcessFileTypeHandler().getSynthId(
                    repo.getEntryManager().getProcessEntry(),
                    repo.getStorageManager().getProcessDir().toString(),
                    outFile));
            Entry    sample    = sameEntries.get(0);
            Object[] newValues = sample.getValues();
            outputEntry.setValues(newValues);

            //System.out.println("ncml id = " + outputEntry.getId());
            return outputEntry;
        }

        return null;
    }

    /**
     * Make a hash key from the values
     * @param values the entry values
     * @param excludeDoops exclude duplicates
     * @return a string representing the values
     */
    public static String makeValuesKey(Object[] values,
                                       boolean excludeDoops) {
        if (values == null) {
            return "nullKey";
        }
        StringBuilder buf     = new StringBuilder();
        List<String>  dupList = new ArrayList<String>();
        int           i       = 0;
        for (Object o : values) {
            i++;
            String ohFace = o.toString();
            if (dupList.contains(ohFace) && excludeDoops) {
                continue;
            }
            dupList.add(ohFace);
            buf.append(o.toString());
            if (i < values.length) {
                buf.append("-");
            }
        }

        return buf.toString();
    }

    /**
     * Build climate years string using default years
     *
     * @param separator   separator between years (e.g., "-", "/", etc)
     *
     * @return  the string (e.g. "1981/2010")
     */
    public static String buildClimateYearsString(String separator) {
        return buildClimateYearsString(
            ClimateModelApiHandler.DEFAULT_CLIMATE_START_YEAR,
            ClimateModelApiHandler.DEFAULT_CLIMATE_END_YEAR, separator);
    }

    /**
     * Build climate years string
     *
     * @param startYear   starting year
     * @param endYear     end year
     * @param separator   separator between years (e.g., "-", "/", etc)
     *
     * @return  the string (e.g. "1981/2010")
     */
    public static String buildClimateYearsString(String startYear,
            String endYear, String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append(startYear);
        sb.append(separator);
        sb.append(endYear);

        return sb.toString();
    }

    /**
     * Get the model/experiment string for the collection
     *
     * @param request  the request
     * @param collection  the collection number
     *
     * @return the string representation
     */
    public static String getModelExperimentString(Request request,
            int collection) {
        String collectionId = (collection == 1)
                              ? ClimateModelApiHandler.ARG_COLLECTION1
                              : ClimateModelApiHandler.ARG_COLLECTION2;
        String modelArg =
            ClimateModelApiHandler.getFieldSelectArg(collectionId,
                ClimateModelApiHandler.MODEL_FIELD_INDEX);
        String expArg =
            ClimateModelApiHandler.getFieldSelectArg(collectionId,
                ClimateModelApiHandler.EXP_FIELD_INDEX);
        String model   = request.getString(modelArg);
        String exp     = request.getString(expArg);
        String climKey = model + " " + exp;

        return climKey;
    }

    /**
     * Sort the operands by collection
     *
     * @param request  the request
     * @param operands all the operands from the ServiceInput
     *
     * @return sorted list
     *
     * @throws Exception problems sorting
     */
    public static List<List<ServiceOperand>> sortOperandsByCollection(
            Request request, List<ServiceOperand> operands)
            throws Exception {
        List<List<ServiceOperand>> sortedList =
            new ArrayList<List<ServiceOperand>>();
        Map<String, List<ServiceOperand>> opMap = new HashMap<String,
                                                      List<ServiceOperand>>();
        for (ServiceOperand op : operands) {
            Object okey =
                op.getProperty(ClimateModelApiHandler.ARG_COLLECTION);
            if (okey == null) {
                Entry e = op.getEntries().get(0);
                okey = e.getTransientProperty(
                    ClimateModelApiHandler.ARG_COLLECTION);
            }
            String key;
            if (okey == null) {
                key = ClimateModelApiHandler.ARG_COLLECTION1;
            } else {
                key = okey.toString();
            }
            List<ServiceOperand> myList = opMap.get(key);
            if (myList == null) {
                myList = new ArrayList<ServiceOperand>();
            }
            myList.add(op);
            opMap.put(key, myList);
        }

        String[] collections = { ClimateModelApiHandler.ARG_COLLECTION1,
                                 ClimateModelApiHandler.ARG_COLLECTION2 };

        for (int i = 0; i < collections.length; i++) {
            List<ServiceOperand> ops = opMap.get(collections[i]);
            if (ops != null) {
                sortedList.add(ops);
            }
        }

        return sortedList;
    }

    /**
     * Copy the entry collection properties if they exist
     *
     * @param oldEntry  the old Entry
     * @param newEntry  the new Entry
     * public static void copyEntryCollectionProperties(Entry oldEntry,
     *       Entry newEntry) {
     *   String prop = oldEntry.getTransientProperty(
     *                     ClimateModelApiHandler.ARG_COLLECTION).toString();
     *   System.err.println(prop);
     *   //TODO: do we want to set a default?
     *   //if (prop == null) {
     *   //    prop = ClimateModelApiHandler.ARG_COLLECTION1;
     *   //}
     *   if (prop != null) {
     *       newEntry.putTransientProperty(
     *           ClimateModelApiHandler.ARG_COLLECTION, prop);
     *   }
     *
     * }
     */

    /**
     * Copy the entry collection properties if they exist
     *
     * @param oldEntry  the old Entry
     * @param newEntries  the new Entries
     * public static void copyEntryCollectionProperties(Entry oldEntry,
     *       List<Entry> newEntries) {
     *   for (Entry e : newEntries) {
     *       copyEntryCollectionProperties(oldEntry, e);
     *   }
     * }
     */


}
