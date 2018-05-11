package ldavip.ormbasico.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 * @author Luis Davi
 */
public class ConnectionFactory {
    
    public static Connection getConnection() {
        try {
            Properties config = Config.getProperties();
            
            String database = config.getProperty("database");
            if (database == null || database.isEmpty()) {
                throw new IllegalArgumentException("O banco de dados utilizado não foi especificado!");
            }
            
            String strCon = config.getProperty("connectionString");
            if (strCon == null || strCon.isEmpty()) {
                throw new IllegalArgumentException("String de conexão não especificada!");
            }
            if (database.toLowerCase().equals("sqlserver")) {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            } else if (database.toLowerCase().equals("mysql")) {
                Class.forName("com.mysql.jdbc.Driver");
            } else if (database.toLowerCase().equals("h2")) {
                Class.forName("org.h2.Driver");
            } else if (database.toLowerCase().equals("sqlite")) {
                Class.forName("org.sqlite.JDBC");
                return DriverManager.getConnection(strCon);
            }
            return DriverManager.getConnection(strCon);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver não encontrado!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
