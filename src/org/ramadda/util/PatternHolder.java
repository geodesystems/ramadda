/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import java.util.regex.*;


import java.util.ArrayList;
import java.util.List;



/**
 * Class description
 *
 * @version        $version$, Wed, Mar 10, '21
 * @author         Enter your name here...
 */
public class PatternHolder {

    /**  */
    private String spattern;

    /**  */
    private Pattern pattern;

    /**  */
    private boolean checkStringIndex = false;


    /**
     * _more_
     *
     * @param spattern _more_
     */
    public PatternHolder(String spattern) {
        this(spattern, false);
    }

    /**
     
     *
     * @param spattern _more_
     * @param checkStringIndex _more_
     */
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

    /**
      * @return _more_
     */
    @Override
    public String toString() {
        return "Pattern:" + spattern;
    }

    /**
     *
     * @param s _more_
      * @return _more_
     */
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
