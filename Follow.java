import java.sql.*;
import redis.clients.jedis.Jedis;

public class Follow {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Follow <email_follower> <email_to_follow>");
            return;
        }

        String emailFollower = args[0];
        String emailToFollow = args[1];

        // Koneksi ke SQLite
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");

        // Simpan ke tabel follows di SQLite
        String sql = "INSERT INTO follows (email, email_to_follow, follow_created) VALUES (?, ?, datetime('now'))";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, emailFollower);
        stmt.setString(2, emailToFollow);
        stmt.executeUpdate();
        stmt.close();
        conn.close();

        // Koneksi ke Redis
        Jedis jedis = new Jedis("redis://localhost:6379");

        // Simpan daftar following (siapa yang diikuti oleh emailFollower)
        String followingKey = "following:" + emailFollower;
        jedis.sadd(followingKey, emailToFollow);

        // Simpan daftar followers (siapa yang mengikuti emailToFollow)
        String followersKey = "followers:" + emailToFollow;
        jedis.sadd(followersKey, emailFollower);

        jedis.close();

        System.out.println(emailFollower + " now follows " + emailToFollow + " (saved in SQLite & Redis)");
    }
}

