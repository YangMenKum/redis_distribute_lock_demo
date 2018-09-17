package redissonredlock;

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * @author zonzie
 * @date 2018/8/13 12:06
 */
public class RedLock {

    public RedissonClient getRedissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.198.128:6379");
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

    public void redLock() throws InterruptedException {
        RLock lock1 = getRedissonClient().getLock("lock1");
        RedissonRedLock redissonRedLock = new RedissonRedLock(lock1);
        boolean b = redissonRedLock.tryLock(100, 10, TimeUnit.SECONDS);
        redissonRedLock.unlock();
    }
}
