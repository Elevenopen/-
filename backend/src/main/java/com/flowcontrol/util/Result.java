package com.flowcontrol.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应包装类
 * 所有接口返回值统一为 Result<T> 格式
 *
 * @param <T> data字段类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 响应码: 200=成功, 4xx=客户端错误, 5xx=服务端错误 */
    private Integer code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 时间戳 */
    private Long timestamp;

    // ==================== 工厂方法 ====================

    public static <T> Result<T> ok() {
        return ok(null, "操作成功");
    }

    public static <T> Result<T> ok(T data) {
        return ok(data, "操作成功");
    }

    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(200, message, data, System.currentTimeMillis());
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> of(Integer code, String message, T data) {
        return new Result<>(code, message, data, System.currentTimeMillis());
    }

    // ==================== 快捷判断 ====================

    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
}
