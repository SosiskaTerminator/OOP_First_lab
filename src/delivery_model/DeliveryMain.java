package delivery_model;

public interface DeliveryMain {
    public String getDeliveryTime();
    public float caclPrice(float orderPrice);
    public boolean isNeedCourier();
}
