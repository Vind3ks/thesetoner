#!/bin/sh
set -e
DATA_DIR="${DATA_DIR:-/data}"
mkdir -p "$DATA_DIR"
# Seed any list files the volume doesn't have yet; never overwrite the bot's own data.
for f in /app/seed/*; do
  name=$(basename "$f")
  [ -e "$DATA_DIR/$name" ] || cp "$f" "$DATA_DIR/$name"
done
cd "$DATA_DIR"
exec java ${JAVA_OPTS:--Xmx256m} -jar /app/thesetoner.jar
