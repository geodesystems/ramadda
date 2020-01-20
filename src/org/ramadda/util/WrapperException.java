/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.util;




/**
 */
public class WrapperException extends Exception {

    /** _more_          */
    private Exception exception;

    /**
     * _more_
     *
     * @param msg _more_
     * @param exc _more_
     */
    public WrapperException(String msg, Exception exc) {
        super(msg);
        this.exception = exc;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Exception getException() {
        return exception;
    }

}
