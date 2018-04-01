package ldavip.ormbasico.exception;

import java.lang.reflect.Field;

/**
 *
 * @author Luis Davi
 */
public class NotNullException extends Exception {

    public NotNullException(Field field) {
        super("Campo nulo: [" + field.getName() + "]");
    }
}
