/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
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

    private String folderClickUrl;

    /**
     * _more_
     *
     * @param link _more_
     * @param folderBlock _more_
     * @param uid _more_
     */
    public EntryLink(String link, String folderBlock, String uid, String folderClickUrl) {
        this.link        = link;
        this.folderBlock = folderBlock;
        this.uid         = uid;
	this.folderClickUrl = folderClickUrl;
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

    /**
       Set the FolderClickUrl property.

       @param value The new value for FolderClickUrl
    **/
    public void setFolderClickUrl (String value) {
	folderClickUrl = value;
    }

    /**
       Get the FolderClickUrl property.

       @return The FolderClickUrl
    **/
    public String getFolderClickUrl () {
	return folderClickUrl;
    }



}
