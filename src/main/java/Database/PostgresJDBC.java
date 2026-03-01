package Database;

import java.sql.*;
import io.github.cdimascio.dotenv.Dotenv;

public class PostgresJDBC {

    public Connection connection; // DATABASE connection :)

    public static void main(String[] args) {
        PostgresJDBC example = new PostgresJDBC();
        example.establishConnection();
        example.createTable();
        example.closeConnection();
    }

    public void establishConnection() {

        // LOAD VARIABLES FROM ENV FILE
        Dotenv dotenv = Dotenv.load();

        String dbName = dotenv.get("DB_NAME");
        String username = dotenv.get("PSQL_USERNAME");
        String password = dotenv.get("PSQL_PASSWORD");

        String jdbcURL = "jdbc:postgresql://localhost:5432/" + dbName;

        try {
            // Load the PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");

            // Assign to class field!
            connection = DriverManager.getConnection(jdbcURL, username, password);
            System.out.println("Connected to PostgreSQL database!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTable() {
        try {
            Statement statement = connection.createStatement();

            // USERS TABLE
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    steam_id VARCHAR(32) UNIQUE NOT NULL,
                    username TEXT,
                    avatar_url TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP,
                    role TEXT DEFAULT 'USER',
                    banned BOOLEAN DEFAULT FALSE
                );
            """;
            statement.execute(createUsersTable);
            System.out.println("Table 'users' ready!");

            // PROFILES TABLE
            String createProfilesTable = """
                CREATE TABLE IF NOT EXISTS profiles (
                    user_id INTEGER PRIMARY KEY,
                    display_name TEXT,
                    bio TEXT,
                    xp INTEGER DEFAULT 0,
                    coins INTEGER DEFAULT 0,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """;
            statement.execute(createProfilesTable);
            System.out.println("Table 'profiles' ready!");

            // Example insert
            String insertSQL = """
                INSERT INTO users (steam_id, username, avatar_url)
                VALUES ('76561198012345678', 'JohnDoe', 'https://avatar.url')
                ON CONFLICT (steam_id) DO NOTHING
            """;
            statement.executeUpdate(insertSQL);
            System.out.println("Inserted example data into 'users' table!");

            // Retrieve data
            String selectSQL = "SELECT * FROM users";
            ResultSet resultSet = statement.executeQuery(selectSQL);

            while (resultSet.next()) {
                System.out.println(
                        "User ID: " + resultSet.getInt("id")
                                + ", SteamID: " + resultSet.getString("steam_id")
                                + ", Username: " + resultSet.getString("username")
                                + ", Avatar: " + resultSet.getString("avatar_url")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // APPLICATION FUNCTIONALITY

    public int findOrCreateUser(String steamId, String username, String avatarUrl){
        // check if user already exist
        String selectsSql = """
                    SELECT id FROM users WHERE steam_id = ?
                """;
        try(PreparedStatement ps = connection.prepareStatement(selectsSql)){
            ps.setString(1, steamId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                int userId = rs.getInt("id");

                String updateSql = """
                                UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?
                           """;
                try (PreparedStatement updatePs = connection.prepareStatement(updateSql)) {
                    updatePs.setInt(1, userId);
                    updatePs.execute();
                }
                return userId;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // CREATE USER IF THEY DO NOT EXIST
        String insertSql = "INSERT INTO users (steam_id, username, avatar_url, last_login) VALUES (?, ?, ?, CURRENT_TIMESTAMP) RETURNING id";
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            // FIX: Uncommented and added all parameters
            ps.setString(1, steamId);
            ps.setString(2, username);
            ps.setString(3, avatarUrl);

            ResultSet rs = ps.executeQuery();
            rs.next();
            int userId = rs.getInt("id");

            // Create profile
            createUserProfile(username, userId );

            return userId;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createUserProfile(String displayname, int user_id){
        String insertProfileSQL = "INSERT INTO profiles (user_id, display_name, bio, xp, coins) VALUES (?, ?, ?, ?, ?)";
        try(PreparedStatement ps = connection.prepareStatement(insertProfileSQL)){
            ps.setInt(1, user_id);
            ps.setString(2, displayname);
            ps.setString(3," "); //DEFAULT BIO?
            ps.setInt(4, 0);
            ps.setInt(5,0);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    // Getter for connection
    public Connection getConnection() {
        return connection;
    }
}