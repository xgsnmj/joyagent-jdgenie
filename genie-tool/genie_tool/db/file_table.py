# -*- coding: utf-8 -*-
# =====================
#
#
# Author: liumin.423
# Date:   2025/7/9
# =====================
from datetime import datetime
from typing import Optional

from sqlalchemy import DateTime, text
from sqlmodel import SQLModel, Field


class FileInfo(SQLModel, table=True):
    """
    文件信息表
    用于存储genie-tool上传的文件元数据，包括搜索结果、生成的报告等
    """
    __tablename__ = "file_info"  # 明确指定表名

    id: int | None = Field(default=None, primary_key=True)
    file_id: str = Field(sa_column_kwargs={"unique": True}, max_length=64, nullable=False)
    filename: str = Field(max_length=255, nullable=False)
    file_path: str = Field(max_length=500, nullable=False)
    description: Optional[str] = Field(default=None, max_length=1000)
    file_size: Optional[int] = Field(default=None)
    status: int = Field(default=0)  # 0-正常 1-已删除
    request_id: Optional[str] = Field(default=None, max_length=200)
    create_time: Optional[datetime] = Field(
        sa_type=DateTime(timezone=False),
        default=None,
        nullable=False,
        sa_column_kwargs={"server_default": text("CURRENT_TIMESTAMP")}
    )