package com.example.aitravelserver.common.exception;


/**
 * 业务异常，用来表达“可以预期、需要明确告知用户”的错误。
 * 与 RuntimeException 的区别在于带错误码，由 GlobalExceptionHandler 原样返回给前端，
 * 不会打印完整堆栈，避免把内部信息泄露出去。
 */
public class BizException extends RuntimeException {

    //错误码，与 Result 中的 code 一致，如 400、401、403
    private final int code;

    //只传消息时默认按 400 处理，多数业务校验失败属于这一类
    public BizException(String message) {
        this(400, message);
    }

    //传入错误码和消息
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
    //获取错误码
    public int getCode() {
        return code;
    }
}
