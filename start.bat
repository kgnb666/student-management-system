@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title 学生成绩管理系统一键启动

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "FRONTEND_DIR=%ROOT%frontend"
set "JAR=%BACKEND_DIR%\target\student-score-1.0.0.jar"
set "ENV_FILE=%ROOT%start.env"

rem ---- 默认配置（可被 start.env 覆盖）----
set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=3306"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD="
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"
set "TOMCAT_PROTOCOL=nio2"

echo.
echo ========================================
echo   学生成绩管理系统
echo ========================================
echo.

if not exist "%ENV_FILE%" (
    if exist "%ROOT%start.env.example" (
        copy /y "%ROOT%start.env.example" "%ENV_FILE%" >nul
        echo 已根据 start.env.example 生成配置文件 start.env。
        echo 如果本机数据库密码不是 123456，请修改 start.env 后重新运行本脚本。
        echo.
    )
)

if exist "%ENV_FILE%" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
        set "CFG_KEY=%%A"
        set "CFG_VALUE=%%B"
        if not "!CFG_KEY!"=="" if not "!CFG_VALUE!"=="" set "!CFG_KEY!=!CFG_VALUE!"
    )
)

if "%MYSQL_PASSWORD%"=="" (
    echo [错误] 未配置数据库密码。
    echo 请在 "%ENV_FILE%" 中设置 MYSQL_PASSWORD 后重新运行。
    goto :failed
)

