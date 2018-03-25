package ldavip.ormbasico.query;

/**
 *
 * @author Luis Davi
 */
public enum Operador {
    ENTRE, CONTEM, NAO_CONTEM, SIMILAR,
    IGUAL, MAIOR_QUE, MENOR_QUE, MAIOR_OU_IGUAL_QUE, MENOR_OU_IGUAL_QUE, DIFERENTE;

    @Override
    public String toString() {
        switch (ordinal()) {
            case 0:
                return "BETWEEN";
            case 1:
                return "IN";
            case 2:
                return "NOT IN";
            case 3:
                return "LIKE";
            case 4:
                return "=";
            case 5:
                return ">";
            case 6:
                return "<";
            case 7:
                return ">=";
            case 8:
                return "<=";
            case 9:
                return "<>";
            default:
                return null;

        }
    }
}
