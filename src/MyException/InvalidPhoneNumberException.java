package MyException;

public class InvalidPhoneNumberException extends Exception{
    public InvalidPhoneNumberException() {
        super("Incorrect value of phone number. Example: +7(999) 999-99-99.");
    }
}
