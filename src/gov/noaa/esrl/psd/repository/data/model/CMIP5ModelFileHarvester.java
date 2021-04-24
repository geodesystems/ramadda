/**
* Copyright (c) 2008-2015 Geode Systems LLC
* This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file 
* ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
*/

/**
 * Copyright (c) 2008-2015 Geode Systems LLC
 * This Software is licensed under the Geode Systems RAMADDA License available in the source distribution in the file
 * ramadda_license.txt. The above copyright notice shall be included in all copies or substantial portions of the Software.
 */
package gov.noaa.esrl.psd.repository.data.model;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.util.FileInfo;
import org.ramadda.repository.harvester.HarvesterFile;
import org.ramadda.repository.harvester.PatternHarvester;
import org.ramadda.repository.type.TypeHandler;

import org.w3c.dom.Element;


import java.io.File;

import java.util.regex.Matcher;
import java.util.Hashtable;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Apr 2, '15
 * @author         Enter your name here...
 */
public class CMIP5ModelFileHarvester extends PatternHarvester {

    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public CMIP5ModelFileHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public CMIP5ModelFileHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    /**
     * Get the TypeHandler class
     *
     * @return  the class
     */
    public Class getTypeHandlerClass() {
        return CMIP5ModelFileTypeHandler.class;
    }

    /**
     * Get the type handler
     *
     * @return  the TypeHandler or null
     *
     * @throws Exception  can't find type handler
     */
    public TypeHandler getTypeHandler() throws Exception {
        return getRepository().getTypeHandler(
            CMIP5ModelFileTypeHandler.TYPE_CMIP5_MODEL_FILE);
    }


    /**
     * harvester description
     *
     * @return harvester description
     */
    public String getDescription() {
        return "CMIP5 Model File";
    }

    /**
     * Check for a .properties file that corresponds to the given data file.
     * If it exists than add it to the entry
     *
     * @param fileInfo File information
     * @param originalFile Data file
     * @param entry New entry
     *
     * @return The entry
     */
    @Override
    public Entry initializeNewEntry(HarvesterFile fileInfo, File originalFile,
                                    Entry entry) {
        try {
            if (entry.getTypeHandler() instanceof CMIP5ModelFileTypeHandler) {
                //jeffmc: comment this out. initNewEntry should be called now
                //                ((CMIP5ModelFileTypeHandler) entry.getTypeHandler())
                //                    .initializeEntry(entry);
            }

            return entry;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Should this harvester harvest the given file
     *
     * @param fileInfo file information
     * @param f the actual file
     * @param matcher pattern matcher
     * @param originalFile  the original file
     * @param entry   the Entry
     *
     * @return the new entry or null if nothing is harvested
     *
     * @throws Exception on badness
     */
    @Override
    public Entry harvestFile(HarvesterFile fileInfo, File f, Matcher matcher,Hashtable<String,Entry> entriesMap)
            throws Exception {
        if ( !f.toString().endsWith(".nc")) {
            return null;
        }

        return super.harvestFile(fileInfo, f, matcher, entriesMap);
    }


}
