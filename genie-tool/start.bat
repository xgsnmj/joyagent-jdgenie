@echo off
REM 激活虚拟环境
call .venv\Scripts\activate.bat

REM 运行Python服务器
python server.py
