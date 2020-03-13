package org.ramadda.geodata.model;

import java.util.List;

import ucar.unidata.util.StringUtil;

public class Formula {
    
    String myName;
    String myExp;
    
    public Formula(String name, String expression) {
        myName = name;
        myExp = expression;
    }
    
    public String getName() {
        return myName;
    }
    
    public String getExpression() {
        return myExp;
    }

    /**
     * Get the operands for a formula
     * @param formula
     * @return
     */
    public static List<String> getFormulaOperands(String exp) {
        //int colon = formula.indexOf(":");
        //String exp = formula.substring(colon+1);
        int leftParen = exp.indexOf("(");
        int rightParen = exp.indexOf(")");
        String fops = exp.substring(leftParen+1,rightParen);
        List<String> ops = StringUtil.split(fops, ",");
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
