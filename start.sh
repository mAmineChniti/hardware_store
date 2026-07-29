#!/bin/sh
if [ -z "$SPRING_DATASOURCE_URL" ] && [ -n "$DB_HOSTNAME" ] && [ -n "$DB_PORT" ] && [ -n "$DB_NAME" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOSTNAME}:${DB_PORT}/${DB_NAME}"
fi
exec java -jar app.jar
