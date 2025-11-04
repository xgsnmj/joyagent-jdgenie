package com.jd.genie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jd.genie.entity.AgentProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 智能体服务商Mapper接口
 * 提供智能体配置的数据库操作
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Mapper
public interface AgentProviderMapper extends BaseMapper<AgentProvider> {

    /**
     * 取消用户的所有默认智能体
     * 用于在设置新默认智能体前，清除旧的默认设置
     *
     * @param userId 用户ID
     * @param excludeId 排除的智能体ID（不取消该ID的默认设置）
     * @return 更新的行数
     */
    @Update("UPDATE agent_provider SET is_default = 0 " +
            "WHERE user_id = #{userId} AND id != #{excludeId} AND is_default = 1")
    int clearUserDefault(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
}
