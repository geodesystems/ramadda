/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Sun, Aug 7, '22
 * @author         Enter your name here...    
 */
@SuppressWarnings("unchecked")
public abstract class FileWrapper {

    /**  */
    private     String path;

    /**  */
    private     String name;

    /**  */
    private     boolean isDirectory;

    /**  */
    private     long length;

    /**  */
    private     long lastModified;

    /**  */
    private     long date;

    private int level = 0;

    /**  */
    private FileWrapper[] files;

    /**
     
     */
    public FileWrapper() {}

    /**
     
     *
     * @param path _more_
     * @param name _more_
     * @param isDirectory _more_
     * @param length _more_
     * @param lastModified _more_
     */
    public FileWrapper(String path, String name, boolean isDirectory,
                       long length, long lastModified) {
        init(path, name, isDirectory, length, lastModified);
    }

    public boolean isRemoteFile() {
	return false;
    }

    public void copyFileTo(java.io.File file) throws Exception {
	throw new RuntimeException("copyFileTo not implemented");
    }    

    public static FileWrapper createFileWrapper(String path) {
	return createFileWrapper(path, false);
    }

    public static FileWrapper createFileWrapper(String path, boolean checkForDir) {	
	path = path.trim();
	FileWrapper newFile;
	if(path.startsWith(S3File.S3PREFIX)) {
	    if(checkForDir && !path.endsWith("/")) path +="/";
	    newFile =  new S3File(path);
	} else {
	    newFile = new FileWrapper.File(path);
	}
	return newFile;
    }

    /**
     *
     * @param path _more_
     * @param name _more_
     * @param isDirectory _more_
     * @param length _more_
     * @param lastModified _more_
     */
    public void init(String path, String name, boolean isDirectory,
                     long length, long lastModified) {
        this.path         = path;
        this.name         = name;
        this.isDirectory  = isDirectory;
        this.length       = length;
        this.lastModified = lastModified;
    }

    /**
      * @return _more_
     */
    public String toString() {
        return path;
    }


    public String toStringVerbose() {
	return (isDirectory()?"dir: ":"file: ") + this +" size:" + this.length() +" date:" + new Date(this.lastModified());
    }


    /**
      * @return _more_
     */
    public abstract boolean exists();

    /**
      * @return _more_
     */
    public abstract FileWrapper getParentFile();

    /**
      * @return _more_
     */
    public abstract java.io.File getFile();

    /**
      * @return _more_
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
      * @return _more_
     */
    public long length() {
        return length;
    }

    /**
      * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
      * @return _more_
     */
    public long lastModified() {
        return lastModified;
    }

    public int getLevel() {
	return level;
    }

    /**
      * @return _more_
     */
    public abstract FileWrapper[] doListFiles();

    /**
      * @return _more_
     */
    public FileWrapper[] listFiles() {
        if (files == null) {
            files = doListFiles();
	    if(files!=null) {
		for(FileWrapper child: files)
		    child.level = this.level+1;
	    }
        }

        return files;
    }



    public static boolean walkDirectory(java.io.File dir,
                                        FileViewer fileViewer)
            throws Exception {
	return walkDirectory(new File(dir), fileViewer);
    }


    /**
     *
     * @param dir _more_
     * @param fileViewer _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean walkDirectory(FileWrapper dir,
                                        FileViewer fileViewer)
            throws Exception {
        return walkDirectory(dir, fileViewer, 0);
    }

    /**
     * Walk the dir tree with the given file viewer
     *
     * @param dir dir
     * @param fileViewer viewer
     * @param level tree depth
     *
     * @return should continue
     *
     * @throws Exception on badness_
     */
    public static boolean walkDirectory(FileWrapper parent,
                                        FileViewer fileViewer, 
					int level)
            throws Exception {
	//Don't push the first file
	if(level>0)
	   fileViewer.push(parent);
	boolean r = walkDirectoryInner(parent,fileViewer, level);
	if(level>0)
	    fileViewer.pop();
	return r;
    }

