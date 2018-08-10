package com.redistest.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zonzie
 * @date 2018/8/10 17:26
 */
@Configuration
public class LettuceConfig {

    @Bean(name = "lettuceConnect")
    public StatefulRedisConnection<String, String> lettuceConfig() {
        RedisClient client = RedisClient.create(RedisURI.create("redis://192.168.198.128:6379"));
        StatefulRedisConnection<String, String> connect = client.connect();
        return connect;
    }
}
