package com.example.aitravelserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 登录入参，只做非空校验，不做格式校验，避免把格式错和密码错区分开被枚举 */
@Data
public class LoginDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}