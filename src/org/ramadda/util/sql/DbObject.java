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
            Date date = new Date(date_ts.getTime());
            System.err.println(
                "TS:"
                + new SimpleDateFormat("yyyy-MM-dd HH:mm z").format(date));

            return date;
        }

        return null;
    }



}
