package order_model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import MyException.DatabaseException;
import MyException.InvalidAgeException;
import MyException.InvalidNameException;
import MyException.InvalidPhoneNumberException;
import delivery_model.Delivery;
import delivery_model.ExpressDelivery;
import delivery_model.FastDelivery;
import delivery_model.StandardDelivery;
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

    /**
     * Каким транспортом можно выполнить каждый способ доставки.
     * Самовывоза здесь нет: для него курьер не нужен.
     */
    private static final Map<Class<? extends Delivery>, Set<Transport>> SUITABLE_TRANSPORT = Map.of(
            ExpressDelivery.class, EnumSet.of(Transport.MOTORCYCLE, Transport.CAR),
            FastDelivery.class, EnumSet.of(Transport.MOTORCYCLE, Transport.CAR),
            StandardDelivery.class, EnumSet.of(Transport.CAR));

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
     * Находит первого свободного курьера, подходящего под способ доставки.
     *
     * <p>Подходящим считается доступный курьер, чей транспорт позволяет
     * выполнить выбранный способ доставки (см. {@link #SUITABLE_TRANSPORT}).
     * Курьеры перебираются по возрастанию идентификатора, возвращается
     * первый подошедший. Для способов доставки, не требующих курьера
     * (например, самовывоза), результат пустой.</p>
     *
     * @param delivery выбранный способ доставки
     * @return идентификатор подходящего курьера, если он есть
     * @throws IllegalArgumentException если способ доставки не задан
     */
    public Optional<Integer> findSuitableCourierId(Delivery delivery) {
        if (delivery == null) {
            throw new IllegalArgumentException("Способ доставки не задан");
        }
        if (!delivery.isNeedCourier()) {
            return Optional.empty();
        }

        Set<Transport> allowed = SUITABLE_TRANSPORT.get(delivery.getClass());
        List<Transport> transports = allowed == null
                ? Collections.emptyList() : new ArrayList<>(allowed);

        StringBuilder sql = new StringBuilder(
                "SELECT courier_id FROM couriers WHERE is_available = 1");
        if (!transports.isEmpty()) {
            sql.append(" AND transport IN (")
                    .append(String.join(", ", Collections.nCopies(transports.size(), "?")))
                    .append(")");
        }
        sql.append(" ORDER BY courier_id LIMIT 1");

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < transports.size(); i++) {
                statement.setString(i + 1, transports.get(i).name());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(resultSet.getInt("courier_id"))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось найти подходящего курьера", e);
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
