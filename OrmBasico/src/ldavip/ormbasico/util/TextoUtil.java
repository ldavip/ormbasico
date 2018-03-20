package ldavip.ormbasico.util;

/**
 *
 * @author Luis Davi
 */
public class TextoUtil {

    public static String agrupar(String[] textos, String separador) {
        StringBuilder sb = new StringBuilder();
        for (String texto : textos) {
            sb.append(texto).append(separador);
        }
        return sb.substring(0, sb.lastIndexOf(separador));
    }
    
    public static String ajustaCamelCase(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
