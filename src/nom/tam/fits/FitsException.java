
package nom.tam.fits;


/*
 * Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 * Many thanks to David Glowacki (U. Wisconsin) for substantial
 * improvements, enhancements and bug fixes.
 */

/**
 * Class description
 *
 *
 * @version        $version$, Thu, Apr 2, '15
 * @author         Enter your name here...
 */
public class FitsException extends Exception {

    /**
     * _more_
     */
    public FitsException() {
        super();
    }

    /**
     * _more_
     *
     * @param msg _more_
     */
    public FitsException(String msg) {
        super(msg);
    }

    /**
     * _more_
     *
     * @param msg _more_
     * @param reason _more_
     */
    public FitsException(String msg, Exception reason) {
        super(msg, reason);
    }
}
