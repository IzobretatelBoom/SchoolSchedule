package service;

import db.Database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportService {

    // Папка выгрузки по умолчанию — создаётся автоматически.
    public static final Path DIRECTORY = Path.of("out");

    private final Connection connection;

    public ExportService() throws SQLException {
        this.connection = Database.getConnection();
    }

    // ------------------------------------------------------------- данные

    // Имена таблиц базы данных (служебные таблицы sqlite_* пропускаются).
    public List<String> tables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", null)) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if ("TABLE".equalsIgnoreCase(rs.getString("TABLE_TYPE"))
                        && !name.startsWith("sqlite_")) {
                    tables.add(name);
                }
            }
        }
        return tables;
    }

    // Имена колонок таблицы.
    public List<String> columns(String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, table, "%")) {
            while (rs.next()) columns.add(rs.getString("COLUMN_NAME"));
        }
        return columns;
    }

    // Количество записей в таблице.
    public int count(String table) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + quote(table))) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // Читает записи таблицы вместе со связанными записями.
    public List<Map<String, Object>> rows(String table) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + quote(table))) {
            while (rs.next()) {
                Map<String, Object> row = row(rs);
                addRelated(table, row);
                rows.add(row);
            }
        }
        return rows;
    }

    // Записи, у которых колонка column равна value.
    private List<Map<String, Object>> where(String table, String column, Object value)
            throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM " + quote(table) + " WHERE " + quote(column) + " = ?")) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(row(rs));
            }
        }
        return rows;
    }

    // Одна строка таблицы: имя поля в camelCase -> значение.
    private Map<String, Object> row(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(camel(meta.getColumnLabel(i)), rs.getObject(i));
        }
        return row;
    }

    // Добавляет связанные записи по внешним ключам: дочерние — списком,
    // родительская — вложенным объектом. Направление связи определяют метаданные.
    private void addRelated(String table, Map<String, Object> row) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        // Много к одному: колонка этой таблицы ссылается на другую таблицу.
        try (ResultSet rs = meta.getImportedKeys(null, null, table)) {
            while (rs.next()) {
                Object key = row.get(camel(rs.getString("FKCOLUMN_NAME")));
                if (key == null) continue;
                List<Map<String, Object>> parents =
                        where(rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME"), key);
                if (!parents.isEmpty()) {
                    row.put(singular(rs.getString("PKTABLE_NAME")), parents.get(0));
                }
            }
        }
        // Один ко многим: другие таблицы ссылаются на эту запись.
        try (ResultSet rs = meta.getExportedKeys(null, null, table)) {
            while (rs.next()) {
                Object key = row.get(camel(rs.getString("PKCOLUMN_NAME")));
                if (key == null) continue;
                List<Map<String, Object>> children =
                        where(rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"), key);
                if (!children.isEmpty()) row.put(rs.getString("FKTABLE_NAME"), children);
            }
        }
    }

    // ------------------------------------------------------------- запись

    // Главный метод: читает таблицу со связанными записями и сохраняет её
    // в четыре файла в папке out (создаётся автоматически).
    public List<Path> export(String table) throws SQLException, IOException {
        return save(table, rows(table));
    }

    // Сохраняет данные в четыре файла в папку out.
    private List<Path> save(String name, List<Map<String, Object>> rows) throws IOException {
        Path dir = DIRECTORY;
        Files.createDirectories(dir);
        List<Path> files = List.of(
                dir.resolve("data.json"), dir.resolve("data.csv"),
                dir.resolve("data.xml"), dir.resolve("data.yaml"));
        List<String> texts = List.of(json(name, rows), csv(rows), xml(name, rows), yaml(name, rows));
        for (int i = 0; i < files.size(); i++) {
            Files.writeString(files.get(i), texts.get(i), StandardCharsets.UTF_8);
        }
        return files;
    }

    // --------------------------------------------------------------- JSON

    private String json(String name, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder("{\n");
        sb.append("  \"table\": ").append(str(name)).append(",\n");
        sb.append("  \"count\": ").append(rows.size()).append(",\n");
        sb.append("  \"rows\": ");
        jsonValue(sb, rows, 1);
        return sb.append("\n}\n").toString();
    }

    // Пишет значение JSON любого уровня вложенности.
    private void jsonValue(StringBuilder sb, Object value, int level) {
        String pad = "  ".repeat(level);
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            sb.append(str(s));
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                sb.append(pad).append("  ").append(str(e.getKey().toString())).append(": ");
                jsonValue(sb, e.getValue(), level + 1);
                sb.append(++i < map.size() ? ",\n" : "\n");
            }
            sb.append(pad).append('}');
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(pad).append("  ");
                jsonValue(sb, list.get(i), level + 1);
                sb.append(i + 1 < list.size() ? ",\n" : "\n");
            }
            sb.append(pad).append(']');
        } else {
            sb.append(str(String.valueOf(value)));
        }
    }

    // Строка в кавычках с экранированием по RFC 8259.
    private static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.append('"').toString();
    }

    // ---------------------------------------------------------------- CSV

    // Плоская таблица: вложенные объекты — колонки через точку (subject.name),
    // списки — одна колонка, элементы через «;;», поля элемента через « | ».
    private String csv(List<Map<String, Object>> rows) {
        List<Map<String, String>> flat = new ArrayList<>();
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Map<String, String> values = new LinkedHashMap<>();
            flatten("", row, values);
            values.forEach(headers::putIfAbsent);
            flat.add(values);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", headers.keySet().stream().map(ExportService::csvCell).toList()))
                .append("\r\n");
        for (Map<String, String> values : flat) {
            List<String> cells = new ArrayList<>();
            for (String header : headers.keySet()) cells.add(csvCell(values.getOrDefault(header, "")));
            sb.append(String.join(",", cells)).append("\r\n");
        }
        return sb.toString();
    }

    // Раскладывает запись в плоскую карту «колонка -> текст».
    private void flatten(String prefix, Map<?, ?> map, Map<String, String> out) {
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = prefix + e.getKey();
            Object value = e.getValue();
            if (value instanceof Map<?, ?> child) {
                flatten(key + ".", child, out);
            } else if (value instanceof List<?> list) {
                List<String> items = new ArrayList<>();
                for (Object item : list) {
                    List<String> cells = new ArrayList<>();
                    if (item instanceof Map<?, ?> m) {
                        m.values().forEach(v -> cells.add(v == null ? "" : String.valueOf(v)));
                    }
                    items.add(String.join(" | ", cells));
                }
                out.put(key, String.join(";;", items));
            } else {
                out.put(key, value == null ? "" : String.valueOf(value));
            }
        }
    }

    // Экранирование поля CSV по RFC 4180.
    private static String csvCell(String value) {
        return value.contains(",") || value.contains("\"") || value.contains("\n")
                ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }

    // ---------------------------------------------------------------- XML

    private String xml(String name, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rows table=\"").append(esc(name)).append("\" count=\"")
                .append(rows.size()).append("\">\n");
        for (Map<String, Object> row : rows) xmlNode(sb, "row", row, 1);
        return sb.append("</rows>\n").toString();
    }

    // Элемент XML: объект — дочерние элементы, список — повторяющиеся <row>.
    private void xmlNode(StringBuilder sb, String tag, Object value, int level) {
        String pad = "  ".repeat(level);
        sb.append(pad).append('<').append(tag).append('>');
        if (value instanceof Map<?, ?> map) {
            sb.append('\n');
            for (Map.Entry<?, ?> e : map.entrySet()) {
                xmlNode(sb, e.getKey().toString(), e.getValue(), level + 1);
            }
            sb.append(pad);
        } else if (value instanceof List<?> list) {
            sb.append('\n');
            for (Object item : list) xmlNode(sb, "row", item, level + 1);
            sb.append(pad);
        } else {
            sb.append(esc(value == null ? "" : String.valueOf(value)));
        }
        sb.append("</").append(tag).append(">\n");
    }

    // Экранирование текста XML.
    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // --------------------------------------------------------------- YAML

    private String yaml(String name, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder("# Выгрузка: ").append(name).append('\n');
        sb.append("table: ").append(str(name)).append('\n');
        sb.append("count: ").append(rows.size()).append('\n');
        if (rows.isEmpty()) return sb.append("rows: []\n").toString();
        sb.append("rows:\n");
        yamlList(sb, rows, 1);
        return sb.toString();
    }

    // Список: каждый элемент начинается с «- ».
    private void yamlList(StringBuilder sb, List<?> list, int level) {
        String pad = "  ".repeat(level);
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && !map.isEmpty()) {
                // Первый ключ пишется в строке с «- », остальные — с отступом на 2 пробела.
                String pad2 = "  ".repeat(level + 1);
                int i = 0;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    sb.append(i++ == 0 ? pad + "- " : pad2);
                    yamlEntry(sb, e.getKey().toString(), e.getValue(), level + 1);
                }
            } else {
                sb.append(pad).append("- ").append(yamlScalar(item)).append('\n');
            }
        }
    }

    // Объект: каждый ключ с нового уровня отступа.
    private void yamlObject(StringBuilder sb, Map<?, ?> map, int level) {
        String pad = "  ".repeat(level);
        for (Map.Entry<?, ?> e : map.entrySet()) {
            sb.append(pad);
            yamlEntry(sb, e.getKey().toString(), e.getValue(), level);
        }
    }

    // Пишет «ключ: значение»; вложенные объекты и списки уходят с нового уровня.
    private void yamlEntry(StringBuilder sb, String name, Object value, int level) {
        sb.append(key(name)).append(':');
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                sb.append(" {}\n");
            } else {
                sb.append('\n');
                yamlObject(sb, map, level + 1);
            }
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                sb.append(" []\n");
            } else {
                sb.append('\n');
                yamlList(sb, list, level);
            }
        } else {
            sb.append(' ').append(yamlScalar(value)).append('\n');
        }
    }

    // Ключ YAML: простой — без кавычек, иначе в кавычках.
    private static String key(String k) {
        return k.matches("[A-Za-z_][A-Za-z0-9_]*") ? k : str(k);
    }

    private static String yamlScalar(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return str(String.valueOf(v));
    }

    // ------------------------------------------------------------ служебное

    // day_of_week -> dayOfWeek.
    private static String camel(String name) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = false;
            }
        }
        return sb.toString();
    }

    // subjects -> subject.
    private static String singular(String table) {
        return table.endsWith("s") ? table.substring(0, table.length() - 1) : table;
    }

    // Имя таблицы в кавычках — защита от зарезервированных слов SQL.
    private static String quote(String name) {
        return '"' + name.replace("\"", "\"\"") + '"';
    }
}
