package MyException;

/**
 * Ошибка уровня доступа к базе данных.
 *
 * <p>Оборачивает низкоуровневые {@link java.sql.SQLException}
 * и ошибки валидации, возникающие при сборке объектов из строк БД,
 * чтобы репозитории не «протекали» деталями JDBC наружу.</p>
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
