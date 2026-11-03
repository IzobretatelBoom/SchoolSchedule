package service;

import dao.ScheduleDao;
import dao.SubjectDao;
import model.ScheduleEntry;

import java.sql.SQLException;
import java.util.List;

public class ScheduleService {
    private final ScheduleDao scheduleDao = new ScheduleDao();
    private final SubjectDao subjectDao = new SubjectDao();

    public List<ScheduleEntry> all() throws SQLException { return scheduleDao.findAll(); }
    public List<ScheduleEntry> byClass(String className) throws SQLException { return scheduleDao.findByClass(className); }
    public List<ScheduleEntry> byTeacher(int teacherId) throws SQLException { return scheduleDao.findByTeacher(teacherId); }

    public void add(int day, int lesson, int subjectId, int teacherId, String className) throws SQLException {
        validate(day, lesson, className);
        if (!subjectDao.existsById(subjectId))
            throw new IllegalArgumentException("Предмет с id=" + subjectId + " не найден");
        scheduleDao.insert(day, lesson, subjectId, teacherId, className);
    }

    public void update(int id, int day, int lesson, int subjectId, int teacherId, String className) throws SQLException {
        validate(day, lesson, className);
        if (!subjectDao.existsById(subjectId))
            throw new IllegalArgumentException("Предмет с id=" + subjectId + " не найден");
        if (!scheduleDao.update(id, day, lesson, subjectId, teacherId, className))
            throw new IllegalArgumentException("Запись с id=" + id + " не найдена");
    }

    public void delete(int id) throws SQLException {
        if (!scheduleDao.deleteById(id))
            throw new IllegalArgumentException("Запись с id=" + id + " не найдена");
    }

    private void validate(int day, int lesson, String className) {
        if (day < 1 || day > 7) throw new IllegalArgumentException("День недели: 1-7");
        if (lesson < 1 || lesson > 10) throw new IllegalArgumentException("Номер урока: 1-10");
        if (className == null || className.isBlank()) throw new IllegalArgumentException("Класс пуст");
    }
}