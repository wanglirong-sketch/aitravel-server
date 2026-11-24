package com.example.aitravelserver.common.exception;

import com.example.aitravelserver.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 * 作用是把各类异常统一转成 Result 结构，避免它们冒到 Spring 默认错误页变成 500。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按异常自带的 code 返回，比如 400 已注册、401 未登录 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：取第一条提示返回，前端直接展示即可 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldErrors().stream()
                .findFirst().orElse(null);
        String msg = first == null ? "参数校验失败" : first.getDefaultMessage();
        return Result.fail(400, msg);
    }

    /** 兜底：未预期的异常打完整堆栈，只给前端一句通用提示，不泄露内部信息 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}

