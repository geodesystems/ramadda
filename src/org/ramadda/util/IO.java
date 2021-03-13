/*
* Copyright (c) 2008-2021 Geode Systems LLC
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

package org.ramadda.util;


import org.apache.commons.net.ftp.*;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;

import java.awt.Image;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.*;


/**
 * A collection of utilities for IO
 *
 * @author Jeff McWhirter
 */

public class IO {

    /** the file separator id */
    public static final String FILE_SEPARATOR = "_file_";

    /** _more_ */
    public static final String ENTRY_ID_REGEX =
        "[a-f|0-9]{8}-([a-f|0-9]{4}-){3}[a-f|0-9]{12}_";


    /** _more_ */
    private static List<File> okToWriteToDirs = new ArrayList<File>();

    /** _more_ */
    private static List<File> okToReadFromDirs = new ArrayList<File>();


    /**
     * _more_
     *
     * @param files _more_
     */
    public static void setOkToWriteToDirs(List<File> files) {
        okToWriteToDirs = files;
    }

    /**
     * _more_
     *
     * @param files _more_
     */
    public static void setOkToReadFromDirs(List<File> files) {
        okToReadFromDirs = files;
    }



    /**
     *  Check if this is an OK path to write to
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean okToWriteTo(String file) throws Exception {
        File f = new File(file);
        if (okToWriteToDirs.size() > 0) {
            boolean ok = false;
            for (File dir : okToWriteToDirs) {
                if (IOUtil.isADescendent(dir, f)) {
                    ok = true;
                }
            }

            return ok;
        }

        return true;
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public static boolean okToReadFrom(String file) {
        File f = new File(file);
        if (okToReadFromDirs.size() > 0) {
            boolean ok = false;
            for (File dir : okToReadFromDirs) {
                if (IOUtil.isADescendent(dir, f)) {
                    ok = true;
                }
            }

            return ok;
        }

        return true;
    }


    /**
     * _more_
     *
     * @param filename _more_
     * @return _more_
     *
     *
     * @throws Exception _more_
     * @throws FileNotFoundException _more_
     */
    public static InputStream getInputStream(String filename)
            throws FileNotFoundException, Exception {
        return getInputStream(filename, IO.class);
    }



    /**
     * _more_
     *
     * @param filename _more_
     * @param origin _more_
     *
     * @return _more_
     *
     *
     * @throws Exception _more_
     * @throws FileNotFoundException _more_
     */
    public static InputStream getInputStream(String filename, Class origin)
            throws FileNotFoundException, Exception {
        checkFile(filename);
        File f = new File(filename);
        if (f.exists()) {
            return new FileInputStream(f);
        }

        try {
            URL url = new URL(filename);

            return getInputStream(url);
        } catch (java.net.MalformedURLException exc) {}

        return IOUtil.getInputStream(filename, origin);
    }


