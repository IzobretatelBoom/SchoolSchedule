package dao;

import db.Database;
import model.ScheduleEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDao {

    private static final String BASE_SELECT = """
        SELECT s.id, s.day_of_week, s.lesson_number,
               s.subject_id, sub.name AS subject_name,
               s.teacher_id, u.username AS teacher_name,
               s.class_name
        FROM schedule s
        JOIN subjects sub ON sub.id = s.subject_id
        JOIN users u ON u.id = s.teacher_id
        """;

    public List<ScheduleEntry> findAll() throws SQLException {
        return runQuery(BASE_SELECT + " ORDER BY s.class_name, s.day_of_week, s.lesson_number", null);
    }

    public List<ScheduleEntry> findByClass(String className) throws SQLException {
        return runQuery(BASE_SELECT + " WHERE s.class_name = ? ORDER BY s.day_of_week, s.lesson_number",
                ps -> ps.setString(1, className));
    }

    public List<ScheduleEntry> findByTeacher(int teacherId) throws SQLException {
        return runQuery(BASE_SELECT + " WHERE s.teacher_id = ? ORDER BY s.day_of_week, s.lesson_number",
                ps -> ps.setInt(1, teacherId));
    }

    public void insert(int day, int lesson, int subjectId, int teacherId, String className) throws SQLException {
        String sql = "INSERT INTO schedule(day_of_week, lesson_number, subject_id, teacher_id, class_name) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, day);
            ps.setInt(2, lesson);
            ps.setInt(3, subjectId);
            ps.setInt(4, teacherId);
            ps.setString(5, className);
            ps.executeUpdate();
        }
    }

    public boolean update(int id, int day, int lesson, int subjectId, int teacherId, String className) throws SQLException {
        String sql = "UPDATE schedule SET day_of_week=?, lesson_number=?, subject_id=?, teacher_id=?, class_name=? WHERE id=?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, day);
            ps.setInt(2, lesson);
            ps.setInt(3, subjectId);
            ps.setInt(4, teacherId);
            ps.setString(5, className);
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM schedule WHERE id = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<ScheduleEntry> runQuery(String sql, Binder binder) throws SQLException {
        List<ScheduleEntry> list = new ArrayList<>();
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            if (binder != null) binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ScheduleEntry(
                            rs.getInt("id"),
                            rs.getInt("day_of_week"),
                            rs.getInt("lesson_number"),
                            rs.getInt("subject_id"),
                            rs.getString("subject_name"),
                            rs.getInt("teacher_id"),
                            rs.getString("teacher_name"),
                            rs.getString("class_name")
                    ));
                }
            }
        }
        return list;
    }
}