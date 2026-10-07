package delivery_model;

public class PickUpDelivery implements Delivery {

    @Override
    public String getName() { return "Самовывоз"; }

    @Override
    public int getDeliveryDays() { return 0; }

    @Override
    public float calcPrice() { return 0; }

    @Override
    public boolean isNeedCourier() { return false; }
}
