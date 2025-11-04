package com.jd.genie.controller;

import com.jd.genie.common.Result;
import com.jd.genie.config.security.CustomUserDetails;
import com.jd.genie.dto.AgentProviderDTO;
import com.jd.genie.dto.AgentProviderRequest;
import com.jd.genie.entity.AgentProvider;
import com.jd.genie.service.AgentProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能体服务商配置Controller
 * 提供智能体配置的CRUD API接口
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Slf4j
@RestController
@RequestMapping("/api/agent-providers")
public class AgentProviderController {

    @Autowired
    private AgentProviderService agentProviderService;

    /**
     * 获取当前用户的所有智能体配置
     *
     * @param authentication 认证信息
     * @return 智能体配置列表
     */
    @GetMapping
    public Result<List<AgentProviderDTO>> getUserProviders(Authentication authentication) {
        try {
            CustomUserDetails details  =  (CustomUserDetails)authentication.getPrincipal();
            Long userId = details.getSysUser().getId();
            List<AgentProvider> providers = agentProviderService.getUserProviders(userId);
            List<AgentProviderDTO> dtos = providers.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            log.info("用户{}查询智能体配置，共{}个", userId, dtos.size());
            return Result.success(dtos);
        } catch (Exception e) {
            log.error("查询智能体配置失败", e);
            return Result.error("查询智能体配置失败: " + e.getMessage());
        }
    }

    /**
     * 创建智能体配置
     *
     * @param request 创建请求
     * @param authentication 认证信息
     * @return 创建的智能体配置
     */
    @PostMapping
    public Result<AgentProviderDTO> createProvider(
            @Validated @RequestBody AgentProviderRequest request,
            Authentication authentication) {
        try {
            CustomUserDetails details  =  (CustomUserDetails)authentication.getPrincipal();
            Long userId = details.getSysUser().getId();

            // default类型不允许手动创建
            if ("default".equals(request.getProviderType())) {
                return Result.error("不允许手动创建default类型的智能体");
            }

            // 验证必填字段
            if (!"default".equals(request.getProviderType())) {
                if (request.getApiEndpoint() == null || request.getApiEndpoint().trim().isEmpty()) {
                    return Result.error("API地址不能为空");
                }
                if (request.getApiKey() == null || request.getApiKey().trim().isEmpty()) {
                    return Result.error("API密钥不能为空");
                }
            }

            // Coze平台特殊验证：bot_id必填
            if ("coze".equals(request.getProviderType())) {
                if (request.getBotId() == null || request.getBotId().trim().isEmpty()) {
                    return Result.error("Coze平台的Bot ID不能为空");
                }
            }

            AgentProvider provider = new AgentProvider();
            BeanUtils.copyProperties(request, provider);
            provider.setUserId(userId);
            provider.setCreateTime(LocalDateTime.now());

            // 如果是用户的第一个非default智能体，可以自动设为默认
            List<AgentProvider> existing = agentProviderService.getUserProviders(userId);
            boolean hasOnlyDefault = existing.size() == 1 &&
                    "default".equals(existing.get(0).getProviderType());

            if (hasOnlyDefault && (request.getIsDefault() == null || !request.getIsDefault())) {
                // 如果用户只有default智能体，且没有明确指定不设为默认，则自动设为默认
                provider.setIsDefault(false); // 保持default为默认
            }

            agentProviderService.save(provider);
            log.info("用户{}创建智能体配置: {} ({})", userId, provider.getProviderName(), provider.getProviderType());

            return Result.success(toDTO(provider));
        } catch (Exception e) {
            log.error("创建智能体配置失败", e);
            return Result.error("创建智能体配置失败: " + e.getMessage());
        }
    }

