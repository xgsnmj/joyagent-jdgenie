#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试sanitize_path_name函数
验证Windows路径非法字符清理功能
"""

import re


def sanitize_path_name(path_name: str) -> str:
    """
    清理路径名中的非法字符，使其在Windows/Linux/Mac系统下都有效

    Windows不允许的字符: < > : " / \\ | ? *
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


def test_sanitize_path_name():
    """测试用例"""
    test_cases = [
        # (输入, 预期输出)
        ("geniesession-1761701208863-4084:1761701208883-4152", "geniesession-1761701208863-4084_1761701208883-4152"),
        ("test<>:|?*name", "test______name"),  # 6个非法字符: < > : | ? *
        ("normal_name", "normal_name"),
        ("path/with\\slash", "path_with_slash"),
        ("  .dotfile.  ", "dotfile"),
        ("", "unnamed"),
        (None, "unnamed"),
        ("中文路径:测试", "中文路径_测试"),
    ]

    print("=" * 60)
    print("测试 sanitize_path_name 函数")
    print("=" * 60)

    all_passed = True
    for i, (input_val, expected) in enumerate(test_cases, 1):
        try:
            result = sanitize_path_name(input_val)
            passed = result == expected
            status = "[通过]" if passed else "[失败]"

            print(f"\n测试 {i}: {status}")
            print(f"  输入:     {repr(input_val)}")
            print(f"  预期:     {repr(expected)}")
            print(f"  实际:     {repr(result)}")

            if not passed:
                all_passed = False
        except Exception as e:
            print(f"\n测试 {i}: [异常]")
            print(f"  输入:     {repr(input_val)}")
            print(f"  错误:     {e}")
            all_passed = False

    print("\n" + "=" * 60)
    if all_passed:
        print("[成功] 所有测试通过！")
    else:
        print("[失败] 部分测试失败！")
    print("=" * 60)

    return all_passed


if __name__ == "__main__":
    test_sanitize_path_name()
