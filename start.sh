#!/bin/sh
echo "DB_HOSTNAME=$DB_HOSTNAME"
echo "DB_PORT=$DB_PORT"
echo "DB_NAME=$DB_NAME"
unset SPRING_DATASOURCE_URL
if [ -n "$DB_HOSTNAME" ] && [ -n "$DB_PORT" ] && [ -n "$DB_NAME" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOSTNAME}:${DB_PORT}/${DB_NAME}"
fi
echo "SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL"
exec java -jar app.jar
