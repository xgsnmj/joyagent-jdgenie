# -*- coding: utf-8 -*-
# =====================
# 
# 
# Author: liumin.423
# Date:   2025/7/9
# =====================
import os
from typing import Callable, AsyncGenerator

from loguru import logger
from sqlalchemy import AsyncAdaptedQueuePool, create_engine
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlmodel import SQLModel


# MySQL 数据库配置（与 genie-backend 共享数据库）
MYSQL_HOST = os.environ.get("MYSQL_HOST", "192.168.4.227")
MYSQL_PORT = os.environ.get("MYSQL_PORT", "3306")
MYSQL_USER = os.environ.get("MYSQL_USER", "genie")
MYSQL_PASSWORD = os.environ.get("MYSQL_PASSWORD", "whj88364399")
MYSQL_DATABASE = os.environ.get("MYSQL_DATABASE", "genie_db")

# 构建MySQL连接字符串
MYSQL_URL = f"mysql+pymysql://{MYSQL_USER}:{MYSQL_PASSWORD}@{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DATABASE}?charset=utf8mb4"
MYSQL_ASYNC_URL = f"mysql+aiomysql://{MYSQL_USER}:{MYSQL_PASSWORD}@{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DATABASE}?charset=utf8mb4"

# 同步引擎（用于初始化建表）
engine = create_engine(MYSQL_URL, echo=True, pool_pre_ping=True)

# 异步引擎（用于业务操作）
async_engine = create_async_engine(
    MYSQL_ASYNC_URL,
    poolclass=AsyncAdaptedQueuePool,
    pool_size=10,
    pool_recycle=3600,
    pool_pre_ping=True,  # MySQL连接保活，避免连接超时
    echo=False,
)

async_session_local: Callable[..., AsyncSession] = sessionmaker(bind=async_engine, class_=AsyncSession)


async def get_async_session() -> AsyncGenerator[AsyncSession, None]:
    """session生成器 作为fast api的Depends选项"""
    async with async_session_local() as session:
        yield session


def init_db():
    from genie_tool.db.file_table import FileInfo
    SQLModel.metadata.create_all(engine)
    logger.info(f"DB init done")


if __name__ == "__main__":
    init_db()
