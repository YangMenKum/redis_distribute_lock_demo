package com.redistest.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author zonzie
 * @date 2018/8/10 11:16
 */
public class JedisPoolUtil {

    private static volatile JedisPool jedisPool = null;

    public static JedisPool getJedisPool() {
        if(jedisPool == null) {
            synchronized (JedisPoolUtil.class) {
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                // 连接池的参数配置
                poolConfig.setMaxIdle(32);
                poolConfig.setMaxTotal(1000);
                poolConfig.setMaxWaitMillis(1000*100);
                poolConfig.setTestOnBorrow(true);
                jedisPool = new JedisPool(poolConfig, "192.168.198.128", 6379, 1000);

            }
        }
        return jedisPool;
    }
    // 释放jedis连接的方法
    public static void release(Jedis jedis) {
        jedis.close();
    }

    private JedisPoolUtil() {
    }
}
