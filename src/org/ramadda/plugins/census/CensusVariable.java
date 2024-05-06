/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.census;


import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Collections;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;





/**
 *     Class description
 *
 *
 *     @version        $version$, Tue, Oct 27, '15
 *     @author         Enter your name here...
 */
@SuppressWarnings("unchecked")
public class CensusVariable implements Comparable, Cloneable {

    public static final int NULL_INDEX=-999;

    /** _more_ */
    public static final String PATTERNS =
        ".*(ancestry|margin of error|hispanic|other race|more races|pacific|latin|puerto|indian|occupation|spanish|white|black|africa|asia).*";

    /** _more_ */
    private static Object MUTEX = new Object();


    /** _more_ */
    private static Hashtable<String, CensusVariable> variableMap;

    /** _more_ */
    private static List<CensusVariable> variables =
        new ArrayList<CensusVariable>();

    /** _more_ */
    private String id;

    /** _more_ */
    private String label;

    private String alias;


    /** _more_ */
    private String conceptId;

    /** _more_ */
    private String concept;

    /** _more_ */
    private int dependsIndex = NULL_INDEX;

    /** _more_ */
    private boolean skip = false;

    /**
     * _more_
     *
     * @param id _more_
     * @param label _more_
     * @param concept _more_
     */
    public CensusVariable(String id, String label, String concept) {
        List<String> toks = StringUtil.split(id, "_");
        this.conceptId = toks.get(0);
        this.concept   = concept.replace(conceptId + ".", "").trim();
        this.id        = id;
        this.label     = label;
    }

    public String toString() {
	return "id:" + id+" label:" + label+" depends:" + dependsIndex;
    }
    /**
     *  @return _more_
     */
    public String getCorpus() {
        return Utils.concatString(id, "-", label, "-", concept).toLowerCase();
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public CensusVariable cloneMe() throws Exception {
        return (CensusVariable) super.clone();
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public int compareTo(Object o) {
        if ( !(o instanceof CensusVariable)) {
            return 0;
        }
        CensusVariable that = (CensusVariable) o;

        return this.id.compareTo(that.id);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static Hashtable<String, CensusVariable> getVariableMap()
            throws Exception {
        if (variableMap == null) {
            //      Runtime.getRuntime().gc();
            double mem1 = Utils.getUsedMemory();

            synchronized (MUTEX) {
                if (variableMap != null) {
                    return variableMap;
                }
                long t1 = System.currentTimeMillis();
                Hashtable<String, CensusVariable> tmp = new Hashtable<String,
                                                            CensusVariable>();
                List<CensusVariable> firstVars =
                    new ArrayList<CensusVariable>();
                List<CensusVariable> lastVars =
                    new ArrayList<CensusVariable>();
                String xml =
                    IOUtil.readContents(
                        "/org/ramadda/plugins/census/resources/variables.xml",
                        AcsFile.class);
                long    t2   = System.currentTimeMillis();
                Element root = XmlUtil.getRoot(xml);
                xml = null;
                long     t3       = System.currentTimeMillis();
                Element  vars     = XmlUtil.findChild(root, "vars");
                NodeList children = XmlUtil.getElements(vars, "var");
                for (int childIdx = 0; childIdx < children.getLength();
                        childIdx++) {
                    Element item   = (Element) children.item(childIdx);
                    String  id     = XmlUtil.getAttribute(item, "xml:id");
                    String  label  = XmlUtil.getAttribute(item, "label");
                    String concept = XmlUtil.getAttribute(item, "concept");
                    CensusVariable var = new CensusVariable(id, label,
                                             concept);
                    tmp.put(var.id, var);
                    if (var.getCorpus().matches(PATTERNS)) {
                        lastVars.add(var);
                    } else {
                        firstVars.add(var);
                    }
                }

                Collections.sort(firstVars);
                Collections.sort(lastVars);
                variables.addAll(firstVars);
                variables.addAll(lastVars);
                variableMap = tmp;
                long t4 = System.currentTimeMillis();
                //            Utils.printTimes("CensusVariable:", t1,t2,t3,t4);
                vars     = null;
                children = null;
                root     = null;
            }
            //      Runtime.getRuntime().gc();
            double mem2 = Utils.getUsedMemory();
            //      System.err.println("census delta:" + Utils.decimals(mem2-mem1,1));
        }

        return variableMap;
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static CensusVariable getVariable(String name) throws Exception {
        return getVariableMap().get(name);
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<CensusVariable> getVariables() throws Exception {
        getVariable("");

        return variables;
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public boolean matches(String... args) {
        for (String s : args) {
            if (id.matches(s) || label.matches(s) || concept.matches(s)) {
                return true;
            }
            if ((id.indexOf(s) >= 0) || (label.indexOf(s) >= 0)
                    || (concept.indexOf(s) >= 0)) {
                return true;
            }
            String corpus = getCorpus();
            if (corpus.matches(s) || (corpus.indexOf(s) >= 0)) {
                return true;
            }
        }

        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        return label;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getConcept() {
        return concept;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getConceptId() {
        return conceptId;
    }


    /**
     * _more_
     *
     * @param a _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] a) throws Exception {
        getVariable("");
    }

    /**
     *  Set the DependsIndex property.
     *
     *  @param value The new value for DependsIndex
     */
    public void setDependsIndex(int value) {
        dependsIndex = value;
    }

    /**
     *  Get the DependsIndex property.
     *
     *  @return The DependsIndex
     */
    public int getDependsIndex() {
        return dependsIndex;
    }


    /**
     *  Set the Skip property.
     *
     *  @param value The new value for Skip
     */
    public void setSkip(boolean value) {
        skip = value;
    }

    /**
     *  Get the Skip property.
     *
     *  @return The Skip
     */
    public boolean getSkip() {
        return skip;
    }

    public void setAlias(String l) {
	alias =l;
    }
    public String getAlias() {
	return alias;
    }    


}
