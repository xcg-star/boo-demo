package com.chenxt.bootdemo.base.enumeration;

import com.chenxt.bootdemo.base.expection.assertion.BusinessExceptionAssert;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码
 *
 * @author chenxt
 * @date 2019/12/25
 */
@Getter
@AllArgsConstructor
public enum CodeStatusEnum implements BusinessExceptionAssert {
    /**
     * 调用成功
     */
    SUCCESS(0, "调用成功"),

    /**
     * 调用失败
     */
    FAIL(-1, "调用失败"),

    /**
     * 语言错误
     */
    LANGUAGE_NOT_EXIST(-247, "语言不存在");

    private int code;
    private String message;
}
