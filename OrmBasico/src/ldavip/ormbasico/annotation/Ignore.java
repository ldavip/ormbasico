package ldavip.ormbasico.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import ldavip.ormbasico.dao.Dao.Operacao;

/**
 *
 * @author Luis Davi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ignore {

    Operacao[] operacao() default Operacao.SELECT;
}
