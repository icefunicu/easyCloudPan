package com.easypan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 日期工具类，提供日期格式化和解析功能.
 */
public class DateUtil {

    private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);
    private static final Object LOCK_OBJ = new Object();
    private static Map<String, ThreadLocal<SimpleDateFormat>> sdfMap = new HashMap<String, ThreadLocal<SimpleDateFormat>>();

    private static SimpleDateFormat getSdf(final String pattern) {
        ThreadLocal<SimpleDateFormat> tl = sdfMap.get(pattern);
        if (tl == null) {
            synchronized (LOCK_OBJ) {
                tl = sdfMap.get(pattern);
                if (tl == null) {
                    tl = new ThreadLocal<SimpleDateFormat>() {
                        @Override
                        protected SimpleDateFormat initialValue() {
                            return new SimpleDateFormat(pattern);
                        }
                    };
                    sdfMap.put(pattern, tl);
                }
            }
        }

        return tl.get();
    }

    /**
     * 格式化日期.
     *
     * @param date 日期对象
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    public static String format(Date date, String pattern) {
        return getSdf(pattern).format(date);
    }

    /**
     * 解析日期字符串.
     *
     * @param dateStr 日期字符串
     * @param pattern 格式模式
     * @return 日期对象
     */
    public static Date parse(String dateStr, String pattern) {
        try {
            return getSdf(pattern).parse(dateStr);
        } catch (ParseException e) {
            logger.error("日期解析失败: dateStr={}, pattern={}", dateStr, pattern, e);
        }
        return new Date();
    }

    /**
     * 获取指定天数后的日期.
     *
     * @param day 天数
     * @return 日期对象
     */
    public static Date getAfterDate(Integer day) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, day);
        return calendar.getTime();
    }
}
