REM To run RAMADDA stand-alone just do

java -Xmx2056m  -Dfile.encoding=utf-8  -jar lib/@REPOSITORYJAR@ -port 8080 %*

REM This will create a directory under ~/.ramadda to store content and the database
REM To change the directory do:
REM java -Xmx2056m  -jar lib/@REPOSITORYJAR@ -port 8080 -Dramadda_home=/some/other/directory


REM The default above is to use Java Derby as the database
REM To run with mysql you do:
REM java -Xmx2056m  -jar @REPOSITORYJAR@ -Dramadda.db=mysql


 









