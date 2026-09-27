package service;


import dao.UserDao;
import model.User;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final UserDao userDao = new UserDao();
    private User currentUser;

    public boolean login(String username, String password) throws SQLException {
        Optional<User> u = userDao.findByUsername(username);
        if (u.isPresent() && u.get().getPassword().equals(password)) {
            currentUser = u.get();
            return true;
        }
        return false;
    }

    public void logout() { currentUser = null; }

    public User getCurrentUser() { return currentUser; }

    public boolean isTeacher() {
        return currentUser != null && currentUser.getRole() == User.Role.TEACHER;
    }
}