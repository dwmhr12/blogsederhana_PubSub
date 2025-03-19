import redis.clients.jedis.Jedis;

public class Mailbox {
    private static final Jedis jedis = RedisManager.getJedis();

    public static void sendMessage(String user, String message) {
        String key = "mailbox:" + user;
        jedis.lpush(key, message);
    }

    public static void getMessages(String user) {
        String key = "mailbox:" + user;
        System.out.println("Mailbox for " + user + ":");
        for (String msg : jedis.lrange(key, 0, -1)) {
            System.out.println("- " + msg);
        }
    }
}
