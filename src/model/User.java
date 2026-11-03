package model;
public class User {
    public enum Role { STUDENT, TEACHER }

    private int id;
    private String username;
    private String password;
    private Role role;

    public User(int id, String username, String password, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }

    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return String.format("User#%d [%s, роль=%s]", id, username, role);
    }
}