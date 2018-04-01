package ldavip.ormbasico.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * @author Luis Davi
 */
public class ConnectionFactory {
    
    public static Connection getConnection() {
        try {
            Map<String, String> config = LeitorXML.carregaXML("ormbasico-config.xml");
            String url = config.get("url");
            String user = config.get("user");
            String password = config.get("password");
            
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver mysql não encontrado!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
