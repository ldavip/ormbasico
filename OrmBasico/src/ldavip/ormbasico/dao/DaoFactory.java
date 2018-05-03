package ldavip.ormbasico.dao;

import ldavip.ormbasico.util.Config;

/**
 *
 * @author Luis Davi
 */
public class DaoFactory {
    
    public static Class<?> getClasseDao(Class<?> classe) {
        String pacote = Config.getProperty("pacote.dao");
        String nomeClasse = classe.getName().substring(classe.getName().lastIndexOf(".") + 1);
        String dao = pacote + "." + nomeClasse + "Dao";
        try {
            return Class.forName(dao);
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Dao: [" + dao + "] não encontrado!");
    }
}
