package model;

public class ScheduleEntry {
    private final int id;
    private final int dayOfWeek;      // 1 = Пн ... 7 = Вс
    private final int lessonNumber;   // 1..10
    private final int subjectId;
    private final String subjectName;
    private final int teacherId;
    private final String teacherName;
    private final String className;

    public ScheduleEntry(int id, int dayOfWeek, int lessonNumber,
                         int subjectId, String subjectName,
                         int teacherId, String teacherName,
                         String className) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.lessonNumber = lessonNumber;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.className = className;
    }

    public int getId() { return id; }
    public int getDayOfWeek() { return dayOfWeek; }
    public int getLessonNumber() { return lessonNumber; }
    public int getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public int getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getClassName() { return className; }

    public static String dayName(int d) {
        return switch (d) {
            case 1 -> "Понедельник";
            case 2 -> "Вторник";
            case 3 -> "Среда";
            case 4 -> "Четверг";
            case 5 -> "Пятница";
            case 6 -> "Суббота";
            case 7 -> "Воскресенье";
            default -> "?";
        };
    }
}