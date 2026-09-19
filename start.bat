@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title 学生成绩管理系统一键启动

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "JAR=%BACKEND_DIR%\target\student-score-1.0.0.jar"
set "MYSQL_PWD=123456"

echo.
echo ========================================
echo   学生成绩管理系统
echo ========================================
echo.

echo [1/4] 检查 MySQL...
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
) else (
    for /f "delims=" %%M in ('where mysql 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%M"
)
if not defined MYSQL_EXE (
    echo 没有找到 MySQL 客户端，请确认 MySQL 已安装。
    goto :failed
)

sc query MySQL80 | findstr /I "RUNNING" >nul
if errorlevel 1 (
    echo MySQL80 未运行，正在尝试启动...
    net start MySQL80 >nul 2>&1
    if errorlevel 1 (
        echo MySQL80 启动失败，请用管理员身份运行本脚本。
        goto :failed
    )
)

set "DB_CHECK=%TEMP%\student_score_db_check.txt"
"%MYSQL_EXE%" -h 127.0.0.1 -P 3306 -u root -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='student_score' AND table_name='student';" >"%DB_CHECK%" 2>nul
set "DB_READY="
if exist "%DB_CHECK%" set /p DB_READY=<"%DB_CHECK%"
del "%DB_CHECK%" >nul 2>&1

if not "%DB_READY%"=="1" (
    echo 正在初始化数据库...
    pushd "%ROOT%sql"
    "%MYSQL_EXE%" -h 127.0.0.1 -P 3306 -u root --default-character-set=utf8mb4 -e "source student_score.sql"
    set "DB_RESULT=%ERRORLEVEL%"
    popd
    if not "!DB_RESULT!"=="0" (
        echo 数据库初始化失败，请检查 MySQL 密码。
        goto :failed
    )
)
echo MySQL 检查完成。
echo.

echo [2/4] 检查后端...
if not exist "%JAR%" (
    where mvn >nul 2>&1
    if errorlevel 1 (
        echo 后端 JAR 不存在，并且系统没有找到 Maven。
        echo 请先用 IntelliJ IDEA 或 Maven 构建 backend。
        goto :failed
    )
    echo 正在构建后端...
    pushd "%BACKEND_DIR%"
    call mvn -DskipTests package
    set "BUILD_RESULT=%ERRORLEVEL%"
    popd
    if not "!BUILD_RESULT!"=="0" goto :failed
)

powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    set "JAVA_EXE="
    for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
    if not defined JAVA_EXE if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    for /d %%J in ("D:\tools\jdk\jdk-*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
    for /d %%J in ("D:\tools\jdk*\jdk-*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
    if not defined JAVA_EXE if exist "%LOCALAPPDATA%\DBeaver\jre\bin\java.exe" set "JAVA_EXE=%LOCALAPPDATA%\DBeaver\jre\bin\java.exe"
    if not defined JAVA_EXE (
        echo 没有找到 Java，请安装 JDK 17 或配置 JAVA_HOME。
        goto :failed
    )

    start "学生成绩管理系统 - 后端" /D "%BACKEND_DIR%" cmd /k ""!JAVA_EXE!" -jar "%JAR%""
    echo 正在等待后端启动...
    powershell -NoProfile -Command "$limit=(Get-Date).AddSeconds(30); while((Get-Date) -lt $limit){ if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Milliseconds 500 }; exit 1" >nul 2>&1
    if errorlevel 1 (
        echo 后端没有在 8080 端口启动，请查看后端窗口中的错误信息。
        goto :failed
    )
) else (
    echo 后端已经在运行。
)
echo 后端启动完成。
echo.

echo [3/4] 检查前端...
if not exist "%FRONTEND_DIR%\node_modules" (
    echo 正在安装前端依赖...
    pushd "%FRONTEND_DIR%"
    call npm install
    set "INSTALL_RESULT=%ERRORLEVEL%"
    popd
    if not "!INSTALL_RESULT!"=="0" goto :failed
)

powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    start "学生成绩管理系统 - 前端" /D "%FRONTEND_DIR%" cmd /k "npm run dev"
    echo 正在等待前端启动...
    powershell -NoProfile -Command "$limit=(Get-Date).AddSeconds(20); while((Get-Date) -lt $limit){ if (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Milliseconds 500 }; exit 1" >nul 2>&1
    if errorlevel 1 (
        echo 前端没有在 5173 端口启动，请查看前端窗口中的错误信息。
        goto :failed
    )
) else (
    echo 前端已经在运行。
)
echo 前端启动完成。
echo.

echo [4/4] 打开浏览器...
start "" "http://localhost:5173"

echo ========================================
echo   启动完成
echo ========================================
echo 管理员：admin / admin123
echo 教师：t001 / 123456
echo 学生：2023001 / 123456
echo.
echo 关闭后端和前端窗口即可停止系统。
powershell -NoProfile -Command "Start-Sleep -Seconds 5" >nul 2>&1
exit /b 0

:failed
echo.
echo 启动失败，请根据上面的提示处理。
pause
exit /b 1
