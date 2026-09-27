package service;

import dao.SubjectDao;
import model.Subject;

import java.sql.SQLException;
import java.util.List;

public class SubjectService {
    private final SubjectDao subjectDao = new SubjectDao();

    public List<Subject> listAll() throws SQLException { return subjectDao.findAll(); }

    public void add(String name) throws SQLException {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Название пусто");
        subjectDao.insert(name.trim());
    }

    public void delete(int id) throws SQLException {
        if (!subjectDao.deleteById(id))
            throw new IllegalArgumentException("Предмет с id=" + id + " не найден");
    }
}