package com.jd.genie.service;

import com.jd.genie.entity.SysUser;
import com.jd.genie.model.dto.UserInfoVO;
import com.jd.genie.model.dto.UserLoginDTO;
import com.jd.genie.model.dto.UserRegisterDTO;

/**
 * 用户服务接口
 * 提供用户相关的业务操作
 *
 * @author JD Genie
 * @since 1.0.0
 */
public interface IUserService {

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 用户信息（包含Token）
     */
    UserInfoVO register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 用户信息（包含Token）
     */
    UserInfoVO login(UserLoginDTO loginDTO);

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户实体
     */
    SysUser getUserByUsername(String username);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息VO
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 更新用户最后登录时间
     *
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);
}
