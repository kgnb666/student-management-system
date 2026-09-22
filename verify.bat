@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title 学生成绩管理系统 - 本地校验

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "ENV_FILE=%ROOT%start.env"

set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=3306"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD="
set "MVN_CMD="

echo.
echo ========================================
echo   本地校验：后端测试+覆盖率 / 前端规范+测试+构建
echo ========================================
echo.

if exist "%ENV_FILE%" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
        set "CFG_KEY=%%A"
        set "CFG_VALUE=%%B"
        if not "!CFG_KEY!"=="" if not "!CFG_VALUE!"=="" set "!CFG_KEY!=!CFG_VALUE!"
    )
)

echo [1/3] 准备后端环境...
call :find_java
if not defined JAVA_EXE (
    echo 没有找到可用的 Java，请安装 JDK 17，或在 start.env 中设置 JAVA_HOME。
    goto :failed
)
set "JAVA_HOME=%JAVA_EXE%\..\.."
for %%I in ("%JAVA_HOME%") do set "JAVA_HOME=%%~fI"
echo 使用 Java：!JAVA_EXE!

if defined MVN_CMD (
    set "MVN=%MVN_CMD%"
) else (
    for /f "delims=" %%M in ('where mvn 2^>nul') do if not defined MVN set "MVN=%%M"
)
if not defined MVN (
    echo 没有找到 Maven。请把 mvn 加入 PATH，或在 start.env 中设置 MVN_CMD。
    goto :failed
)
echo 使用 Maven：!MVN!

echo.
echo [2/3] 后端：mvn verify（含单元测试、集成测试与覆盖率门槛）
pushd "%BACKEND_DIR%"
call "!MVN!" -B verify
set "BACKEND_RESULT=%ERRORLEVEL%"
popd
if not "!BACKEND_RESULT!"=="0" (
    echo 后端校验失败。
    goto :failed
)
echo 后端校验通过。

echo.
echo [3/3] 前端：lint + 单元测试 + 生产构建
pushd "%FRONTEND_DIR%"
call npm run lint
if errorlevel 1 ( popd & echo 前端 lint 失败。 & goto :failed )
call npm run test
if errorlevel 1 ( popd & echo 前端单元测试失败。 & goto :failed )
call npm run build
if errorlevel 1 ( popd & echo 前端构建失败。 & goto :failed )
popd
echo 前端校验通过。

echo.
echo ========================================
echo   全部校验通过
echo ========================================
exit /b 0

:find_java
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
if not defined JAVA_EXE for /d %%J in ("%ProgramFiles%\Java\jdk-17*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
if not defined JAVA_EXE for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
if not defined JAVA_EXE for /d %%J in ("D:\tools\jdk\jdk-17*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
goto :eof

:failed
echo.
echo 校验未通过，请根据上面的提示处理。
pause
exit /b 1
