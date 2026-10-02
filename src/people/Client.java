package people;

import MyException.InvalidAgeException;
import MyException.InvalidPhoneNumberException;

public class Client extends Person {
    public Client(short age, String phoneNumber)
            throws InvalidAgeException, InvalidPhoneNumberException{
        setAge(age);
        setPhoneNumber(phoneNumber);
    }

    public void setAge(short age) throws InvalidAgeException {
        if (!isValidAge(age)) {
            throw new InvalidAgeException();
        }

        this.age = age;
    }

    public void setPhoneNumber(String phoneNumber) throws InvalidPhoneNumberException {
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException();
        }

        this.phoneNumber = phoneNumber;
    }
}
