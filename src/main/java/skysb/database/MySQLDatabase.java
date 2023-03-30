package skysb.database;

import skysb.skysb.SkySB;

import java.sql.*;

public class MySQLDatabase {
    private Connection connection;

    private String url;
    private String username;
    private String password;
    private String database;
    private String port;

    public MySQLDatabase(String url, String database, String port, String username, String password) {
        this.url = url;
        this.database = database;
        this.port = port;
        this.username = username;
        this.password = password;

        open();
    }

    public void open()
    {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + url + ":" + port + "/" + database, username, password);

            SkySB.Instance.getLogger().info("Database successfully connected");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet executeQuery(String query) {
        try {
            Statement statement = connection.createStatement();

            return statement.executeQuery(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int executeUpdate(String query) {
        try {
            Statement statement = connection.createStatement();

            return statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed())
            return connection;

        open();

        return connection;
    }

    public void close() {
        if (connection == null)
            return;

        try {
            connection.close();
            System.out.println("Database connection successfully closed");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}