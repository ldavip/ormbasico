package ldavip.ormbasico.query;

/**
 *
 * @author Luis Davi
 */
public class Criterios {

    private Where where;
    private Join join;

    public Where getWhere() {
        return where;
    }

    public void setWhere(Where where) {
        this.where = where;
    }

    public Join getJoin() {
        return join;
    }

    public void setJoin(Join join) {
        this.join = join;
    }
}
