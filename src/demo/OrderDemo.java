package demo;

import java.util.List;

import MyException.DatabaseException;
import delivery_model.ExpressDelivery;
import order_model.ClientRepository;
import order_model.CourierRepository;
import order_model.DatabaseManager;
import order_model.Order;
import order_model.OrderItem;
import order_model.OrderRepository;
import order_model.OrderStatus;
import people.Client;
import people.Courier;
import people.Transport;

/**
 * Ручная проверка слоя заказов и работы с базой данных.
 *
 * <p>Запускается как обычная программа (метод {@code main}) и по шагам
 * показывает: сохранение клиента, курьера и заказа, назначение курьера,
 * смену статусов, повторное чтение из базы, вывод всех заказов и удаление.
 * База {@code delivery.db} создаётся в текущей рабочей папке.</p>
 */
public final class OrderDemo {

    private OrderDemo() {
    }

    public static void main(String[] args) {
        new java.io.File("delivery.db").delete();

        DatabaseManager database = new DatabaseManager("jdbc:sqlite:delivery.db");
        ClientRepository clients = new ClientRepository(database);
        CourierRepository couriers = new CourierRepository(database);
        OrderRepository orders = new OrderRepository(database, clients, couriers);

        try {
            Client client = new Client((short) 30, 1, "+7 999 123-45-67",
                    "Иван", "Петров", "ivan@mail.ru", "Москва, ул. Ленина 1");
            clients.save(client);
            System.out.println("1) Клиент сохранён: " + client.getClientID());

            Courier courier = new Courier((short) 25, 10, "+7 999 765-43-21",
                    "Пётр", "Сидоров", Transport.CAR);
            couriers.save(courier);
            System.out.println("2) Курьер сохранён: " + courier.getCourierID());

            Order order = new Order(client, "Москва, ул. Гагарина 5");
            order.addItem(new OrderItem("Книга", 2, 500f));
            order.addItem(new OrderItem("Ручка", 3, 50f));
            order.setDelivery(new ExpressDelivery());
            orders.save(order);
            System.out.println("3) Заказ сохранён: " + order);
            
            
            order.assignCourier(courier);
            orders.update(order);
            System.out.println("4) Курьер назначен: " + order.getStatus()
                    + ", курьер занят=" + !courier.isAvailable());

            Order loaded = orders.findByNumber(order.getOrderNumber())
                    .orElseThrow(() -> new DatabaseException("Заказ не найден"));
            System.out.println("5) Прочитан из БД: " + loaded);
            System.out.println("   позиций=" + loaded.getItems().size()
                    + ", итого=" + loaded.getTotalPrice());

            loaded.changeStatus(OrderStatus.IN_DELIVERY);
            loaded.changeStatus(OrderStatus.DELIVERED);
            orders.update(loaded);
            System.out.println("6) Статус доставлен: " + loaded.getStatus()
                    + ", курьер снова свободен=" + loaded.getCourier().isAvailable());

            List<Order> all = orders.findAll();
            System.out.println("7) Всего заказов в БД: " + all.size());

            try {
                loaded.addItem(new OrderItem("Товар", 1, 100f));
                System.out.println("8) ОШИБКА: позиция добавилась после доставки");
            } catch (IllegalStateException e) {
                System.out.println("8) Правильно запрещено менять: " + e.getMessage());
            }

            orders.delete(loaded.getOrderNumber());
            System.out.println("9) Заказ удалён, осталось заказов: " + orders.findAll().size());

            Order sample = new Order(client, "Санкт-Петербург, Невский пр. 10");
            sample.addItem(new OrderItem("Монитор", 1, 15000f));
            sample.addItem(new OrderItem("Кабель HDMI", 2, 700f));
            sample.setDelivery(new delivery_model.StandardDelivery());
            orders.save(sample);
            System.out.println("10) Оставил заказ №" + sample.getOrderNumber()
                    + " в БД для просмотра через sqlite3");
            System.out.println("ГОТОВО: всё отработало без ошибок");
        } catch (Exception e) {
            System.err.println("ОШИБКА: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
