/**
    Copyright (c) 2008-2026 Geode Systems LLC
    SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;


public class SeesvException extends RuntimeException {
    private String  message;
    private String  context;

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

