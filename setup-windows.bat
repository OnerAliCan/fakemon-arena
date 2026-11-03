@echo off
rem Met en place le projet Fakemon Arena sur Windows : double-clic, ou depuis un terminal
rem avec des options, par exemple : setup-windows.bat --repo alice/fakemon-arena
setlocal
cd /d "%~dp0"

set "PY="
py -3 --version >nul 2>nul && set "PY=py -3"
if not defined PY (
    python --version >nul 2>nul && set "PY=python"
)
if not defined PY (
    echo Python 3 est introuvable : installation avec winget.
    winget install --id Python.Python.3.12 --exact --source winget
    echo.
    echo Fermez cette fenetre puis relancez setup-windows.bat.
    goto :end
)

%PY% "%~dp0scripts\setup_project.py" %*

:end
echo.
pause
