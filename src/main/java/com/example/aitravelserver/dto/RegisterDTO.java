package com.example.aitravelserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册入参。校验注解由 Controller 上的 @Valid 触发，
 * 校验失败抛 MethodArgumentNotValidException，由全局异常处理器转成 400。
 */
@Data
public class RegisterDTO {

    /**
     * 手机号，必填且需符合大陆号段格式
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 密码，8 到 20 位
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需在8到20位之间")
    private String password;

    /**
     * 昵称，可不填，为空时业务层给默认值
     */
    @Size(max = 20, message = "昵称最长20个字符")
    private String nickname;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
