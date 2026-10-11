package order_model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import delivery_model.Delivery;
import people.Client;
import people.Courier;

/**
 * Заказ службы доставки.
 *
 * <p>Сущность хранит номер, клиента, адрес доставки, список позиций,
 * выбранный способ доставки, назначенного курьера и текущий статус.
 * Все изменения состояния проходят через методы класса и проверяются
 * на допустимость: состав и способ доставки можно менять только пока
 * заказ в статусе {@link OrderStatus#CREATED}, курьер назначается
 * только для способов доставки, которые этого требуют, а статус
 * меняется лишь по разрешённым переходам.</p>
 */
public class Order {

    private int orderNumber;
    private final Client client;
    private String address;
    private final List<OrderItem> items = new ArrayList<>();
    private Delivery delivery;
    private Courier courier;
    private OrderStatus status;
    private float totalPrice;

    /**
     * Создаёт новый заказ в статусе {@link OrderStatus#CREATED}.
     *
     * @param client  клиент, оформляющий заказ, не {@code null}
     * @param address адрес доставки, не пустой
     * @throws IllegalArgumentException если данные некорректны
     */
    public Order(Client client, String address) {
        if (client == null) {
            throw new IllegalArgumentException("Клиент обязателен для заказа");
        }
        this.client = client;
        setAddress(address);
        this.status = OrderStatus.CREATED;
        this.totalPrice = 0f;
    }

    /**
     * Восстанавливает полностью сформированный заказ.
     * Используется репозиторием при чтении из базы данных.
     */
    Order(int orderNumber, Client client, String address, Delivery delivery,
          Courier courier, OrderStatus status, List<OrderItem> items) {
        this(client, address);
        this.orderNumber = orderNumber;
        this.delivery = delivery;
        this.courier = courier;
        this.status = status == null ? OrderStatus.CREATED : status;
        if (items != null) {
            this.items.addAll(items);
        }
        recalculateTotal();
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Client getClient() {
        return client;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Адрес доставки не может быть пустым");
        }
        this.address = address;
    }

    /**
     * @return неизменяемый список позиций заказа
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public Courier getCourier() {
        return courier;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public float getTotalPrice() {
        return totalPrice;
    }

    /**
     * Добавляет позицию в заказ.
     *
     * @param item новая позиция, не {@code null}
     * @throws IllegalStateException если заказ уже нельзя изменять
     */
    public void addItem(OrderItem item) {
        requireMutable();
        if (item == null) {
            throw new IllegalArgumentException("Позиция не может быть пустой");
        }
        items.add(item);
        recalculateTotal();
    }

    /**
     * Задаёт способ доставки. Менять его можно только до назначения курьера.
     *
     * @param delivery способ доставки, не {@code null}
     * @throws IllegalStateException если заказ уже нельзя изменять
     */
    public void setDelivery(Delivery delivery) {
        requireMutable();
        if (delivery == null) {
            throw new IllegalArgumentException("Способ доставки не может быть пустым");
        }
        this.delivery = delivery;
        recalculateTotal();
    }

    /**
     * Назначает курьера и помечает его занятым.
     *
     * <p>Курьер назначается, только если выбранный способ доставки требует
     * курьера и сам курьер свободен. При успешном назначении заказ
     * переводится в статус {@link OrderStatus#ASSIGNED}.</p>
     *
     * @param courier свободный курьер
     * @throws IllegalStateException если способ доставки не требует курьера,
     *                               курьер занят или заказ уже закрыт
     */
    public void assignCourier(Courier courier) {
        if (status.isFinal()) {
            throw new IllegalStateException("Нельзя изменить завершённый заказ");
        }
        if (delivery == null) {
            throw new IllegalStateException("Сначала выберите способ доставки");
        }
        if (!delivery.isNeedCourier()) {
            throw new IllegalStateException("Выбранный способ доставки не требует курьера");
        }
        if (courier == null) {
            throw new IllegalArgumentException("Курьер не может быть пустым");
        }
        if (!courier.isAvailable()) {
            throw new IllegalStateException("Курьер недоступен: у него уже есть заказ");
        }

        this.courier = courier;
        courier.takeOrder();
        if (status == OrderStatus.CREATED) {
            changeStatus(OrderStatus.ASSIGNED);
        }
    }

    /**
     * Переводит заказ в новый статус.
     *
     * @param next целевой статус
     * @throws IllegalStateException если переход недопустим или не выполнены
     *                               условия для нового статуса
     */
    public void changeStatus(OrderStatus next) {
        if (next == null) {
            throw new IllegalArgumentException("Новый статус не задан");
        }
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Недопустимый переход статуса: " + status + " -> " + next);
        }
        requireCourierFor(next);

        this.status = next;
        releaseCourierIfFinished();
    }

    /**
     * Пересчитывает итоговую стоимость: сумма позиций плюс стоимость доставки.
     */
    public void recalculateTotal() {
        float itemsTotal = 0f;
        for (OrderItem item : items) {
            itemsTotal += item.getTotal();
        }
        float deliveryPrice = delivery == null ? 0f : delivery.calcPrice();
        this.totalPrice = itemsTotal + deliveryPrice;
    }

    private void requireMutable() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Заказ можно изменять только в статусе CREATED, сейчас: " + status);
        }
    }

    private void requireCourierFor(OrderStatus next) {
        boolean needsCourier = delivery != null && delivery.isNeedCourier();
        if (next == OrderStatus.ASSIGNED && (courier == null || !needsCourier)) {
            throw new IllegalStateException(
                    "Статус ASSIGNED требует назначенного курьера");
        }
        if (next == OrderStatus.IN_DELIVERY && needsCourier && courier == null) {
            throw new IllegalStateException(
                    "Перед отправкой в доставку нужно назначить курьера");
        }
    }

    private void releaseCourierIfFinished() {
        if (status.isFinal() && courier != null && !courier.isAvailable()) {
            courier.release();
        }
    }

    @Override
    public String toString() {
        return "Заказ №" + orderNumber
                + ", клиент: " + client.getName() + " " + client.getSurname()
                + ", адрес: " + address
                + ", позиций: " + items.size()
                + ", доставка: " + (delivery == null ? "не выбрана" : delivery.getName())
                + ", курьер: " + (courier == null ? "не назначен" : courier.getName())
                + ", статус: " + status
                + ", итого: " + totalPrice;
    }
}
