import os
import re
from typing import List

from fastapi import UploadFile
from sqlmodel import select

from genie_tool.db.file_table import FileInfo
from genie_tool.db.db_engine import async_session_local
from genie_tool.util.log_util import timer


def sanitize_path_name(path_name: str) -> str:
    """
    清理路径名中的非法字符，使其在Windows/Linux/Mac系统下都有效

    Windows不允许的字符: < > : " / \ | ? *
    Linux/Mac通常只禁止 / 和 null字符
    将非法字符替换为下划线，确保跨平台兼容性

    Args:
        path_name: 原始路径名（如 "session-123:456"）

    Returns:
        清理后的路径名（如 "session-123_456"）
    """
    if not path_name:
        return 'unnamed'

    # 替换Windows非法字符为下划线
    illegal_chars = r'[<>:"/\\|?*]'
    sanitized = re.sub(illegal_chars, '_', path_name)

    # 移除首尾空格和点号（Windows不允许目录名以点号结尾）
    sanitized = sanitized.strip('. ')

    return sanitized if sanitized else 'unnamed'


class _FileDB(object):
    def __init__(self):
        self._work_dir = os.getenv("FILE_SAVE_PATH", "file_db_dir")
        if not os.path.exists(self._work_dir):
            os.makedirs(self._work_dir)

    async def save(self, file_name, content, scope) -> str:
        """
        保存文件内容到指定scope目录

        Args:
            file_name: 文件名
            content: 文件内容
            scope: 作用域（通常是request_id），会自动清理非法字符

        Returns:
            保存的文件完整路径
        """
        if "." in file_name:
            file_name = os.path.basename(file_name)
        else:
            file_name = f"{file_name}.txt"

        # 清理scope中的非法字符（如冒号），确保Windows系统兼容
        safe_scope = sanitize_path_name(scope)
        save_path = os.path.join(self._work_dir, safe_scope)

        if not os.path.exists(save_path):
            os.makedirs(save_path)

        # 使用utf-8编码，避免中文乱码
        with open(f"{save_path}/{file_name}", "w", encoding='utf-8') as f:
            f.write(content)

        return f"{save_path}/{file_name}"
    
    async def save_by_data(self, file: UploadFile) -> str:
        """
        保存上传的文件数据

        Args:
            file: 上传的文件对象

        Returns:
            保存的文件完整路径
        """
        # 清理文件名中的非法字符
        file_name = sanitize_path_name(file.filename)
        file_data = file.file.read()
        save_path = os.path.join(self._work_dir, file_name)

        with open(save_path, "wb") as f:
            f.write(file_data)

        return save_path


FileDB = _FileDB()


class FileInfoOp(object):

    @classmethod
    @timer()
    async def add_by_content(cls, filename: str, content: str, file_id: str, description: str = None,
                             request_id: str = None) -> FileInfo:
        file_path = await FileDB.save(filename, content, scope=request_id)
        file_info = FileInfo(
            file_id=file_id,
            filename=filename,
            file_path=file_path,
            description=description,
            file_size=os.path.getsize(file_path),
            status=1,
            request_id=request_id
        )
        return await cls.add(file_info)
    
    @staticmethod
    @timer()
    async def add_by_file(file: UploadFile, file_id: str, request_id: str = None) -> FileInfo:
        file_path = await FileDB.save_by_data(file)
        
        file_info = FileInfo(
            file_id=file_id,
            filename=file.filename,
            file_path=file_path,
            description="",
            file_size=os.path.getsize(file_path),
            status=1,
            request_id=request_id
        )
        return await FileInfoOp.add(file_info)

    @staticmethod
    @timer()
    async def add(file_info: FileInfo) -> FileInfo:
        file_id = file_info.file_id
        f = await FileInfoOp.get_by_file_id(file_info.file_id)
        async with async_session_local() as session:
            if f:
                f.status = 1
                f.file_size = file_info.file_size
                session.add(f)
            else:
                session.add(file_info)
            await session.commit()
        return await FileInfoOp.get_by_file_id(file_id)

    @staticmethod
    @timer()
    async def get_by_file_id(file_id: str) -> FileInfo:
        async with async_session_local() as session:
            state = select(FileInfo).where(FileInfo.file_id == file_id)
            result = await session.execute(state)
            return result.scalars().one_or_none()

    @staticmethod
    @timer()
    async def get_by_file_ids(file_ids: List[str]) -> List[FileInfo]:
        async with async_session_local() as session:
            state = select(FileInfo).where(FileInfo.file_id.in_(file_ids))
            result = await session.execute(state)
            return result.scalars().all()

    @staticmethod
    @timer()
    async def get_by_request_id(request_id: str) -> List[FileInfo]:
        async with async_session_local() as session:
            state = select(FileInfo).where(FileInfo.request_id == request_id)
            result = await session.execute(state)
            return result.scalars().all()

def get_file_preview_url(file_id: str, file_name: str):
    return f"{os.getenv('FILE_SERVER_URL')}/preview/{file_id}/{file_name}"


def get_file_download_url(file_id: str, file_name: str):
    return f"{os.getenv('FILE_SERVER_URL')}/download/{file_id}/{file_name}"
