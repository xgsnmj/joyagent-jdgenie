#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
根据实际数据库结构更新database_schema.md文档
"""
import json

def load_db_structure():
    """加载导出的数据库结构"""
    with open('database_structure.json', 'r', encoding='utf-8') as f:
        return json.load(f)

def generate_table_markdown(table_name, table_info):
    """为指定表生成Markdown文档"""
    columns = table_info['columns']
    indexes = table_info['indexes']
    comment = table_info['comment']
    create_sql = table_info['create_sql']

    # 提取表结构信息生成表格
    md = f"### {table_name} ({comment})\n\n"
    md += "| 字段名 | 类型 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |\n"
    md += "|--------|------|----------|--------|------|------|------|\n"

    column_comments = table_info.get('column_comments', {})

    for col in columns:
        field_name = col['Field']
        field_type = col['Type']
        null_str = "YES" if col['Null'] == "YES" else "NO"
        default_val = col['Default'] if col['Default'] else "-"
        is_pk = "YES" if col['Key'] == "PRI" else "NO"
        auto_inc = "YES" if "auto_increment" in col['Extra'] else "NO"
        col_comment = column_comments.get(field_name, "")

        md += f"| {field_name} | {field_type} | {null_str} | {default_val} | {is_pk} | {auto_inc} | {col_comment} |\n"

    md += "\n**索引**:\n"
    unique_indexes = {}
    normal_indexes = []

    for idx in indexes:
        key_name = idx['Key_name']
        if key_name == 'PRIMARY':
            md += f"- PRIMARY KEY (`{idx['Column_name']}`)\n"
        elif idx['Non_unique'] == 0:
            if key_name not in unique_indexes:
                unique_indexes[key_name] = []
            unique_indexes[key_name].append(idx['Column_name'])
        else:
            if key_name not in [ni['name'] for ni in normal_indexes]:
                cols = [i['Column_name'] for i in indexes if i['Key_name'] == key_name]
                normal_indexes.append({'name': key_name, 'cols': cols})

    for uk_name, uk_cols in unique_indexes.items():
        md += f"- UNIQUE KEY `{uk_name}` ({', '.join([f'`{c}`' for c in uk_cols])})\n"

    for idx in normal_indexes:
        md += f"- KEY `{idx['name']}` ({', '.join([f'`{c}`' for c in idx['cols']])})\n"

    md += "\n**建表SQL**:\n```sql\n"
    md += create_sql + "\n```\n\n"

    return md

# 加载数据库结构
db_structure = load_db_structure()

# 打印表名和注释
print("数据库中的所有表:\n")
for table_name in sorted(db_structure.keys()):
    table_comment = db_structure[table_name]['comment']
    print(f"- {table_name}: {table_comment}")

# 生成sse_message_cache表的文档
print("\n\n=== sse_message_cache 表文档 ===\n")
sse_md = generate_table_markdown('sse_message_cache', db_structure['sse_message_cache'])
print(sse_md)
