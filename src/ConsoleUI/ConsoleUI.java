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
            return;
//            int choice = readInt("Выберите раздел: ");
//            try {
//                switch (choice) {
//                    case 1 -> showDashboard();
//                    case 2 -> ordersMenu();
//                    case 3 -> clientsMenu();
//                    case 4 -> couriersMenu();
//                    case 0 -> running = false;
//                    default -> System.out.println("Нет такого пункта меню.");
//                }
//            } catch (DatabaseException e) {
//                System.out.println("Ошибка работы с базой данных: " + e.getMessage());
//            } catch (IllegalArgumentException | IllegalStateException e) {
//                System.out.println("Не удалось выполнить действие: " + e.getMessage());
//            }
//            if (running) pause();
        }
//        System.out.println("Работа административной панели завершена.");
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
}
