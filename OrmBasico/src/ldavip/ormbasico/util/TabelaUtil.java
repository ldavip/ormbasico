package ldavip.ormbasico.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import ldavip.ormbasico.annotation.AutoIncrement;
import ldavip.ormbasico.annotation.CampoEnum;
import ldavip.ormbasico.annotation.Coluna;
import ldavip.ormbasico.annotation.ForeignKey;
import ldavip.ormbasico.annotation.Ignore;
import ldavip.ormbasico.annotation.NotNull;
import ldavip.ormbasico.annotation.PrimaryKey;
import ldavip.ormbasico.annotation.Tabela;
import ldavip.ormbasico.dao.Dao;
import ldavip.ormbasico.dao.Dao.Operacao;
import ldavip.ormbasico.exception.NotNullException;
import static ldavip.ormbasico.util.TextoUtil.ajustaCamelCase;
import ldavip.ormbasico.annotation.Data;

/**
 *
 * @author Luis Davi
 */
public class TabelaUtil {

    public static String getNomeGetter(Field field) {
        StringBuilder getterName = new StringBuilder();
        if (field.getType().getName().toLowerCase().contains("boolean")) {
            getterName.append("is");
        } else {
            getterName.append("get");
        }
        getterName.append(ajustaCamelCase(field.getName()));
        return getterName.toString();
    }
    
    public static String getNomeGetter(Class classe) {
        StringBuilder getterName = new StringBuilder();
        if (classe.getName().toLowerCase().contains("boolean")) {
            getterName.append("is");
        } else {
            getterName.append("get");
        }
        getterName.append(ajustaCamelCase(classe.getName()));
        return getterName.toString();
    }

    public static String getNomeSetter(Field field) {
        return "set" + ajustaCamelCase(field.getName());
    }

    public static String getNomeSetter(Class<?> classe) {
        return "set" + ClasseUtil.getNomeClasse(classe);
    }

