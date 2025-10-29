package com.jd.genie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体类
 *
 * <p>用于存储系统用户的基本信息，包括账号、密码、个人信息等。
 * 使用MyBatis-Plus进行ORM映射，支持逻辑删除功能。</p>
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（登录账号）
     * 唯一标识，用于用户登录
     */
    private String username;

    /**
     * 密码（加密存储）
     * 建议使用BCrypt等强加密算法
     */
    private String password;

    /**
     * 用户昵称（显示名称）
     */
    private String nickname;

    /**
     * 电子邮箱
     * 用于找回密码、接收通知等
     */
    private String email;

    /**
     * 手机号码
     * 用于短信验证、找回密码等
     */
    private String phone;

    /**
     * 用户头像URL
     * 存储头像图片的访问路径
     */
    private String avatar;

    /**
     * 用户状态
     * 0-正常：用户可以正常登录和使用系统
     * 1-停用：临时停用，可以恢复
     * 2-禁用：永久禁用，不允许登录
     */
    private Integer status;

    /**
     * 是否为管理员
     * 0-否：普通用户
     * 1-是：管理员用户，拥有更多权限
     */
    private Integer isAdmin;

    /**
     * 创建时间
     * 记录用户账号的创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 记录用户信息的最后修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 最后登录时间
     * 记录用户最近一次成功登录的时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 逻辑删除标识
     * 0-未删除
     * 1-已删除
     * 使用MyBatis-Plus的@TableLogic注解实现逻辑删除
     */
    @TableLogic
    private Integer yn;
}
