REM To run RAMADDA stand-alone just do


java -Xmx512m  -Dfile.encoding=utf-8  -jar lib/@REPOSITORYJAR@ -port 8080 %*


REM This will create a directory under ~/.ramadda to store content and the database
REM To change the directory do:
REM java -Xmx512m  -jar lib/@REPOSITORYJAR@ -port 8080 -Dramadda_home=/some/other/directory


REM The default above is to use Java Derby as the database
REM To run with mysql you do:
REM java -Xmx512m  -jar @REPOSITORYJAR@ -Dramadda.db=mysql

REM For more information see:
REM http://facdev.unavco.org/repository/help/installing.html

 









