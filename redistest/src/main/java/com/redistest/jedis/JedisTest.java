package com.redistest.jedis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

/**
 * @author zonzie
 * @date 2018/8/10 12:38
 */
@RestController
public class JedisTest {

    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";

    /**
     * 加锁
     * @param key
     * @param value
     * @return
     */
    @GetMapping("jedisTryLock")
    public String tryLock(String key, String value) {
        JedisPool jedisPool = JedisPoolUtil.getJedisPool();
        Jedis jedis = jedisPool.getResource();
        // 一条命令保证设置时的原子性 如果不再存在就set,并且有超时时间
        String set = jedis.set(key, value, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, 5000);
        System.out.println("set result: "+set);
        return set;
    }

    /**
     * 阻塞锁
     * @param key
     * @param value
     * @throws InterruptedException
     */
    @GetMapping("jedisLock")
    public String lock(String key, String value) throws InterruptedException {
        for(;;) {
            Jedis resource = JedisPoolUtil.getJedisPool().getResource();
            String set = resource.set(key, value, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, 10000);
            System.out.println("result: "+set);
            if("OK".equalsIgnoreCase(set)) {
                return "success";
            }
            Thread.sleep(1000);
        }
    }

    /**
     * 解锁
     * @param key
     * @param value
     */
    @GetMapping("jedisUnlock")
    public Object unLock(String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Jedis resource = JedisPoolUtil.getJedisPool().getResource();
        Object eval = resource.eval(luaScript, Collections.singletonList(key), Collections.singletonList(value));
        System.out.println(eval);
        return eval;
    }

}
