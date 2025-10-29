package com.jd.genie.config.security;

import com.jd.genie.entity.SysUser;
import com.jd.genie.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义UserDetailsService实现类
 * 用于加载用户信息，供Spring Security进行认证
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private IUserService userService;

    /**
     * 根据用户名加载用户信息
     * Spring Security会调用此方法进行认证
     *
     * @param username 用户名
     * @return UserDetails对象
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户信息: username={}", username);

        // 从数据库查询用户
        SysUser sysUser = userService.getUserByUsername(username);

        // 如果用户不存在，抛出异常
        if (sysUser == null) {
            log.error("用户不存在: username={}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 检查用户状态
        if (sysUser.getStatus() != null && sysUser.getStatus() != 0) {
            log.error("用户已被禁用: username={}", username);
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 将SysUser包装为CustomUserDetails并返回
        return new CustomUserDetails(sysUser);
    }
}
