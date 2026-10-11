package order_model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import MyException.DatabaseException;
import MyException.InvalidAgeException;
import MyException.InvalidEmailException;
import MyException.InvalidNameException;
import MyException.InvalidPhoneNumberException;
import people.Client;

/**
 * Доступ к таблице {@code clients}.
 *
 * <p>Класс инкапсулирует SQL и перевод строк базы данных в объекты
 * {@link Client}. Внешний код работает только с сущностями.</p>
 */
public class ClientRepository {

    private static final String COLUMNS =
            "client_id, name, surname, age, phone_number, email, address";

    private final DatabaseManager databaseManager;

    public ClientRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Сохраняет нового клиента.
     *
     * @param client клиент для сохранения
     */
    public void save(Client client) {
        String sql = "INSERT INTO clients (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, client);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось сохранить клиента " + client.getClientID(), e);
        }
    }

    /**
     * Находит клиента по идентификатору.
     *
     * @param clientId идентификатор клиента
     * @return клиент, если найден
     */
    public Optional<Client> findById(int clientId) {
        String sql = "SELECT " + COLUMNS + " FROM clients WHERE client_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось найти клиента " + clientId, e);
        }
    }

    /**
     * @return все клиенты, упорядоченные по идентификатору
     */
    public List<Client> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM clients ORDER BY client_id";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Client> clients = new ArrayList<>();
            while (resultSet.next()) {
                clients.add(map(resultSet));
            }
            return clients;
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось получить список клиентов", e);
        }
    }

    /**
     * Обновляет данные существующего клиента.
     *
     * @param client клиент с новыми данными
     */
    public void update(Client client) {
        String sql = "UPDATE clients SET name = ?, surname = ?, age = ?, "
                + "phone_number = ?, email = ?, address = ? WHERE client_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, client.getName());
            statement.setString(2, client.getSurname());
            statement.setInt(3, client.getAge());
            statement.setString(4, client.getPhoneNumber());
            statement.setString(5, client.getEmail());
            statement.setString(6, client.getAddress());
            statement.setInt(7, client.getClientID());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось обновить клиента " + client.getClientID(), e);
        }
    }

    /**
     * Удаляет клиента по идентификатору.
     *
     * @param clientId идентификатор клиента
     */
    public void delete(int clientId) {
        String sql = "DELETE FROM clients WHERE client_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clientId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось удалить клиента " + clientId, e);
        }
    }

    private void fillStatement(PreparedStatement statement, Client client) throws SQLException {
        statement.setInt(1, client.getClientID());
        statement.setString(2, client.getName());
        statement.setString(3, client.getSurname());
        statement.setInt(4, client.getAge());
        statement.setString(5, client.getPhoneNumber());
        statement.setString(6, client.getEmail());
        statement.setString(7, client.getAddress());
    }

    private Client map(ResultSet resultSet) throws SQLException {
        try {
            return new Client(
                    (short) resultSet.getInt("age"),
                    resultSet.getInt("client_id"),
                    resultSet.getString("phone_number"),
                    resultSet.getString("name"),
                    resultSet.getString("surname"),
                    resultSet.getString("email"),
                    resultSet.getString("address"));
        } catch (InvalidAgeException | InvalidPhoneNumberException
                 | InvalidNameException | InvalidEmailException e) {
            throw new DatabaseException("Повреждённые данные клиента в базе", e);
        }
    }
}
