package com.jd.genie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jd.genie.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * 继承 MyBatis-Plus 的 BaseMapper，提供基础的 CRUD 操作
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // 继承 BaseMapper 后，已包含常用的增删改查方法：
    // - insert: 插入一条记录
    // - deleteById: 根据 ID 删除
    // - updateById: 根据 ID 更新
    // - selectById: 根据 ID 查询
    // - selectOne: 根据条件查询单条记录
    // - selectList: 根据条件查询列表
    // - selectPage: 分页查询
    // 如需自定义SQL，可在此添加方法
}
