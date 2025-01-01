/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biblio;



/**
 */
public interface BiblioConstants {


    /** _more_ */
    public static final String TAG_BIBLIO_TYPE = "%0";

    /** _more_ */
    public static final String TAG_BIBLIO_AUTHOR = "%A";

    /** _more_ */
    public static final String TAG_BIBLIO_INSTITUTION = "%I";

    /** _more_ */
    public static final String TAG_BIBLIO_DATE = "%D";

    /** _more_ */
    public static final String TAG_BIBLIO_TAG = "%K";

    /** _more_ */
    public static final String TAG_BIBLIO_TITLE = "%T";

    /** _more_ */
    public static final String TAG_BIBLIO_PUBLICATION = "%J";

    /** _more_ */
    public static final String TAG_BIBLIO_KEYWORD = "%K";

    /** _more_ */
    public static final String TAG_BIBLIO_VOLUME = "%V";

    /** _more_ */
    public static final String TAG_BIBLIO_ISSUE = "%N";

    /** _more_ */
    public static final String TAG_BIBLIO_PAGE = "%P";

    /** _more_ */
    public static final String TAG_BIBLIO_DOI = "%R";

    /** _more_ */
    public static final String TAG_BIBLIO_DESCRIPTION = "%X";

    /** _more_ */
    public static final String TAG_BIBLIO_URL = "%U";

    /** _more_ */
    public static final String TAG_BIBLIO_EXTRA = "%>";


    /** _more_ */
    public static final int IDX_PRIMARY_AUTHOR = 0;

    /** _more_ */
    public static final int IDX_TYPE = 1;

    /** _more_ */
    public static final int IDX_INSTITUTION = 2;

    /** _more_ */
    public static final int IDX_OTHER_AUTHORS = 3;

    /** _more_ */
    public static final int IDX_PUBLICATION = 4;

    /** _more_ */
    public static final int IDX_VOLUME = 5;

    /** _more_ */
    public static final int IDX_ISSUE = 6;

    /** _more_ */
    public static final int IDX_PAGE = 7;

    /** _more_ */
    public static final int IDX_DOI = 8;

    /** _more_ */
    public static final int IDX_LINK = 9;


    /** _more_ */
    public static final String[] TAGS = {
        TAG_BIBLIO_INSTITUTION, TAG_BIBLIO_PUBLICATION, TAG_BIBLIO_VOLUME,
        TAG_BIBLIO_ISSUE, TAG_BIBLIO_PAGE, TAG_BIBLIO_DOI, TAG_BIBLIO_URL,
    };

    /** _more_ */
    public static final int[] INDICES = {
        IDX_INSTITUTION, IDX_PUBLICATION, IDX_VOLUME, IDX_ISSUE, IDX_PAGE,
        IDX_DOI, IDX_LINK,
    };




}
