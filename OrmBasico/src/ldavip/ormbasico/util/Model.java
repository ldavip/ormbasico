package ldavip.ormbasico.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import static ldavip.ormbasico.util.TabelaUtil.isCampoNulo;
import static ldavip.ormbasico.util.TabelaUtil.isNotNull;

/**
 *
 * @author Luis Davi
 */
public class Model {

    public static boolean validar(Object obj) throws Exception {
        
        Class classeObj = obj.getClass();
        for (Field field : classeObj.getDeclaredFields()) {
            if (isNotNull(field) && isCampoNulo(field, obj)) {
                throw new Exception("O campo: [" + field.getName() + "] está anotado com @NotNull e está nulo!");
            }
        }
        return true;
    }
}
