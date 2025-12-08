/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.util.regex.*;

import java.util.ArrayList;
import java.util.List;

public class PatternHolder {
    private String spattern;
    private Pattern pattern;
    private boolean checkStringIndex = false;

    public PatternHolder(String spattern) {
        this(spattern, false);
    }

    public PatternHolder(String spattern, boolean checkStringIndex) {
        this.checkStringIndex = checkStringIndex;
        this.spattern         = spattern;
        try {
            pattern = Pattern.compile(spattern);
        } catch (Exception exc) {
            System.err.println("PatternHolder: bad pattern:" + spattern);
        }
    }

    public static List<PatternHolder> parseLines(String lines) {
	List<PatternHolder> patterns = new ArrayList<PatternHolder>();
	for(String line: Utils.split(lines,"\n",true,true)) {
	    patterns.add(new PatternHolder(line,true));
	}
	return patterns;
    }

    public static boolean checkPatterns(List<PatternHolder> patterns, String s) {
	for(PatternHolder pattern: patterns) {
	    if(pattern.matches(s)) {
		return true;
	    }
	}
	return false;
    }

    @Override
    public String toString() {
        return "Pattern:" + spattern;
    }

    public boolean matches(String s) {
        if (s == null) {
            return false;
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                return true;
            }
            //TODO: do we always check the string
            if ( !checkStringIndex) {
                return false;
            }
        }

        return s.indexOf(spattern) >= 0;
    }

}
