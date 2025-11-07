package com.jd.genie.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体适配器工厂
 * 根据平台类型返回对应的适配器实例
 * 使用工厂模式实现适配器的统一管理和动态分发
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Slf4j
@Component
public class AgentAdapterFactory {

    @Autowired
    private List<AgentAdapter> adapters;

    private final Map<String, AgentAdapter> adapterMap = new HashMap<>();

    /**
     * 初始化适配器映射
     * Spring启动时自动注册所有适配器实例
     */
    @PostConstruct
    public void init() {
        // 将所有适配器注册到Map中
        for (AgentAdapter adapter : adapters) {
            String providerType = adapter.getProviderType();
            adapterMap.put(providerType, adapter);
            log.info("注册智能体适配器 - 类型: {}, 实现类: {}",
                    providerType, adapter.getClass().getSimpleName());
        }

        log.info("智能体适配器工厂初始化完成，共注册{}个适配器", adapterMap.size());
    }

    /**
     * 根据平台类型获取适配器
     *
     * @param providerType 平台类型（default/coze/ronghui/tongyi/dify）
     * @return 对应的适配器实例
     */
    public AgentAdapter getAdapter(String providerType) {
        AgentAdapter adapter = adapterMap.get(providerType);

        if (adapter == null) {
            log.warn("未找到平台{}的适配器，使用默认适配器", providerType);
            return adapterMap.get("default");
        }

        log.debug("获取智能体适配器 - 类型: {}", providerType);
        return adapter;
    }

    /**
     * 获取所有已注册的平台类型
     *
     * @return 平台类型列表
     */
    public List<String> getSupportedProviderTypes() {
        return new java.util.ArrayList<>(adapterMap.keySet());
    }

    /**
     * 检查平台类型是否支持
     *
     * @param providerType 平台类型
     * @return 是否支持
     */
    public boolean isSupported(String providerType) {
        return adapterMap.containsKey(providerType);
    }
}
