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
import MyException.InvalidNameException;
import MyException.InvalidPhoneNumberException;
import people.Courier;
import people.Transport;

/**
 * Доступ к таблице {@code couriers}.
 *
 * <p>Класс инкапсулирует SQL и перевод строк базы данных в объекты
 * {@link Courier}. Доступность курьера восстанавливается через его
 * собственные методы {@code takeOrder()} и {@code release()}.</p>
 */
public class CourierRepository {

    private static final String COLUMNS =
            "courier_id, name, surname, age, phone_number, transport, is_available";

    private final DatabaseManager databaseManager;

    public CourierRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Сохраняет нового курьера.
     *
     * @param courier курьер для сохранения
     */
    public void save(Courier courier) {
        String sql = "INSERT INTO couriers (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, courier);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось сохранить курьера " + courier.getCourierID(), e);
        }
    }

    /**
     * Находит курьера по идентификатору.
     *
     * @param courierId идентификатор курьера
     * @return курьер, если найден
     */
    public Optional<Courier> findById(int courierId) {
        String sql = "SELECT " + COLUMNS + " FROM couriers WHERE courier_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, courierId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось найти курьера " + courierId, e);
        }
    }

    /**
     * @return все курьеры, упорядоченные по идентификатору
     */
    public List<Courier> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM couriers ORDER BY courier_id";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Courier> couriers = new ArrayList<>();
            while (resultSet.next()) {
                couriers.add(map(resultSet));
            }
            return couriers;
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось получить список курьеров", e);
        }
    }

    /**
     * Обновляет данные существующего курьера, включая доступность.
     *
     * @param courier курьер с новыми данными
     */
    public void update(Courier courier) {
        String sql = "UPDATE couriers SET name = ?, surname = ?, age = ?, "
                + "phone_number = ?, transport = ?, is_available = ? WHERE courier_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courier.getName());
            statement.setString(2, courier.getSurname());
            statement.setInt(3, courier.getAge());
            statement.setString(4, courier.getPhoneNumber());
            statement.setString(5, courier.getTransport().name());
            statement.setInt(6, courier.isAvailable() ? 1 : 0);
            statement.setInt(7, courier.getCourierID());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось обновить курьера " + courier.getCourierID(), e);
        }
    }

    /**
     * Удаляет курьера по идентификатору.
     *
     * @param courierId идентификатор курьера
     */
    public void delete(int courierId) {
        String sql = "DELETE FROM couriers WHERE courier_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, courierId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось удалить курьера " + courierId, e);
        }
    }

    private void fillStatement(PreparedStatement statement, Courier courier) throws SQLException {
        statement.setInt(1, courier.getCourierID());
        statement.setString(2, courier.getName());
        statement.setString(3, courier.getSurname());
        statement.setInt(4, courier.getAge());
        statement.setString(5, courier.getPhoneNumber());
        statement.setString(6, courier.getTransport().name());
        statement.setInt(7, courier.isAvailable() ? 1 : 0);
    }

    private Courier map(ResultSet resultSet) throws SQLException {
        try {
            Courier courier = new Courier(
                    (short) resultSet.getInt("age"),
                    resultSet.getInt("courier_id"),
                    resultSet.getString("phone_number"),
                    resultSet.getString("name"),
                    resultSet.getString("surname"),
                    Transport.valueOf(resultSet.getString("transport")));
            if (resultSet.getInt("is_available") == 0) {
                courier.takeOrder();
            }
            return courier;
        } catch (InvalidAgeException | InvalidPhoneNumberException | InvalidNameException e) {
            throw new DatabaseException("Повреждённые данные курьера в базе", e);
        }
    }
}
