package com.jd.genie.controller;

import com.jd.genie.common.Result;
import com.jd.genie.model.dto.MessageVO;
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
     * 获取用户的会话列表
     *
     * @param request HTTP请求对象
     * @return 会话列表
     */
    @GetMapping("/sessions")
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有会话")
    public Result<List<SessionVO>> getUserSessions(HttpServletRequest request) {
        try {
            // 从Token中获取用户ID
            Long userId = getUserIdFromRequest(request);
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 获取会话列表
            List<SessionVO> sessions = chatHistoryService.getUserSessions(userId);
            return Result.success(sessions);
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
    @DeleteMapping("/session/{sessionId}")
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
