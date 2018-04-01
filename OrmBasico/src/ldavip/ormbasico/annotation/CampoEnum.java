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
public @interface CampoEnum {
    
    /**
     * Define o tipo de dado que será guardado na tabela.
     *      
     *      <code>int.class</code> ou 
     *      <code>String.class</code>
     * @return 
     */
    Class<?> tipoDaColuna() default String.class;
}
