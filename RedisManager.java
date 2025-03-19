import redis.clients.jedis.Jedis;

public class RedisManager {
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static Jedis jedis;

    static {
        jedis = new Jedis(REDIS_HOST, REDIS_PORT);
        System.out.println("Redis Connected: " + jedis.ping());
    }

    public static Jedis getJedis() {
        return jedis;
    }

    public static void close() {
        jedis.close();
    }
}
