#!/bin/sh
# Runs the bot and restarts it if it disconnects.
cd "$(dirname "$0")"
while true; do
  sbt run
  echo "Setoner stopped, restarting in 30 seconds (Ctrl+C to quit)..."
  sleep 30
done
