/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

import java.io.*;


public class StripNull {

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
