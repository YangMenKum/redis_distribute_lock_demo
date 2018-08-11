package com.redistemplate.templatetest;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;

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

    /**
     * 加锁
     * @param key
     * @param value
     * @return
     */
    @GetMapping(value = "redisTemplateLock")
    public Object lock(String key, String value) {
        String script = "local key = KEYS[1]; local value = ARGV[1]; if redis.call('set', key, value, 'NX' ,'PX', 5000) then return 1 else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Object execute = redisTemplate.execute(redisScript, Collections.singletonList(key), Collections.singletonList(value));
        System.out.println(execute);
        return execute;
    }

    /**
     * 阻塞锁
     */
    @GetMapping(value = "blockLock")
    public String blockLock(String key, String value) throws InterruptedException {
        // 被阻塞的时间超过5秒就停止获取锁
        int blockTime = 5000;
        // 默认的间隔时间
        int defaultTime = 1000;
        for(;;) {
            if(blockTime >= 0) {
                String script = "local key = KEYS[1]; local value = ARGV[1]; if redis.call('set', key, value, 'NX' ,'PX', 5000) then return 1 else return 0 end";
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, long.class);
                Long result = redisTemplate.execute(redisScript, Collections.singletonList(key), value);
                System.out.println("try lock ... ,result: "+result);
                if(result != null && result == 1) {
                    // 得到了锁
                    return "lock success";
                } else {
                    blockTime -= defaultTime;
                    Thread.sleep(1000);
                }
            } else {
                // 已经超时
                return "lock timeout..., please retry later...";
            }
        }
    }

    /**
     * 解锁
     * @param key
     * @param value
     */
    @GetMapping("redisTemplateUnlock")
    public String unlock(String key, String value) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long execute = redisTemplate.execute(redisScript, Collections.singletonList(key), value);
        System.out.println("unlock result: "+execute);
        if(execute != null && execute != 0) {
            // 解锁成功
            return "unlock success";
        } else {
            return "unlock failed";
        }
    }
}