echo [1/4] 检查 MySQL...
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
) else (
    for /f "delims=" %%M in ('where mysql 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%M"
)
if not defined MYSQL_EXE (
    echo 没有找到 MySQL 客户端，请确认 MySQL 已安装，或把 mysql.exe 所在目录加入 PATH。
    goto :failed
)

sc query MySQL80 | findstr /I "RUNNING" >nul
if errorlevel 1 (
    echo MySQL80 未运行，正在尝试启动...
    net start MySQL80 >nul 2>&1
    if errorlevel 1 (
        echo MySQL80 启动失败，请用管理员身份运行本脚本，或手动启动 MySQL 服务。
        goto :failed
    )
)

set "MYSQL_PWD=%MYSQL_PASSWORD%"
set "DB_CHECK=%TEMP%\student_score_db_check.txt"
"%MYSQL_EXE%" -h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER% -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='student_score' AND table_name='student';" >"%DB_CHECK%" 2>nul
set "DB_READY="
if exist "%DB_CHECK%" set /p DB_READY=<"%DB_CHECK%"
del "%DB_CHECK%" >nul 2>&1

if not "%DB_READY%"=="1" (
    echo 正在初始化数据库...
    pushd "%ROOT%sql"
    "%MYSQL_EXE%" -h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER% --default-character-set=utf8mb4 -e "source student_score.sql"
    set "DB_RESULT=%ERRORLEVEL%"
    popd
    if not "!DB_RESULT!"=="0" (
        echo 数据库初始化失败，请检查 start.env 中的数据库账号密码。
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
        echo 请先用 IntelliJ IDEA 或 Maven 构建 backend 目录。
        goto :failed
    )
    echo 正在构建后端...
    pushd "%BACKEND_DIR%"
    call mvn -DskipTests package
    set "BUILD_RESULT=%ERRORLEVEL%"
    popd
    if not "!BUILD_RESULT!"=="0" goto :failed
)

set "BACKEND_RUNNING="
rem 只看 /actuator/health 会把「其他 Spring Boot 应用」误判成本项目后端，
rem 因此同时检查 /actuator/info 中的应用标识
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:%BACKEND_PORT%/actuator/info' -UseBasicParsing -TimeoutSec 3; if ($r.Content -match 'student-score') { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if not errorlevel 1 set "BACKEND_RUNNING=1"

if defined BACKEND_RUNNING (
    echo 后端已经在 %BACKEND_PORT% 端口运行。
    goto :backend_ready
)

call :ensure_free_port BACKEND_PORT 后端
if not defined BACKEND_PORT goto :failed

call :find_java
if not defined JAVA_EXE (
    echo 没有找到可用的 Java，请安装 JDK 17，或在 start.env 中设置 JAVA_HOME。
    goto :failed
)
echo 使用 Java：!JAVA_EXE!
set "SERVER_PORT=%BACKEND_PORT%"
set "SPRING_DATASOURCE_URL=jdbc:mysql://%MYSQL_HOST%:%MYSQL_PORT%/student_score?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
set "SPRING_DATASOURCE_USERNAME=%MYSQL_USER%"
set "SPRING_DATASOURCE_PASSWORD=%MYSQL_PASSWORD%"
start "学生成绩管理系统 - 后端" /D "%BACKEND_DIR%" cmd /k ""!JAVA_EXE!" -jar "%JAR%""
echo 正在等待后端启动...
powershell -NoProfile -Command "$limit=(Get-Date).AddSeconds(45); while((Get-Date) -lt $limit){ if (Get-NetTCPConnection -LocalPort %BACKEND_PORT% -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Milliseconds 500 }; exit 1" >nul 2>&1
if errorlevel 1 (
    echo 后端没有在 %BACKEND_PORT% 端口启动，请查看后端窗口中的错误信息。
    goto :failed
)

:backend_ready
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

set "FRONTEND_RUNNING="
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:%FRONTEND_PORT%/' -UseBasicParsing -TimeoutSec 3; if ($r.Content -match '学生成绩管理系统') { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if not errorlevel 1 set "FRONTEND_RUNNING=1"

if defined FRONTEND_RUNNING (
    echo 前端已经在 %FRONTEND_PORT% 端口运行。
    goto :frontend_ready
)

call :ensure_free_port FRONTEND_PORT 前端
if not defined FRONTEND_PORT goto :failed

set "VITE_PROXY_TARGET=http://localhost:%BACKEND_PORT%"
start "学生成绩管理系统 - 前端" /D "%FRONTEND_DIR%" cmd /k "npm run dev -- --port %FRONTEND_PORT%"
echo 正在等待前端启动...
powershell -NoProfile -Command "$limit=(Get-Date).AddSeconds(40); while((Get-Date) -lt $limit){ if (Get-NetTCPConnection -LocalPort %FRONTEND_PORT% -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Milliseconds 500 }; exit 1" >nul 2>&1
if errorlevel 1 (
    echo 前端没有在 %FRONTEND_PORT% 端口启动，请查看前端窗口中的错误信息。
    goto :failed
)

:frontend_ready
echo 前端启动完成。
echo.

echo [4/4] 打开浏览器...
start "" "http://localhost:%FRONTEND_PORT%"

echo ========================================
echo   启动完成
echo ========================================
echo 管理员：admin / admin123
echo 教师：t001 / 123456
echo 学生：2023001 / 123456
echo.
echo 关闭后端和前端窗口即可停止系统。
echo 如需修改数据库连接或端口，请编辑 start.env。
powershell -NoProfile -Command "Start-Sleep -Seconds 5" >nul 2>&1
exit /b 0

:find_java
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
if not defined JAVA_EXE for /d %%J in ("%ProgramFiles%\Java\jdk-17*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
if not defined JAVA_EXE for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
if not defined JAVA_EXE for /d %%J in ("D:\tools\jdk\jdk-17*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
if not defined JAVA_EXE for /d %%J in ("%LOCALAPPDATA%\DBeaver\jre*") do if not defined JAVA_EXE if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
goto :eof

:port_in_use
rem 参数：端口号。被占用时设置 PORT_USED=1
set "PORT_USED="
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort %~1 -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if not errorlevel 1 set "PORT_USED=1"
goto :eof

:ensure_free_port
rem 参数1：保存端口的变量名；参数2：用于提示的服务名
call set "CUR_PORT=%%%~1%%"
call :port_in_use !CUR_PORT!
if not defined PORT_USED goto :eof
set "FALLBACK_PORT="
for /l %%P in (1,1,10) do (
    if not defined FALLBACK_PORT (
        set /a "TRY_PORT=!CUR_PORT!+%%P"
        call :port_in_use !TRY_PORT!
        if not defined PORT_USED set "FALLBACK_PORT=!TRY_PORT!"
    )
)
if not defined FALLBACK_PORT (
    echo [错误] 端口 !CUR_PORT! 及其后 10 个端口都被占用，无法启动%~2。
    echo 请关闭占用端口的程序，或在 start.env 中修改端口后重试。
    set "%~1="
    goto :eof
)
echo [提示] 端口 !CUR_PORT! 已被其他程序占用，本次%~2自动改用 !FALLBACK_PORT!。
set "%~1=!FALLBACK_PORT!"
goto :eof

:failed
echo.
echo 启动失败，请根据上面的提示处理。
pause
exit /b 1
