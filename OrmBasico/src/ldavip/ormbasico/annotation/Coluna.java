package ldavip.ormbasico.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Luis Davi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Coluna {

    public enum Tipo {
        BOOLEAN, INT, LONG, DOUBLE, BIGDECIMAL, STRING, DATE
    }

    String nome() default "";
}
