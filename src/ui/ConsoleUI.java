package ui;

import model.ScheduleEntry;
import model.Subject;
import model.User;
import service.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final Scanner sc = new Scanner(System.in);
    private final AuthService auth = new AuthService();
    private final UserService users = new UserService();
    private final SubjectService subjects = new SubjectService();
    private final ScheduleService schedule = new ScheduleService();

    public void run() {
        clearScreen();
        System.out.println("=== Система учёта школьного расписания ===");
        while (true) {
            if (auth.getCurrentUser() == null) {
                if (!mainMenu()) return;
            } else if (auth.isTeacher()) {
                teacherMenu();
            } else {
                studentMenu();
            }
        }
    }

    // ---------- Главное меню (неавторизован) ----------
    private boolean mainMenu() {
        clearScreen();
        System.out.println("""
            --- Главное меню ---
            1. Войти
            2. Зарегистрироваться (ученик)
            0. Выход""");
        switch (readInt("> ")) {
            case 1 -> doLogin();
            case 2 -> doRegisterStudent();
            case 0 -> { return false; }
            default -> System.out.println("Неверный пункт");
        }
        pause();
        return true;
    }

    private void doLogin() {
        try {
            String u = readLine("Логин: ");
            String p = readLine("Пароль: ");
            if (auth.login(u, p)) {
                System.out.println("Добро пожаловать, " + u + " (" + auth.getCurrentUser().getRole() + ")");
            } else {
                System.out.println("Неверный логин или пароль");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void doRegisterStudent() {
        try {
            String u = readLine("Логин: ");
            String p = readLine("Пароль: ");
            users.registerStudent(u, p);
            System.out.println("Ученик зарегистрирован!");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    // ---------- Меню ученика ----------
    private void studentMenu() {
        clearScreen();
        System.out.println("""
            --- Меню ученика ---
            1. Просмотр расписания (всё)
            2. Просмотр расписания класса
            3. Просмотр расписания учителя
            9. Выйти из аккаунта
            0. Выход""");
        switch (readInt("> ")) {
            case 1 -> viewScheduleSafe(() -> schedule.all());
            case 2 -> {
                String c = readLine("Класс: ");
                viewScheduleSafe(() -> schedule.byClass(c));
            }
            case 3 -> {
                int tid = readInt("ID учителя: ");
                viewScheduleSafe(() -> schedule.byTeacher(tid));
            }
            case 9 -> { auth.logout(); return; }
            case 0 -> { System.out.println("До свидания!"); System.exit(0); }
            default -> System.out.println("Неверный пункт");
        }
        pause();
    }

    // ---------- Меню учителя ----------
    private void teacherMenu() {
        clearScreen();
        System.out.println("""
            --- Меню учителя ---
            1.  Просмотр всего расписания
            2.  Просмотр расписания класса
            3.  Просмотр расписания учителя
            4.  Добавить урок в расписание
            5.  Изменить урок
            6.  Удалить урок
            7.  Список предметов
            8.  Добавить предмет
            9.  Удалить предмет
            10. Список пользователей
            11. Зарегистрировать ученика
            12. Создать учителя
            13. Удалить пользователя
            14. Сменить пароль пользователя
            99. Выйти из аккаунта
            0.  Выход""");
        switch (readInt("> ")) {
            case 1 -> viewScheduleSafe(() -> schedule.all());
            case 2 -> {
                String c = readLine("Класс: ");
                viewScheduleSafe(() -> schedule.byClass(c));
            }
            case 3 -> {
                int tid = readInt("ID учителя: ");
                viewScheduleSafe(() -> schedule.byTeacher(tid));
            }
            case 4 -> addLesson();
            case 5 -> updateLesson();
            case 6 -> deleteLesson();
            case 7 -> listSubjects();
            case 8 -> addSubject();
            case 9 -> deleteSubject();
            case 10 -> listUsers();
            case 11 -> doRegisterStudent();
            case 12 -> createTeacher();
            case 13 -> deleteUser();
            case 14 -> changePassword();
            case 99 -> { auth.logout(); return; }
            case 0 -> { System.out.println("До свидания!"); System.exit(0); }
            default -> System.out.println("Неверный пункт");
        }
        pause();
    }

    // ---------- Операции ----------
    private void addLesson() {
        try {
            printSubjects();
            int day = readInt("День (1-7): ");
            int lesson = readInt("Номер урока (1-10): ");
            int subjectId = readInt("ID предмета: ");
            int teacherId = readInt("ID учителя: ");
            String className = readLine("Класс: ");
            schedule.add(day, lesson, subjectId, teacherId, className);
            System.out.println("Урок добавлен.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void updateLesson() {
        try {
            viewScheduleSafe(() -> schedule.all());
            int id = readInt("ID записи для изменения: ");
            int day = readInt("День (1-7): ");
            int lesson = readInt("Номер урока (1-10): ");
            printSubjects();
            int subjectId = readInt("ID предмета: ");
            int teacherId = readInt("ID учителя: ");
            String className = readLine("Класс: ");
            schedule.update(id, day, lesson, subjectId, teacherId, className);
            System.out.println("Изменено.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void deleteLesson() {
        try {
            int id = readInt("ID записи для удаления: ");
            schedule.delete(id);
            System.out.println("Удалено.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void listSubjects() { printSubjects(); }

    private void addSubject() {
        try {
            String name = readLine("Название предмета: ");
            subjects.add(name);
            System.out.println("Предмет добавлен.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void deleteSubject() {
        try {
            printSubjects();
            int id = readInt("ID предмета для удаления: ");
            subjects.delete(id);
            System.out.println("Удалено.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void listUsers() {
        try {
            System.out.println("--- Пользователи ---");
            for (User u : users.listAll()) System.out.println(u);
        } catch (SQLException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void createTeacher() {
        try {
            String u = readLine("Логин: ");
            String p = readLine("Пароль: ");
            users.createTeacher(u, p);
            System.out.println("Учитель создан.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void deleteUser() {
        try {
            int id = readInt("ID пользователя: ");
            users.delete(id);
            System.out.println("Удалено.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void changePassword() {
        try {
            int id = readInt("ID пользователя: ");
            String p = readLine("Новый пароль: ");
            users.changePassword(id, p);
            System.out.println("Пароль изменён.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    // ---------- Вспомогательные ----------
    private void printSubjects() {
        try {
            List<Subject> list = subjects.listAll();
            if (list.isEmpty()) {
                System.out.println("(нет предметов)");
                return;
            }
            System.out.println("--- Предметы ---");
            for (Subject s : list) System.out.println(s.getId() + ") " + s.getName());
        } catch (SQLException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ScheduleLoader { List<ScheduleEntry> load() throws SQLException; }

    private void viewScheduleSafe(ScheduleLoader loader) {
        try {
            printSchedule(loader.load());
        } catch (SQLException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    private void printSchedule(List<ScheduleEntry> list) {
        if (list.isEmpty()) {
            System.out.println("(расписание пусто)");
            return;
        }
        System.out.printf("%-4s %-12s %-4s %-18s %-15s %-10s%n",
                "ID", "День", "Урок", "Предмет", "Учитель", "Класс");
        for (ScheduleEntry e : list) {
            System.out.printf("%-4d %-12s %-4d %-18s %-15s %-10s%n",
                    e.getId(),
                    ScheduleEntry.dayName(e.getDayOfWeek()),
                    e.getLessonNumber(),
                    e.getSubjectName(),
                    e.getTeacherName(),
                    e.getClassName());
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.println("Введите число."); }
        }
    }

    // ---------- Очистка и пауза ----------

    /**
     * Очищает консоль ANSI-последовательностью.
     * Работает в Windows Terminal, PowerShell (7+), Linux, macOS.
     * В IntelliJ IDEA нужно включить
     * Run → Edit Configurations → Modify options → Emulate terminal in output console.
     */
    private void clearScreen() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls")
                        .inheritIO()
                        .start()
                        .waitFor();
            } else {
                new ProcessBuilder("clear")
                        .inheritIO()
                        .start()
                        .waitFor();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Ждёт нажатия Enter. */
    private void pause() {
        System.out.print("\nНажмите Enter, чтобы вернуться в меню...");
        sc.nextLine();
    }
}