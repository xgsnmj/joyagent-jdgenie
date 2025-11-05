package com.jd.genie.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * 可拦截的SSE发射器
 * 用于在数据发送前进行拦截和处理
 *
 * 使用场景：
 * 当需要拦截SSE流中的数据进行额外处理时使用
 * 例如：MultiAgentServiceImpl需要通过AgentResponseHandler处理数据
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
@Slf4j
public class InterceptableSseEmitter extends SseEmitter {

    /**
     * 数据拦截回调
     * 当send()被调用时，会先调用此回调处理数据
     */
    private final Consumer<Object> interceptor;

    /**
     * 构造函数
     *
     * @param timeout 超时时间（毫秒）
     * @param interceptor 数据拦截回调
     */
    public InterceptableSseEmitter(Long timeout, Consumer<Object> interceptor) {
        super(timeout);
        this.interceptor = interceptor;
    }

    /**
     * 重写send方法，添加拦截逻辑
     *
     * 执行流程：
     * 1. 调用拦截器处理数据
     * 2. 拦截器负责将处理后的数据发送到真正的SseEmitter
     * 3. 不调用super.send()，避免重复发送
     *
     * @param object 要发送的数据
     * @throws IOException IO异常
     */
    @Override
    public void send(Object object) throws IOException {
        // 调用拦截器处理数据
        if (interceptor != null) {
            try {
                interceptor.accept(object);
            } catch (Exception e) {
                log.error("SSE拦截器处理数据失败", e);
                throw new IOException("SSE拦截器处理失败", e);
            }
        }
        // 注意：不调用super.send()，因为拦截器会负责发送
    }
}
