package delivery_model;

public interface Delivery {
    String getName();
    int getDeliveryDays();
    float calcPrice();
    boolean isNeedCourier();
}