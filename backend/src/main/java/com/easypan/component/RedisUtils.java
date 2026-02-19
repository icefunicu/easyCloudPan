package com.easypan.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类，提供基本的缓存操作.
 */
@Component("redisUtils")
public class RedisUtils<V> {

    @Resource
    private RedisTemplate<String, V> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    /**
     * 删除缓存.
     *
     * @param key 可以传一个值或多个
     */
    public void delete(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /**
     * 获取缓存值.
     *
     * @param key 键
     * @return 值
     */
    public V get(String key) {
        if (key == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入.
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, V value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("设置redisKey:{},value:{}失败", key, value);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间.
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false失败
     */
    public boolean setex(String key, V value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("设置redisKey:{},value:{}失败", key, value);
            return false;
        }
    }

    /**
     * 指定缓存失效时间.
     *
     * @param key  键
     * @param time 时间(秒)
     * @return boolean
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            logger.error("设置过期时间失败 key:{}", key, e);
            return false;
        }
    }

    /**
     * Set放入.
     *
     * @param key    键
     * @param values 值
     * @return 成功个数
     */
    @SafeVarargs
    public final long setSet(String key, V... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            logger.error("Set放入失败 key:{}", key, e);
            return 0;
        }
    }

    /**
     * 获取Set缓存的大小.
     *
     * @param key 键
     * @return long
     */
    public long getSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            logger.error("获取Set大小失败 key:{}", key, e);
            return 0;
        }
    }

    /**
     * 递增.
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return long
     */
    public long increment(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 缓存空值标记（用于防止缓存穿透）.
     *
     * @param key   键
     * @param time  过期时间（秒）
     * @return 是否成功
     */
    public boolean setNullMarker(String key, long time) {
        try {
            redisTemplate.opsForValue().set(key, null, time, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            logger.error("设置空值标记失败 key:{}", key, e);
            return false;
        }
    }

    /**
     * 检查是否为空值标记.
     *
     * @param key 键
     * @return 是否存在空值标记
     */
    public boolean isNullMarker(String key) {
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(hasKey)) {
                V value = redisTemplate.opsForValue().get(key);
                return value == null;
            }
            return false;
        } catch (Exception e) {
            logger.error("检查空值标记失败 key:{}", key, e);
            return false;
        }
    }

    /**
     * 获取缓存值，支持空值标记检查.
     *
     * @param key           键
     * @param nullMarkerKey 空值标记键
     * @return 值（如果存在空值标记返回特殊的 NULL_MARKER）
     */
    @SuppressWarnings("unchecked")
    public V getWithNullMarker(String key, String nullMarkerKey) {
        // 先检查空值标记
        if (isNullMarker(nullMarkerKey)) {
            return (V) "NULL_MARKER";
        }
        return get(key);
    }
}
