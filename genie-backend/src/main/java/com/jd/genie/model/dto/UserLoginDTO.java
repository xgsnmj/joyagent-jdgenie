package com.jd.genie.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 用户登录请求DTO
 * 用于接收前端登录请求参数
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
public class UserLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录账号
     * 支持用户名或邮箱登录
     * 必填字段
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * 密码
     * 必填字段
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
