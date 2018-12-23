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

package org.ramadda.plugins.chat;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.type.*;


import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 */
public class ChatTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    private ChatOutputHandler chatOutputHandler;

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ChatTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private ChatOutputHandler getChatOutputHandler() {
        if (chatOutputHandler == null) {
            try {
                chatOutputHandler =
                    (ChatOutputHandler) getRepository().getOutputHandler(
                        ChatOutputHandler.OUTPUT_CHATROOM);
            } catch (Exception exc) {
                throw new RuntimeException(exc);

            }
        }

        return chatOutputHandler;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry,
                                 List<Entry> subGroups, List<Entry> entries)
            throws Exception {
        return getChatOutputHandler().outputEntry(request,
                chatOutputHandler.OUTPUT_CHATROOM, entry);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean getForUser() {
        if (getChatOutputHandler() == null) {
            return false;
        }
        if (getChatOutputHandler().getChatPort() > 0) {
            return true;
        }

        return false;
    }


}
