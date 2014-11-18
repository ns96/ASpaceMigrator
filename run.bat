@ECHO OFF
REM simple script to run the ASpaceMigrator program
java -Xmx512m -Dfile.encoding=UTF-8 -cp "lib/*" org.nyu.edu.dlts.dbCopyFrame
