package com.chenxt.bootdemo.base.expection.i18n;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 语言工具类
 *
 * @author chenxt
 * @date 2020/07/09
 */
public class LocaleHolder {
    /**
     * 获取当前语言
     *
     * @return lang lang
     */
    public static String getLang() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale.toString();
    }

    /**
     * 获取当前语言
     *
     * @return locale locale
     */
    public static Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}
