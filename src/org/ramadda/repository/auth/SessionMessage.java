/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.auth;

import org.ramadda.repository.*;

import org.ramadda.repository.database.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;

import ucar.unidata.util.Cache;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

@SuppressWarnings("unchecked")
public class SessionMessage {
    boolean sticky =false;
    Object key;
    String message;

    public SessionMessage(String message,Object key) {
	this.message = message;
	this.key = key;
    }
    
    public boolean match(Object k) {
	return Misc.equals(k,key);
    }
    
    public  void setMessage(String message) {
	this.message=message;
    }

    public String toString() {
	return message;
    }
}

