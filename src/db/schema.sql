-- Схема базы данных SchoolSchedule.
-- Выполняется при запуске приложения (Database.init).

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('STUDENT','TEACHER'))
);

CREATE TABLE IF NOT EXISTS subjects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL
);

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
