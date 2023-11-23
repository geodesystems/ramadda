/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.docs;


import org.ramadda.util.seesv.Row;
import org.ramadda.util.seesv.TextReader;




import java.util.List;


/**
 */
public interface TabularVisitor {

    /**
     * _more_
     *
     *
     * @param info _more_
     * @param sheetName _more_
     * @param rows _more_
     *
     * @return _more_
     */
    public boolean visit(TextReader info, String sheetName,
                         List<List<Object>> rows);

}
