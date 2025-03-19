import java.sql.*;
import redis.clients.jedis.Jedis;

public class DeletePost {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java DeletePost <email> <post_id>");
            return;
        }

        String email = args[0];
        int postId = Integer.parseInt(args[1]);

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");

        // Ambil judul postingan sebelum dihapus
        String getPostSQL = "SELECT title FROM posts WHERE id = ? AND email = ?";
        PreparedStatement getPostStmt = conn.prepareStatement(getPostSQL);
        getPostStmt.setInt(1, postId);
        getPostStmt.setString(2, email);
        ResultSet rs = getPostStmt.executeQuery();

        String postTitle = null;
        if (rs.next()) {
            postTitle = rs.getString("title");
        } else {
            System.out.println("Post tidak ditemukan atau bukan milik Anda.");
            return;
        }
        rs.close();
        getPostStmt.close();

        // Hapus dari SQLite
        String deleteSQL = "DELETE FROM posts WHERE id = ? AND email = ?";
        PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL);
        deleteStmt.setInt(1, postId);
        deleteStmt.setString(2, email);

        int rowsDeleted = deleteStmt.executeUpdate();
        deleteStmt.close();
        conn.close();

        if (rowsDeleted > 0) {
            System.out.println("Post deleted successfully!");
            
            // **Hapus dari Redis**
            Jedis jedis = new Jedis("localhost", 6379);

            // Hapus dari daftar postingan user di Redis
            jedis.lrem("posts:" + email, 1, postTitle + " - Isi Post"); // Sesuaikan format isi post

            // Hapus dari mailbox followers
            String getFollowersSQL = "SELECT email FROM follows WHERE email_to_follow = ?";
            conn = DriverManager.getConnection("jdbc:sqlite:database.db");
            PreparedStatement followersStmt = conn.prepareStatement(getFollowersSQL);
            followersStmt.setString(1, email);
            ResultSet followersRs = followersStmt.executeQuery();

            while (followersRs.next()) {
                String followerEmail = followersRs.getString("email");
                String notification = "🔔 " + email + " posted: " + postTitle;
                jedis.lrem("mailbox:" + followerEmail, 1, notification);
            }

            followersRs.close();
            followersStmt.close();
            conn.close();
            jedis.close();
        } else {
            System.out.println("Gagal menghapus post. Pastikan post ID dan email benar.");
        }
    }
}

