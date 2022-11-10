/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
// Copyright (c) 2008-2021 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0

package org.ramadda.util;


import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Dictionary;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 */

public class PatternProps {

    /**  */
    private Dictionary<String, String> props;

    /**  */
    private List<Pattern> patterns = new ArrayList<Pattern>();

    /**  */
    private List<String> strings = new ArrayList<String>();


    /**
     *
     *
     * @param props _more_
     */
    public PatternProps(Dictionary<String, String> props) {
        this.props = props;
        for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            if (StringUtil.containsRegExp(key)) {
                String value = props.get(key);
                patterns.add(Pattern.compile(key, Pattern.MULTILINE));
                strings.add(value);
            }
        }
    }

    /**
     *
     * @param key _more_
     *  @return _more_
     */
    public String get(String key) {
        for (int i = 0; i < patterns.size(); i++) {
            Matcher matcher = patterns.get(i).matcher(key);
            if (matcher.find()) {
                String v = strings.get(i);
                for (int groupIdx = 0; groupIdx < matcher.groupCount();
                        groupIdx++) {
                    v = v.replace("$" + (groupIdx + 1) + "",
                                  matcher.group(groupIdx + 1));
                }

                return v;
            }
        }

        return props.get(key);
    }


}
