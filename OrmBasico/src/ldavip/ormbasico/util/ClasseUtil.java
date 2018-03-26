package ldavip.ormbasico.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Luis Davi
 */
public abstract class ClasseUtil {
    
    public static Class<?> buscaClassePrincipal(String atributo) {
        try {
            String[] caminhos = atributo.split(Pattern.quote("."));
            if (caminhos.length < 2) {
                throw new IllegalArgumentException("Erro de sintaxe no atributo: " + atributo + "! - Esperado: NomeDaClasse.nomeDoAtributo!");
            }
            String pacoteModel = LeitorXML.getValor("pacote-model", "ormbasico-config.xml");
            if (pacoteModel == null || pacoteModel.isEmpty()) {
                throw new IllegalStateException("Pacote do Model não encontrado!");
            }

            String nomeClassePrincipal = caminhos[0].substring(0, 1).toUpperCase() + caminhos[0].substring(1);
            Class classePrincipal = Class.forName(pacoteModel + "." + nomeClassePrincipal);
            return classePrincipal;
        } catch (ClassNotFoundException | IllegalArgumentException | IllegalStateException e) {
            throw new IllegalArgumentException("Classe do objeto não encontrada!");
        }
    }
    
    public static Class<?> buscaClasseDoAtributo(String atributo) {
        try {
            String[] caminhos = atributo.split(Pattern.quote("."));
            if (caminhos.length < 2) {
                throw new IllegalArgumentException("Erro de sintaxe no atributo: " + atributo + "! - Esperado: NomeDaClasse.nomeDoAtributo!");
            }
            String pacoteModel = LeitorXML.getValor("pacote-model", "ormbasico-config.xml");
            if (pacoteModel == null || pacoteModel.isEmpty()) {
                throw new IllegalStateException("Pacote do Model não encontrado!");
            }

            String nomeClassePrincipal = caminhos[0].substring(0, 1).toUpperCase() + caminhos[0].substring(1);
            Class classePrincipal = Class.forName(pacoteModel + "." + nomeClassePrincipal);

            Class classeFk = classePrincipal;
            for (int i = 1; i < caminhos.length - 1; i++) {
                Field field = classeFk.getDeclaredField(caminhos[i]);

                if (field != null && TabelaUtil.isForeignKey(field)) {
                    classeFk = field.getType();

                } else {
                    throw new IllegalArgumentException("O atributo: " + caminhos[1] + " não é um campo anotado com @ForeignKey!");
                }
            }

            return classeFk;
            
        } catch (ClassNotFoundException | IllegalArgumentException | IllegalStateException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException("Classe do objeto não encontrada!");
        }
    }
    
    public static Class[] buscaClassesFk(String atributo) {
        try {
            String[] caminhos = atributo.split(Pattern.quote("."));
            if (caminhos.length < 2) {
                throw new IllegalArgumentException("Erro de sintaxe no atributo: " + atributo + "! - Esperado: NomeDaClasse.nomeDoAtributo!");
            }
            String pacoteModel = LeitorXML.getValor("pacote-model", "ormbasico-config.xml");
            if (pacoteModel == null || pacoteModel.isEmpty()) {
                throw new IllegalStateException("Pacote do Model não encontrado!");
            }

            List<Class> classesFk = new ArrayList<>();
            String nomeClassePrincipal = caminhos[0].substring(0, 1).toUpperCase() + caminhos[0].substring(1);
            Class classePrincipal = Class.forName(pacoteModel + "." + nomeClassePrincipal);

            Class classeFk = classePrincipal;
            for (int i = 1; i < caminhos.length - 1; i++) {
                Field field = classeFk.getDeclaredField(caminhos[i]);

                if (field != null && TabelaUtil.isForeignKey(field)) {
                    classeFk = field.getType();
                    classesFk.add(classeFk);

                } else {
                    throw new IllegalArgumentException("O atributo: " + caminhos[1] + " não é um campo anotado com @ForeignKey!");
                }
            }

            return classesFk.toArray(new Class[]{});
            
        } catch (ClassNotFoundException | IllegalArgumentException | IllegalStateException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException("Classe do objeto não encontrada!");
        }
    }
}
