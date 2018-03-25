package ldavip.ormbasico.query;

import java.util.ArrayList;
import java.util.List;
import ldavip.ormbasico.annotation.Tabela;
import ldavip.ormbasico.util.TabelaUtil;

/**
 *
 * @author Luis Davi
 */
@Deprecated
public class Join {
    
    private enum Tipo {
        INNER_JOIN, 
        LEFT_JOIN, 
        RIGHT_JOIN, 
        LEFT_OUTER_JOIN, 
        RIGHT_OUTER_JOIN;
        
        @Override        
        public String toString() {
            switch (ordinal()) {
                case 0:
                    return " INNER JOIN ";
                case 1:
                    return " LEFT JOIN ";
                case 2:
                    return " RIGHT JOIN ";
                case 3:
                    return " LEFT OUTER JOIN ";
                case 4:
                    return " RIGHT OUTER JOIN ";
                default:
                    return null;
            }
        }
    }
    
    private List<String> joins = new ArrayList<>();
    protected final Class<?> entidade;
    
    private Join(Class<?> entidade) {
        this.entidade = entidade;
    }
    
    public static Join createJoin(Class<?> entidade) {
        return new Join(entidade);
    }
    
    public void innerJoin(Class<?> tabelaSecundaria) {
        adiciona(Tipo.INNER_JOIN, tabelaSecundaria);
    }
    
    public void leftJoin(Class<?> tabelaSecundaria) {
        adiciona(Tipo.LEFT_JOIN, tabelaSecundaria);
    }
    
    public void rightJoin(Class<?> tabelaSecundaria) {
        adiciona(Tipo.RIGHT_JOIN, tabelaSecundaria);
    }
    
    public void leftOuterJoin(Class<?> tabelaSecundaria) {
        adiciona(Tipo.LEFT_OUTER_JOIN, tabelaSecundaria);
    }
    
    public void rightOuterJoin(Class<?> tabelaSecundaria) {
        adiciona(Tipo.RIGHT_OUTER_JOIN, tabelaSecundaria);
    }
    
    private void adiciona(Tipo tipo, Class<?> tabelaSecundaria) {
        if (entidade.isAnnotationPresent(Tabela.class) && tabelaSecundaria.isAnnotationPresent(Tabela.class)) {
            
            String nomeTabelaPrincipal = TabelaUtil.getNomeTabela(entidade);
            String nomeTabelaSecundaria = TabelaUtil.getNomeTabela(tabelaSecundaria);
            
            String campoIdFk = TabelaUtil.getCampoIdFk(entidade, tabelaSecundaria);
            String campoIdSecundario = TabelaUtil.getNomeCampoId(tabelaSecundaria);
            
            StringBuilder join = new StringBuilder();
            join.append("\r\n ");
            join.append(tipo.toString()).append(nomeTabelaSecundaria);
            join.append(" ON ");
            join.append(nomeTabelaSecundaria).append(".").append(campoIdSecundario);
            join.append(" = ");
            join.append(nomeTabelaPrincipal).append(".").append(campoIdFk);
            
            this.joins.add(join.toString());
        } else {
            throw new RuntimeException("As entidades não relacionam entidades do banco!");
        }
    }

    public String getJoins() {
        StringBuilder sb = new StringBuilder();
        for (String join : joins) {
            sb.append(join);
        }
        return sb.toString();
    }
}
