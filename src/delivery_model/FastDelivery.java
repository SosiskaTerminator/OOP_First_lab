package delivery_model;

public class FastDelivery implements Delivery{
    @Override
    public String getName() { return "Быстрая доставка"; }

    @Override
    public int getDeliveryDays() { return 3; }

    @Override
    public float calcPrice() { return 467; }

    @Override
    public boolean isNeedCourier() { return true; }
}
