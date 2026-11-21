package db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String URL = "jdbc:sqlite:school.db";

    /** Файл со схемой БД лежит рядом с этим классом в classpath. */
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void init() {
        try (Statement st = getConnection().createStatement()) {
            st.executeUpdate(readSchema());
            seedAdmin();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка инициализации БД", e);
        }
    }

    /** Читает SQL-схему из ресурса {@value #SCHEMA_RESOURCE}. */
    private static String readSchema() {
        try (InputStream in = Database.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Не найден файл схемы БД в classpath: " + SCHEMA_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка чтения схемы БД: " + SCHEMA_RESOURCE, e);
        }
    }

    private static void seedAdmin() throws SQLException {
        String check = "SELECT COUNT(*) FROM users WHERE role = 'TEACHER'";
        try (var rs = getConnection().createStatement().executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO users(username, password, role) VALUES('admin','admin','TEACHER')";
                getConnection().createStatement().execute(insert);
                System.out.println("[init] Создан учитель по умолчанию: admin / admin");
            }
        }
    }
}
