package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String URL = "jdbc:sqlite:school.db";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void init() {
        String users = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('STUDENT','TEACHER'))
            );
            """;

        String subjects = """
            CREATE TABLE IF NOT EXISTS subjects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            );
            """;

        String schedule = """
            CREATE TABLE IF NOT EXISTS schedule (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
                lesson_number INTEGER NOT NULL CHECK(lesson_number BETWEEN 1 AND 10),
                subject_id INTEGER NOT NULL,
                teacher_id INTEGER NOT NULL,
                class_name TEXT NOT NULL,
                FOREIGN KEY(subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
                FOREIGN KEY(teacher_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE(day_of_week, lesson_number, class_name)
            );
            """;

        try (Statement st = getConnection().createStatement()) {
            st.execute(users);
            st.execute(subjects);
            st.execute(schedule);
            seedAdmin();
            seedTestData();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка инициализации базы данных", e);
        }
    }

    private static void seedAdmin() throws SQLException {
        String check = "SELECT COUNT(*) FROM users WHERE role = 'TEACHER'";
        try (var rs = getConnection().createStatement().executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO users(username, password, role) VALUES('admin','admin','TEACHER')";
                getConnection().createStatement().execute(insert);
                System.out.println("[инициализация] Создан учитель по умолчанию: admin / admin");
            }
        }
    }

    private static void seedTestData() throws SQLException {
        // Тестовые предметы
        String[] subjects = { "Математика", "Физика", "Информатика" };
        String insertSubject = "INSERT OR IGNORE INTO subjects(name) VALUES(?)";
        try (var ps = getConnection().prepareStatement(insertSubject)) {
            for (String s : subjects) {
                ps.setString(1, s);
                ps.executeUpdate();
            }
        }

        // Тестовые студенты
        String[] students = { "Иванов", "Петров", "Сидоров" };
        String insertUser = "INSERT OR IGNORE INTO users(username, password, role) VALUES(?, ?, 'STUDENT')";
        try (var ps = getConnection().prepareStatement(insertUser)) {
            for (String u : students) {
                ps.setString(1, u);
                ps.setString(2, "студент"); // пароль по умолчанию
                ps.executeUpdate();
            }
        }

        // Получаем идентификатор учителя и предметов
        int teacherId = -1;
        try (var rs = getConnection().createStatement()
                .executeQuery("SELECT id FROM users WHERE username = 'admin' LIMIT 1")) {
            if (rs.next()) teacherId = rs.getInt(1);
        }
        if (teacherId < 0) return;

        int mathId = -1, physId = -1;
        try (var rs = getConnection().createStatement()
                .executeQuery("SELECT id, name FROM subjects")) {
            while (rs.next()) {
                if ("Математика".equals(rs.getString("name"))) mathId = rs.getInt("id");
                if ("Физика".equals(rs.getString("name")))     physId = rs.getInt("id");
            }
        }

        // Тестовое расписание
        String insertSchedule = """
            INSERT OR IGNORE INTO schedule(day_of_week, lesson_number, subject_id, teacher_id, class_name)
            VALUES(?, ?, ?, ?, ?)
            """;
        try (var ps = getConnection().prepareStatement(insertSchedule)) {
            if (mathId > 0) {
                ps.setInt(1, 1); ps.setInt(2, 1); ps.setInt(3, mathId);
                ps.setInt(4, teacherId); ps.setString(5, "10А");
                ps.executeUpdate();
            }
            if (physId > 0) {
                ps.setInt(1, 1); ps.setInt(2, 2); ps.setInt(3, physId);
                ps.setInt(4, teacherId); ps.setString(5, "10А");
                ps.executeUpdate();
            }
        }
    }
}