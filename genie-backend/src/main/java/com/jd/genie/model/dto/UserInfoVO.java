package com.jd.genie.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息响应VO
 * 用于返回给前端的用户信息（不包含密码等敏感信息）
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 用户状态
     * 0-正常，1-停用，2-禁用
     */
    private Integer status;

    /**
     * 是否为管理员
     * 0-否，1-是
     */
    private Integer isAdmin;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * JWT Token
     * 登录成功后返回
     */
    private String token;
}
