/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.apache.commons.net.ftp.*;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.*;
import org.apache.http.entity.mime.content.*;
import org.apache.http.entity.ContentType;

import org.w3c.dom.*;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.awt.Image;

import java.io.*;

import java.net.*;

import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

@SuppressWarnings("unchecked")
public class IO {

    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_GET = "GET";    


    /** the file separator id */
    public static final String FILE_SEPARATOR = "_file_";

    /** _more_ */
    public static final String ENTRY_ID_REGEX =
        "[a-f|0-9]{8}-([a-f|0-9]{4}-){3}[a-f|0-9]{12}_";


    /** _more_ */
    private static List<File> okToWriteToDirs = new ArrayList<File>();

    /** _more_ */
    private static List<File> okToReadFromDirs = new ArrayList<File>();



    /** _more_ */
    private static File cacheDir;


    /**
     * Set the cache location. This is used by PhoneUtils for ismobile lookup
     * and by GeoUtils for geocoding
     * This won't set it if it has been set already. Also, it will first check if there
     * is an environment variable RAMADDA_CACHE_DIR and use that
     *
     * @param file _more_
     */
    public static void setCacheDir(File file) {
        if (cacheDir == null) {
            //call getCacheDir which checks the env variable
            cacheDir = getCacheDir();
            if (cacheDir == null) {
                cacheDir = file;
            }
        }
    }

    /**
     * _more_
     *
     * @param file _more_
      * @return _more_
     */
    public static File getCacheDir() {
        if (cacheDir == null) {
            String env = System.getenv("RAMADDA_CACHE_DIR");
            if (env != null) {
                cacheDir = new File(env);
            }
        }

        return cacheDir;
    }


    /**
     *
     * @param filename _more_
      * @return _more_
     */
    public static File getCacheFile(String filename) {
        if (cacheDir == null) {
            throw new IllegalStateException("No Utils.cacheDir defined");
        }

        return new File(cacheDir, filename);
    }


