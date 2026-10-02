package MyException;

public class InvalidAgeException extends Exception{
    public InvalidAgeException() {
        super("Incorrect value of age. The value must be within the interval from 0 to 200.");
    }
}
