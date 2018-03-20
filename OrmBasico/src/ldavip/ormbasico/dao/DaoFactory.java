package ldavip.ormbasico.dao;

import ldavip.ormbasico.util.LeitorXML;

/**
 *
 * @author Luis Davi
 */
public class DaoFactory {
    
    public static Class<?> getClasseDao(Class<?> classe) {
        String pacote = LeitorXML.getValor("pacote-dao", "ormbasico-config.xml");
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
