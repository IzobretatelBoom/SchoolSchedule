import db.Database;
import ui.ConsoleUI;

public class Main {
    public static void main(String[] args) {
        Database.init();
        new ConsoleUI().run();
    }
}