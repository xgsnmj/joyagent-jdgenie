package com.jd.genie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jd.genie.entity.SysUser;
import com.jd.genie.mapper.SysUserMapper;
import com.jd.genie.model.dto.UserInfoVO;
import com.jd.genie.model.dto.UserLoginDTO;
import com.jd.genie.model.dto.UserRegisterDTO;
import com.jd.genie.service.IUserService;
import com.jd.genie.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 * 实现用户相关的业务逻辑
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private com.jd.genie.service.AgentProviderService agentProviderService;

    /**
     * BCrypt密码编码器
     * 用于密码加密和验证
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 用户信息（包含Token）
     */
    @Override
    public UserInfoVO register(UserRegisterDTO registerDTO) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, registerDTO.getUsername());
        SysUser existUser = sysUserMapper.selectOne(usernameWrapper);

        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在（邮箱为必填，必须唯一）
        LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(SysUser::getEmail, registerDTO.getEmail());
        SysUser existEmail = sysUserMapper.selectOne(emailWrapper);
        if (existEmail != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 创建新用户
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword())); // 加密密码
        user.setNickname(registerDTO.getUsername()); // 昵称默认使用用户名
        user.setStatus(0); // 默认状态：正常
        user.setIsAdmin(0); // 默认非管理员
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存到数据库
        int result = sysUserMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("注册失败");
        }

        // 为新用户创建默认智能体配置
        try {
            agentProviderService.createDefaultProvider(user.getId());
            log.info("用户注册成功并创建默认智能体 - 用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
        } catch (Exception e) {
            log.error("创建默认智能体失败 - 用户ID: {}", user.getId(), e);
            // 不影响注册流程，仅记录日志
        }

        // 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 更新最后登录时间
        updateLastLoginTime(user.getId());

        // 构建返回数据
        return buildUserInfoVO(user, token);
    }

    /**
     * 用户登录
     * 支持用户名或邮箱登录
     *
     * @param loginDTO 登录信息
     * @return 用户信息（包含Token）
     */
    @Override
    public UserInfoVO login(UserLoginDTO loginDTO) {
        // 根据账号查询用户（支持用户名或邮箱）
        SysUser user = getUserByAccount(loginDTO.getAccount());
        if (user == null) {
            throw new RuntimeException("账号或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("账号或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new RuntimeException("用户已被禁用");
        }

        // 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 更新最后登录时间
        updateLastLoginTime(user.getId());

        // 构建返回数据
        return buildUserInfoVO(user, token);
    }

    /**
     * 根据账号获取用户信息
     * 支持用户名或邮箱查询
     *
     * @param account 账号（用户名或邮箱）
     * @return 用户实体
     */
    private SysUser getUserByAccount(String account) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 同时查询用户名和邮箱
        wrapper.eq(SysUser::getUsername, account)
                .or()
                .eq(SysUser::getEmail, account);
        return sysUserMapper.selectOne(wrapper);
    }

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户实体
     */
    @Override
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return sysUserMapper.selectOne(wrapper);
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息VO
     */
    @Override
    public UserInfoVO getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return buildUserInfoVO(user, null);
    }

    /**
     * 更新用户最后登录时间
     *
     * @param userId 用户ID
     */
    @Override
    public void updateLastLoginTime(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    /**
     * 构建用户信息VO
     *
     * @param user  用户实体
     * @param token JWT Token
     * @return 用户信息VO
     */
    private UserInfoVO buildUserInfoVO(SysUser user, String token) {
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setToken(token);
        return vo;
    }
}
