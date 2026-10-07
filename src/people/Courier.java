package people;

import MyException.InvalidAgeException;
import MyException.InvalidNameException;
import MyException.InvalidPhoneNumberException;

public class Courier extends Person {
    private int courierID;
    private boolean isAvailable;
    private Transport transport;

    public Courier(short age, int courierID, String phoneNumber,
                   String name, String surname, Transport transport)
            throws InvalidAgeException, InvalidPhoneNumberException,
            InvalidNameException {

        setAge(age);
        setPhoneNumber(phoneNumber);
        setName(name);
        setSurname(surname);
        setCourierID(courierID);
        setTransport(transport);

        // При создании курьер свободен
        isAvailable = true;
    }

    public int getCourierID() {
        return courierID;
    }

    public void setCourierID(int courierID) {
        this.courierID = courierID;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public void takeOrder() {
        isAvailable = false;
    }

    public void release() {
        isAvailable = true;
    }

    @Override
    public String toString() {
        return name + " " + surname +
                ", phone number:" + phoneNumber +
                ", transport:" + transport +
                ", available:" + isAvailable;
    }
}

