package ldavip.ormbasico.query;

import java.util.ArrayList;
import java.util.List;
import ldavip.ormbasico.util.TabelaUtil;

/**
 *
 * @author Luis Davi
 */
@Deprecated
public class OrderBy {

    private List<String> campos;
    private List<Class<?>> classes;

    public OrderBy() {
        this.campos = new ArrayList<>();
        this.classes = new ArrayList<>();
    }

    public void add(Class<?> classe, String atributo) {
        String nomeTabela = TabelaUtil.getNomeTabela(classe);
        String nomeColuna = TabelaUtil.getNomeColuna(classe, atributo);

        campos.add(nomeTabela + "." + nomeColuna);
        if (!classes.contains(classe)) {
            classes.add(classe);
        }
    }

    public String getOrdem() {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n ").append(" ORDER BY ");
        for (String campo : campos) {
            sb.append(campo).append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }

    public List<Class<?>> getClasses() {
        return classes;
    }
}
