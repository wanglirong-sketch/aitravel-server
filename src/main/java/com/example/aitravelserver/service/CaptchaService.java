package com.example.aitravelserver.service;

import com.example.aitravelserver.dto.request.CaptchaVO;

public interface CaptchaService {

    CaptchaVO createCaptcha();

    boolean verify(String captchaId, String captchaCode);
}
