package com.jd.genie.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果类
 * 用于封装所有API接口的返回数据
 *
 * @param <T> 返回数据的泛型类型
 * @author JD Genie
 * @since 1.0.0
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     * 200-成功，其他为失败
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 私有构造方法，防止外部直接实例化
     */
    private Result() {
    }

    /**
     * 私有构造方法
     *
     * @param code 状态码
     * @param msg  消息
     * @param data 数据
     */
    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功返回（无数据）
     *
     * @return Result对象
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "成功", null);
    }

    /**
     * 成功返回（带数据）
     *
     * @param data 返回数据
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }

    /**
     * 成功返回（带消息和数据）
     *
     * @param msg  返回消息
     * @param data 返回数据
     * @return Result对象
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    /**
     * 失败返回（带消息）
     *
     * @param msg 错误消息
     * @return Result对象
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    /**
     * 失败返回（带状态码和消息）
     *
     * @param code 状态码
     * @param msg  错误消息
     * @return Result对象
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    /**
     * 失败返回（带状态码、消息和数据）
     *
     * @param code 状态码
     * @param msg  错误消息
     * @param data 返回数据
     * @return Result对象
     */
    public static <T> Result<T> error(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }
}
