package com.example.aitravelserver.service;

import com.example.aitravelserver.dto.LoginDTO;
import com.example.aitravelserver.dto.RegisterDTO;
import com.example.aitravelserver.vo.LoginVO;

public interface AuthService {

    /**
     * 注册
     */
    void register(RegisterDTO dto);

    /**
     * 登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 刷新令牌
     */
    LoginVO refresh(String refreshToken);
}
