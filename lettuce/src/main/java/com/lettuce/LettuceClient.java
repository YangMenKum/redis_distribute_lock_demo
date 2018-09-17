package com.lettuce;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zonzie
 * @date 2018/8/10 17:25
 */
public class LettuceClient {

//    @Resource(name = "lettuceConnect")
//    private StatefulRedisConnection<String, String> connection;

    private static StatefulRedisConnection<String, String> connection;

    private static ThreadPoolExecutor poolExecutor;

    // 互斥锁的key
    private static final String MUTEX_KEY = "mutex_key";

    static {
        // 初始化connection
        RedisClient client = RedisClient.create(RedisURI.create("redis://192.168.198.128:6379"));
        connection = client.connect();
        // 初始化线程池
        poolExecutor = new ThreadPoolExecutor(5, 10, 200, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(5), new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    private static final int DEFAULT_TIME = 1000;
    private static final String LUA_SCRIPT = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";

    /**
     * 同步set
     * @param key
     * @param value
     */
    public String lettuceSet(String key, String value) {
        RedisCommands<String, String> commands = connection.sync();
        SetArgs px = SetArgs.Builder.nx().px(5000);
        return commands.set(key, value, px);
    }

    /**
     * 同步get
     * @param key
     * @return
     */
    public String lettuceGet(String key) {
        RedisCommands<String, String> sync = connection.sync();
        return sync.get(key);
    }

    /**
     * 异步set
     * @param key
     * @param value
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public String lettuceAsyncSet(String key, String value) throws ExecutionException, InterruptedException {
        RedisAsyncCommands<String, String> async = connection.async();
        // 加锁
        RedisFuture<String> set = async.set(key, value, SetArgs.Builder.nx().px(10000));
        String s = set.get();
        System.out.println(s);
        return s;
    }

    /**
     * 阻塞锁
     * @param key
     * @param value
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public String tryLock(String key, String value) throws ExecutionException, InterruptedException {
        // 自定义阻塞的时间
        int blockTIme = 5000;
        RedisAsyncCommands<String, String> async = connection.async();
        for (;;) {
            if(blockTIme >= 0) {
                // 加锁
                RedisFuture<String> set = async.set(key, value, SetArgs.Builder.nx().px(10000));
                String s = set.get();
                System.out.println(s == null ? "waiting..." : s);
                if("OK".equalsIgnoreCase(s)) {
                    break;
                }
                blockTIme -= DEFAULT_TIME;
                Thread.sleep(DEFAULT_TIME);
            } else {
                System.out.println("over blocktime....");
                break;
            }
        }
        return "success";
    }

    /**
     * 解锁
     * @param key
     * @param value
     */
    public Object unlock(String key, String value) throws ExecutionException, InterruptedException {

        RedisAsyncCommands<String, String> async = connection.async();
        String[] strings = {key};
        RedisFuture<Long> eval = async.eval(LUA_SCRIPT, ScriptOutputType.INTEGER, strings, value);
        Long aLong = eval.get();
        System.out.println("解锁结果-result: " + aLong);
        return aLong;
    }

    /**
     * 异步get
     * @param key
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public String lettuceAsyncGet(String key) throws ExecutionException, InterruptedException {
        RedisAsyncCommands<String, String> async = connection.async();
        RedisFuture<String> stringRedisFuture = async.get(key);
        String s = stringRedisFuture.get();
        System.out.println(s);
        return s;
    }

    /**
     * 解决缓存击穿问题, 方法一
     * 互斥锁
     * 为了解决缓存击穿的问题, 业界常用的做法--设置mutex key
     * @param key 互斥锁的key
     */
    public String mutexGet(String key) throws InterruptedException {
        RedisCommands<String, String> sync = connection.sync();
        String value = sync.get(key);
        if(value == null) {
            String set = sync.set(MUTEX_KEY, "1", SetArgs.Builder.nx().px(3 * 60));
            if("OK".equalsIgnoreCase(set)) {
                // 从数据库获取value
                value = "getValueByKeyFromDB";
                sync.set(key, value, SetArgs.Builder.px(1000 * 60 * 60 * 2));
                sync.del(MUTEX_KEY);
                return value;
            } else {
                Thread.sleep(50);
                // 递归重试
                mutexGet(key);
            }
        }
        return value;
    }

    /**
     * 解决缓存击穿的问题,方法二
     * 提前更新key
     * @param key
     */
    public String get(final String key) throws InterruptedException {
        final RedisCommands<String, String> sync = connection.sync();
        String value = sync.get(key);
        Long ttl = sync.ttl(key);
        // 如果key已经过期,立即从数据库获取新的value
        if(value == null) {
            String set = sync.set(MUTEX_KEY, "1", SetArgs.Builder.nx().px(3 * 60));
            if("OK".equalsIgnoreCase(set)) {
                // 从数据库获取value
                String var = "getValueByKeyFromDB";
                sync.set(key, var, SetArgs.Builder.px(1000 * 60 * 60 * 2));
                sync.del(MUTEX_KEY);
                // 返回新的值
                return var;
            } else {
                Thread.sleep(50);
                // 递归重试
                get(key);
            }
        }
        // 10秒后key过期
        if(ttl < 10) {
            // 异步执行缓存更新操作
            poolExecutor.execute(new Runnable() {
                public void run() {
                    // 设置互斥锁
                    String set = sync.set(MUTEX_KEY, "1", SetArgs.Builder.nx().px(3 * 60));
                    if("OK".equalsIgnoreCase(set)) {
                        // 从数据库获取value
                        String var = "getValueByKeyFromDB";
                        // 设置新的value
                        sync.setex(key, 2*60*60, var);
                        // 删除互斥锁
                        sync.del(MUTEX_KEY);
                    }
                }
            });
            // 返回旧的值
            return value;
        }
        return value;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        LettuceClient lettuceClient = new LettuceClient();
//        String s = lettuceClient.lettuceSet("hello", "world");
//        System.out.println(s);
//        String s1 = lettuceClient.tryLock("hello", "world");
//        lettuceClient.unlock("hello","world");
        lettuceClient.get("hello");
    }

}
