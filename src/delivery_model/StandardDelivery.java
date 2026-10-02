package delivery_model;

public class StandardDelivery implements DeliveryMain{
    @Override
    public String getDeliveryTime() {
        return "7 дней";
    }

    @Override
    public float caclPrice(float orderPrice) {
        return orderPrice + 300;
    }

    @Override
    public boolean isNeedCourier() {
        return true;
    }
}
