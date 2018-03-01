/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.data.docs;


import org.ramadda.util.text.Row;
import org.ramadda.util.text.TextReader;




import java.util.List;


/**
 */
public interface TabularVisitor {

    /**
     * _more_
     *
     *
     * @param info _more_
     * @param sheetName _more_
     * @param rows _more_
     *
     * @return _more_
     */
    public boolean visit(TextReader info, String sheetName,
                         List<List<Object>> rows);

}
