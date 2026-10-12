import ConsoleUI.ConsoleUI;
import order_model.ClientRepository;
import order_model.CourierRepository;
import order_model.DatabaseManager;
import order_model.OrderRepository;

public class Main {
    public static void main(String args[]) {
        DatabaseManager databaseManager = new DatabaseManager("jdbc:sqlite:delivery.db");

        ClientRepository clientRepository = new ClientRepository(databaseManager);
        CourierRepository courierRepository = new CourierRepository(databaseManager);
        OrderRepository orderRepository = new OrderRepository(
                databaseManager, clientRepository, courierRepository);

        ConsoleUI consoleUI = new ConsoleUI(
                clientRepository, courierRepository, orderRepository);
        consoleUI.start();
    }
}