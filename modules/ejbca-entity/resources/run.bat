@echo off 
REM $Id: run.bat 14103 2012-02-16 09:03:47Z anatom $

REM Memory settings can be specified using parameters like: -Xms20480m -Xmx20480m -XX:MaxPermSize=384m
java -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+ExplicitGCInvokesConcurrent -XX:-UseGCOverheadLimit -Djava.endorsed.dirs=endorsed -jar ejbca-db-cli.jar %1 %2 %3 %4 %5 %6 %7 %8

IF ERRORLEVEL 1 GOTO ERROR

goto END

:ERROR
    echo If you see errors while running the CLI similar to "JDBC Driver class not found" your should copy your JDBC driver JAR to endorsed.

:END
