package ldavip.ormbasico.query;

import ldavip.ormbasico.util.TabelaUtil;

/**
 *
 * @author Luis Davi
 */
@Deprecated
public class Expressao {
    
    private String expressao;
    private Class<?> classe;
    
    private Expressao(Class<?> classe, String expressao) {
        this.expressao = expressao;
        this.classe = classe;
    }

    public String getExpressao() {
        return expressao;
    }

    public Class<?> getClasse() {
        return classe;
    }
    
    public static Expressao e(Criterio criterio) {
        StringBuilder exp = new StringBuilder();
        exp.append("\r\n ");
        exp.append(" AND ");
        exp.append(criterio.getCriterio());
        exp.append(" ");
        return new Expressao(criterio.getClasse(), exp.toString());
    }
    
    public static Expressao ou(Criterio criterio) {
        StringBuilder exp = new StringBuilder();
        exp.append(" OR ");
        exp.append(criterio.getCriterio());
        exp.append(" ");
        return new Expressao(criterio.getClasse(), exp.toString());
    }
    
    private static Criterio expressaoSimples(Class<?> classe, String atributo, Object valor, String expressao) {
        StringBuilder criterio = new StringBuilder();
        String nomeTabela = TabelaUtil.getNomeTabela(classe);
        criterio.append(nomeTabela).append(".").append(TabelaUtil.getNomeColuna(classe, atributo));
        criterio.append(" ").append(expressao).append(" ");
        
        if (valor != null) {
            String strValor = getStrValor(valor);
            criterio.append(getValor(valor.getClass(), strValor, false));
            criterio.append(" ");
        }
        return new Criterio(classe, criterio.toString());
    }

    public static Criterio igual(Class<?> classe, String atributo, Object valor) {
        return expressaoSimples(classe, atributo, valor, "=");
    }
    
    public static Criterio maiorQue(Class<?> classe, String atributo, Object valor) {
        return expressaoSimples(classe, atributo, valor, ">");
    }

    public static Criterio menorQue(Class<?> classe, String atributo, Object valor) {
        return expressaoSimples(classe, atributo, valor, "<");
    }

    public static Criterio maiorOuIgualQue(Class<?> classe, String atributo, Object valor) {
        return expressaoSimples(classe, atributo, valor, ">=");
    }

    public static Criterio menorOuIgualQue(Class<?> classe, String atributo, Object valor) {
        return expressaoSimples(classe, atributo, valor, "<=");
    }

    public static Criterio diferente(Class<?> classe, String atributo, Object valor) {
        return expressaoSimples(classe, atributo, valor, "<>");
    }

    public static Criterio nulo(Class<?> classe, String atributo) {
        return expressaoSimples(classe, atributo, null, "IS NULL");
    }

    public static Criterio naoNulo(Class<?> classe, String atributo) {
        return expressaoSimples(classe, atributo, null, "IS NOT NULL");
    }
    
    public static Criterio like(Class<?> classe, String atributo, Object valor) {
        StringBuilder criterio = new StringBuilder();
        String nomeTabela = TabelaUtil.getNomeTabela(classe);
        criterio.append(nomeTabela).append(".").append(TabelaUtil.getNomeColuna(classe, atributo));
        criterio.append(" LIKE ");
        String strValor = getStrValor(valor);
        criterio.append(getValor(valor.getClass(), strValor, true));
        criterio.append(" ");
        return new Criterio(classe, criterio.toString());
    }
    
    public static Criterio entre(Class<?> classe, String atributo, Object valor1, Object valor2) {
        StringBuilder criterio = new StringBuilder();
        String nomeTabela = TabelaUtil.getNomeTabela(classe);
        criterio.append(nomeTabela).append(".").append(TabelaUtil.getNomeColuna(classe, atributo));
        criterio.append(" BETWEEN ");
        String strValor1 = getStrValor(valor1);
        String strValor2 = getStrValor(valor2);
        criterio.append(getValor(valor1.getClass(), strValor1, false));
        criterio.append(" AND ");
        criterio.append(getValor(valor2.getClass(), strValor2, false));
        criterio.append(" ");
        return new Criterio(classe, criterio.toString());
    }
    
    private static String getStrValor(Object valor) {
        String strValor;
        if (valor.getClass() == java.util.Date.class) {
            strValor = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss S").format(valor);
        } else {
            strValor = String.valueOf(valor);
        }
        return strValor;
    }
    
    private static String getValor(Class<?> objClass, String strValor, boolean like) {
        StringBuilder valor = new StringBuilder();
        if (like) {
            valor.append("'%").append(strValor).append("%'");
        } else {
            if (objClass == java.lang.String.class || objClass == java.util.Date.class) {
                valor.append("'").append(strValor).append("'");
            } else {
                valor.append(strValor);
            }
        }
        return valor.toString();
    }
}
