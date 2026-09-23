package com.flashsale.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flashsale.dto.LoginRequest;
import com.flashsale.dto.LoginResponse;
import com.flashsale.dto.RegisterRequest;
import com.flashsale.entity.User;

public interface UserService extends IService<User> {

    /**
     * 注册，用户名重复时抛出 BusinessException
     */
    User register(RegisterRequest request);

    /**
     * 登录，成功返回 JWT 与用户信息，失败抛出 BusinessException
     */
    LoginResponse login(LoginRequest request);
}
