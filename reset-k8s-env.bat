@echo off
REM ==========================================================================
REM reset-k8s-env.bat — Full reset of the kubernetes-java test environment
REM ==========================================================================
REM
REM What this does:
REM   1. Deletes all kubernetes-java-* minikube profiles
REM   2. Kills all dockerd processes in WSL2
REM   3. Shuts down WSL entirely (clean slate)
REM   4. Removes the ~/.kubernetes-java directory (instances, certs, profiles)
REM
REM Usage:
REM   reset-k8s-env.bat          — Interactive (asks before each step)
REM   reset-k8s-env.bat --force  — No prompts, do everything
REM
REM After running this, the next `gradlew integrationTest` will start fresh.
REM ==========================================================================

setlocal enabledelayedexpansion

set FORCE=0
if "%~1"=="--force" set FORCE=1

set K8S_HOME=%USERPROFILE%\.kubernetes-java
set MINIKUBE_EXE=%K8S_HOME%\bin\minikube.exe

echo.
echo ============================================
echo  kubernetes-java Environment Reset
echo ============================================
echo.

REM --- Step 1: Delete minikube profiles ---
echo [Step 1/4] Deleting kubernetes-java minikube profiles...

if exist "%MINIKUBE_EXE%" (
    REM List all profiles and delete kubernetes-java-* ones
    for /f "tokens=*" %%p in ('"%MINIKUBE_EXE%" profile list --output=json 2^>nul ^| findstr /R "kubernetes-java-"') do (
        echo   Found profile reference, running purge...
    )
    
    REM Use minikube delete with profile pattern — delete all known profiles
    if exist "%K8S_HOME%\profiles" (
        for /d %%d in ("%K8S_HOME%\profiles\kubernetes-java-*") do (
            set PROFILE_NAME=%%~nxd
            echo   Deleting profile: !PROFILE_NAME!
            "%MINIKUBE_EXE%" delete --profile "!PROFILE_NAME!" --purge 2>nul
        )
    )
    echo   Done.
) else (
    echo   No minikube binary found, skipping profile deletion.
)

echo.

REM --- Step 2: Kill dockerd in WSL ---
echo [Step 2/4] Killing Docker daemons in WSL2...

wsl -d Ubuntu -- sudo pkill -9 dockerd 2>nul
if %ERRORLEVEL% EQU 0 (
    echo   Docker daemons killed.
) else (
    echo   No Docker daemons found or WSL not available.
)

echo.

REM --- Step 3: Shutdown WSL ---
echo [Step 3/4] Shutting down WSL...

if %FORCE%==0 (
    set /p CONFIRM="  This will terminate ALL WSL instances. Continue? [y/N] "
    if /i not "!CONFIRM!"=="y" (
        echo   Skipped WSL shutdown.
        goto skip_wsl
    )
)

wsl --shutdown
echo   WSL shut down. It will restart automatically on next use.

:skip_wsl
echo.

REM --- Step 4: Remove kubernetes-java data directory ---
echo [Step 4/4] Removing %K8S_HOME%...

if exist "%K8S_HOME%" (
    if %FORCE%==0 (
        set /p CONFIRM="  This deletes all cached binaries and data. Continue? [y/N] "
        if /i not "!CONFIRM!"=="y" (
            echo   Skipped directory removal.
            goto skip_dir
        )
    )
    
    rmdir /s /q "%K8S_HOME%"
    if exist "%K8S_HOME%" (
        echo   WARNING: Some files could not be deleted (may be locked).
        echo   Try again after WSL fully shuts down.
    ) else (
        echo   Removed successfully.
    )
) else (
    echo   Directory does not exist, nothing to remove.
)

:skip_dir
echo.
echo ============================================
echo  Reset complete. Ready for a fresh test:
echo    gradlew integrationTest
echo ============================================
echo.

endlocal
