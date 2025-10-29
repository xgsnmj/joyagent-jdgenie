package com.jd.genie.config.security;

import com.jd.genie.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 自定义UserDetails实现类
 * 包装SysUser实体，提供Spring Security所需的用户信息
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    /**
     * 系统用户实体
     */
    private SysUser sysUser;

    /**
     * 获取用户权限集合
     * 根据用户的isAdmin字段返回相应的角色
     *
     * @return 权限集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 如果是管理员，返回ROLE_ADMIN角色
        if (sysUser.getIsAdmin() != null && sysUser.getIsAdmin() == 1) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        // 普通用户返回ROLE_USER角色
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 获取用户密码
     *
     * @return 加密后的密码
     */
    @Override
    public String getPassword() {
        return sysUser.getPassword();
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    @Override
    public String getUsername() {
        return sysUser.getUsername();
    }

    /**
     * 账号是否未过期
     * 这里始终返回true，可根据业务需求扩展
     *
     * @return true-未过期
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账号是否未锁定
     * 根据用户状态判断：status=0表示正常
     *
     * @return true-未锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        return sysUser.getStatus() == null || sysUser.getStatus() == 0;
    }

    /**
     * 凭证（密码）是否未过期
     * 这里始终返回true，可根据业务需求扩展
     *
     * @return true-未过期
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账号是否启用
     * 根据用户状态判断：status=0表示启用
     *
     * @return true-启用
     */
    @Override
    public boolean isEnabled() {
        return sysUser.getStatus() == null || sysUser.getStatus() == 0;
    }

    /**
     * 获取原始的SysUser对象
     * 方便在业务代码中获取完整的用户信息
     *
     * @return SysUser实体
     */
    public SysUser getSysUser() {
        return sysUser;
    }
}
