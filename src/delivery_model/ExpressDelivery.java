package delivery_model;

public class ExpressDelivery implements Delivery {

    @Override
    public String getName() { return "Экспресс-доставка"; }

    @Override
    public int getDeliveryDays() { return 1; }

    @Override
    public float calcPrice() { return 900; }

    @Override
    public boolean isNeedCourier() { return true; }
}