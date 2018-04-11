package ldavip.ormbasico.query;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Luis Davi
 */
@Deprecated
public class Where {
    
    private List<Expressao> expressoes;
    private List<Class<?>> classes;
    
    public Where() {
        this.expressoes = new ArrayList<>();
        this.classes = new ArrayList<>();
    }
    
    public Where add(Expressao expressao) {
        this.expressoes.add(expressao);
        if (!this.classes.contains(expressao.getClasse())) {
            this.classes.add(expressao.getClasse());
        }
        return this;
    }

    public String getClausulasWhere() {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n ").append(" WHERE 1 = 1 ");
        for (Expressao exp : expressoes) {
            sb.append(exp.getExpressao());
        }
        return sb.toString();
    }

    public List<Class<?>> getClasses() {
        return classes;
    }
}
