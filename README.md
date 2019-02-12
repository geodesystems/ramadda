


RAMADDA is a freely available content and data management platform that is simple to install and use - in the cloud or even on your laptop. With its open and extensible framework, RAMADDA provides a place for all of your documents, data and digital stuff.

See the main site [here](https://geodesystems.com).
Check out the [Boulder Data Hub](https://boulderdata.org).
Here are some [examples](https://geodesystems.com/repository/alias/example_charts) of what you can do with RAMADDA:

<center><img src=content/gallery.png width="80%"></center>


# Download
You can build from source (below) or download prebuilt versions of RAMADDA at 
[https://geodesystems.com/repository/alias/release/](https://geodesystems.com/repository/alias/release/)


# Building


To build RAMADDA run:
ant

This builds:
dist/repository.war  - For Tomcat
dist/ramadda<version>.zip  - Stand-alone release

The war is created with the "repository" name so the context path for tomcat
is /repository


# Running stand-alone

You can run RAMADDA stand-alone from the source tree. After building do:
   cd  dist/ramadda<version>/
   sh ramadda.sh

You can also run from your classpath.

Add:
classpath="<ramadda dir>/src:<ramadda dir>/lib"
and run:
java -XX:MaxPermSize=256m -Xmx2048m org.ramadda.repository.server.JettyServer




# Plugins
To build the ramadda.org plugins run:
ant plugins
This compiles all of the released plugins and installs them in your local
~/.ramadda/plugins
directory

You can build the individual plugins from their build.xml in their own directory, e.g.:
    cd src/org/ramadda/geodata/data
    ant

The user guide and the workshop plugins use a tcl script from the IDV source release.
We have a copy of that in bin/idvdocgen. This relies on having tclsh in your path. 
If you don't have this then you can either define the path to tclsh in the build.properties
file or when you run ant do:

     ant -Dtclsh=<path to tclsh>



# Making a release

Just do: 
ant release

This does:
ant purge;  //does a clean and deletes  the dist directory.
ant plugins; //builds most of the plugins and makes the allplugins.zip file
ant ramadda

The allplugins.zip gets copied into the
ramadda/repository/resources/plugins
dir and is included in the ramadda release.

The result of the release target is:
dist/repository.war  - The war to be used by Tomcat
dist/ramadda<version>.zip  - The zip file that holds the stand-alone RAMADDA release (which uses Jetty)
dist/repositoryclient.jar  - Used by 3rd party clients (e.g., IDV, JGRASS) 
dist/repositoryclient.zip  - To run the stand-alone command line client




# RAMADDA Source  Tree

src:
The main source of RAMADDA is in src/org/ramadda/repository
There is a build.xml there that does all of the building. The top level
build.xml here can be used to build ramadda and the plugins

The plugins are in
src/org/ramadda/plugins
src/org/ramadda/geodata

lib:
Contains all of the jars RAMADDA depends on.

bin:
Contains a copy of the RAMADDA's document generation package.

dist:
This directory is  created during the build process. All build products
get placed there.



