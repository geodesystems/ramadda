:: Note: We are not windows developers so please pardon our dust
:: Consult the README for more information

:: To change the RAMADDA home directory set the environment variable
:: RAMADDA_HOME=C:\path\to\ramadda\home

:: This script assumes you have the JAVA_HOME environment variable set to
:: where you have Java installed. If you don't have this set then
:: then we this defaults to
:: C:\Program Files\Java\jre1.8.0_321


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





 









