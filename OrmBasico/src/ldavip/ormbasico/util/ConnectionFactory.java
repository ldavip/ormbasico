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
            String database = config.get("database");
            if (database == null || database.isEmpty()) {
                throw new IllegalArgumentException("O banco de dados utilizado não foi especificado!");
            }
            
            if (database.toLowerCase().equals("sqlserver")) {
                String strCon = config.get("sqlserverConnectionString");
                if (strCon == null || strCon.isEmpty()) {
                    throw new IllegalArgumentException("O banco de dados especificado foi o SQLServer mas não foi especificada a string de conexão!");
                }
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                return DriverManager.getConnection(strCon);
            } else if (database.toLowerCase().equals("mysql")) {
                Class.forName("com.mysql.jdbc.Driver");
                return DriverManager.getConnection(url, user, password);
            }
            return null;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver não encontrado!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