    public static String getNomeColuna(Class<?> classe, String atributo) {
        try {
            if (classe.isAnnotationPresent(Tabela.class)) {
                Field field = classe.getDeclaredField(atributo);
                if (field.isAnnotationPresent(Coluna.class)) {
                    Coluna coluna = field.getAnnotation(Coluna.class);
                    if (coluna.nome() == null || coluna.nome().isEmpty()) {
                        return field.getName();
                    } else {
                        return coluna.nome();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Não encontrado atributo: [" + atributo + "] na classe: [" + classe.getName() + "]");
    }

    public static String getCampoIdFk(Class<?> classePrincipal, Class<?> classeFk) {
        if (classePrincipal.isAnnotationPresent(Tabela.class) && classeFk.isAnnotationPresent(Tabela.class)) {
            for (Field field : classePrincipal.getDeclaredFields()) {
                if (field.isAnnotationPresent(ForeignKey.class) && field.getType() == classeFk) {
                    if (field.isAnnotationPresent(Coluna.class)) {
                        String campoId;
                        Coluna coluna = field.getAnnotation(Coluna.class);
                        if (coluna.nome() == null || coluna.nome().isEmpty()) {
                            campoId = field.getName();
                        } else {
                            campoId = coluna.nome();
                        }
                        return campoId;
                    }
                }
            }
        }
        return null;
//        throw new RuntimeException("Não foi encontrado um campo com chave estrangeira para: " + classeFk.getName() + " na entidade: " + classePrincipal.getName());
    }

    public static String getNomeGetterId(Object obj) {
        Field campoId = getCampoId(obj.getClass());
        String nomeCampo = campoId.getName();
        return new StringBuilder("get")
                .append(TextoUtil.ajustaCamelCase(nomeCampo))
                .toString();
    }

    public static String getNomeTabela(Class<?> classeDaEntidade) {
        if (isTabela(classeDaEntidade)) {
            Tabela tabela = getTabela(classeDaEntidade);
            String nomeTabela;
            if (tabela.nome() == null || tabela.nome().isEmpty()) {
                nomeTabela = classeDaEntidade.getName();
            } else {
                nomeTabela = tabela.nome();
            }
            return nomeTabela;
        }
        throw new RuntimeException("A classe: " + classeDaEntidade.getName() + " não referencia uma entidade do banco!");
    }

    public static boolean isCampoNulo(Field campo, Object obj) throws Exception {
        Method getter = obj.getClass().getDeclaredMethod(getNomeGetter(campo));
        Object valor = getter.invoke(obj);
        if (valor != null && isForeignKey(campo)) {
            Field[] camposIdFk = getCamposId(valor.getClass());
            for (Field fieldFk : camposIdFk) {
                if (isCampoNulo(fieldFk, valor)) {
                    return true;
                }
            }
        }
        return valor == null;
    }

    public static String[] getNomeCamposInsert(Object obj, Dao.Operacao operacao) throws Exception {
        List<String> campos = new ArrayList<>();
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (isIgnore(field, operacao)) {
                continue;
            }
            if (isColuna(field)) {
                if (isAutoIncrement(field) && (operacao == Dao.Operacao.INSERT || operacao == Dao.Operacao.UPDATE)) {
                    continue;
                }
                if (isCampoNulo(field, obj)) {
                    if (isNotNull(field)) {
                        throw new NotNullException(field);
                    } else {
                        continue;
                    }
                } else {
                    campos.add(getNomeColuna(field));
                }
            }
        }
        return campos.toArray(new String[]{});
    }

    public static String[] getNomeCampos(Class<?> classe, Dao.Operacao operacao) {
        String nomeTabela = getNomeTabela(classe);
        List<String> campos = new ArrayList<>();
        for (Field field : classe.getDeclaredFields()) {
            if (isColuna(field)) {
                if (isIgnore(field, operacao)) {
                    continue;
                }
                if (isAutoIncrement(field) && (operacao == Dao.Operacao.INSERT || operacao == Dao.Operacao.UPDATE)) {
                    continue;
                }
                if (isPrimaryKey(field) && operacao == Dao.Operacao.UPDATE) {
                    continue;
                }
                campos.add(nomeTabela + "." + getNomeColuna(field));
            }
        }
        return campos.toArray(new String[]{});
    }

    public static String getNomeColuna(Field field) {
        if (!isColuna(field)) {
            return null;
        }
        Coluna coluna = getColuna(field);
        String nomeCampo;
        if (coluna.nome() == null || coluna.nome().isEmpty()) {
            nomeCampo = field.getName();
        } else {
            nomeCampo = coluna.nome();
        }
        return nomeCampo;
    }

    public static Field getCampoId(Class<?> classe) {
        for (Field field : classe.getDeclaredFields()) {
            if (isPrimaryKey(field)) {
                return field;
            }
        }
        throw new RuntimeException("Nenhum campo do tipo Primary Key encontrado na classe: [" + classe.getName() + "]");
    }
    
    public static Field[] getCamposId(Class<?> classe) {
        List<Field> campos = new ArrayList<>();
        for (Field field : classe.getDeclaredFields()) {
            if (isPrimaryKey(field)) {
                 campos.add(field);
            }
        }
        if (campos.isEmpty()) {
            throw new RuntimeException("Nenhum campo do tipo Primary Key encontrado na classe: [" + classe.getName() + "]");
        }
        return campos.toArray(new Field[]{});
    }

    public static String getNomeCampoId(Class<?> classeDaEntidade) {
        return getNomeColuna(getCampoId(classeDaEntidade));
    }
    
    public static String[] getNomesCamposId(Class<?> classeDaEntidade) {
        List<String> nomes = new ArrayList<>();
        Field[] camposId = getCamposId(classeDaEntidade);
        for (Field field : camposId) {
            nomes.add(getNomeColuna(field));
        }
        return nomes.toArray(new String[]{});
    }

    public static Coluna getColuna(Field field) {
        return field.getAnnotation(Coluna.class);
    }

    public static Tabela getTabela(Class<?> classe) {
        return classe.getAnnotation(Tabela.class);
    }
    
    public static Data getData(Class<?> classe) {
        return classe.getAnnotation(Data.class);
    }

    public static boolean isTabela(Class<?> classe) {
        return classe.isAnnotationPresent(Tabela.class);
    }

    public static boolean isColuna(Field field) {
        return field.isAnnotationPresent(Coluna.class);
    }

    public static boolean isPrimaryKey(Field field) {
        return field.isAnnotationPresent(PrimaryKey.class);
    }

    public static boolean isForeignKey(Field field) {
        return field.isAnnotationPresent(ForeignKey.class);
    }

    public static boolean isAutoIncrement(Field field) {
        return field.isAnnotationPresent(AutoIncrement.class);
    }

    public static boolean isNotNull(Field field) {
        return field.isAnnotationPresent(NotNull.class)
                || field.isAnnotationPresent(PrimaryKey.class);
    }
    
    public static boolean isData(Field field) {
        return field.isAnnotationPresent(Data.class);
    }

    public static void checaAtributo(String atributo, Class<?> classe) {
        try {
            classe.getDeclaredField(atributo);
        } catch (NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException("Atributo: " + atributo
                    + " não encontrado na classe: " + classe.getName());
        }
    }
    
    public static Field getCampoAutoIncrement(Class classe) throws Exception {
        for (Field field : classe.getDeclaredFields()) {
            if (isAutoIncrement(field)) {
                if (TabelaUtil.isPrimaryKey(field)) {
                    return field;
                } else {
                    throw new Exception("O campo: [" + field.getName() + "] é Auto Increment mas não é chave!");
                }
            }
        }
        return null;
    }
    
    public static boolean isIgnore(Field campo, Operacao operacao) {
        if (campo.isAnnotationPresent(Ignore.class)) {
            Ignore ignore = campo.getAnnotation(Ignore.class);
            for (Operacao op : ignore.operacao()) {
                if (op == operacao) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static CampoEnum getCampoEnum(Field field) {
        return field.getAnnotation(CampoEnum.class);
    }
}
