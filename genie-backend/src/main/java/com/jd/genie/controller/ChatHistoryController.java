package com.jd.genie.controller;

import com.jd.genie.common.Result;
import com.jd.genie.model.dto.MessageVO;
import com.jd.genie.model.dto.PageResponse;
import com.jd.genie.model.dto.SessionVO;
import com.jd.genie.service.IChatHistoryService;
import com.jd.genie.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话历史控制器
 * 提供会话历史相关的API接口
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@Tag(name = "会话管理", description = "会话历史查询、删除、更新等接口")
public class ChatHistoryController {

    @Autowired
    private IChatHistoryService chatHistoryService;

    /**
     * 获取用户的会话列表（支持分页）
     *
     * @param page     页码（从1开始，默认1）
     * @param pageSize 每页大小（默认10）
     * @param request  HTTP请求对象
     * @return 分页会话列表
     */
    @GetMapping("/sessions")
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有会话（支持分页）")
    public Result<PageResponse<SessionVO>> getUserSessions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        try {
            // 从Token中获取用户ID
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 获取所有会话
            List<SessionVO> allSessions = chatHistoryService.getUserSessions(userId);

            // 手动分页处理
            int total = allSessions.size();
            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, total);

            // 边界检查
            List<SessionVO> pagedSessions;
            if (startIndex >= total) {
                // 起始索引超出范围，返回空列表
                pagedSessions = new java.util.ArrayList<>();
            } else {
                pagedSessions = allSessions.subList(startIndex, endIndex);
            }

            // 构建分页响应
            PageResponse<SessionVO> pageResponse = PageResponse.<SessionVO>builder()
                    .list(pagedSessions)
                    .total((long) total)
                    .page(page)
                    .pageSize(pageSize)
                    .build();

