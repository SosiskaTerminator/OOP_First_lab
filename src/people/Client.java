package people;

import java.util.regex.Pattern;
import MyException.InvalidAgeException;
import MyException.InvalidEmailException;
import MyException.InvalidNameException;
import MyException.InvalidPhoneNumberException;

public class Client extends Person {
    private String email;
    private String address;
    private int clientID;

    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_LOCAL_PART_LENGTH = 64;
    private static final int MAX_DOMAIN_PART_LENGTH = 190;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*" +
                    "@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
                    "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*" +
                    "\\.[A-Za-z]{2,63}$"
    );

    public Client(short age, int clientID, String phoneNumber, String name,
                  String surname, String email, String address)
            throws InvalidAgeException, InvalidPhoneNumberException, InvalidNameException, InvalidEmailException {
        setAge(age);
        setPhoneNumber(phoneNumber);
        setName(name);
        setSurname(surname);
        setEmail(email);
        setAddress(address);
        setClientID(clientID);
    }

    private String isValidEmail(String email) {
        if (email == null) {
            return "значение не задано (null)";
        }

        if (email.isEmpty()) {
            return "значение пустое";
        }

        if (email.length() > MAX_EMAIL_LENGTH) {
            return "длина превышает " + MAX_EMAIL_LENGTH + " символов";
        }

        int atPos = email.indexOf('@');

        if (atPos < 1) {
            return "некорректный формат: отсутствует @ или локальная часть";
        }

        if (atPos == email.length() - 1) {
            return "некорректный формат: отсутствует домен";
        }

        if (atPos > MAX_LOCAL_PART_LENGTH) {
            return "локальная часть превышает " + MAX_LOCAL_PART_LENGTH + " символов";
        }

        int domainLength = email.length() - atPos - 1;

        if (domainLength > MAX_DOMAIN_PART_LENGTH) {
            return "домен превышает " + MAX_DOMAIN_PART_LENGTH + " символов";
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "email содержит недопустимые символы или некорректный формат";
        }

        return null;
    }

    public void setEmail(String email) throws InvalidEmailException {
        String error = isValidEmail(email);
        if (error != null) {
            throw new InvalidEmailException(error);
        }

        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public int getClientID() {
        return clientID;
    }

    //для это надо будет в ConsoleUI, а точнее в соответсвующем методе получения данных просто по очереди нужные брать
    //а на проверку реально ли такое существует я думаю похуй. главное что не пустое, но тут обратно его просто в поле ввода возвращать
    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return name + " " + surname + ", email:" + email + ", phone number:" + phoneNumber;
    }
}
