package order_model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import MyException.DatabaseException;

/**
 * Отвечает за подключение к базе данных SQLite и создание схемы.
 *
 * <p>Экземпляр класса создаёт все необходимые таблицы, если их ещё нет,
 * и выдаёт соединения остальным компонентам (репозиториям).
 * URL базы задаётся снаружи, поэтому класс легко переиспользовать
 * и подменять в тестах.</p>
 */
public class DatabaseManager {

    private static final String[] SCHEMA = {
            "CREATE TABLE IF NOT EXISTS clients ("
                    + "client_id INTEGER PRIMARY KEY, "
                    + "name TEXT NOT NULL, "
                    + "surname TEXT NOT NULL, "
                    + "age INTEGER NOT NULL, "
                    + "phone_number TEXT NOT NULL, "
                    + "email TEXT NOT NULL, "
                    + "address TEXT)",

            "CREATE TABLE IF NOT EXISTS couriers ("
                    + "courier_id INTEGER PRIMARY KEY, "
                    + "name TEXT NOT NULL, "
                    + "surname TEXT NOT NULL, "
                    + "age INTEGER NOT NULL, "
                    + "phone_number TEXT NOT NULL, "
                    + "transport TEXT NOT NULL, "
                    + "is_available INTEGER NOT NULL)",

            "CREATE TABLE IF NOT EXISTS orders ("
                    + "order_number INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "client_id INTEGER NOT NULL, "
                    + "address TEXT, "
                    + "delivery_type TEXT, "
                    + "courier_id INTEGER, "
                    + "status TEXT NOT NULL, "
                    + "total_price REAL NOT NULL, "
                    + "FOREIGN KEY (client_id) REFERENCES clients(client_id), "
                    + "FOREIGN KEY (courier_id) REFERENCES couriers(courier_id))",

            "CREATE TABLE IF NOT EXISTS order_items ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "order_number INTEGER NOT NULL, "
                    + "product_name TEXT NOT NULL, "
                    + "quantity INTEGER NOT NULL, "
                    + "price REAL NOT NULL, "
                    + "FOREIGN KEY (order_number) REFERENCES orders(order_number))"
    };

    private final String url;

    /**
     * @param url JDBC-URL базы, например {@code jdbc:sqlite:delivery.db}
     */
    public DatabaseManager(String url) {
        this.url = url;
        initializeSchema();
    }

    /**
     * Открывает новое соединение с включённой проверкой внешних ключей.
     *
     * @return активное соединение с базой
     * @throws SQLException если соединение не удалось установить
     */
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void initializeSchema() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось инициализировать схему базы данных", e);
        }
    }
}
