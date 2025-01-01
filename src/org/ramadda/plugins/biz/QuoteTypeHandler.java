/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.Date;
import java.util.List;
import java.util.regex.*;



/**
 *
 *
 */
public class QuoteTypeHandler extends ExtensibleGroupTypeHandler {


    /** _more_ */
    public static final int IDX_CUSTOMER_NAME = 0;

    /** _more_ */
    public static final int IDX_MATERIAL = 1;

    /** _more_ */
    public static final int IDX_QUANTITY = 2;

    /** _more_ */
    public static final int IDX_UNIT_COST = 3;

    /** _more_ */
    public static final int IDX_AMOUNT = 4;



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public QuoteTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param fromImport _more_
     *
     * @throws Exception _more_
     */
    @Override
    public void initializeNewEntry(Request request, Entry entry,NewType newType)
            throws Exception {
        super.initializeNewEntry(request, entry, newType);
        String   desc       = entry.getDescription();
        String[] descHolder = new String[] { desc };

        Object[] values     = getEntryValues(entry);


        String[] quantityPatterns = { "(?i)(#|quantity)\\s*:\\s*\\$?\\s*(\\d+)" };
        String quantity = findMatch(descHolder, quantityPatterns, 2);
        if (quantity != null) {
            int d = (int) Double.parseDouble(quantity);
            values[IDX_QUANTITY] = Integer.valueOf(d);
        }

        String[] materialPatterns = { "(?i)(mat|material)\\s*:\\s*\\$?\\s*([^$]+)" };
        String material = findMatch(descHolder, materialPatterns, 2);
        if (material != null) {
            values[IDX_MATERIAL] = material;
        }

        String[] costPatterns = { "(?i)(cost|unit)\\s*:\\s*\\$?\\s*(\\d+)" };
        String   cost         = findMatch(descHolder, costPatterns, 2);
        if (cost != null) {
            values[IDX_UNIT_COST] = Double.parseDouble(cost);
        }

        entry.setDescription(descHolder[0]);


    }

    /**
     * _more_
     *
     * @param text _more_
     * @param patterns _more_
     * @param idx _more_
     *
     * @return _more_
     */
    private String findMatch(String[] text, String[] patterns, int idx) {
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text[0]);
            if ( !matcher.find()) {
                continue;
            }
            text[0] = text[0].replaceAll(pattern, "");

            return matcher.group(idx);
        }

        return null;

    }




}
