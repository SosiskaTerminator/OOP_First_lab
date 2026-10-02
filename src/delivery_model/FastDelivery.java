package delivery_model;

public class FastDelivery implements DeliveryMain{
    @Override
    public String getDeliveryTime() {
        return "3 дней";
    }

    @Override
    public float caclPrice(float orderPrice) {
        return orderPrice + 467;
    }

    @Override
    public boolean isNeedCourier() {
        return true;
    }
}
