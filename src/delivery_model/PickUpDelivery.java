package delivery_model;

public class PickUpDelivery implements DeliveryMain{
    @Override
    public String getDeliveryTime() {
        return "Сегодня";
    }

    @Override
    public float caclPrice(float orderPrice) {
        return orderPrice + 0;
    }

    @Override
    public boolean isNeedCourier() {
        return false;
    }
}
