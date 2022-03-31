package com.example.faq.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 封装redis常用操作
 */
@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * get方法
     *
     * @param key 键
     * @return 值
     */
    public Object get(Object key) {
        return redisTemplate.opsForValue().get(key);
    }


    /**
     * set方法
     *
     * @param key   键
     * @param value 值
     */
    public void set(Object key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }


    /**
     * 设置过期时间
     *
     * @param key  键
     * @param time 过期时间，默认单位为分钟（minutes）
     */
    public void expire(Object key, long time) {
        redisTemplate.expire(key, time, TimeUnit.MINUTES);
    }

    /**
     * 设置过期时间
     *
     * @param key      键
     * @param time     过期时间
     * @param timeUnit 时间单位
     */
    public void expire(Object key, long time, TimeUnit timeUnit) {
        redisTemplate.expire(key, time, timeUnit);
    }

}
