package people;

public class Person {
    protected short age;
    protected String phoneNumber;

    protected boolean isValidPhoneNumber(String number) {
        if (number == null || number.isEmpty()) {
            return false;
        }

        String digitsOnly = number.replaceAll("\\D", "");

        return number.matches("^\\+?[0-9\\s\\-()]+$")
                && digitsOnly.length() >= 10
                && digitsOnly.length() <= 15;
    }

    protected boolean isValidAge(short age) {
        return 0 <= age && age <= 200;
    }
}
