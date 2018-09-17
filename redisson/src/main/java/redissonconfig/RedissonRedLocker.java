package redissonconfig;

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author zonzie
 * @date 2018/8/13 12:01
 */
public class RedissonRedLocker {
    private static final int WAIT_TIME = 10; // 10s
    private static final long TIME_OUT = 10; // 10s

    private static final String REDIS_NODES_URL = "ip1:port,ip2:port,ip3:port";
    private static RedissonClient[] clients;

    static {
        initRedisInstance();
    }

    private static void initRedisInstance() {
        String[] redisAddrs = REDIS_NODES_URL.split(",");
        List<RedissonClient> list = new ArrayList<RedissonClient>();
        for(String addr: redisAddrs) {
            list.add(getRedisInstance(addr));
        }
        clients = list.toArray(clients);
    }

    private static RedissonClient getRedisInstance(String addr) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + addr);
        return Redisson.create(config);
    }

    private static RedissonRedLock getRedLock(String resource) {
        List<RLock> locks = new ArrayList<RLock>();
        for(RedissonClient client : clients) {
            locks.add(client.getLock(resource));
        }
        return new RedissonRedLock((RLock[]) locks.toArray());
    }

    private static boolean tryLock(RedissonRedLock lock) {
        boolean res = false;
        try {
            res = lock.tryLock(WAIT_TIME, TIME_OUT, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            e.printStackTrace();
            lock.unlock();
        }
        return res;
    }
}
