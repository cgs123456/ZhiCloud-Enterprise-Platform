@echo off
rem 前台开发服务器（vite，端口 80 系列见 vite 配置），日志输出到 frontend.log
cd /d D:\Desktop\zhicloud-ui-admin-vue3
call npm run dev > "%~dp0frontend.log" 2>&1
