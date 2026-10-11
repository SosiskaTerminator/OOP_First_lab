package order_model;

import delivery_model.Delivery;
import delivery_model.ExpressDelivery;
import delivery_model.FastDelivery;
import delivery_model.PickUpDelivery;
import delivery_model.StandardDelivery;

/**
 * Создаёт объекты {@link Delivery} по их строковому представлению.
 *
 * <p>Нужен, чтобы репозиторий мог восстановить способ доставки заказа
 * из базы данных: там хранится только короткое имя класса.
 * Все реализации {@link Delivery} не хранят состояния, поэтому фабрика
 * просто возвращает новый экземпляр нужного типа.</p>
 */
public final class DeliveryFactory {

    private DeliveryFactory() {
    }

    /**
     * @param type короткое имя класса ({@code "StandardDelivery"}) или
     *             название способа доставки ({@code "Стандартная доставка"})
     * @return объект способа доставки
     * @throws IllegalArgumentException если тип неизвестен
     */
    public static Delivery create(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Тип доставки не задан");
        }
        return switch (type) {
            case "StandardDelivery", "Стандартная доставка" -> new StandardDelivery();
            case "ExpressDelivery", "Экспресс-доставка" -> new ExpressDelivery();
            case "FastDelivery", "Быстрая доставка" -> new FastDelivery();
            case "PickUpDelivery", "Самовывоз" -> new PickUpDelivery();
            default -> throw new IllegalArgumentException("Неизвестный тип доставки: " + type);
        };
    }

    /**
     * @param delivery способ доставки
     * @return короткое имя класса для хранения в БД, или {@code null}
     */
    public static String typeName(Delivery delivery) {
        return delivery == null ? null : delivery.getClass().getSimpleName();
    }
}
