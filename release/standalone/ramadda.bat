:: Note: We are not windows developers so please pardon our dust
:: Consult the README for more information
:: This script uses the environment variables: JAVA_HOME, RAMADDA_HOME and  RAMADDA_PORT

:: JAVA_HOME should point to where you have Java installed where the java executable is at:
:: %JAVA_HOME%\bin\java
:: The default is:
:: JAVA_HOME=C:\Program Files\Java\jre1.8.0_321


:: To change the RAMADDA home directory set the environment variable
:: RAMADDA_HOME=C:\path\to\ramadda\home

:: The RAMADDA_PORT environment variable is the default port for http access
:: this defaults to 80


@set mypath=%~dp0

@if "%JAVA_HOME%"=="" (
@echo No JAVA_HOME environment variable set 
@echo Defaulting to C:\Program Files\Java\jre1.8.0_321
@set JAVA_HOME=C:\Program Files\Java\jre1.8.0_321
)

@if "%RAMADDA_HOME%"=="" (
@set RAMADDA_HOME=default
)

@if "%RAMADDA_PORT%"=="" (
@set RAMADDA_PORT=80
)


@set java=%JAVA_HOME%\bin\java

:: The -Xmx2056m is to start up Java with 2 GB of memory
:: The -jar lib/ramadda.jar is relative to this folder  and is the main entry point for RAMADDA
:: The -port specifies the http port
:: Go to http://localhost:<PORT> (or http://127.0.0.1:<PORT>) to finish the installation
@set ramaddajar=%mypath%lib\ramadda.jar
"%java%"  -Xmx2056m  -Dramadda_home="%RAMADDA_HOME%"  -Dfile.encoding=utf-8  -jar "%ramaddajar%"  -port "%RAMADDA_PORT%" %*





 









