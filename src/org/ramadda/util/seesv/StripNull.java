/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

import java.io.*;

/**
 * Class description
 *
 *
 * @version        $version$, Tue, Sep 20, '16
 * @author         Enter your name here...
 */
public class StripNull {

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(args[0])));
        BufferedWriter writer =
            new BufferedWriter(new OutputStreamWriter(System.out));
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = reader.read();
            if (c == -1) {
                break;
            }
            if (c == 0x00) {
                while (c == 0x00) {
                    c = reader.read();
                    if (c != 0x00) {
                        System.err.println("Stripped nulls");
                        writer.write('\n');
                    }
                }
            }
            writer.write((char) c);
        }
        writer.flush();
    }

}
