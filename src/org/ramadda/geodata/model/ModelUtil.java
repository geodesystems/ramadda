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

package org.ramadda.geodata.model;


import org.ramadda.geodata.cdmdata.NcmlUtil;
import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Resource;
import org.ramadda.repository.type.GranuleTypeHandler;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOperand;

import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * Some utility methods for model processing
 */
public class ModelUtil {

    /** months */
    private static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
        "Nov", "Dec"
    };

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
        Repository    repo     = request.getRepository();
        StringBuilder sb       = new StringBuilder();
        NcmlUtil      ncmlUtil = new NcmlUtil(NcmlUtil.AGG_JOINEXISTING);
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
            sb.append(XmlUtil.tag(NcmlUtil.TAG_NETCDF,
                                  XmlUtil.attrs(NcmlUtil.ATTR_LOCATION,
                                          IOUtil.getURL(s,
                                                  new ModelUtil().getClass())
                                                  .toString(),
                                          NcmlUtil.ATTR_ENHANCE,
                                          "true"),
                                  ""));
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
    	return makeValuesKey(values, excludeDoops, "-");
    }
    
    /**
     * Make a hash key from the values
     * @param values the entry values
     * @param excludeDoops exclude duplicates
     * @return a string representing the values
     */
    public static String makeValuesKey(Object[] values,
                                       boolean excludeDoops, String delimiter) {
        if (values == null) {
            return "nullKey";
        }
        StringBuilder buf     = new StringBuilder();
        List<String>  dupList = new ArrayList<String>();
        int           i       = 0;
        for (Object o : values) {
            i++;
            String ohFace = (o == null)
                            ? "null"
                            : o.toString();
            if (dupList.contains(ohFace) && excludeDoops) {
                continue;
            }
            dupList.add(ohFace);
            buf.append(ohFace);
            if (i < values.length) {
                buf.append(delimiter);
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
     * Group the entries by those with the same column values
     * @param request the request
     * @param entries the entries
     * @return a list of lists of grouped entries
     */
    public static List<List<Entry>> groupEntriesByColumn(Request request,
            List<Entry> entries) {
        Hashtable<String, List<Entry>> table = new Hashtable<String,
                                                   List<Entry>>();
        for (Entry entry : entries) {
            String      valuesKey = makeValuesKey(entry.getValues(), true);
            List<Entry> myEntries = table.get(valuesKey);
            if (myEntries == null) {
                myEntries = new ArrayList<Entry>();
            }
            myEntries.add(entry);
            table.put(valuesKey, myEntries);
        }
        List<List<Entry>> newEntries =
            new ArrayList<List<Entry>>(table.size());
        for (String entryKey : table.keySet()) {
            List<Entry> sameEntries = table.get(entryKey);
            if (sameEntries.isEmpty()) {
                continue;
            } else {
                newEntries.add(sameEntries);
            }
        }

        return newEntries;
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
     * Get the collection number from the ServiceOperand
     *
     * @param so  the ServiceOperand
     *
     * @return the collection number (0 (default) or 1)
     */
    public static int getOperandCollectionNumber(ServiceOperand so) {
        Object collection =
            so.getProperty(ClimateModelApiHandler.ARG_COLLECTION);
        if ((collection == null)
                || collection.toString().equals(
                    ClimateModelApiHandler.ARG_COLLECTION1)) {
            return 0;
        } else if (collection.toString().equals(
                ClimateModelApiHandler.ARG_COLLECTION2)) {
            return 1;
        }

        return 2;
    }

    /**
     * Get the time frequency for this entry
     *
     * @param request  the request
     * @param sample   the sample Entry
     *
     * @return  the time frequency (e.g. monthly, daily, etc)
     */
    public static String getFrequency(Request request, Entry sample) {
        Entry collection = GranuleTypeHandler.getCollectionEntry(request,
                               sample);

        return getCollectionFrequency(request, collection);
    }

    /**
     * Get the time frequency for this collection
     *
     * @param request  the request
     * @param collection the collection
     *
     * @return  the time frequency (e.g. monthly, daily, etc)
     */
    public static String getCollectionFrequency(Request request,
            Entry collection) {
        String frequency = CDOOutputHandler.FREQUENCY_MONTHLY;
        if (collection != null) {
            String sval = collection.getValue(request,0).toString();
            if ( !sval.toLowerCase().contains("mon")) {
                frequency = CDOOutputHandler.FREQUENCY_DAILY;
            }
        }

        return frequency;
    }

    /**
     * Copy the specified properties from one SI to another
     *
     * @param in  the input SI
     * @param out  the output SI
     * @param strings  the list of string keys
     */
    public static void copyServiceInputProperties(ServiceInput in,
            ServiceInput out, String... strings) {
        for (String str : strings) {
            Object foo = in.getProperty(str);
            if (foo != null) {
                out.putProperty(str, foo);
            }
        }
    }

    /**
     * Build an output name for an operand entry and request
     *
     * @param request the request
     * @param values the entry values
     * @param opNum the operator number
     *
     * @return the string name of this request
     */
    public static String buildOutputName(Request request, Object[] values,
                                         int opNum) {
    	return buildOutputName(request, values, opNum, false);
    }

    /**
     * Build an output name for an operand entry and request
     *
     * @param request the request
     * @param values the entry values
     * @param opNum the operator number
     * @param isDaily true if is daily data
     *
     * @return the string name of this request
     */
    public static String buildOutputName(Request request, Object[] values,
                                         int opNum, boolean isDaily) {

        StringBuilder outputName = new StringBuilder();
        // values = collection,model,experiment,ens,var
        // model
        outputName.append(values[1].toString().toUpperCase());
        outputName.append(" ");
        // experiment
        outputName.append(values[2]);
        outputName.append(" ");
        // ens
        String ens = values[3].toString();
        if (ens.equals("mean") || ens.equals("sprd") || ens.equals("clim")) {
            outputName.append("ens");
        }
        outputName.append(ens);
        outputName.append(" ");
        // var
        /*
        outputName.append(values[4]);
        outputName.append(" ");
        outputName.append(stat);
        outputName.append(" ");
        */

        StringBuilder dateSB  = new StringBuilder();
        String        yearNum = (opNum == 0)
                                ? ""
                                : String.valueOf(opNum + 1);
        int           startMonth, endMonth;
        int           startDay=1, endDay=1;
        if (request.getString(
                CDOOutputHandler.ARG_CDO_MONTHS).equalsIgnoreCase("all")) {
            startMonth = 1;
            endMonth   = 12;
        } else {
            startMonth = request.defined(CDOOutputHandler.ARG_CDO_STARTMONTH)
                         ? request.get(CDOOutputHandler.ARG_CDO_STARTMONTH, 1)
                         : 1;
            endMonth = request.defined(CDOOutputHandler.ARG_CDO_ENDMONTH)
                       ? request.get(CDOOutputHandler.ARG_CDO_ENDMONTH,
                                     startMonth)
                       : startMonth;
        }
        if (isDaily) {
            startDay = request.defined(CDOOutputHandler.ARG_CDO_STARTDAY)
                         ? request.get(CDOOutputHandler.ARG_CDO_STARTDAY, 1)
                         : 1;
            endDay = request.defined(CDOOutputHandler.ARG_CDO_ENDDAY)
                   ? request.get(CDOOutputHandler.ARG_CDO_ENDDAY,
                                 startDay)
                   : startDay;
        	
        }
        if (startMonth == endMonth) {
            dateSB.append(MONTHS[startMonth - 1]);
            if (isDaily ) {
            	dateSB.append(" ");
            	dateSB.append(startDay);
                dateSB.append("-");
            	dateSB.append(endDay);
            }
        } else {
            dateSB.append(MONTHS[startMonth - 1]);
            if (isDaily) {
            	dateSB.append(" ");
            	dateSB.append(startDay);
            } 
            dateSB.append("-");
            dateSB.append(MONTHS[endMonth - 1]);
            if (isDaily) {
            	dateSB.append(" ");
            	dateSB.append(endDay);
            }
        }
        dateSB.append(" ");
        if (request.defined(CDOOutputHandler.ARG_CDO_YEARS + yearNum)) {
            dateSB.append(request.getString(CDOOutputHandler.ARG_CDO_YEARS
                                            + yearNum));
        } else if (request.defined(CDOOutputHandler.ARG_CDO_YEARS)
                   && !(request.defined(
                       CDOOutputHandler.ARG_CDO_STARTYEAR
                       + yearNum) || request.defined(
                           CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum))) {
            dateSB.append(request.getString(CDOOutputHandler.ARG_CDO_YEARS));
        } else {
            String startYear =
                request.defined(CDOOutputHandler.ARG_CDO_STARTYEAR + yearNum)
                ? request.getString(CDOOutputHandler.ARG_CDO_STARTYEAR
                                    + yearNum)
                : request.defined(CDOOutputHandler.ARG_CDO_STARTYEAR)
                  ? request.getString(CDOOutputHandler.ARG_CDO_STARTYEAR, "")
                  : "";
            String endYear = request.defined(CDOOutputHandler.ARG_CDO_ENDYEAR
                                             + yearNum)
                             ? request.getString(
                                 CDOOutputHandler.ARG_CDO_ENDYEAR + yearNum)
                             : request.defined(
                                 CDOOutputHandler.ARG_CDO_ENDYEAR)
                               ? request.getString(
                                   CDOOutputHandler.ARG_CDO_ENDYEAR,
                                   startYear)
                               : startYear;
            if (startYear.equals(endYear)) {
                dateSB.append(startYear);
            } else {
                dateSB.append(startYear);
                dateSB.append("-");
                dateSB.append(endYear);
            }
        }
        outputName.append(dateSB);

        return outputName.toString();

    }

}
