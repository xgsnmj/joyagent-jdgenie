package com.jd.genie.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jd.genie.entity.AgentProvider;
import com.jd.genie.mapper.AgentProviderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 智能体服务商Service
 * 提供智能体配置的业务逻辑处理
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Slf4j
@Service
public class AgentProviderService extends ServiceImpl<AgentProviderMapper, AgentProvider> {

    /**
     * 获取用户的所有智能体配置
     *
     * @param userId 用户ID
     * @return 智能体配置列表（按默认优先、创建时间倒序排列）
     */
    public List<AgentProvider> getUserProviders(Long userId) {
        return list(new LambdaQueryWrapper<AgentProvider>()
                .eq(AgentProvider::getUserId, userId)
                .eq(AgentProvider::getStatus, 1)
                .orderByDesc(AgentProvider::getIsDefault)
                .orderByDesc(AgentProvider::getCreateTime));
    }

    /**
     * 获取用户的默认智能体
     *
     * @param userId 用户ID
     * @return 默认智能体配置，如果不存在则返回null
     */
    public AgentProvider getUserDefaultProvider(Long userId) {
        return getOne(new LambdaQueryWrapper<AgentProvider>()
                .eq(AgentProvider::getUserId, userId)
                .eq(AgentProvider::getIsDefault, true)
                .eq(AgentProvider::getStatus, 1)
                .last("LIMIT 1"));
    }

    /**
     * 创建用户的default智能体配置
     * 在用户注册时自动调用
     *
     * @param userId 用户ID
     * @return 创建的默认智能体配置
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentProvider createDefaultProvider(Long userId) {
        // 检查是否已存在default配置
        AgentProvider existing = getOne(new LambdaQueryWrapper<AgentProvider>()
                .eq(AgentProvider::getUserId, userId)
                .eq(AgentProvider::getProviderType, "default"));

        if (existing != null) {
            log.info("用户{}的默认智能体已存在", userId);
            return existing;
        }

        // 创建default配置
        AgentProvider defaultProvider = new AgentProvider();
        defaultProvider.setUserId(userId);
        defaultProvider.setProviderType("default");
        defaultProvider.setProviderName("JDGenie");
        defaultProvider.setIsDefault(true);
        defaultProvider.setStatus(1);

        save(defaultProvider);
        log.info("为用户{}创建默认智能体配置，ID: {}", userId, defaultProvider.getId());

        return defaultProvider;
    }

    /**
     * 设置默认智能体
     *
     * @param userId 用户ID
     * @param providerId 智能体ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultProvider(Long userId, Long providerId) {
        // 先清除用户的所有默认设置
        baseMapper.clearUserDefault(userId, providerId);

        // 设置新的默认
        AgentProvider provider = getById(providerId);
        if (provider != null && provider.getUserId().equals(userId)) {
            provider.setIsDefault(true);
            updateById(provider);
            log.info("用户{}设置默认智能体为{}", userId, providerId);
        } else {
            throw new RuntimeException("智能体配置不存在或无权操作");
        }
    }

    /**
     * 验证智能体是否属于指定用户
     *
     * @param providerId 智能体ID
     * @param userId 用户ID
     * @return 是否属于该用户
     */
    public boolean isProviderOwnedByUser(Long providerId, Long userId) {
        AgentProvider provider = getById(providerId);
        return provider != null && provider.getUserId().equals(userId);
    }

    /**
     * 根据用户ID和平台类型获取智能体配置
     *
     * @param userId 用户ID
     * @param providerType 平台类型
     * @return 智能体配置
     */
    public AgentProvider getByUserIdAndType(Long userId, String providerType) {
        return getOne(new LambdaQueryWrapper<AgentProvider>()
                .eq(AgentProvider::getUserId, userId)
                .eq(AgentProvider::getProviderType, providerType)
                .eq(AgentProvider::getStatus, 1)
                .last("LIMIT 1"));
    }
}
