import java.sql.*;
import redis.clients.jedis.Jedis;

public class UpdatePost {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java UpdatePost <email> <post_id> <new_title> <new_content>");
            return;
        }

        String email = args[0];
        int postId = Integer.parseInt(args[1]);
        String newTitle = args[2];
        String newContent = args[3];

        // Koneksi ke SQLite
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");

        // Ambil postingan lama sebelum update
        String oldPost = null;
        String sqlSelect = "SELECT title, content FROM posts WHERE id = ? AND email = ?";
        PreparedStatement stmtSelect = conn.prepareStatement(sqlSelect);
        stmtSelect.setInt(1, postId);
        stmtSelect.setString(2, email);
        ResultSet rs = stmtSelect.executeQuery();

        if (rs.next()) {
            oldPost = rs.getString("title") + " - " + rs.getString("content");
        } else {
            System.out.println("Post not found or not owned by you!");
            return;
        }

        // Update di SQLite
        String sqlUpdate = "UPDATE posts SET title = ?, content = ? WHERE id = ? AND email = ?";
        PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate);
        stmtUpdate.setString(1, newTitle);
        stmtUpdate.setString(2, newContent);
        stmtUpdate.setInt(3, postId);
        stmtUpdate.setString(4, email);

        int rowsUpdated = stmtUpdate.executeUpdate();
        if (rowsUpdated > 0) {
            System.out.println("[INFO] Post updated in SQLite!");
        } else {
            System.out.println("[ERROR] Update failed! Check post ID and email.");
            return;
        }

        // Koneksi ke Redis
        Jedis jedis = new Jedis("localhost", 6379);

        // Update di Redis (hapus yang lama, tambahkan yang baru)
        jedis.lrem("posts:" + email, 1, oldPost); // Hapus postingan lama
        String newPost = newTitle + " - " + newContent;
        jedis.lpush("posts:" + email, newPost); // Tambahkan postingan baru

        System.out.println("[INFO] Post updated in Redis!");

        // Ambil daftar followers dari SQLite
        String sqlFollowers = "SELECT email FROM follows WHERE email_to_follow = ?";
        PreparedStatement stmtFollowers = conn.prepareStatement(sqlFollowers);
        stmtFollowers.setString(1, email);
        ResultSet rsFollowers = stmtFollowers.executeQuery();

        // Kirim notifikasi ke followers di Redis
        while (rsFollowers.next()) {
            String followerEmail = rsFollowers.getString("email");
            String message = "🔔 " + email + " updated their post: " + newTitle;
            jedis.lpush("mailbox:" + followerEmail, message);
            System.out.println("[INFO] Notification sent to: " + followerEmail);
        }

        // Tutup koneksi
        rs.close();
        stmtSelect.close();
        stmtUpdate.close();
        stmtFollowers.close();
        conn.close();
        jedis.close();

        System.out.println("Post updated successfully!");
    }
}

