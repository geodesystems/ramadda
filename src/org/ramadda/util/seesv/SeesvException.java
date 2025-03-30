/**
    Copyright (c) 2008-2025 Geode Systems LLC
    SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

/**
 * Class description
 *
 *
 * @version        $version$, Wed, Apr 13, '22
 * @author         Enter your name here...    
 */
public class SeesvException extends RuntimeException {
    private String  message;
    private String  context;

    /**
     *
     * @param msg _more_
     */
    public SeesvException(SeesvOperator op, String msg) {
	this.message = msg;
	context = op.getClass().getSimpleName();
    }

    public SeesvException(String msg) {
	this.message = msg;
    }    

    public String getFullMessage() {
	if(context!=null) return context+": " + message;
	return message;
    }

    public String getContext() {
	return context;
    }

    public String getMessage() {
	return message;
    }

}

