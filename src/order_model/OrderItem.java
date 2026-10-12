package order_model;

import java.util.Objects;

/**
 * Позиция заказа: одна строка списка товаров.
 *
 * <p>Название товара, количество и цена задаются при создании и не меняются:
 * чтобы изменить позицию, создаётся новая. Стоимость позиции считается
 * произведением количества на цену.</p>
 */
public class OrderItem {

    private final String productName;
    private final int quantity;
    private final float price;

    /**
     * @param productName название товара, не пустое
     * @param quantity    количество, строго больше нуля
     * @param price       цена за единицу, не отрицательная
     * @throws IllegalArgumentException если данные некорректны
     */
    public OrderItem(String productName, int quantity, float price) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Название товара не может быть пустым");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше нуля");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Цена не может быть отрицательной");
        }

        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getPrice() {
        return price;
    }

    /**
     * @return стоимость позиции с учётом количества
     */
    public float getTotal() {
        return quantity * price;
    }

    @Override
    public String toString() {
        return productName + " x" + quantity + " (" + price + " за шт.) = " + getTotal();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OrderItem item)) {
            return false;
        }
        return quantity == item.quantity
                && Float.compare(price, item.price) == 0
                && productName.equals(item.productName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productName, quantity, price);
    }
}