    /**
     * _more_
     *
     * @param is _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public static String readInputStream(InputStream is) throws IOException {
        return IOUtil.readContents(is);
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public static Image readImage(String file) {

        if (file == null) {
            return null;
        }
        try {
            InputStream is = Utils.getInputStream(file, Utils.class);
            if (is != null) {
                byte[] bytes = IOUtil.readBytes(is);

                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
            System.err.println("Could not read image:" + file);
        } catch (Exception exc) {
            System.err.println(exc + " getting image:  " + file);

            return null;
        }

        return null;
    }





    /**
     * _more_
     *
     * @param filename _more_
     * @param buffered _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public static InputStream doMakeInputStream(String filename,
            boolean buffered)
            throws IOException {
        return doMakeInputStream(filename, buffered, 0);
    }


    /**
     * _more_
     *
     * @param filename _more_
     * @param buffered _more_
     * @param tries _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    private static InputStream doMakeInputStream(String filename,
            boolean buffered, int tries)
            throws IOException {
        checkFile(filename);
        int         size = 8000;
        InputStream is   = null;
        if (new File(filename).exists()) {
            is = new FileInputStream(filename);
        } else {
            //Try it as a url
            if (filename.startsWith("//")) {
                filename = "https:" + filename;
            }
            URL           url        = new URL(filename);
            URLConnection connection = null;
            try {
                //              System.err.println ("URL: " + url);
                connection = url.openConnection();
                connection.addRequestProperty("Accept", "*/*");
                connection.addRequestProperty("Host", url.getHost());
                connection.addRequestProperty("User-Agent", "ramadda");


                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection huc = (HttpURLConnection) connection;
                    int               response = huc.getResponseCode();
                    //Check for redirect
                    if ((response == HttpURLConnection
                            .HTTP_MOVED_TEMP) || (response == HttpURLConnection
                            .HTTP_MOVED_PERM) || (response == HttpURLConnection
                            .HTTP_SEE_OTHER)) {
                        String newUrl = connection.getHeaderField("Location");
                        //                      System.err.println("redirect:" + newUrl);
                        //Don't follow too many redirects
                        if (tries > 10) {
                            throw new IllegalArgumentException(
                                "Too many nested URL fetches:" + filename);
                        }

                        //call this method recursively with the new URL
                        return doMakeInputStream(newUrl, buffered, tries + 1);
                    }
                }
                //              System.err.println ("OK: " + url);
                is = connection.getInputStream();
            } catch (Exception exc) {
                System.err.println("Error URL: " + filename);
                String msg = "An error has occurred";
                if ((connection != null)
                        && (connection instanceof HttpURLConnection)) {
                    HttpURLConnection huc = (HttpURLConnection) connection;
                    msg = "Response code: " + huc.getResponseCode() + " ";
                    try {
                        InputStream err = huc.getErrorStream();
                        msg += " Message: "
                               + new String(readBytes(err, 10000));
                    } catch (Exception ignoreIt) {}
                }

                throw new IOException(msg);
            }
        }

        if (filename.toLowerCase().endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }

        if (filename.toLowerCase().endsWith(".zip")) {
            ZipEntry       ze  = null;
            ZipInputStream zin = new ZipInputStream(is);
            //Read into the zip stream to the first entry
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }

                break;
                /*                String path = ze.getName();
                                  if(path.toLowerCase().endsWith(".las")) {
                                  break;
                                  }
                */
            }
            is = zin;
        }


        if ( !buffered) {
            //            System.err.println("not buffered");
            //            return is;
            //            size = 8*3;
        }

        if (buffered) {
            size = 1000000;
        }

        //        System.err.println("buffer size:" + size);
        return new BufferedInputStream(is, size);
    }



    /**
     * _more_
     *
     * @param is _more_
     * @param maxSize _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public static byte[] readBytes(InputStream is, int maxSize)
            throws IOException {
        int    totalRead = 0;
        byte[] content   = new byte[100000];
        try {
            while (true) {
                int howMany = is.read(content, totalRead,
                                      content.length - totalRead);
                if (howMany < 0) {
                    break;
                }
                if (howMany == 0) {
                    continue;
                }
                totalRead += howMany;
                if (totalRead >= content.length) {
                    byte[] tmp       = content;
                    int    newLength = ((content.length < 25000000)
                                        ? content.length * 2
                                        : content.length + 5000000);
                    content = new byte[newLength];
                    System.arraycopy(tmp, 0, content, 0, totalRead);
                }
                if ((maxSize >= 0) && (totalRead >= maxSize)) {
                    break;
                }
            }
        } finally {
            IOUtil.close(is);
        }
        byte[] results = new byte[totalRead];
        System.arraycopy(content, 0, results, 0, totalRead);

        return results;
    }



    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FTPClient makeFTPClient(URL url) throws Exception {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(url.getHost());
        ftpClient.login("anonymous", "");
        int reply = ftpClient.getReplyCode();
        if ( !FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            System.err.println("FTP server refused connection.");

            return null;
        }
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();

        return ftpClient;
    }


    /**
     * _more_
     *
     * @param ftpClient _more_
     */
    public static void closeConnection(FTPClient ftpClient) {
        try {
            ftpClient.logout();
        } catch (Exception exc) {}
        try {
            ftpClient.disconnect();
        } catch (Exception exc) {}
    }



    /**
     * _more_
     *
     * @param url _more_
     * @param os _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean writeFile(URL url, OutputStream os)
            throws Exception {
        if (url.getProtocol().equals("ftp")) {
            FTPClient ftpClient = null;
            try {
                ftpClient = Utils.makeFTPClient(url);
                if (ftpClient == null) {
                    return false;
                }
                if (ftpClient.retrieveFile(url.getPath(), os)) {
                    return true;
                }

                return false;
            } finally {
                closeConnection(ftpClient);
            }
        } else {
            InputStream is = Utils.getInputStream(url.toString(),
                                 Utils.class);
            IOUtil.writeTo(is, os);

            return true;
        }
    }




    /**
     * _more_
     *
     * @param contentName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String readContents(String contentName) throws Exception {
        return readContents(contentName, IO.class);
    }


    /**
     * _more_
     *
     * @param contentName _more_
     * @param clazz _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    public static String readContents(String contentName, Class clazz)
            throws Exception {
        checkFile(contentName);
        boolean isUrl = false;
        try {
            URL testUrl = new URL(contentName);
            isUrl = true;
        } catch (Exception ignoreThis) {}
        if (isUrl) {
            return readUrl(contentName);
        }

        return IOUtil.readContents(contentName, clazz);
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    public static String readContents(File file) throws IOException {
        checkFile(file.toString());

        return IOUtil.readContents(file);
    }

    /**
     * _more_
     *
     * @param contentName _more_
     * @param dflt _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    public static String readContents(String contentName, String dflt)
            throws Exception {
        checkFile(contentName);
        boolean isUrl = false;
        try {
            System.err.println("testing");
            URL testUrl = new URL(contentName);
            isUrl = true;
            System.err.println("OK");
        } catch (Exception ignoreThis) {}
        if (isUrl) {
            return readUrl(contentName);
        }

        return IOUtil.readContents(contentName, dflt);
    }




    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String readUrl(String url) throws Exception {
        checkFile(url);
        URL         u  = new URL(url);
        InputStream is = getInputStream(u);
        String      s  = IOUtil.readContents(is);
        IOUtil.close(is);

        return s;
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String readUrl(URL url) throws Exception {
        String u = url.toString();
        if ( !u.startsWith("http:") && !u.startsWith("https:")) {
            throw new IllegalArgumentException("Bad URL:" + u);
        }
        InputStream is = getInputStream(url);
        String      s  = IOUtil.readContents(is);
        IOUtil.close(is);

        return s;
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static InputStream getInputStream(URL url) throws Exception {
        return getInputStream(url, 0);
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param tries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static InputStream getInputStream(URL url, int tries)
            throws Exception {
        checkFile(url.toString());
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "ramadda");
        connection.setRequestProperty("Host", url.getHost());
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection huc      = (HttpURLConnection) connection;
            int               response = huc.getResponseCode();
            if ((response == HttpURLConnection.HTTP_MOVED_TEMP)
                    || (response == HttpURLConnection.HTTP_MOVED_PERM)
                    || (response == HttpURLConnection.HTTP_SEE_OTHER)) {
                String newUrl = connection.getHeaderField("Location");
		//                System.err.println(newUrl);
                //Don't follow too many redirects
                if (tries > 10) {
                    throw new IllegalArgumentException(
                        "Too many nested URL fetches:" + url);
                }

                //call this method recursively with the new URL
                return getInputStream(new URL(newUrl), tries + 1);
            }
            if ( !("" + response).substring(0, 1).equals("2")) {
                throw new IOException("Error code:" + response + " "
                                      + huc.getResponseMessage());
            }
        }

        return connection.getInputStream();
    }







    /**
     * _more_
     *
     * @param url _more_
     * @param body _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doPost(URL url, String body, String... args)
            throws Exception {
        return doHttpRequest("POST", url, body, args);
    }

    /**
     * _more_
     *
     * @param action _more_
     * @param url _more_
     * @param body _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doHttpRequest(String action, URL url, String body,
                                       String... args)
            throws Exception {
        //        URL url = new URL(request); 
        checkFile(url);
        HttpURLConnection connection =
            (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(action);
        connection.setRequestProperty("charset", "utf-8");
        for (int i = 0; i < args.length; i += 2) {
            //            System.err.println(args[i]+":" + args[i+1]);
            connection.setRequestProperty(args[i], args[i + 1]);
        }
        if (body != null) {
            connection.setRequestProperty("Content-Length",
                                          Integer.toString(body.length()));

            connection.getOutputStream().write(body.getBytes());
        }
        try {
            return readString(
                new BufferedReader(
                    new InputStreamReader(
                        connection.getInputStream(), "UTF-8")));
        } catch (Exception exc) {
            System.err.println("Utils: error doing http request:" + action
                               + "\nURL:" + url + "\nreturn code:"
                               + connection.getResponseCode() + "\nBody:"
                               + body);
            System.err.println(readError(connection));
            System.err.println(connection.getHeaderFields());

            throw exc;
            //            System.err.println(connection.getContent());
        }
    }


    /**
     * _more_
     *
     * @param conn _more_
     *
     * @return _more_
     */
    public static String readError(HttpURLConnection conn) {
        try {
            return readString(
                new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "UTF-8")));

        } catch (Exception exc) {
            return "No error message";
        }
    }

    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String readString(BufferedReader input) throws Exception {
        StringBuilder sb = new StringBuilder();
        String        line;
        while ((line = input.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }




    /**
     * _more_
     *
     * @param url _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doGet(URL url, String... args) throws Exception {
        checkFile(url);
        HttpURLConnection connection =
            (HttpURLConnection) url.openConnection();
        //        connection.setDoOutput(true);
        //        connection.setDoInput(true);
        //        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        //        connection.setRequestProperty("charset", "utf-8");
        for (int i = 0; i < args.length; i += 2) {
            //System.err.println(args[i]+":" + args[i+1]);
            connection.setRequestProperty(args[i], args[i + 1]);
        }
        try {
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        connection.getInputStream(),
                                        "UTF-8"));

            StringBuilder sb = new StringBuilder();
            String        line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception exc) {
            System.err.println("Error:" + connection.getResponseCode());
            System.err.println(connection.getHeaderFields());

            throw exc;
            //            System.err.println(connection.getContent());
        }
    }


    /**
     * This will prune out any leading &lt;unique id&gt;_file_&lt;actual file name&gt;
     *
     * @param fileName the filename
     *
     * @return  the pruned filename
     */
    public static String getFileTail(String fileName) {
        int idx = fileName.indexOf(FILE_SEPARATOR);
        if (idx >= 0) {
            fileName = fileName.substring(idx + FILE_SEPARATOR.length());
        } else {
            /*
              We have this here for files from old versions of RAMADDA where we did
              not add the StorageManager.FILE_SEPARATOR delimiter and it looked something like:
              "62712e31-6123-4474-a96a-5e4edb608fd5_<filename>"
            */
            fileName = fileName.replaceFirst(ENTRY_ID_REGEX, "");
        }

        //Check for Rich's problem
        idx = fileName.lastIndexOf("\\");
        if (idx >= 0) {
            fileName = fileName.substring(idx + 1);
        }
        String tail = IOUtil.getFileTail(fileName);

        return tail;


    }





    /**
     * _more_
     *
     * @param from _more_
     * @param file _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    public static long writeTo(URL from, File file) throws Exception {
        URLConnection    connection = from.openConnection();
        InputStream is = Utils.getInputStream(from.toString(), Utils.class);
        int              length     = connection.getContentLength();
        long             numBytes   = -1;
        FileOutputStream fos        = new FileOutputStream(file);
        try {
            long result = IOUtil.writeTo(is, fos);
            numBytes = result;
        } finally {
            IOUtil.close(fos);
            IOUtil.close(is);
            if (numBytes <= 0) {
                try {
                    file.delete();
                } catch (Exception exc) {}

            }
        }

        return numBytes;
    }


    /**
     * _more_
     *
     * @param inputStream _more_
     */
    public static void close(InputStream inputStream) {
        IOUtil.close(inputStream);
    }








    /** _more_ */
    private static FileChecker fileChecker;

    /**
     * _more_
     *
     * @param checker _more_
     */
    public static void setFileChecker(FileChecker checker) {
        fileChecker = checker;
    }

    /**
     * _more_
     *
     * @param url _more_
     */
    public static void checkFile(URL url) {
        try {
            checkFile(url.toURI().toString());
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param file _more_
     */
    public static void checkFile(String file) {
        if (fileChecker != null) {
            fileChecker.checkFile(file);
        }
    }

    /**
     * Interface description
     *
     *
     * @author         Enter your name here...
     */
    public interface FileChecker {

        /**
         * _more_
         *
         * @param file _more_
         */
        public void checkFile(String file);
    }




    /**
     * Merge each row in the given files out. e.g., if file1 has
     *  1,2,3
     *  4,5,6
     * and file2 has
     * 8,9,10
     * 11,12,13
     * the result would be
     * 1,2,3,8,9,10
     * 4,5,6,11,12,13
     * Gotta figure out how to handle different numbers of rows
     *
     * @param files files
     * @param out output
     *
     * @throws Exception On badness
     */
    public static void concat(List<String> files, OutputStream out)
            throws Exception {
        PrintWriter          writer    = new PrintWriter(out);
        String               delimiter = ",";
        List<BufferedReader> readers   = new ArrayList<BufferedReader>();
        for (String file : files) {
            readers.add(
                new BufferedReader(
                    new InputStreamReader(new FileInputStream(file))));
        }
        while (true) {
            int nullCnt = 0;
            for (int i = 0; i < readers.size(); i++) {
                BufferedReader br   = readers.get(i);
                String         line = br.readLine();
                if (line == null) {
                    nullCnt++;

                    continue;
                }
                if (i > 0) {
                    writer.print(delimiter);
                }
                writer.print(line);
                writer.flush();
            }
            if (nullCnt == readers.size()) {
                break;
            }
            writer.println("");
        }

    }


    private static boolean debuggingStderr = false;

    public static void debugStderr() throws Exception {
	if(debuggingStderr) return;
	debuggingStderr = true;
        final PrintStream oldErr = System.err;
	final PrintStream oldOut = System.out;
	System.setErr(new PrintStream(oldOut){
		public void     println(String x) {
		    new RuntimeException("stderr").printStackTrace();
		    oldErr.println(x);
		}
	    });
    }	



}