    /**
     * _more_
     *
     * @param files _more_
     */
    public static void addOkToWriteToDirs(List<File> files) {
        synchronized (okToWriteToDirs) {
            for (File f : files) {
                if ( !okToWriteToDirs.contains(f)) {
                    okToWriteToDirs.add(f);
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param files _more_
     */
    public static void addOkToReadFromDirs(List<File> files) {
        synchronized (okToReadFromDirs) {
            for (File f : files) {
                if ( !okToReadFromDirs.contains(f)) {
                    okToReadFromDirs.add(f);
                }
            }
        }
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
            for (File dir : okToWriteToDirs) {
                if (isADescendent(dir, f)) {
                    return true;
                }
            }

            return false;
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
        for (FileChecker checker : fileCheckers) {
            if (checker.canReadFile(file)) {
                return true;
            }
        }

        File f = new File(file);
        if (okToReadFromDirs.size() > 0) {
            boolean ok = false;
            for (File dir : okToReadFromDirs) {
                if (isADescendent(dir, f)) {
                    return true;
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
     *
     * @param filename _more_
     * @param convertZipIfNeeded _more_
     * @return _more_
     *
     * @throws Exception _more_
     * @throws FileNotFoundException _more_
     */
    public static InputStream getInputStream(String filename,
                                             boolean convertZipIfNeeded)
            throws FileNotFoundException, Exception {
        return getInputStream(filename, IO.class, convertZipIfNeeded);
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
        //Check for malformed URL
        if (filename.matches("(?i)^https:/[^/]+.*")) {
            filename = filename.replace("https:/", "https://");
            //      System.err.println("BAD:" + filename);
        }


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
     *
     * @param filename _more_
     * @param origin _more_
     * @param convertZipIfNeeded _more_
     * @return _more_
     *
     * @throws Exception _more_
     * @throws FileNotFoundException _more_
     */
    public static InputStream getInputStream(String filename, Class origin,
                                             boolean convertZipIfNeeded)
            throws FileNotFoundException, Exception {
        InputStream inputStream = getInputStream(filename, origin);
        if (convertZipIfNeeded) {
            return convertInputStream(filename, inputStream);
        }

        return inputStream;
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
        return org.apache.commons.io.IOUtils.toString(is,
                StandardCharsets.UTF_8);
        //        return IOUtil.readContents(is);
    }


    /**
     * _more_
     *
     * @param channel _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String readChannel(ReadableByteChannel channel)
            throws Exception {
        ByteArrayOutputStream bos       = new ByteArrayOutputStream();
        ByteBuffer            buffer    = ByteBuffer.allocate(32000);
        int                   bytesRead = 0;
        while ((bytesRead = channel.read(buffer)) > 0) {
            bos.write(buffer.array(), 0, bytesRead);
            buffer.clear();
        }

        return bos.toString();
    }


    /**
     *
     * @param f _more_
     *
     * @return _more_
     */
    public static final String cleanFileName(String f) {
        if (f == null) {
            return null;
        }
        f = f.replaceAll("[^\\.a-zA-Z_0-9 ]+",
                         "_").trim().replaceAll("\\.\\.+", ".");

        return f;
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
                //                byte[] bytes = IOUtil.readBytes(is);
                //                return ImageIO.read(new ByteArrayInputStream(bytes));
                return ImageIO.read(is);
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
            URL           url              = new URL(filename);
            URLConnection connection       = null;
            boolean       handlingRedirect = false;

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
                        System.err.println("redirect from:" + url);
                        System.err.println("redirect to:" + newUrl);
                        //Don't follow too many redirects
                        if (tries > 10) {
                            throw new IllegalArgumentException(
                                "Too many nested URL fetches:" + filename);
                        }
                        //call this method recursively with the new URL
                        handlingRedirect = true;

                        return doMakeInputStream(newUrl, buffered, tries + 1);
                    }
                }
                //              System.err.println ("OK: " + url);
                is = connection.getInputStream();
            } catch (IOException exc) {
                if (handlingRedirect) {
                    throw exc;
                }
                System.err.println("Error URL: " + filename);
                String msg = "An error has occurred";
                if ((connection != null)
                        && (connection instanceof HttpURLConnection)) {
                    HttpURLConnection huc  = (HttpURLConnection) connection;
                    int               code = huc.getResponseCode();
                    if (code == 403) {
                        msg = "Access forbidden";
                    } else {
                        msg = "Response code: " + code + " ";
                        try {
                            InputStream err = huc.getErrorStream();
                            String response = new String(readBytes(err,
                                                  10000));
                            msg += " Message: " + response;
                        } catch (Exception ignoreIt) {}
                    }
                }

                throw new IOException(msg);
            }
        }

        try {
            is = convertInputStream(filename, is);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
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
     *
     * @param filename _more_
     * @param is _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static InputStream convertInputStream(String filename,
            InputStream is)
            throws Exception {
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

        return is;
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
            URL testUrl = new URL(contentName);
            isUrl = true;
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
                System.err.println("Error reading URL:" + url
                                   + " Error code:" + response + " "
                                   + huc.getResponseMessage());

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
        return doHttpRequest(HTTP_METHOD_POST, url, body, args);
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
            //      System.err.println(args[i]+":" + args[i+1]);
            if (args[i + 1] == null) {
                continue;
            }
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
            System.err.println("IO: error doing http request:" + action
                               + "\nURL:" + url + "\nreturn code:"
                               + connection.getResponseCode() + "\nBody:"
                               + body);
            String error = readError(connection);
            System.err.println(error);
            System.err.println(connection.getHeaderFields());
            exc.printStackTrace();

            throw new RuntimeException("Error reading URL:" + error);
            //            throw exc;
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
        Result result = doGetResult(url, args);
        if (result.error) {
            throw new RuntimeException(result.result);
        }

        return result.result;
    }

    /**
     *
     * @param url _more_
     * @param args _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public static Result doGetResult(URL url, String... args)
            throws Exception {
	return getHttpResult(HTTP_METHOD_GET,url,null, args);
    }

    public static Result getHttpResult(String type, URL url, String body, String... args)
            throws Exception {	

        checkFile(url);
        HttpURLConnection connection =
            (HttpURLConnection) url.openConnection();
	if(type.equals(HTTP_METHOD_POST)) 
	    connection.setDoOutput(true);
        //        connection.setDoInput(true);
        //        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(type);
        //        connection.setRequestProperty("charset", "utf-8");
        //      System.err.println("header:");
        for (int i = 0; i < args.length; i += 2) {
            //            System.err.println(args[i]+":" + args[i+1]);
            connection.setRequestProperty(args[i], args[i + 1]);
        }
        if (body != null) {
            connection.setRequestProperty("Content-Length",
                                          Integer.toString(body.length()));
            connection.getOutputStream().write(body.getBytes("UTF-8"));
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

            return new Result(sb.toString());
        } catch (Throwable exc) {
            String error = readError(connection);
            return new Result(error, connection.getResponseCode(), true, exc);
        }
    }

    public static class FileWrapper {
	File file;
	String name;
	String mimeType;
	public FileWrapper(File file, String name, String mimeType) {
	    this.file = file;
	    this.name = name;
	    this.mimeType = mimeType;
	}

	public String toString() {
	    return "file:" + file +" name:" + name +" mime:" + mimeType;
	}
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Dec 8, '22
     * @author         Enter your name here...    
     */
    public static class Result {

        /**  */
        int code;

        /**  */
        boolean error = false;

        /**  */
        String result;

        /**  */
        Throwable exc;

        /**  */
        InputStream inputStream;

        /**
         
         *
         * @param result _more_
         */
        Result(String result) {
            this.result = result;
        }

        /**
         
         *
         * @param result _more_
         * @param code _more_
         * @param error _more_
         * @param exc _more_
         */
        Result(String result, int code, boolean error, Throwable exc) {
            this.result = result;
            this.code   = code;
            this.error  = error;
            this.exc    = exc;
        }

        /**
         
         *
         * @param inputStream _more_
         */
        Result(InputStream inputStream) {
            this.inputStream = inputStream;
        }

	public String toString() {
	    return result;
	}

        /**
         *  Set the Result property.
         *
         *  @param value The new value for Result
         */
        public void setResult(String value) {
            result = value;
        }

        /**
         *  Get the Result property.
         *
         *  @return The Result
         */
        public String getResult() {
            return result;
        }

        /**
         *  Set the Code property.
         *
         *  @param value The new value for Code
         */
        public void setCode(int value) {
            code = value;
        }

        /**
         *  Get the Code property.
         *
         *  @return The Code
         */
        public int getCode() {
            return code;
        }

        /**
         *  Set the Error property.
         *
         *  @param value The new value for Error
         */
        public void setError(boolean value) {
            error = value;
        }

        /**
         *  Get the Error property.
         *
         *  @return The Error
         */
        public boolean getError() {
            return error;
        }

        /**
         *  Set the InputStream property.
         *
         *  @param value The new value for InputStream
         */
        public void setInputStream(InputStream value) {
            inputStream = value;
        }

        /**
         *  Get the InputStream property.
         *
         *  @return The InputStream
         */
        public InputStream getInputStream() {
            return inputStream;
        }



    }


    /**
     *
     * @param url _more_
     * @param args _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Result getInputStreamFromGet(URL url, String... args)
            throws Exception {
        checkFile(url);

        HttpURLConnection connection =
            (HttpURLConnection) url.openConnection();
        try {
            //        connection.setDoOutput(true);
            //        connection.setDoInput(true);
            //        connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(HTTP_METHOD_GET);
            //        connection.setRequestProperty("charset", "utf-8");
            //      System.err.println("header:");
            for (int i = 0; i < args.length; i += 2) {
                //            System.err.println(args[i]+":" + args[i+1]);
                connection.setRequestProperty(args[i], args[i + 1]);
            }

            return new Result(connection.getInputStream());
        } catch (Throwable exc) {
            String error = readError(connection);
            System.err.println("Error reading URL:" + url + "\ncode:"
                               + connection.getResponseCode());
            System.err.println("Error:" + error);
            System.err.println("Fields:" + connection.getHeaderFields());

            return new Result(error, connection.getResponseCode(), true, exc);
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









    /**  */
    private static List<FileChecker> fileCheckers =
        new ArrayList<FileChecker>();


    /**
     *
     * @param checker _more_
     */
    public static void addFileChecker(FileChecker checker) {
        fileCheckers.add(checker);
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
        if ( !okToReadFrom(file)) {
            throw new RuntimeException("Cannot read file:" + file);
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
         * @return _more_
         */
        public boolean canReadFile(String file);
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


    /**
     * Merge the CSV files
     *
     * @param files files
     * @param out output
     * @param rowSkip _more_
     *
     * @throws Exception On badness
     */
    public static void append(List<String> files, OutputStream out,
                              int rowSkip)
            throws Exception {
        PrintWriter writer    = new PrintWriter(out);
        String      delimiter = ",";
        for (int i = 0; i < files.size(); i++) {
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(files.get(i))));
            int skip = rowSkip;
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (i > 0) {
                    if (skip-- > 0) {
                        continue;
                    }
                }
                writer.print(line);
                writer.print("\n");
                writer.flush();
            }
        }
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public static List<File> getFilelessDirectories(File file) {
        List<File> files    = new ArrayList<File>();
        File[]     children = file.listFiles();
        for (File child : children) {
            if (child.isDirectory()) {
                //              System.err.println("checking:" + child.getName());
                getFilelessDirectories(child, files);
            }
        }

        return files;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param dirs _more_
     *
     * @return _more_
     */
    public static boolean getFilelessDirectories(File file, List<File> dirs) {
        File[]  children            = file.listFiles();
        boolean haveDescendentFiles = false;
        for (File child : children) {
            //      System.err.println("\tchild:" + child.getName());
            if ( !child.isDirectory()) {
                haveDescendentFiles = true;
            } else {
                if ( !getFilelessDirectories(child, dirs)) {
                    haveDescendentFiles = true;
                }
            }
        }
        if ( !haveDescendentFiles) {
            //      System.err.println("\tno descendent files:" + file.getName());
            dirs.add(file);
        }

        return !haveDescendentFiles;
    }


    /**
     *
     * @param parent _more_
     * @param child _more_
     *  @return _more_
     */
    public static boolean isADescendent(File parent, File child) {
        if ((parent == null) || (child == null)) {
            return false;
        }

        try {
            //Convert this to get of "..", etc
            parent = new File(parent.getCanonicalPath());
            child  = new File(child.getCanonicalPath());

            return isADescendentNonCanonical(parent, child);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }

    /**
     *
     * @param parent _more_
     * @param child _more_
     *  @return _more_
     */
    public static boolean isADescendentNonCanonical(File parent, File child) {
        if ((parent == null) || (child == null)) {
            return false;
        }
        //      System.err.println("\tparent: " + parent +" child:" + child);
        if (parent.equals(child)) {
            //      System.err.println("\tparent equals child");
            return true;
        }
        File newParent = child.getParentFile();

        return isADescendentNonCanonical(parent, newParent);
    }


    /** _more_ */
    private static boolean debuggingStderr = false;

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public static void debugStderr() throws Exception {
        if (debuggingStderr) {
            return;
        }
        debuggingStderr = true;
        final PrintStream oldErr = System.err;
        final PrintStream oldOut = System.out;
        System.setErr(new PrintStream(oldOut) {
            @Override
            public void println(Object x) {
                oldErr.println("**************   ERROR\n"
                               + Utils.getStack(10) + "\n************");
                oldErr.println(x);
            }
            @Override
            public void print(Object x) {
                oldErr.println("**************   ERROR\n"
                               + Utils.getStack(10) + "\n************");
                oldErr.print(x);
            }
            @Override
            public void println(boolean x) {
                oldErr.println("**************   ERROR\n"
                               + Utils.getStack(10) + "\n************");
                oldErr.println(x);
            }
            @Override
            public void print(boolean x) {
                oldErr.println("**************   ERROR\n"
                               + Utils.getStack(10) + "\n************");
                oldErr.print(x);
            }

            @Override
            public void println(String x) {
                oldErr.println("**************   ERROR\n"
                               + Utils.getStack(10) + "\n************");
                oldErr.println(x);
            }
            @Override
            public void print(String x) {
                oldErr.println("**************   ERROR\n"
                               + Utils.getStack(10) + "\n************");
                oldErr.print(x);
            }
            @Override
            public void println(int x) {
                oldErr.println("**************   ERROR\n"
                               + Utils.getStack(10) + "\n************");
                oldErr.println(x);
            }
            @Override
            public void print(int x) {
                oldErr.println("**************   ERROR\n"
                               + Utils.getStack(10) + "\n************");
                oldErr.print(x);
            }



        });
    }



    /** _more_ */
    private static boolean debuggingStdout = false;

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public static void debugStdout() throws Exception {
        if (debuggingStdout) {
            return;
        }
        debuggingStdout = true;
        final PrintStream oldErr = System.err;
        final PrintStream oldOut = System.out;
        System.setOut(new PrintStream(oldErr) {
            @Override
            public void print(Object x) {
                oldOut.print("**************   OUT\n" + Utils.getStack(10)
                             + "\n************");
                oldOut.print(x);
            }

            @Override
            public void println(Object x) {
                oldOut.println("**************   OUT\n" + Utils.getStack(10)
                               + "\n************");
                oldOut.println(x);
            }
        });
    }



    /**
     * _more_
     *
     * @param files _more_
     * @param ascending _more_
     *
     * @return _more_
     */
    public static File[] sortFilesOnSize(File[] files,
                                         final boolean ascending) {

        ArrayList<IOUtil.FileWrapper> sorted =
            (ArrayList<IOUtil.FileWrapper>) new ArrayList();

        for (int i = 0; i < files.length; i++) {
            sorted.add(new IOUtil.FileWrapper(files[i], ascending));
        }

        Collections.sort(sorted, new FileSizeCompare(ascending));


        for (int i = 0; i < files.length; i++) {
            files[i] = sorted.get(i).getFile();
        }

        return files;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Apr 12, '14
     * @author         Enter your name here...
     */
    private static class FileSizeCompare implements Comparator<IOUtil.FileWrapper> {

        /** _more_ */
        private boolean ascending;

        /**
         * _more_
         *
         * @param ascending _more_
         */
        public FileSizeCompare(boolean ascending) {
            this.ascending = ascending;
        }

        /**
         * _more_
         *
         * @param o1 _more_
         * @param o2 _more_
         *
         * @return _more_
         */
        public int compare(IOUtil.FileWrapper o1, IOUtil.FileWrapper o2) {
            int result;
            if (o1.length() < o2.length()) {
                result = -1;
            } else if (o1.length() > o2.length()) {
                result = 1;
            } else {
                result = o1.getFile().compareTo(o1.getFile());
            }
            if ( !ascending || (result == 0)) {
                return result;
            }

            return -result;

        }

    }




    /**
     *
     * @param files _more_
     * @param descending _more_
     * @return _more_
     */
    public static File[] sortFilesOnNumber(File[] files,
                                           final boolean descending) {
        List tmp = new ArrayList();
        for (File file : files) {
            String s1 = StringUtil.findPattern(file.getName(), "([0-9]+)");
            if (s1 == null) {
                s1 = "9999";
            }
            double v1 = Double.parseDouble(s1);
            tmp.add(new Object[] { file, v1 });
        }
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Object[] t1     = (Object[]) o1;
                Object[] t2     = (Object[]) o2;
                double   v1     = (double) t1[1];
                double   v2     = (double) t2[1];
                int      result = (v1 < v2)
                                  ? -1
                                  : (v1 == v2)
                                    ? 0
                                    : 1;
                if (descending) {
                    if (result >= 1) {
                        return -1;
                    } else if (result <= -1) {
                        return 1;
                    }

                    return 0;
                }

                return result;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = tmp.toArray();
        Arrays.sort(array, comp);
        List<File> result = new ArrayList<File>();
        for (int i = 0; i < array.length; i++) {
            Object[] tuple = (Object[]) array[i];
            files[i] = (File) tuple[0];
        }

        return files;
    }


    /**
     *
     * @param fileOrUrl _more_
     * @param suffix _more_
      * @return _more_
     */
    public static boolean hasSuffix(String fileOrUrl, String suffix) {
        fileOrUrl = fileOrUrl.toLowerCase();
        if (fileOrUrl.endsWith(suffix)) {
            return true;
        }

        if (fileOrUrl.indexOf(suffix + "?") >= 0) {
            return true;
        }

        if (fileOrUrl.matches(suffix)) {
            return true;
        }

        return false;

    }

    public static Result  doMultipartPost(URL url,String[] requestArgs,List postArgs) throws Exception {
	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	connection.setDoOutput(true);
	connection.setRequestMethod(HTTP_METHOD_POST);
	if(requestArgs!=null) {
	    for (int i = 0; i < requestArgs.length; i += 2) {
		connection.setRequestProperty(requestArgs[i], requestArgs[i + 1]);
	    }
	}

	MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	for(int i=0;i<postArgs.size();i+=2) {
	    String arg = (String) postArgs.get(i);
	    Object value = postArgs.get(i+1);
	    if(value instanceof File) {
		builder.addPart(arg, new FileBody((File) value));
	    } else   if(value instanceof FileWrapper) {
		FileWrapper fw= (FileWrapper) value;
		builder.addPart(arg, new FileBody(fw.file,
						  ContentType.create(fw.mimeType),
						  fw.name));
	    } else {
		builder.addTextBody(arg,value.toString());
	    }
	}
	HttpEntity entity = builder.build();
	connection.setRequestProperty("Content-Type", entity.getContentType().getValue());
	OutputStream out = connection.getOutputStream();
	try {
	    entity.writeTo(out);
	} finally {
	    out.close();
	}

        try {
	    InputStream is = connection.getInputStream();
            return new Result(readInputStream(is));
        } catch (Throwable exc) {
            String error = readError(connection);
            return new Result(error, connection.getResponseCode(), true, exc);
        }
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
	if(true) {
	    //	    args = new String[]{"Authorization"," Bearer openai key"};
	    List postArgs   =new ArrayList();
	    //	    Utils.add(postArgs,"audio-file",new File("/Users/jeffmc/test.webm"));
	    Utils.add(postArgs, "model","whisper-1","file", new File("/Users/jeffmc/test.webm"));
	    //	    URL url = new URL("http://localhost:8080/repository/gpt/transcribe");
	    URL url = new URL("http://localhost:8080/repository/gpt/transcribe");
	    url = new URL("https://api.openai.com/v1/audio/transcriptions");
	    Result result =  doMultipartPost(url, args,postArgs);
	    System.err.println("R:" + result);
	    return;
	}


        for (String f : args) {
            System.err.println("f:" + f);
            getInputStream(f);
            System.err.println("ok");
        }
        if (true) {
            return;
        }

        final PipedOutputStream pos       = new PipedOutputStream();
        final PipedInputStream  pis       = new PipedInputStream(pos);
        final boolean           running[] = { true };


        ucar.unidata.util.Misc.run(new Runnable() {
            public void run() {
                try {
                    PrintWriter pw = new PrintWriter(pos);
                    for (int i = 0; i < 10; i++) {
                        pw.println("LINE:" + i);
                        pw.flush();
                        ucar.unidata.util.Misc.sleep(500);
                    }
                    System.err.println("done writing");
                    pos.close();
                } catch (Exception exc) {
                    System.err.println("write err:" + exc);
                    exc.printStackTrace();
                }
            }
        });

        ucar.unidata.util.Misc.run(new Runnable() {
            public void run() {
                try {
                    InputStreamReader isr =
                        new InputStreamReader(pis,
                            java.nio.charset.StandardCharsets.UTF_8);
                    BufferedReader reader = new BufferedReader(isr);
                    String         line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("read:" + line);
                    }
                    running[0] = false;
                    System.err.println("read: done");
                } catch (Exception exc) {
                    System.err.println("write err:" + exc);
                    exc.printStackTrace();
                }
            }
        });


        while (running[0]) {
            ucar.unidata.util.Misc.sleepSeconds(10);
        }
        Utils.exitTest(0);

        if (true) {
            String url =
                "https://thredds.ucar.edu/thredds/ncss/grib/NCEP/GFS/Global_onedeg/Best?var=Temperature_surface&var=Visibility_surface&var=Water_equivalent_of_accumulated_snow_depth_surface&var=Wind_speed_gust_surface&latitude=%24%7Blatitude%7D&longitude=%24%7Blongitude%7D&time_start=2021-03-30&time_end=2021-04-09&vertCoord=&accept=csv";
            doMakeInputStream(url, false);

            return;
        }



        for (String f : args) {
            System.err.println("F:" + f + " childless:"
                               + getFilelessDirectories(new File(f)));
        }
    }




}
