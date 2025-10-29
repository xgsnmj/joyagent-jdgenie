package com.jd.genie.model.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户注册请求DTO
 * 用于接收前端注册请求参数
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
public class UserRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     * 必填，长度2-20字符，只支持中文、英文字母、下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z_]+$", message = "用户名只能包含中文、英文字母和下划线")
    private String username;

    /**
     * 邮箱
     * 必填，必须是有效的邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 密码
     * 必填，长度6-20字符
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;
}
