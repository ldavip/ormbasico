package ldavip.ormbasico.query;

/**
 *
 * @author Luis Davi
 */
public class Criterio {

    private String criterio;
    private Class<?> classe;

    public Criterio(Class<?> classe, String criterio) {
        this.criterio = criterio;
        this.classe = classe;
    }

    public String getCriterio() {
        return criterio;
    }

    public Class<?> getClasse() {
        return classe;
    }
}
