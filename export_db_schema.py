#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
导出数据库表结构脚本
"""
import pymysql
import json

# 数据库连接配置
config = {
    'host': '10.2.243.53',
    'port': 3306,
    'user': 'genie',
    'password': 'whj88364399',
    'database': 'genie_db',
    'charset': 'utf8mb4'
}

def export_table_structure():
    """导出所有表结构"""
    try:
        conn = pymysql.connect(**config)
        cursor = conn.cursor()

        # 获取所有表名
        cursor.execute('SHOW TABLES')
        tables = [row[0] for row in cursor.fetchall()]

        result = {}

        for table in tables:
            print(f'\n正在导出表: {table}')
            table_info = {}

            # 获取表结构
            cursor.execute(f'SHOW CREATE TABLE `{table}`')
            create_table_sql = cursor.fetchone()[1]
            table_info['create_sql'] = create_table_sql

            # 获取字段信息
            cursor.execute(f'DESC `{table}`')
            columns = []
            for row in cursor.fetchall():
                columns.append({
                    'Field': row[0],
                    'Type': row[1],
                    'Null': row[2],
                    'Key': row[3],
                    'Default': str(row[4]) if row[4] is not None else None,
                    'Extra': row[5]
                })
            table_info['columns'] = columns

            # 获取索引信息
            cursor.execute(f'SHOW INDEX FROM `{table}`')
            indexes = []
            for row in cursor.fetchall():
                indexes.append({
                    'Table': row[0],
                    'Non_unique': row[1],
                    'Key_name': row[2],
                    'Seq_in_index': row[3],
                    'Column_name': row[4],
                    'Collation': row[5],
                    'Cardinality': row[6],
                    'Index_type': row[10]
                })
            table_info['indexes'] = indexes

            # 获取表注释
            cursor.execute(f"""
                SELECT TABLE_COMMENT
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = '{config['database']}'
                AND TABLE_NAME = '{table}'
            """)
            comment_row = cursor.fetchone()
            table_info['comment'] = comment_row[0] if comment_row else ''

            # 获取字段注释
            cursor.execute(f"""
                SELECT COLUMN_NAME, COLUMN_COMMENT
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = '{config['database']}'
                AND TABLE_NAME = '{table}'
            """)
            column_comments = {}
            for row in cursor.fetchall():
                if row[1]:
                    column_comments[row[0]] = row[1]
            table_info['column_comments'] = column_comments

            result[table] = table_info

        cursor.close()
        conn.close()

        # 保存为JSON文件
        with open('database_structure.json', 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)

        print(f'\n\n导出完成! 共导出 {len(tables)} 个表的结构信息')
        print('结果已保存到: database_structure.json')

        return result

    except Exception as e:
        print(f'导出失败: {e}')
        import traceback
        traceback.print_exc()
        return None

if __name__ == '__main__':
    export_table_structure()
