package com.jd.genie.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体执行器工厂
 * 根据平台类型返回对应的执行器实现
 *
 * 设计思想：
 * - 工厂模式：根据平台类型创建对应的执行器
 * - 自动注册：利用Spring的依赖注入自动注册所有执行器
 * - 线程安全：使用ConcurrentHashMap保证并发安全
 *
 * 使用方式：
 * 1. 实现AgentExecutor接口
 * 2. 添加@Component注解
 * 3. Spring自动注册到此工厂
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
@Slf4j
@Component
public class AgentExecutorFactory {

    /**
     * 执行器注册表
     * key: 平台类型（default/external等）
     * value: 对应的执行器实例
     */
    private final Map<String, AgentExecutor> executorMap = new ConcurrentHashMap<>();

    /**
     * 构造函数：自动注册所有执行器
     *
     * Spring会自动注入所有实现了AgentExecutor接口的Bean
     *
     * @param executors 所有执行器实例列表
     */
    @Autowired
    public AgentExecutorFactory(List<AgentExecutor> executors) {
        // 遍历所有执行器，注册到Map中
        executors.forEach(executor -> {
            String platformType = executor.getPlatformType();
            executorMap.put(platformType, executor);
            log.info("[执行器工厂] 注册执行器 - 平台类型: {}, 实现类: {}",
                    platformType, executor.getClass().getSimpleName());
        });

        log.info("[执行器工厂] 初始化完成 - 共注册 {} 个执行器", executorMap.size());
    }

    /**
     * 获取执行器
     *
     * 路由规则：
     * - "default"：返回DefaultAgentExecutor
     * - 其他（coze/ronghui等）：返回ExternalAgentExecutor
     *
     * @param platformType 平台类型（default/coze/ronghui等）
     * @return 对应的执行器实例
     * @throws IllegalArgumentException 如果找不到对应的执行器
     */
    public AgentExecutor getExecutor(String platformType) {
        log.debug("[执行器工厂] 获取执行器 - 平台类型: {}", platformType);

        // 如果是默认平台，直接返回默认执行器
        if ("default".equals(platformType)) {
            AgentExecutor executor = executorMap.get("default");
            if (executor == null) {
                throw new IllegalArgumentException("默认执行器未注册");
            }
            return executor;
        }

        // 外部平台统一使用ExternalAgentExecutor
        AgentExecutor executor = executorMap.get("external");
        if (executor == null) {
            throw new IllegalArgumentException("外部平台执行器未注册");
        }

        log.debug("[执行器工厂] 返回执行器 - 平台类型: {}, 实现类: {}",
                platformType, executor.getClass().getSimpleName());

        return executor;
    }

    /**
     * 获取所有已注册的执行器
     *
     * @return 执行器Map（只读）
     */
    public Map<String, AgentExecutor> getAllExecutors() {
        return Map.copyOf(executorMap);
    }
}
