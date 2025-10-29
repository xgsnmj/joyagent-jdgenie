@echo off
REM 开始启动后端程序
set BASEDIR=.\target\genie-backend
set CLASSPATH=%BASEDIR%\conf\;%BASEDIR%\lib\*
set MAIN_MODULE=com.jd.genie.GenieApplication
set LOGFILE=.\genie-backend_startup.log

echo starting %APP_NAME% :)
start /B java -classpath "%CLASSPATH%" -Dbasedir="%BASEDIR%" -Dfile.encoding="UTF-8" %MAIN_MODULE% > %LOGFILE% 2>&1
