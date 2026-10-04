package people;

import MyException.InvalidAgeException;
import MyException.InvalidNameException;
import MyException.InvalidPhoneNumberException;

public class Client extends Person {
    public Client(short age, String phoneNumber, String name, String surname)
            throws InvalidAgeException, InvalidPhoneNumberException, InvalidNameException {
        setAge(age);
        setPhoneNumber(phoneNumber);
        setName(name);
        setSurname(surname);
    }
}
