#!/usr/bin/env bash

/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P ${SA_PASSWORD} -Q "CREATE DATABASE [zhicloud_platform];
GO"
/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P ${SA_PASSWORD} -d 'zhicloud_platform' -i /tmp/schema.sql
