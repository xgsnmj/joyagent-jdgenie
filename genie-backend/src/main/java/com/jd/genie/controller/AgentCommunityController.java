package com.jd.genie.controller;

import com.jd.genie.entity.AgentProvider;
import com.jd.genie.service.AgentProviderService;
import com.jd.genie.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体社区API控制器
 * 提供智能体广场、我的智能体等功能
 *
 * @author JDGenie Team
 * @since 2025-01-05
 */
@Slf4j
@RestController
@RequestMapping("/api/agent-community")
public class AgentCommunityController {

    @Autowired
    private AgentProviderService agentProviderService;

    /**
     * 获取智能体社区列表（所有公开智能体）
     * GET /api/agent-community/public
     *
     * @param category 分类标签（可选）
     * @return 公开智能体列表
     */
    @GetMapping("/public")
    public Map<String, Object> getPublicAgents(
            @RequestParam(required = false) String category) {

        log.info("获取公开智能体列表, 分类: {}", category);

        try {
            List<AgentProvider> providers = category != null
                    ? agentProviderService.getPublicProvidersByCategory(category)
                    : agentProviderService.getAllPublicProviders();

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", providers);
            response.put("message", "获取成功");

            log.info("获取公开智能体成功, 数量: {}", providers.size());
            return response;

        } catch (Exception e) {
            log.error("获取公开智能体失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 获取我创建的智能体列表
     * GET /api/agent-community/my-agents
     *
     * @param request HTTP请求（包含JWT Token）
     * @return 我创建的智能体列表
     */
    @GetMapping("/my-agents")
    public Map<String, Object> getMyAgents(HttpServletRequest request) {
        try {
            // 从JWT Token中获取用户ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = JwtUtil.getUserIdFromToken(token);

            log.info("获取我的智能体, 用户ID: {}", userId);

            List<AgentProvider> providers = agentProviderService.getMyCreatedProviders(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", providers);
            response.put("message", "获取成功");

            log.info("获取我的智能体成功, 数量: {}", providers.size());
            return response;

        } catch (Exception e) {
            log.error("获取我的智能体失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 搜索智能体
     * GET /api/agent-community/search?keyword=xxx
     *
     * @param keyword 搜索关键词
     * @return 匹配的智能体列表
     */
    @GetMapping("/search")
    public Map<String, Object> searchAgents(@RequestParam String keyword) {
        log.info("搜索智能体, 关键词: {}", keyword);

        try {
            List<AgentProvider> providers = agentProviderService.searchProviders(keyword);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", providers);
            response.put("message", "搜索成功");

            log.info("搜索智能体成功, 结果数量: {}", providers.size());
            return response;

        } catch (Exception e) {
            log.error("搜索智能体失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "搜索失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 增加智能体使用次数
     * POST /api/agent-community/use/{id}
     *
     * @param id 智能体ID
     * @return 操作结果
     */
    @PostMapping("/use/{id}")
    public Map<String, Object> recordUsage(@PathVariable Long id) {
        log.info("记录智能体使用, ID: {}", id);

        try {
            agentProviderService.incrementUsageCount(id);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "记录成功");

            return response;

        } catch (Exception e) {
            log.error("记录智能体使用失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "记录失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 获取智能体分类列表
     * GET /api/agent-community/categories
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    public Map<String, Object> getCategories() {
        log.info("获取智能体分类列表");

        try {
            // TODO: 从数据库动态获取，这里先写死常用分类
            List<String> categories = List.of(
                    "全部",
                    "教育",
                    "新零售",
                    "消费",
                    "金融",
                    "医疗",
                    "营销",
                    "客服",
                    "数据分析",
                    "办公助手"
            );

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", categories);
            response.put("message", "获取成功");

            return response;

        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
            return response;
        }
    }
}
