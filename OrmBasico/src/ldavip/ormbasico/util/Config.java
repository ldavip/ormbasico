package ldavip.ormbasico.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 *
 * @author Luis Davi
 */
public class Config {
    
    private static final String FILE_NAME = "ormbasico.properties";
    private static Properties properties;
    
    public static Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            try (FileInputStream fis = new FileInputStream(FILE_NAME)) {
                properties.load(fis);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Arquivo de configuração [" + FILE_NAME + "] não encontrado!");
            }
        }
        return properties;
    }
    
    public static String getProperty(String prop) {
        return getProperties().getProperty(prop);
    }
    
    public static void setProperty(String prop, String valor) {
        try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
            properties.setProperty(prop, valor);
            properties.store(fos, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
