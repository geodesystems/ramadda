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

package org.ramadda.repository;


import org.ramadda.util.HtmlUtils;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class EntryLink {

    /** _more_ */
    private String link;

    /** _more_ */
    private String folderBlock;

    /** _more_ */
    private String uid;

    /**
     * _more_
     *
     * @param link _more_
     * @param folderBlock _more_
     * @param uid _more_
     */
    public EntryLink(String link, String folderBlock, String uid) {
        this.link        = link;
        this.folderBlock = folderBlock;
        this.uid         = uid;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return link + HtmlUtils.br() + folderBlock;
    }

    /**
     *  Set the Link property.
     *
     *  @param value The new value for Link
     */
    public void setLink(String value) {
        link = value;
    }

    /**
     *  Get the Link property.
     *
     *  @return The Link
     */
    public String getLink() {
        return link;
    }

    /**
     *  Set the FolderBlock property.
     *
     *  @param value The new value for FolderBlock
     */
    public void setFolderBlock(String value) {
        folderBlock = value;
    }

    /**
     *  Get the FolderBlock property.
     *
     *  @return The FolderBlock
     */
    public String getFolderBlock() {
        return folderBlock;
    }

    /**
     *  Set the Uid property.
     *
     *  @param value The new value for Uid
     */
    public void setUid(String value) {
        uid = value;
    }

    /**
     *  Get the Uid property.
     *
     *  @return The Uid
     */
    public String getUid() {
        return uid;
    }



}
