package com.easypan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {
    
    private static final Logger logger = LoggerFactory.getLogger(CopyTools.class);
    
    @SuppressWarnings("null")
    public static <T, S> List<T> copyList(List<S> sList, Class<T> classz) {
        List<T> list = new ArrayList<T>();
        for (S s : sList) {
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

    @SuppressWarnings("null")
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
