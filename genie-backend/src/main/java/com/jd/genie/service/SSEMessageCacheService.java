package com.jd.genie.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jd.genie.entity.SSEMessageCache;
import com.jd.genie.mapper.SSEMessageCacheMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SSE消息缓存Service
 * 提供SSE消息缓存的业务逻辑处理
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Slf4j
@Service
public class SSEMessageCacheService extends ServiceImpl<SSEMessageCacheMapper, SSEMessageCache> {

    /**
     * 缓存SSE消息
     *
     * @param sessionId 会话ID（字符串格式）
     * @param sequence 消息序号
     * @param eventType 事件类型
     * @param eventData 事件数据
     * @param rawData 原始数据
     */
    public void cacheMessage(String sessionId, Integer sequence, String eventType,
                            String eventData, String rawData) {
        SSEMessageCache cache = new SSEMessageCache();
        cache.setSessionId(sessionId);
        cache.setMessageSequence(sequence);
        cache.setEventType(eventType);
        cache.setEventData(eventData);
        cache.setRawData(rawData);
        cache.setIsPersisted(false);

        save(cache);
        log.debug("缓存SSE消息 - 会话ID: {}, 序号: {}, 类型: {}", sessionId, sequence, eventType);
    }

    /**
     * 获取会话的所有缓存消息（按序号排序）
     *
     * @param sessionId 会话ID（字符串格式）
     * @return 缓存消息列表
     */
    public List<SSEMessageCache> getSessionCachedMessages(String sessionId) {
        return list(new LambdaQueryWrapper<SSEMessageCache>()
                .eq(SSEMessageCache::getSessionId, sessionId)
                .eq(SSEMessageCache::getIsPersisted, false)
                .orderByAsc(SSEMessageCache::getMessageSequence));
    }

    /**
     * 标记消息为已持久化
     *
     * @param sessionId 会话ID（字符串格式）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsPersisted(String sessionId) {
        lambdaUpdate()
                .eq(SSEMessageCache::getSessionId, sessionId)
                .set(SSEMessageCache::getIsPersisted, true)
                .update();

        log.debug("标记会话{}的缓存消息为已持久化", sessionId);
    }

    /**
     * 清理已持久化的缓存消息
     *
     * @param sessionId 会话ID（字符串格式）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanPersistedCache(String sessionId) {
        int deleted = baseMapper.delete(new LambdaQueryWrapper<SSEMessageCache>()
                .eq(SSEMessageCache::getSessionId, sessionId)
                .eq(SSEMessageCache::getIsPersisted, true));

        log.info("清理会话{}的已持久化缓存消息，删除{}条", sessionId, deleted);
    }

    /**
     * 清理所有已持久化的缓存消息
     * 可由定时任务调用
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanAllPersistedCache() {
        int deleted = baseMapper.delete(new LambdaQueryWrapper<SSEMessageCache>()
                .eq(SSEMessageCache::getIsPersisted, true));

        log.info("清理所有已持久化的缓存消息，删除{}条", deleted);
        return deleted;
    }
}
