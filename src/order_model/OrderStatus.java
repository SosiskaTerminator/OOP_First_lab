package order_model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Статусы заказа и правила перехода между ними.
 *
 * <p>Основная цепочка: {@code CREATED -> ASSIGNED -> IN_DELIVERY -> DELIVERED}.
 * Для самовывоза (курьер не нужен) допускается шаг
 * {@code CREATED -> IN_DELIVERY} напрямую. Из любого незавершённого
 * статуса заказ можно отменить.</p>
 */
public enum OrderStatus {

    /** Заказ создан, состав и способ доставки ещё можно менять. */
    CREATED,

    /** Назначен курьер. */
    ASSIGNED,

    /** Заказ в пути / ожидает получения. */
    IN_DELIVERY,

    /** Заказ доставлен клиенту. Конечный статус. */
    DELIVERED,

    /** Заказ отменён. Конечный статус. */
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(CREATED, EnumSet.of(ASSIGNED, IN_DELIVERY, CANCELLED));
        ALLOWED_TRANSITIONS.put(ASSIGNED, EnumSet.of(IN_DELIVERY, CANCELLED));
        ALLOWED_TRANSITIONS.put(IN_DELIVERY, EnumSet.of(DELIVERED, CANCELLED));
        ALLOWED_TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    /**
     * Проверяет, допустим ли переход из текущего статуса в указанный.
     *
     * @param next целевой статус
     * @return {@code true}, если переход разрешён
     */
    public boolean canTransitionTo(OrderStatus next) {
        return next != null && ALLOWED_TRANSITIONS.get(this).contains(next);
    }

    /**
     * @return {@code true}, если статус является конечным
     */
    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
