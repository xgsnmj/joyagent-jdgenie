package com.jd.genie.controller;

import com.jd.genie.config.GenieConfig;
import com.jd.genie.model.req.GptQueryReq;
import com.jd.genie.service.IGptProcessService;
import com.jd.genie.service.IChatHistoryService;
import com.jd.genie.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@RestController
@RequestMapping("/")
public class GenieController {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    private static final long HEARTBEAT_INTERVAL = 10_000L; // 10秒心跳间隔
    @Autowired
    protected GenieConfig genieConfig;
    @Autowired
    private IGptProcessService gptProcessService;
    @Autowired
    private IChatHistoryService chatHistoryService;







    /**
     * 探活接口
     *
     * @return
     */
    @RequestMapping(value = "/web/health", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }


    /**
     * 处理Agent流式增量查询请求，返回SSE事件流
     * 支持JWT认证和会话历史保存
     *
     * @param params 查询请求参数对象，包含GPT查询所需信息
     * @param httpRequest HTTP请求对象，用于获取JWT token
     * @return 返回SSE事件发射器，用于流式传输增量响应结果
     */
    @RequestMapping(value = "/web/api/v1/gpt/queryAgentStreamIncr", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryAgentStreamIncr(@RequestBody GptQueryReq params, HttpServletRequest httpRequest) {
        // 获取当前用户ID（从JWT token中解析）
        Long userId = getUserIdFromRequest(httpRequest);
        // 打印认证信息用于调试
        log.info("{} queryAgentStreamIncr: userId={}, sessionId={}, query={}",
                params.getRequestId(), userId, params.getSessionId(), params.getQuery());
        // 将userId设置到params.user字段（用于后续流程）
        if (userId != null) {
            params.setUser(String.valueOf(userId));
        }
        // 调用原有的处理逻辑
        return gptProcessService.queryMultiAgentIncrStream(params);
    }

    /**
     * 从请求中提取用户ID
     * 从JWT Token中解析出当前登录用户的ID
     *
     * @param request HTTP请求对象
     * @return 用户ID，如果未登录或Token无效则返回null
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        // 提取Token（去除Bearer前缀）
        String token = authorization;
        if (authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        // 从Token中获取用户ID
        return JwtUtil.getUserIdFromToken(token);
    }

}
    