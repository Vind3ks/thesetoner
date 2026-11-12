@echo off
rem Runs the bot and restarts it if it disconnects.
:loop
call sbt run
echo Setoner stopped, restarting in 30 seconds (Ctrl+C to quit)...
timeout /t 30
goto loop
