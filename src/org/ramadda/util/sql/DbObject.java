/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.sql;


import org.ramadda.util.sql.SqlUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;



/**
 * Class description
 *
 *
 * @version        $version$, Wed, Jun 10, '20
 * @author         Enter your name here...
 */
public class DbObject {

    /** _more_ */
    private static Calendar cal;


    /**
     */
    public DbObject() {}



    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Date getDate(ResultSet results, String col)
            throws Exception {
        if (cal == null) {
            cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        Timestamp date_ts = results.getTimestamp(col, cal);
        if (date_ts != null) {
            return new Date(date_ts.getTime());
        }

        return null;
    }



}
