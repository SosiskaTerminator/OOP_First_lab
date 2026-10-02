package delivery_model;

public class ExpressDelivery implements DeliveryMain{
    @Override
    public String getDeliveryTime() {
        return "1 дней";
    }

    @Override
    public float caclPrice(float orderPrice) {
        return orderPrice + 900;
    }

    @Override
    public boolean isNeedCourier() {
        return true;
    }
}
