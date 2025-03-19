import java.sql.*;
import redis.clients.jedis.Jedis;
import java.util.List;

public class Timeline {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Timeline <email>");
            return;
        }

        String email = args[0];

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");

        // Ambil notifikasi dari Redis
        Jedis jedis = new Jedis("localhost", 6379);
        List<String> notifications = jedis.lrange("mailbox:" + email, 0, -1);
        
        if (!notifications.isEmpty()) {
            System.out.println("🔔 New Notifications:");
            for (String notif : notifications) {
                System.out.println("- " + notif);
            }
            System.out.println("=================================");
            jedis.del("mailbox:" + email); // Hapus notifikasi setelah ditampilkan
        }
        jedis.close();

        // Ambil timeline dari SQLite
        String sql = "SELECT p.name, p.title, p.content, p.created_at FROM posts p " +
                     "JOIN follows f ON p.email = f.email_to_follow " +
                     "WHERE f.email = ? ORDER BY p.created_at DESC";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            System.out.println("👤 " + rs.getString("name"));
            System.out.println("📝 " + rs.getString("title"));
            System.out.println("📅 " + rs.getString("created_at"));
            System.out.println("📖 " + rs.getString("content"));
            System.out.println("=================================");
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}

