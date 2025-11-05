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

    /**
     * 获取所有公开的智能体配置（智能体社区）
     * 所有用户可见，按使用次数和创建时间排序
     *
     * @return 公开智能体列表
     */
    public List<AgentProvider> getAllPublicProviders() {
        return list(new LambdaQueryWrapper<AgentProvider>()
                .eq(AgentProvider::getIsPublic, true)
                .eq(AgentProvider::getStatus, 1)
                .orderByDesc(AgentProvider::getUsageCount)
                .orderByDesc(AgentProvider::getCreateTime));
    }

    /**
     * 获取指定分类的公开智能体
     *
     * @param category 分类标签
     * @return 该分类下的智能体列表
     */
    public List<AgentProvider> getPublicProvidersByCategory(String category) {
        LambdaQueryWrapper<AgentProvider> wrapper = new LambdaQueryWrapper<AgentProvider>()
                .eq(AgentProvider::getIsPublic, true)
                .eq(AgentProvider::getStatus, 1);

        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(AgentProvider::getCategory, category);
        }

        return list(wrapper.orderByDesc(AgentProvider::getUsageCount)
                .orderByDesc(AgentProvider::getCreateTime));
    }

    /**
     * 获取我创建的智能体（包括公开和私有）
     *
     * @param creatorId 创建者用户ID
     * @return 我创建的智能体列表
     */
    public List<AgentProvider> getMyCreatedProviders(Long creatorId) {
        return list(new LambdaQueryWrapper<AgentProvider>()
                .eq(AgentProvider::getCreatorId, creatorId)
                .eq(AgentProvider::getStatus, 1)
                .orderByDesc(AgentProvider::getIsDefault)
                .orderByDesc(AgentProvider::getCreateTime));
    }

    /**
     * 增加智能体使用次数
     * 每次用户选择该智能体发起对话时调用
     *
     * @param providerId 智能体ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsageCount(Long providerId) {
        AgentProvider provider = getById(providerId);
        if (provider != null) {
            Integer currentCount = provider.getUsageCount() == null ? 0 : provider.getUsageCount();
            provider.setUsageCount(currentCount + 1);
            updateById(provider);
            log.debug("智能体{}使用次数+1，当前: {}", providerId, provider.getUsageCount());
        }
    }

    /**
     * 搜索智能体（按名称或描述）
     *
     * @param keyword 搜索关键词
     * @return 匹配的智能体列表
     */
    public List<AgentProvider> searchProviders(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPublicProviders();
        }

        return list(new LambdaQueryWrapper<AgentProvider>()
                .eq(AgentProvider::getIsPublic, true)
                .eq(AgentProvider::getStatus, 1)
                .and(wrapper -> wrapper
                        .like(AgentProvider::getProviderName, keyword)
                        .or()
                        .like(AgentProvider::getDescription, keyword))
                .orderByDesc(AgentProvider::getUsageCount)
                .orderByDesc(AgentProvider::getCreateTime));
    }
}
