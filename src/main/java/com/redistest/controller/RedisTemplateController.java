package com.redistest.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author zonzie
 * @date 2018/8/10 10:39
 */
@RestController
@RequestMapping
public class RedisTemplateController {

    @Resource(name = "myRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @GetMapping(value = "set")
    public String set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        return "success";
    }

    @GetMapping(value = "get")
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
