:: Note: We are not windows developers so please pardon this rather primitive script
:: Consult the README for more information

@set mypath=%~dp0

:: This script assumes you have the JAVA_HOME environment variable set to
:: where you have Java installed. If you don't have this set then either
:: set it or set the absolute path as below
:: For now we are hard coding the path to Java here. Change this to point to your Java install

@if "%JAVA_HOME%"=="" (
@echo No JAVA_HOME environment variable set
@echo Defaulting to C:\Program Files\Java\jre1.8.0_321
@set JAVA_HOME=C:\Program Files\Java\jre1.8.0_321
)

@set java=%JAVA_HOME%\bin\java

:: The -Xmx2056m is to start up Java with 2 GB of memory
:: The -jar lib/ramadda.jar is relative to this folder  and is the main entry point for RAMADDA
:: The -port 80 says to start on port 80
:: Go to http://localhost (or http://127.0.0.1) to finish the installation
@set ramaddajar=%mypath%lib\ramadda.jar
@echo Java version:
"%java%"  -version
"%java%"  -Xmx2056m  -Dfile.encoding=utf-8  -jar "%ramaddajar%"  -port 80 %*

:: To change the home directory add the command line argument:
:: -Dramadda_home=/some/other/directory 
:: The default above is to use Java Derby as the database
:: To run with mysql you add the following to the command line
:: -Dramadda.db=mysql



 









