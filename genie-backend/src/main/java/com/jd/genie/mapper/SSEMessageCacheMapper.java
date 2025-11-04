package com.jd.genie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jd.genie.entity.SSEMessageCache;
import org.apache.ibatis.annotations.Mapper;

/**
 * SSE消息缓存Mapper接口
 * 提供SSE消息缓存的数据库操作
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Mapper
public interface SSEMessageCacheMapper extends BaseMapper<SSEMessageCache> {
}