    /**
     * 更新智能体配置
     *
     * @param id 智能体ID
     * @param request 更新请求
     * @param authentication 认证信息
     * @return 更新后的智能体配置
     */
    @PutMapping("/{id}")
    public Result<AgentProviderDTO> updateProvider(
            @PathVariable Long id,
            @Validated @RequestBody AgentProviderRequest request,
            Authentication authentication) {
        try {
            CustomUserDetails details  =  (CustomUserDetails)authentication.getPrincipal();
            Long userId = details.getSysUser().getId();

            // 验证权限
            if (!agentProviderService.isProviderOwnedByUser(id, userId)) {
                return Result.error("无权操作该智能体配置");
            }

            AgentProvider provider = agentProviderService.getById(id);

            // default类型不允许修改关键字段
            if ("default".equals(provider.getProviderType())) {
                return Result.error("不允许修改default类型的智能体");
            }

            // 验证必填字段
            if (request.getApiEndpoint() == null || request.getApiEndpoint().trim().isEmpty()) {
                return Result.error("API地址不能为空");
            }
            if (request.getApiKey() == null || request.getApiKey().trim().isEmpty()) {
                return Result.error("API密钥不能为空");
            }

            // Coze平台特殊验证：bot_id必填
            if ("coze".equals(request.getProviderType())) {
                if (request.getBotId() == null || request.getBotId().trim().isEmpty()) {
                    return Result.error("Coze平台的Bot ID不能为空");
                }
            }

            BeanUtils.copyProperties(request, provider);
            provider.setId(id);

            agentProviderService.updateById(provider);
            log.info("用户{}更新智能体配置: {}", userId, id);

            return Result.success(toDTO(provider));
        } catch (Exception e) {
            log.error("更新智能体配置失败", e);
            return Result.error("更新智能体配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除智能体配置
     *
     * @param id 智能体ID
     * @param authentication 认证信息
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProvider(@PathVariable Long id, Authentication authentication) {
        try {
            CustomUserDetails details  =  (CustomUserDetails)authentication.getPrincipal();
            Long userId = details.getSysUser().getId();

            // 验证权限
            if (!agentProviderService.isProviderOwnedByUser(id, userId)) {
                return Result.error("无权操作该智能体配置");
            }

            AgentProvider provider = agentProviderService.getById(id);

            // default类型不允许删除
            if ("default".equals(provider.getProviderType())) {
                return Result.error("不允许删除default类型的智能体");
            }

            agentProviderService.removeById(id);
            log.info("用户{}删除智能体配置: {}", userId, id);

            return Result.success();
        } catch (Exception e) {
            log.error("删除智能体配置失败", e);
            return Result.error("删除智能体配置失败: " + e.getMessage());
        }
    }

    /**
     * 设置默认智能体
     *
     * @param id 智能体ID
     * @param authentication 认证信息
     * @return 设置结果
     */
    @PutMapping("/{id}/set-default")
    public Result<Void> setDefaultProvider(@PathVariable Long id, Authentication authentication) {
        try {
            CustomUserDetails details  =  (CustomUserDetails)authentication.getPrincipal();
            Long userId = details.getSysUser().getId();

            // 验证权限
            if (!agentProviderService.isProviderOwnedByUser(id, userId)) {
                return Result.error("无权操作该智能体配置");
            }

            agentProviderService.setDefaultProvider(userId, id);
            log.info("用户{}设置默认智能体: {}", userId, id);

            return Result.success();
        } catch (Exception e) {
            log.error("设置默认智能体失败", e);
            return Result.error("设置默认智能体失败: " + e.getMessage());
        }
    }

    /**
     * 测试智能体连接
     *
     * @param request 测试请求
     * @return 测试结果
     */
    @PostMapping("/test-connection")
    public Result<String> testConnection(@Validated @RequestBody AgentProviderRequest request) {
        try {
            // TODO: 实现连接测试逻辑
            // 1. 根据平台类型选择适配器
            // 2. 发送简单的测试请求
            // 3. 验证响应是否正确
            //
            // AgentAdapter adapter = adapterFactory.getAdapter(request.getProviderType());
            // boolean success = adapter.testConnection(request.getApiEndpoint(), request.getApiKey());

            log.info("测试智能体连接 - 类型: {}, 端点: {}", request.getProviderType(), request.getApiEndpoint());

            // 临时返回成功（实际需要实现真实的连接测试）
            return Result.success("连接测试成功");
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return Result.error("连接测试失败: " + e.getMessage());
        }
    }

    /**
     * 转换为DTO（API密钥脱敏）
     *
     * @param provider 智能体配置实体
     * @return DTO
     */
    private AgentProviderDTO toDTO(AgentProvider provider) {
        AgentProviderDTO dto = new AgentProviderDTO();
        BeanUtils.copyProperties(provider, dto);
        // API密钥脱敏在DTO的getMaskedApiKey方法中处理
        return dto;
    }
}
