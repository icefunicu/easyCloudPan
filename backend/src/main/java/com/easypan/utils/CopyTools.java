package com.easypan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 对象复制工具类.
 */
@SuppressWarnings("all")
public class CopyTools {

    private static final Logger logger = LoggerFactory.getLogger(CopyTools.class);

    /**
     * 复制列表.
     *
     * @param sourceList 源列表
     * @param classz 目标类型
     * @param <T> 目标类型
     * @param <S> 源类型
     * @return 目标列表
     */
    public static <T, S> List<T> copyList(List<S> sourceList, Class<T> classz) {
        List<T> list = new ArrayList<T>();
        for (S s : sourceList) {
            T t = null;
            try {
                t = classz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                logger.error("对象复制失败: class={}", classz.getName(), e);
            }
            if (t != null) {
                BeanUtils.copyProperties(s, t);
                list.add(t);
            }
        }
        return list;
    }

    /**
     * 复制对象.
     *
     * @param s 源对象
     * @param classz 目标类型
     * @param <T> 目标类型
     * @param <S> 源类型
     * @return 目标对象
     */
    public static <T, S> T copy(S s, Class<T> classz) {
        T t = null;
        try {
            t = classz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("对象复制失败: class={}", classz.getName(), e);
        }
        if (t != null) {
            BeanUtils.copyProperties(s, t);
        }
        return t;
    }
}
