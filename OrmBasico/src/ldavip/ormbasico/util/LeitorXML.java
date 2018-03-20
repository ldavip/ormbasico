package ldavip.ormbasico.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Luis Davi
 */
public class LeitorXML {

    public static Map<String, String> carregaXML(String nomeArquivo) {
        Map<String, String> map = new HashMap<>();
        try (FileInputStream fileInput = new FileInputStream(new File(nomeArquivo))) {
            Properties properties = new Properties();
            properties.loadFromXML(fileInput);

            Enumeration enuKeys = properties.keys();
            while (enuKeys.hasMoreElements()) {
                String key = (String) enuKeys.nextElement();
                String value = properties.getProperty(key);
                map.put(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
    
    public static String getValor(String propriedade, String nomeArquivo) {
        Map<String, String> map = carregaXML(nomeArquivo);
        return map.get(propriedade);
    }
}
