
package nom.tam.fits;


import java.util.HashMap;
import java.util.Map;


/**
 * Class description
 *
 *
 * @version        $version$, Thu, Apr 2, '15
 * @author         Enter your name here...
 */
public class HeaderCommentsMap {

    /** _more_ */
    private static Map<String, String> commentMap = new HashMap<String,
                                                        String>();
    static {
        commentMap.put("header:extend:1", "Extensions are permitted");
        commentMap.put("header:simple:1",
                       "Java FITS: " + new java.util.Date());
        commentMap.put("header:xtension:1",
                       "Java FITS: " + new java.util.Date());
        commentMap.put("header:naxis:1", "Dimensionality");
        commentMap.put("header:extend:2", "Extensions are permitted");
        commentMap.put("asciitable:pcount:1", "No group data");
        commentMap.put("asciitable:gcount:1", "One group");
        commentMap.put("asciitable:tfields:1", "Number of fields in table");
        commentMap.put("asciitable:tbcolN:1", "Column offset");
        commentMap.put("asciitable:naxis1:1", "Size of row in bytes");
        commentMap.put("undefineddata:naxis1:1", "Number of Bytes");
        commentMap.put("undefineddata:extend:1", "Extensions are permitted");
        commentMap.put("binarytablehdu:pcount:1", "Includes heap");
        commentMap.put("binarytable:naxis1:1", "Bytes per row");
        commentMap.put("fits:checksum:1",
                       "as of " + FitsDate.getFitsDateString());
        commentMap.put("basichdu:extend:1", "Allow extensions");
        commentMap.put("basichdu:gcount:1", "Required value");
        commentMap.put("basichdu:pcount:1", "Required value");
        commentMap.put("imagedata:extend:1", "Extension permitted");
        commentMap.put("imagedata:pcount:1", "No extra parameters");
        commentMap.put("imagedata:gcount:1", "One group");
        commentMap.put("tablehdu:tfields:1", "Number of table fields");
        /* Null entries:
         *      header:bitpix:1
         *      header:simple:2
         *      header:bitpix:2
         *      header:naxisN:1
         *      header:naxis:2
         *      undefineddata:pcount:1
         *      undefineddata:gcount:1
         *      randomgroupsdata:naxis1:1
         *      randomgroupsdata:naxisN:1
         *      randomgroupsdata:groups:1
         *      randomgroupsdata:gcount:1
         *      randomgroupsdata:pcount:1
         *      binarytablehdu:theap:1
         *      binarytablehdu:tdimN:1
         *      asciitable:tformN:1
         *      asciitablehdu:tnullN:1
         *      asciitablehdu:tfields:1
         *      binarytable:pcount:1
         *      binarytable:gcount:1
         *      binarytable:tfields:1
         *      binarytable:tformN:1
         *      binarytable:tdimN:1
         *      tablehdu:naxis2:1
         */
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public static String getComment(String key) {
        return commentMap.get(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param comment _more_
     */
    public static void updateComment(String key, String comment) {
        commentMap.put(key, comment);
    }

    /**
     * _more_
     *
     * @param key _more_
     */
    public static void deleteComment(String key) {
        commentMap.remove(key);
    }
}
