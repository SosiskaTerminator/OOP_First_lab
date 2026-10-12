package ConsoleUI;

import MyException.DatabaseException;
import MyException.InvalidAgeException;
import MyException.InvalidEmailException;
import MyException.InvalidNameException;
import MyException.InvalidPhoneNumberException;
import delivery_model.Delivery;
import order_model.*;
import people.*;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ConsoleUI {
    private final Scanner scanner = new Scanner(System.in);
    private final ClientRepository clientRepository;
    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;

    public ConsoleUI(ClientRepository clientRepository,
                     CourierRepository courierRepository,
                     OrderRepository orderRepository) {
        this.clientRepository = clientRepository;
        this.courierRepository = courierRepository;
        this.orderRepository = orderRepository;
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Выберите раздел: ");
            try {
                switch (choice) {
                    case 1 -> showDashboard();
                    case 2 -> ordersMenu();
                    case 3 -> clientsMenu();
                    case 4 -> couriersMenu();
                    case 0 -> running = false;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (DatabaseException e) {
                System.out.println("Ошибка работы с базой данных: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Не удалось выполнить действие: " + e.getMessage());
            }
            if (running) pause();
        }
        System.out.println("Работа административной панели завершена.");
    }

    private void printMainMenu() {
        System.out.println("\n==================================");
        System.out.println("       DELIVERY ADMIN PANEL");
        System.out.println("==================================");
        System.out.println("1. Главная панель / статистика");
        System.out.println("2. Управление заказами");
        System.out.println("3. Управление клиентами");
        System.out.println("4. Управление курьерами");
        System.out.println("0. Выход");
        System.out.println("----------------------------------");
    }

    private void showDashboard() {
        List<Order> orders = orderRepository.findAll();
        List<Client> clients = clientRepository.findAll();
        List<Courier> couriers = courierRepository.findAll();

        int created = 0;
        int assigned = 0;
        int inDelivery = 0;
        int delivered = 0;
        int cancelled = 0;
        for (Order order : orders) {
            switch (order.getStatus()) {
                case CREATED -> created++;
                case ASSIGNED -> assigned++;
                case IN_DELIVERY -> inDelivery++;
                case DELIVERED -> delivered++;
                case CANCELLED -> cancelled++;
            }
        }

        int availableCouriers = 0;
        for (Courier courier : couriers) {
            if (courier.isAvailable()) availableCouriers++;
        }

        System.out.println("\n========== СТАТИСТИКА ==========");
        System.out.println("Всего заказов: " + orders.size());
        System.out.println("  Создано: " + created);
        System.out.println("  Назначен курьер: " + assigned);
        System.out.println("  В доставке / ожидает получения: " + inDelivery);
        System.out.println("  Доставлено: " + delivered);
        System.out.println("  Отменено: " + cancelled);
        System.out.println("Всего клиентов: " + clients.size());
        System.out.println("Всего курьеров: " + couriers.size());
        System.out.println("Свободных курьеров: " + availableCouriers);
    }

    private void ordersMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========== ЗАКАЗЫ ==========");
            System.out.println("1. Показать все заказы");
            System.out.println("2. Найти заказ по номеру");
            System.out.println("3. Создать заказ");
            System.out.println("4. Изменить статус заказа");
            System.out.println("5. Назначить курьера");
            System.out.println("6. Удалить заказ");
            System.out.println("0. Назад");
            int choice = readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> showOrders();
                    case 2 -> findOrder();
                    case 3 -> createOrder();
                    case 4 -> changeOrderStatus();
                    case 5 -> assignCourier();
                    case 6 -> deleteOrder();
                    case 0 -> back = true;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (DatabaseException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Не удалось выполнить действие: " + e.getMessage());
            }
            if (!back) pause();
        }
    }

    private void showOrders() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            System.out.println("Заказов пока нет.");
            return;
        }
        System.out.println("\n========== СПИСОК ЗАКАЗОВ ==========");
        for (Order order : orders) {
            System.out.println(order);
        }
    }

    private void findOrder() {
        int id = readInt("Номер заказа: ");
        Optional<Order> found = orderRepository.findByNumber(id);
        if (found.isEmpty()) {
            System.out.println("Заказ №" + id + " не найден.");
            return;
        }
        Order order = found.get();
        System.out.println("\n" + order);
        System.out.println("Товары:");
        if (order.getItems().isEmpty()) {
            System.out.println("  Список товаров пуст.");
        } else {
            for (OrderItem item : order.getItems()) {
                System.out.println("  - " + item);
            }
        }
        System.out.println("Стоимость доставки: "
                + (order.getDelivery() == null ? "не выбрана" : order.getDelivery().calcPrice()));
    }

    private void createOrder() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty()) {
            System.out.println("Нельзя создать заказ: сначала добавьте клиента в разделе «Клиенты».");
            return;
        }

        System.out.println("\nКлиенты:");
        for (Client client : clients) {
            System.out.println(client.getClientID() + ". " + client.getName() + " "
                    + client.getSurname() + ", телефон: " + client.getPhoneNumber());
        }
        int clientId = readInt("ID клиента: ");
        Optional<Client> clientOptional = clientRepository.findById(clientId);
        if (clientOptional.isEmpty()) {
            System.out.println("Клиент с ID " + clientId + " не найден.");
            return;
        }

        String address = readNonEmpty("Адрес доставки (для самовывоза можно указать пункт выдачи): ");
        Order order = new Order(clientOptional.get(), address);

        System.out.println("\nСпособ доставки:");
        System.out.println("1. Стандартная доставка (300, 7 дней)");
        System.out.println("2. Экспресс-доставка (900, 1 день)");
        System.out.println("3. Быстрая доставка (467, 3 дня)");
        System.out.println("4. Самовывоз (0, 0 дней)");
        int deliveryChoice = readInt("Выберите способ: ");
        Delivery delivery = switch (deliveryChoice) {
            case 1 -> DeliveryFactory.create("StandardDelivery");
            case 2 -> DeliveryFactory.create("ExpressDelivery");
            case 3 -> DeliveryFactory.create("FastDelivery");
            case 4 -> DeliveryFactory.create("PickUpDelivery");
            default -> throw new IllegalArgumentException("Неизвестный способ доставки.");
        };
        order.setDelivery(delivery);

        System.out.println("\nДобавление товаров. Чтобы закончить, введите пустое название.");
        while (true) {
            String productName = readLine("Название товара: ");
            if (productName.isBlank()) {
                if (order.getItems().isEmpty()) {
                    System.out.println("Добавьте хотя бы один товар.");
                    continue;
                }
                break;
            }
            int quantity = readInt("Количество: ");
            float price = readFloat("Цена за единицу: ");
            order.addItem(new OrderItem(productName, quantity, price));
        }

        if (delivery.isNeedCourier()) {
            System.out.println("\nДоступные курьеры:");
            List<Courier> available = courierRepository.findAll().stream()
                    .filter(Courier::isAvailable).toList();
            if (available.isEmpty()) {
                System.out.println("Внимание: сейчас нет свободных курьеров. Заказ будет сохранён без назначения.");
            } else {
                for (Courier courier : available) {
                    System.out.println(courier.getCourierID() + ". " + courier.getName() + " "
                            + courier.getSurname() + " (" + courier.getTransport() + ")");
                }
                if (readYesNo("Назначить курьера сейчас? (д/н): ")) {
                    int courierId = readInt("ID курьера: ");
                    Optional<Courier> courier = courierRepository.findById(courierId);
                    if (courier.isEmpty()) {
                        System.out.println("Курьер не найден. Заказ сохранится без назначения.");
                    } else {
                        order.assignCourier(courier.get());
                    }
                }
            }
        }

        orderRepository.save(order);
        if (order.getCourier() != null) {
            courierRepository.update(order.getCourier());
        }
        System.out.println("Заказ успешно создан. Номер заказа: " + order.getOrderNumber());
        System.out.println("Итоговая стоимость: " + order.getTotalPrice());
    }

    private void changeOrderStatus() {
        Order order = getOrderByPrompt();
        if (order == null) return;

        System.out.println("Текущий статус: " + order.getStatus());
        System.out.println("Доступные статусы:");
        for (OrderStatus status : OrderStatus.values()) {
            System.out.println((status.ordinal() + 1) + ". " + status);
        }
        int choice = readInt("Новый статус (номер): ");
        if (choice < 1 || choice > OrderStatus.values().length) {
            System.out.println("Некорректный номер статуса.");
            return;
        }

        Courier courier = order.getCourier();
        order.changeStatus(OrderStatus.values()[choice - 1]);
        orderRepository.update(order);
        if (courier != null) courierRepository.update(courier);
        System.out.println("Статус заказа обновлён: " + order.getStatus());
    }

    private void assignCourier() {
        Order order = getOrderByPrompt();
        if (order == null) return;

        List<Courier> available = courierRepository.findAll().stream()
                .filter(Courier::isAvailable).toList();
        if (available.isEmpty()) {
            System.out.println("Нет свободных курьеров.");
            return;
        }
        System.out.println("Свободные курьеры:");
        for (Courier courier : available) {
            System.out.println(courier.getCourierID() + ". " + courier.getName() + " "
                    + courier.getSurname() + " (" + courier.getTransport() + ")");
        }
        int courierId = readInt("ID курьера: ");
        Optional<Courier> found = courierRepository.findById(courierId);
        if (found.isEmpty()) {
            System.out.println("Курьер не найден.");
            return;
        }
        order.assignCourier(found.get());
        orderRepository.update(order);
        courierRepository.update(found.get());
        System.out.println("Курьер назначен заказу №" + order.getOrderNumber());
    }

    private void deleteOrder() {
        Order order = getOrderByPrompt();
        if (order == null) return;
        if (!readYesNo("Точно удалить заказ №" + order.getOrderNumber() + "? (д/н): ")) {
            System.out.println("Удаление отменено.");
            return;
        }
        Courier courier = order.getCourier();
        if (courier != null && !courier.isAvailable()) {
            courier.release();
            courierRepository.update(courier);
        }
        orderRepository.delete(order.getOrderNumber());
        System.out.println("Заказ удалён.");
    }

    private Order getOrderByPrompt() {
        int id = readInt("Номер заказа: ");
        Optional<Order> found = orderRepository.findByNumber(id);
        if (found.isEmpty()) {
            System.out.println("Заказ №" + id + " не найден.");
            return null;
        }
        return found.get();
    }

    private void clientsMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========== КЛИЕНТЫ ==========");
            System.out.println("1. Показать всех клиентов");
            System.out.println("2. Найти клиента по ID");
            System.out.println("3. Добавить клиента");
            System.out.println("4. Удалить клиента");
            System.out.println("0. Назад");
            int choice = readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> showClients();
                    case 2 -> findClient();
                    case 3 -> createClient();
                    case 4 -> deleteClient();
                    case 0 -> back = true;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (DatabaseException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Не удалось выполнить действие: " + e.getMessage());
            }
            if (!back) pause();
        }
    }

    private void showClients() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty()) {
            System.out.println("Клиентов пока нет.");
            return;
        }
        for (Client client : clients) {
            System.out.println("ID: " + client.getClientID() + " | " + client
                    + " | адрес: " + client.getAddress());
        }
    }

    private void findClient() {
        int id = readInt("ID клиента: ");
        Optional<Client> found = clientRepository.findById(id);
        if (found.isEmpty()) System.out.println("Клиент не найден.");
        else System.out.println("ID: " + found.get().getClientID() + " | "
                + found.get() + " | адрес: " + found.get().getAddress());
    }

    private void createClient() {
        int id = readInt("Новый ID клиента (целое число): ");
        String name = readNonEmpty("Имя: ");
        String surname = readNonEmpty("Фамилия: ");
        short age = readShort("Возраст: ");
        String phone = readNonEmpty("Телефон: ");
        String email = readNonEmpty("Email: ");
        String address = readNonEmpty("Адрес: ");
        try {
            Client client = new Client(age, id, phone, name, surname, email, address);
            clientRepository.save(client);
            System.out.println("Клиент добавлен.");
        } catch (InvalidAgeException | InvalidPhoneNumberException |
                 InvalidNameException | InvalidEmailException e) {
            System.out.println("Некорректные данные клиента: " + e.getMessage());
        }
    }

    private void deleteClient() {
        int id = readInt("ID клиента для удаления: ");
        if (!clientRepository.findById(id).isPresent()) {
            System.out.println("Клиент не найден.");
            return;
        }
        if (!readYesNo("Удалить клиента? Если на него ссылается заказ, база не разрешит удаление. (д/н): ")) {
            System.out.println("Удаление отменено.");
            return;
        }
        clientRepository.delete(id);
        System.out.println("Клиент удалён.");
    }

    private void couriersMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========== КУРЬЕРЫ ==========");
            System.out.println("1. Показать всех курьеров");
            System.out.println("2. Найти курьера по ID");
            System.out.println("3. Добавить курьера");
            System.out.println("4. Изменить доступность курьера");
            System.out.println("5. Удалить курьера");
            System.out.println("0. Назад");
            int choice = readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> showCouriers();
                    case 2 -> findCourier();
                    case 3 -> createCourier();
                    case 4 -> toggleCourierAvailability();
                    case 5 -> deleteCourier();
                    case 0 -> back = true;
                    default -> System.out.println("Нет такого пункта меню.");
                }
            } catch (DatabaseException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Не удалось выполнить действие: " + e.getMessage());
            }
            if (!back) pause();
        }
    }

    private void showCouriers() {
        List<Courier> couriers = courierRepository.findAll();
        if (couriers.isEmpty()) {
            System.out.println("Курьеров пока нет.");
            return;
        }
        for (Courier courier : couriers) {
            System.out.println("ID: " + courier.getCourierID() + " | " + courier);
        }
    }

    private void findCourier() {
        int id = readInt("ID курьера: ");
        Optional<Courier> found = courierRepository.findById(id);
        if (found.isEmpty()) System.out.println("Курьер не найден.");
        else System.out.println("ID: " + found.get().getCourierID() + " | " + found.get());
    }

    private void createCourier() {
        int id = readInt("Новый ID курьера (целое число): ");
        String name = readNonEmpty("Имя: ");
        String surname = readNonEmpty("Фамилия: ");
        short age = readShort("Возраст: ");
        String phone = readNonEmpty("Телефон: ");
        System.out.println("Транспорт:");
        Transport[] transports = Transport.values();
        for (int i = 0; i < transports.length; i++) {
            System.out.println((i + 1) + ". " + transports[i]);
        }
        int choice = readInt("Выберите транспорт: ");
        if (choice < 1 || choice > transports.length) {
            throw new IllegalArgumentException("Неизвестный вид транспорта.");
        }
        try {
            Courier courier = new Courier(age, id, phone, name, surname, transports[choice - 1]);
            courierRepository.save(courier);
            System.out.println("Курьер добавлен.");
        } catch (InvalidAgeException | InvalidPhoneNumberException | InvalidNameException e) {
            System.out.println("Некорректные данные курьера: " + e.getMessage());
        }
    }

    private void toggleCourierAvailability() {
        int id = readInt("ID курьера: ");
        Optional<Courier> found = courierRepository.findById(id);
        if (found.isEmpty()) {
            System.out.println("Курьер не найден.");
            return;
        }
        Courier courier = found.get();
        if (courier.isAvailable()) {
            courier.takeOrder();
            System.out.println("Курьер помечен как занятый.");
        } else {
            courier.release();
            System.out.println("Курьер помечен как свободный.");
        }
        courierRepository.update(courier);
    }

    private void deleteCourier() {
        int id = readInt("ID курьера для удаления: ");
        Optional<Courier> found = courierRepository.findById(id);
        if (found.isEmpty()) {
            System.out.println("Курьер не найден.");
            return;
        }
        if (!found.get().isAvailable()) {
            System.out.println("Курьер занят. Сначала завершите или переназначьте его заказ.");
            return;
        }
        if (!readYesNo("Удалить курьера? Если на него ссылается заказ, база не разрешит удаление. (д/н): ")) {
            System.out.println("Удаление отменено.");
            return;
        }
        courierRepository.delete(id);
        System.out.println("Курьер удалён.");
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число.");
            }
        }
    }

    private short readShort(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                return Short.parseShort(value);
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число в допустимом диапазоне.");
            }
        }
    }

    private float readFloat(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim().replace(',', '.');
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                System.out.println("Введите число, например 125.50.");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private String readNonEmpty(String prompt) {
        while (true) {
            String value = readLine(prompt).trim();
            if (!value.isEmpty()) return value;
            System.out.println("Поле не должно быть пустым.");
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt).trim().toLowerCase();
            if (answer.equals("д") || answer.equals("да") || answer.equals("y") || answer.equals("yes")) return true;
            if (answer.equals("н") || answer.equals("нет") || answer.equals("n") || answer.equals("no")) return false;
            System.out.println("Введите «д» или «н».");
        }
    }

    private void pause() {
        System.out.print("\nНажмите Enter, чтобы продолжить...");
        scanner.nextLine();
    }
}
