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
