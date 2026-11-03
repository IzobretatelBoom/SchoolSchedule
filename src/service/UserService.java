package service;

import dao.UserDao;
import model.User;

import java.sql.SQLException;
import java.util.List;

public class UserService {
    private final UserDao userDao = new UserDao();

    public void registerStudent(String username, String password) throws SQLException {
        validate(username, password);
        if (userDao.findByUsername(username).isPresent())
            throw new IllegalArgumentException("Пользователь уже существует");
        userDao.insert(username, password, User.Role.STUDENT);
    }

    public void createTeacher(String username, String password) throws SQLException {
        validate(username, password);
        if (userDao.findByUsername(username).isPresent())
            throw new IllegalArgumentException("Пользователь уже существует");
        userDao.insert(username, password, User.Role.TEACHER);
    }

    public List<User> listAll() throws SQLException { return userDao.findAll(); }
    public List<User> listStudents() throws SQLException { return userDao.findAllByRole(User.Role.STUDENT); }
    public List<User> listTeachers() throws SQLException { return userDao.findAllByRole(User.Role.TEACHER); }

    public void delete(int id) throws SQLException {
        if (!userDao.deleteById(id))
            throw new IllegalArgumentException("Пользователь с id=" + id + " не найден");
    }

    public void changePassword(int id, String newPassword) throws SQLException {
        if (newPassword == null || newPassword.length() < 3)
            throw new IllegalArgumentException("Пароль слишком короткий");
        if (!userDao.updatePassword(id, newPassword))
            throw new IllegalArgumentException("Пользователь не найден");
    }

    private void validate(String username, String password) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Логин пуст");
        if (password == null || password.length() < 3) throw new IllegalArgumentException("Пароль слишком короткий");
    }
}