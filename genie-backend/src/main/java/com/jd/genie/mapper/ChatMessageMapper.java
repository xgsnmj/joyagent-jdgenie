package com.jd.genie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jd.genie.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息 Mapper 接口
 * 继承 MyBatis-Plus 的 BaseMapper，提供基础的 CRUD 操作
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    // 继承 BaseMapper 后，已包含常用的增删改查方法
    // 如需自定义SQL，可在此添加方法
}
