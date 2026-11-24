package com.example.aitravelserver.vo;

import lombok.Builder;
import lombok.Data;

/** 登录成功返回体。expiresIn 单位为秒，前端据此决定何时静默续期 */
@Data
@Builder
public class LoginVO {

   // 访问令牌，放在 Authorization: Bearer 后面
    private String accessToken;

    //刷新令牌，同时存了一份在 Redis，用于校验是否被注销
    private String refreshToken;

//    访问令牌有效期为秒
    private long expiresIn;

    private Long userId;
    private String nickname;
    private String role;

}