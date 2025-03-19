import java.sql.*;

public class Profile {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Profile <email>");
            return;
        }

        String email = args[0];

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");

        // Ambil semua postingan pengguna berdasarkan email
        String sql = "SELECT name, email, title, content, created_at FROM posts WHERE email = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();

        boolean found = false;
        while (rs.next()) {
            found = true;
            System.out.println("Nama       : " + rs.getString("name"));
            System.out.println("Email      : " + rs.getString("email"));
            System.out.println("Judul      : " + rs.getString("title"));
            System.out.println("Konten     : " + rs.getString("content"));
            System.out.println("Dibuat pada: " + rs.getString("created_at"));
            System.out.println("--------------------------------------");
        }

        if (!found) {
            System.out.println("Pengguna dengan email " + email + " belum memiliki postingan.");
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}

