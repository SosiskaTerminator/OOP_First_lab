package people;

import java.util.regex.Pattern;
import MyException.InvalidAgeException;
import MyException.InvalidNameException;
import MyException.InvalidPhoneNumberException;

public abstract class Person {
    protected short age;
    protected String phoneNumber;
    protected String name;
    protected String surname;

    private static final int MAX_NAME_LENGTH = 100;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}]+(?:[-'’][\\p{L}]+)*$");

    private boolean isValidPhoneNumber(String number) {
        if (number == null || number.isEmpty()) {
            return false;
        }

        String digitsOnly = number.replaceAll("\\D", "");

        return number.matches("^\\+?[0-9\\s\\-()]+$")
                && digitsOnly.length() >= 10
                && digitsOnly.length() <= 15;
    }

    private boolean isValidAge(short age) {
        return 0 <= age && age <= 200;
    }

    private String isValidNameSurname(String value) {
        if (value == null) return "значение не задано (null)";
        if (value.isEmpty()) return "значение пустое";
        if (value.length() > MAX_NAME_LENGTH)
            return "длина превышает " + MAX_NAME_LENGTH + " символов";

        if (!NAME_PATTERN.matcher(value).matches())
            return "содержит недопустимые символы";
        return null;
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

    public void setName(String name) throws InvalidNameException {
        String error = isValidNameSurname(name);
        if (error != null) {
            throw new InvalidNameException("Некорректное имя: " + error);
        }

        this.name = name;
    }

    public void setSurname(String surname) throws InvalidNameException {
        String error = isValidNameSurname(surname);
        if (error != null) {
            throw new InvalidNameException("Некорректное имя: " + error);
        }

        this.surname = surname;
    }

    public short getAge() {
        return age;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }
}
