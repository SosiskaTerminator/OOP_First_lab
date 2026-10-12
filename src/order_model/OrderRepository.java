package order_model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import MyException.DatabaseException;
import delivery_model.Delivery;
import people.Client;
import people.Courier;

/**
 * Доступ к таблицам {@code orders} и {@code order_items}.
 *
 * <p>Класс отвечает за сохранение заказов вместе с их позициями и за
 * восстановление объектов {@link Order} из базы данных. Заказ и его
 * позиции пишутся в одной транзакции, поэтому «половинчатых» заказов
 * в базе не остаётся. Связанные сущности (клиент и курьер) читаются
 * через свои репозитории.</p>
 */
public class OrderRepository {

    private static final String COLUMNS =
            "order_number, client_id, address, delivery_type, courier_id, status, total_price";

    private final DatabaseManager databaseManager;
    private final ClientRepository clientRepository;
    private final CourierRepository courierRepository;

    public OrderRepository(DatabaseManager databaseManager,
                           ClientRepository clientRepository,
                           CourierRepository courierRepository) {
        this.databaseManager = databaseManager;
        this.clientRepository = clientRepository;
        this.courierRepository = courierRepository;
    }

    /**
     * Сохраняет заказ и все его позиции. Номер заказа генерирует база,
     * после вставки он проставляется в переданный объект.
     *
     * @param order заказ для сохранения
     * @throws DatabaseException если транзакция не удалась
     */
    public void save(Order order) {
        String orderSql = "INSERT INTO orders (client_id, address, delivery_type, "
                + "courier_id, status, total_price) VALUES (?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_number, product_name, quantity, price) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int orderNumber;
                try (PreparedStatement statement =
                             connection.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    fillOrderStatement(statement, order);
                    statement.executeUpdate();
                    orderNumber = readGeneratedKey(statement);
                }

                try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                    for (OrderItem item : order.getItems()) {
                        statement.setInt(1, orderNumber);
                        statement.setString(2, item.getProductName());
                        statement.setInt(3, item.getQuantity());
                        statement.setFloat(4, item.getPrice());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                connection.commit();
                order.setOrderNumber(orderNumber);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось сохранить заказ", e);
        }
    }