    private static boolean walkDirectoryInner(FileWrapper parent,
					      FileViewer fileViewer, 
					      int level)
	throws Exception {	
        FileWrapper[] children = parent.listFiles();
        if (children == null) {
            return true;
        }
	//For now don't sort
        //children = sortFilesOnName(children);
        for (int i = 0; i < children.length; i++) {
            int what = fileViewer.viewFile(level, children[i], children);
            if (what == FileViewer.DO_STOP) {
                return false;
            }
            if (what == FileViewer.DO_CONTINUE) {
                if ( !walkDirectory(children[i], fileViewer,  level + 1)) {
                    return false;
                }
            }
        }

        return true;
    }



    /**
     * _more_
     *
     * @param files _more_
     *
     * @return _more_
     */
    public static FileWrapper[] sortFilesOnName(FileWrapper[] files) {
        return sortFilesOnName(files, false);
    }

    /**
     * _more_
     *
     * @param files _more_
     * @param descending _more_
     *
     * @return _more_
     */
    public static FileWrapper[] sortFilesOnName(FileWrapper[] files,
						boolean descending) {
        List tuples = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            tuples.add(new Object[] { files[i].getName().toLowerCase(),
                                      files[i] });
        }
        tuples = Misc.sortTuples(tuples, !descending);

        files  = new FileWrapper[tuples.size()];
	for (int i = 0; i < tuples.size(); i++) {
            Object[] tuple = (Object[]) tuples.get(i);
            files[i] = (FileWrapper) tuple[1];
        }

        return files;
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Sun, Aug 7, '22
     * @author         Enter your name here...    
     */
    public static class File extends FileWrapper {

        /**  */
        private java.io.File theFile;

        /**
         
         *
         * @param file _more_
         */
        public File(String file) {
            this(new java.io.File(file));
        }


        /**
         *
         * @param file _more_
         */
        public File(java.io.File file) {
            super(file.getPath(), file.getName(), file.isDirectory(),
                  file.length(), file.lastModified());
            this.theFile = file;
        }

        /**
          * @return _more_
         */
        public FileWrapper[] doListFiles() {
            java.io.File[] files = theFile.listFiles();
            if (files == null) {
                return null;
            }
            FileWrapper[] fw = new FileWrapper[files.length];
            for (int i = 0; i < files.length; i++) {
                fw[i] = new FileWrapper.File(files[i]);
            }

            return fw;
        }

        /**
         *
         * @param o _more_
          * @return _more_
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof File) {
                return this.theFile.equals(((File) o).theFile);
            }

            return false;
        }

	/**
	 * @return _more_
	 */
	@Override
	public long lastModified() {
	    return theFile.lastModified();
	}


	public long length() {
	    return theFile.length();
	}
        /**
          * @return _more_
         */
        @Override
        public FileWrapper getParentFile() {
            return new File(theFile.getParentFile());
        }

        /**
          * @return _more_
         */
        public java.io.File getFile() {
            return theFile;
        }

        /**
          * @return _more_
         */
        @Override
        public int hashCode() {
            return theFile.hashCode();
        }

        /**
          * @return _more_
         */
        public boolean exists() {
            return theFile.exists();
        }
    }


    /**
     * FileViewer  is used to walk dir trees
     */
    public static abstract class FileViewer {
	protected List<FileWrapper> stack = new ArrayList<FileWrapper>();

        /** return action */
        public static int DO_CONTINUE = 1;

        /** return action */
        public static int DO_DONTRECURSE = 2;

        /** return action */
        public static int DO_STOP = 3;

        /**
         * View this file.
         *
         * @param f file
         *
         * @return One of the return actions
         *
         * @throws Exception on badness
         */
        public abstract int viewFile(int level, FileWrapper f,FileWrapper[] children) throws Exception;

	public void push(FileWrapper f) {
	    stack.add(f);
	}
	public FileWrapper  pop() {
	    if(stack.size()>0) {
		FileWrapper f = stack.get(stack.size()-1);
		stack.remove(stack.size()-1);
		return f;
	    }
	    return null;
	}	
    }
    


}
