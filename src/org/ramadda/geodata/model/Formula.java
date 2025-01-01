/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.model;


import ucar.unidata.util.StringUtil;

import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Nov 4, '21
 * @author         Enter your name here...
 */
public class Formula {

    /**  */
    String myName;

    /**  */
    String myExp;

    /**
     *
     *
     * @param name _more_
     * @param expression _more_
     */
    public Formula(String name, String expression) {
        myName = name;
        myExp  = expression;
    }

    /**
     *  @return _more_
     */
    public String getName() {
        return myName;
    }

    /**
     *  @return _more_
     */
    public String getExpression() {
        return myExp;
    }

    /**
     * Get the operands for a formula
     *
     * @param exp _more_
     * @return
     */
    public static List<String> getFormulaOperands(String exp) {
        //int colon = formula.indexOf(":");
        //String exp = formula.substring(colon+1);
        int          leftParen  = exp.indexOf("(");
        int          rightParen = exp.indexOf(")");
        String       fops       = exp.substring(leftParen + 1, rightParen);
        List<String> ops        = StringUtil.split(fops, ",");

        return ops;
    }

    /**
     * Get the operands for a formula
     * @param formula
     * @return
     */
    public static List<String> getFormulaOperands(Formula formula) {
        return getFormulaOperands(formula.getExpression());
    }

}
