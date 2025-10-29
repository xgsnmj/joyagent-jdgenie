package com.jd.genie.controller;

import com.jd.genie.common.Result;
import com.jd.genie.model.dto.UserInfoVO;
import com.jd.genie.model.dto.UserLoginDTO;
import com.jd.genie.model.dto.UserRegisterDTO;
import com.jd.genie.service.IUserService;
import com.jd.genie.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 提供用户相关的API接口
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户注册、登录、信息查询等接口")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 用户信息（包含Token）
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账号")
    public Result<UserInfoVO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        try {
            log.info("用户注册请求: username={}", registerDTO.getUsername());
            UserInfoVO userInfo = userService.register(registerDTO);
            return Result.success("注册成功", userInfo);
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 用户信息（包含Token）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录并获取JWT Token")
    public Result<UserInfoVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        try {
            log.info("用户登录请求: username={}", loginDTO.getAccount());
            UserInfoVO userInfo = userService.login(loginDTO);
            return Result.success("登录成功", userInfo);
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param request HTTP请求对象
     * @return 用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "根据Token获取当前登录用户信息")
    public Result<UserInfoVO> getUserInfo(HttpServletRequest request) {
        try {
            // 从请求头中获取Token
            String authorization = request.getHeader("Authorization");
            if (authorization == null || authorization.isEmpty()) {
                return Result.error(401, "未登录");
            }

            // 提取Token（去除Bearer前缀）
            String token = authorization;
            if (authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            }

            // 从Token中获取用户ID
            Long userId = JwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return Result.error(401, "Token无效");
            }

            // 获取用户信息
            UserInfoVO userInfo = userService.getUserInfo(userId);
            return Result.success(userInfo);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
