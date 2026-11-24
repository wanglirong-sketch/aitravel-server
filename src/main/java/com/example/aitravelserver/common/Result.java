package com.example.aitravelserver.common;

import lombok.Data;

/**
 * 统一返回体。约定：code 为 0 表示成功，非 0 表示失败。
 * 前面错用了 200 作为成功码，这里统一改成 0，前端只认这个值。
 */
@Data
public class Result<T> {

    //0 成功；400 参数或业务校验失败；401 未登录；403 无权限；500 系统异常
    private int code;

    //提示信息，失败时前端可直接展示
    private String message;

    //业务数据，失败时为 null
    private T data;

    //成功并携带数据
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    //成功但没有数据体，如注册成功
    public static <T> Result<T> ok() {
        return ok(null);
    }

    //失败，指定错误码与提示
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
