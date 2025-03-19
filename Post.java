import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import redis.clients.jedis.Jedis;

public class Post {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java Post <name> <email> <title> <content>");
            return;
        }

        String name = args[0];
        String email = args[1];
        String title = args[2];
        String content = args[3];

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String createdAt = now.format(formatter);

        // Koneksi ke SQLite
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");

        // Simpan postingan ke SQLite
        String sql = "INSERT INTO posts (name, email, created_at, title, content) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, name);
        stmt.setString(2, email);
        stmt.setString(3, createdAt);
        stmt.setString(4, title);
        stmt.setString(5, content);
        stmt.executeUpdate();
        stmt.close();
 	// **Buat koneksi ke Redis**
        Jedis jedis = new Jedis("localhost", 6379);

        // Simpan postingan ke Redis
        jedis.lpush("posts:" + email, title + " - " + content);
        System.out.println("[DEBUG] Postingan disimpan di Redis: posts:" + email);

        // Ambil daftar followers dari SQLite
        sql = "SELECT email FROM follows WHERE email_to_follow = ?";
        stmt = conn.prepareStatement(sql);
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();

        System.out.println("[DEBUG] Mencari followers untuk: " + email);

        // Simpan notifikasi ke Redis Mailbox tiap followers
        boolean hasFollowers = false;
        while (rs.next()) {
            hasFollowers = true;
            String followerEmail = rs.getString("email");
            String message = "🔔 " + name + " posted: " + title;

            // Debugging: Print notifikasi yang akan dikirim
            System.out.println("[DEBUG] Mengirim notifikasi ke: " + followerEmail + " -> " + message);

            jedis.lpush("mailbox:" + followerEmail, message);
        }

        if (!hasFollowers) {
            System.out.println("[DEBUG] Tidak ada followers yang menerima notifikasi.");
        }

        rs.close();
        stmt.close();
        conn.close();
        jedis.close();
 System.out.println("Post added successfully!");
    }
}