            log.info("获取会话列表成功: userId={}, total={}, page={}, pageSize={}",
                     userId, total, page, pageSize);
            return Result.success(pageResponse);
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建新会话
     *
     * @param params  请求参数（包含标题）
     * @param request HTTP请求对象
     * @return 新创建的会话信息
     */
    @PostMapping("/sessions")
    @Operation(summary = "创建新会话", description = "创建一个新的聊天会话")
    public Result<SessionVO> createSession(
            @RequestBody(required = false) Map<String, String> params,
            HttpServletRequest request) {
        try {
            // 从Token中获取用户ID
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 获取标题（可选）
            String title = params != null ? params.get("title") : null;

            // 创建新会话
            SessionVO session = chatHistoryService.createSession(userId, title);
            return Result.success("创建成功", session);
        } catch (Exception e) {
            log.error("创建会话失败: error={}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取会话详情
     *
     * @param sessionId 会话ID
     * @param request   HTTP请求对象
     * @return 会话详情
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取会话详情", description = "根据会话ID获取会话详细信息")
    public Result<SessionVO> getSessionDetail(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            HttpServletRequest request) {
        try {
            // 从Token中获取用户ID
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 获取会话详情
            SessionVO session = chatHistoryService.getSessionDetail(sessionId, userId);
            return Result.success(session);
        } catch (Exception e) {
            log.error("获取会话详情失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取会话的消息列表
     *
     * @param sessionId 会话ID
     * @param request   HTTP请求对象
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取会话消息", description = "获取指定会话的所有消息记录")
    public Result<List<MessageVO>> getSessionMessages(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            HttpServletRequest request) {
        try {
            // 从Token中获取用户ID
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 获取消息列表
            List<MessageVO> messages = chatHistoryService.getSessionMessages(sessionId, userId);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取会话消息失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @param request   HTTP请求对象
     * @return 删除结果
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除会话", description = "删除指定的会话（逻辑删除）")
    public Result<Boolean> deleteSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            HttpServletRequest request) {
        try {
            // 从Token中获取用户ID
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 删除会话
            boolean result = chatHistoryService.deleteSession(sessionId, userId);
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除会话失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param params    请求参数（包含新标题）
     * @param request   HTTP请求对象
     * @return 更新结果
     */
    @PutMapping("/session/{sessionId}/title")
    @Operation(summary = "更新会话标题", description = "修改指定会话的标题")
    public Result<Boolean> updateSessionTitle(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {
        try {
            // 从Token中获取用户ID
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 获取新标题
            String title = params.get("title");
            if (title == null || title.isEmpty()) {
                return Result.error("标题不能为空");
            }

            // 更新标题
            boolean result = chatHistoryService.updateSessionTitle(sessionId, userId, title);
            return Result.success("更新成功", result);
        } catch (Exception e) {
            log.error("更新会话标题失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 接收前端上报的完整multiAgent数据
     *
     * <p>前端在对话完成后，主动上报完整的multiAgent数据结构，
     * 包含AI思考、任务执行、工具调用等所有前端渲染所需的信息。
     * 这些数据将保存到chat_message表的metadata字段中，
     * 用于历史会话的完整恢复和显示。</p>
     *
     * @param sessionId 会话ID
     * @param request 包含multiAgent数据的请求体
     * @param httpRequest HTTP请求对象，用于获取用户认证信息
     * @return 成功/失败响应
     */
    @PostMapping("/sessions/{sessionId}/multiagent")
    @Operation(summary = "上报multiAgent数据", description = "前端上报完整的对话数据结构用于历史会话恢复")
    public Result<Void> uploadMultiAgent(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @RequestBody com.jd.genie.model.req.UploadMultiAgentRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 1. 验证用户权限（JWT）
            Long userId = getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 2. 验证会话归属
            if (!chatHistoryService.isSessionOwner(sessionId, userId)) {
                log.warn("会话归属验证失败: sessionId={}, userId={}", sessionId, userId);
                return Result.error(403, "无权限操作此会话");
            }

            // 3. 查找对应的assistant消息
            Long messageId = chatHistoryService.findAssistantMessageByRequestId(
                sessionId,
                request.getRequestId()
            );

            if (messageId == null) {
                log.warn("未找到对应的assistant消息: sessionId={}, requestId={}",
                         sessionId, request.getRequestId());
                return Result.error(404, "未找到对应的消息");
            }

            // 4. 读取原有metadata（保留后端保存的rawMessages等数据）
            com.jd.genie.entity.ChatMessage existingMessage =
                chatHistoryService.getMessageById(messageId);
            Map<String, Object> existingMetadata = new java.util.HashMap<>();

            if (existingMessage != null &&
                existingMessage.getMetadata() != null &&
                !existingMessage.getMetadata().isEmpty()) {
                try {
                    existingMetadata = com.alibaba.fastjson.JSON.parseObject(
                        existingMessage.getMetadata(),
                        new com.alibaba.fastjson.TypeReference<Map<String, Object>>() {}
                    );
                    log.debug("成功读取原有metadata: messageId={}, keys={}",
                             messageId, existingMetadata.keySet());
                } catch (Exception e) {
                    log.warn("解析原有metadata失败，使用空对象: messageId={}, error={}",
                            messageId, e.getMessage());
                }
            }

            // 5. 构建metadata（合并原有数据，保留rawMessages）
            Map<String, Object> metadata = new java.util.HashMap<>(existingMetadata);
            metadata.put("multiAgent", request.getMultiAgent());
            metadata.put("version", "1.0");
            metadata.put("source", "frontend+backend");  // 标识为合并数据
            metadata.put("timestamp", System.currentTimeMillis());
            metadata.put("requestId", request.getRequestId());
            // rawMessages会自动保留（如果原metadata中存在）

            // 6. 更新metadata字段
            String metadataJson = com.alibaba.fastjson.JSON.toJSONString(metadata);
            chatHistoryService.updateMessageMetadata(messageId, metadataJson);

            log.info("MultiAgent数据上报成功: sessionId={}, messageId={}, requestId={}, size={}",
                     sessionId, messageId, request.getRequestId(), metadataJson.length());

            return Result.success();
        } catch (Exception e) {
            log.error("MultiAgent数据上报失败: sessionId={}, requestId={}, error={}",
                      sessionId, request.getRequestId(), e.getMessage(), e);
            return Result.error("上报失败: " + e.getMessage());
        }
    }

    /**
     * 从请求中提取用户ID
     *
     * @param request HTTP请求对象
     * @return 用户ID
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
