package delivery_model;

public class StandardDelivery implements Delivery {
    @Override
    public String getName() { return "Стандартная доставка"; }

    @Override
    public int getDeliveryDays() { return 7; }

    @Override
    public float calcPrice() { return 300; }

    @Override
    public boolean isNeedCourier() { return true; }
}