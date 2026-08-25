@echo off
rem 智云平台 - 后台启动后端（解包目录方式，绕开 Spring Boot 嵌套 jar 在 Windows 上的类路径扫描性能问题）
set REDIS_PASSWORD=redis@2026
set JAVA_HOME=D:\Java\tools\jdk-21.0.11+10
cd /d D:\Desktop\zhicloud\zhicloud-extracted
"%JAVA_HOME%\bin\java.exe" -Xms512m -Xmx3g -Dfile.encoding=UTF-8 -cp "BOOT-INF\classes;BOOT-INF\lib\*" cn.zhicloud.server.ZhiCloudServerApplication --spring.profiles.active=local --spring.flyway.baseline-version=82 > "D:\Desktop\zhicloud\server.log" 2>&1 &