    /**
     * Находит заказ по номеру вместе с позициями, клиентом и курьером.
     *
     * @param orderNumber номер заказа
     * @return заказ, если найден
     */
    public Optional<Order> findByNumber(int orderNumber) {
        String sql = "SELECT " + COLUMNS + " FROM orders WHERE order_number = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                OrderRow row = readRow(resultSet);
                List<OrderItem> items = loadItems(connection, orderNumber);
                return Optional.of(buildOrder(row, items));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось найти заказ " + orderNumber, e);
        }
    }

    /**
     * @return все заказы вместе с позициями, упорядоченные по номеру
     */
    public List<Order> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM orders ORDER BY order_number";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<OrderRow> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(readRow(resultSet));
            }
            Map<Integer, List<OrderItem>> itemsByOrder = loadAllItems(connection);

            Map<Integer, Client> clients = indexClients();
            Map<Integer, Courier> couriers = indexCouriers();

            List<Order> orders = new ArrayList<>();
            for (OrderRow row : rows) {
                orders.add(buildOrder(row, itemsByOrder.getOrDefault(row.orderNumber(), List.of()),
                        clients, couriers));
            }
            return orders;
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось получить список заказов", e);
        }
    }

    /**
     * Обновляет заказ и полностью перезаписывает его позиции.
     *
     * @param order заказ с новыми данными
     */
    public void update(Order order) {
        String updateSql = "UPDATE orders SET client_id = ?, address = ?, delivery_type = ?, "
                + "courier_id = ?, status = ?, total_price = ? WHERE order_number = ?";
        String deleteItemsSql = "DELETE FROM order_items WHERE order_number = ?";
        String insertItemSql = "INSERT INTO order_items (order_number, product_name, quantity, price) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setInt(1, order.getClient().getClientID());
                    statement.setString(2, order.getAddress());
                    statement.setString(3, DeliveryFactory.typeName(order.getDelivery()));
                    if (order.getCourier() == null) {
                        statement.setNull(4, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(4, order.getCourier().getCourierID());
                    }
                    statement.setString(5, order.getStatus().name());
                    statement.setFloat(6, order.getTotalPrice());
                    statement.setInt(7, order.getOrderNumber());
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(deleteItemsSql)) {
                    statement.setInt(1, order.getOrderNumber());
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(insertItemSql)) {
                    for (OrderItem item : order.getItems()) {
                        statement.setInt(1, order.getOrderNumber());
                        statement.setString(2, item.getProductName());
                        statement.setInt(3, item.getQuantity());
                        statement.setFloat(4, item.getPrice());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось обновить заказ " + order.getOrderNumber(), e);
        }
    }

    /**
     * Удаляет заказ вместе с его позициями.
     *
     * @param orderNumber номер заказа
     */
    public void delete(int orderNumber) {
        String deleteItemsSql = "DELETE FROM order_items WHERE order_number = ?";
        String deleteOrderSql = "DELETE FROM orders WHERE order_number = ?";

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(deleteItemsSql)) {
                    statement.setInt(1, orderNumber);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(deleteOrderSql)) {
                    statement.setInt(1, orderNumber);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось удалить заказ " + orderNumber, e);
        }
    }

    private void fillOrderStatement(PreparedStatement statement, Order order) throws SQLException {
        statement.setInt(1, order.getClient().getClientID());
        statement.setString(2, order.getAddress());
        statement.setString(3, DeliveryFactory.typeName(order.getDelivery()));
        if (order.getCourier() == null) {
            statement.setNull(4, java.sql.Types.INTEGER);
        } else {
            statement.setInt(4, order.getCourier().getCourierID());
        }
        statement.setString(5, order.getStatus().name());
        statement.setFloat(6, order.getTotalPrice());
    }

    private int readGeneratedKey(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("База не вернула номер сохранённого заказа");
    }

    private OrderRow readRow(ResultSet resultSet) throws SQLException {
        int courierId = resultSet.getInt("courier_id");
        Integer courier = resultSet.wasNull() ? null : courierId;
        return new OrderRow(
                resultSet.getInt("order_number"),
                resultSet.getInt("client_id"),
                resultSet.getString("address"),
                resultSet.getString("delivery_type"),
                courier,
                resultSet.getString("status"),
                resultSet.getFloat("total_price"));
    }

    private List<OrderItem> loadItems(Connection connection, int orderNumber) throws SQLException {
        String sql = "SELECT product_name, quantity, price FROM order_items "
                + "WHERE order_number = ? ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
                return items;
            }
        }
    }

    private Map<Integer, List<OrderItem>> loadAllItems(Connection connection) throws SQLException {
        String sql = "SELECT order_number, product_name, quantity, price FROM order_items "
                + "ORDER BY order_number, id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, List<OrderItem>> itemsByOrder = new HashMap<>();
            while (resultSet.next()) {
                itemsByOrder.computeIfAbsent(resultSet.getInt("order_number"), key -> new ArrayList<>())
                        .add(mapItem(resultSet));
            }
            return itemsByOrder;
        }
    }

    private OrderItem mapItem(ResultSet resultSet) throws SQLException {
        return new OrderItem(
                resultSet.getString("product_name"),
                resultSet.getInt("quantity"),
                resultSet.getFloat("price"));
    }

    private Order buildOrder(OrderRow row, List<OrderItem> items) {
        Client client = clientRepository.findById(row.clientId())
                .orElseThrow(() -> new DatabaseException(
                        "Не найден клиент " + row.clientId() + " для заказа " + row.orderNumber()));
        Courier courier = row.courierId() == null ? null
                : courierRepository.findById(row.courierId())
                .orElseThrow(() -> new DatabaseException(
                        "Не найден курьер " + row.courierId() + " для заказа " + row.orderNumber()));
        return buildOrder(row, items, client, courier);
    }

    private Order buildOrder(OrderRow row, List<OrderItem> items,
                             Map<Integer, Client> clients, Map<Integer, Courier> couriers) {
        Client client = clients.get(row.clientId());
        if (client == null) {
            throw new DatabaseException(
                    "Не найден клиент " + row.clientId() + " для заказа " + row.orderNumber());
        }
        Courier courier = row.courierId() == null ? null : couriers.get(row.courierId());
        if (row.courierId() != null && courier == null) {
            throw new DatabaseException(
                    "Не найден курьер " + row.courierId() + " для заказа " + row.orderNumber());
        }
        return buildOrder(row, items, client, courier);
    }

    private Order buildOrder(OrderRow row, List<OrderItem> items, Client client, Courier courier) {
        Delivery delivery = row.deliveryType() == null
                ? null : DeliveryFactory.create(row.deliveryType());
        return new Order(
                row.orderNumber(),
                client,
                row.address(),
                delivery,
                courier,
                OrderStatus.valueOf(row.status()),
                items);
    }

    private Map<Integer, Client> indexClients() {
        Map<Integer, Client> clients = new HashMap<>();
        for (Client client : clientRepository.findAll()) {
            clients.put(client.getClientID(), client);
        }
        return clients;
    }

    private Map<Integer, Courier> indexCouriers() {
        Map<Integer, Courier> couriers = new HashMap<>();
        for (Courier courier : courierRepository.findAll()) {
            couriers.put(courier.getCourierID(), courier);
        }
        return couriers;
    }

    /**
     * Плоское представление строки таблицы {@code orders}.
     */
    private record OrderRow(int orderNumber, int clientId, String address,
                            String deliveryType, Integer courierId,
                            String status, float totalPrice) {
    }
}
