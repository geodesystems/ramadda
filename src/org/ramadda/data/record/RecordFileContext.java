/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;


import org.ramadda.data.record.filter.*;


import org.ramadda.util.PropertyProvider;
import org.ramadda.util.Utils;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.*;
import java.util.zip.GZIPInputStream;


/**
 *
 * @author Jeff McWhirter
 */
public interface RecordFileContext {

    /**
     * _more_
     *
     * @param field _more_
     * @param key _more_
     *
     * @return _more_
     */
    public String getFieldProperty(String field, String key);

    /**
     *  @return _more_
     */
    public PropertyProvider getPropertyProvider();
}
