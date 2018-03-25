package ldavip.ormbasico.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ldavip.ormbasico.query.Criterios;
import ldavip.ormbasico.query.Operador;
import ldavip.ormbasico.query.Where;
import ldavip.ormbasico.util.TabelaUtil;
import static ldavip.ormbasico.util.TabelaUtil.checaAtributo;
import static ldavip.ormbasico.util.TabelaUtil.getCampoId;
import static ldavip.ormbasico.util.TabelaUtil.getCampoIdFk;
import static ldavip.ormbasico.util.TabelaUtil.getNomeCampoId;
import static ldavip.ormbasico.util.TabelaUtil.getNomeCampos;
import static ldavip.ormbasico.util.TabelaUtil.getNomeColuna;
import static ldavip.ormbasico.util.TabelaUtil.getNomeGetter;
import static ldavip.ormbasico.util.TabelaUtil.getNomeGetterId;
import static ldavip.ormbasico.util.TabelaUtil.getNomeSetter;
import static ldavip.ormbasico.util.TabelaUtil.getNomeTabela;
import static ldavip.ormbasico.util.TabelaUtil.isAutoIncrement;
import static ldavip.ormbasico.util.TabelaUtil.isCampoNulo;
import static ldavip.ormbasico.util.TabelaUtil.isColuna;
import static ldavip.ormbasico.util.TabelaUtil.isForeignKey;
import static ldavip.ormbasico.util.TabelaUtil.isNotNull;
import ldavip.ormbasico.util.TextoUtil;
import static ldavip.ormbasico.util.TextoUtil.ajustaCamelCase;

/**
 *
 * @author Luis Davi
 */
public abstract class Dao<T> {

    public enum Operacao {
        INSERT, SELECT, UPDATE, DELETE
    }

    private Connection conexao;
    private Operacao operacao;

    private final Class<?> classeDaEntidade;
    private List<Class<?>> classes = new ArrayList<>();

    private StringBuilder query;
    private StringBuilder join = new StringBuilder();
    private StringBuilder where = new StringBuilder("\r\n WHERE 1 = 1 ");
    private StringBuilder orderBy = new StringBuilder();
    
    private boolean queryIniciada = false;
    private boolean orderByInserido = false;

    private Dao() {
        this.classeDaEntidade = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    public Dao(Connection conexao) {
        this();
        this.conexao = conexao;
    }

    public void insere(T obj) throws Exception {
        this.operacao = Operacao.INSERT;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(obj, operacao);
        String[] parametros = new String[campos.length];
        Arrays.fill(parametros, "?");

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tabela);
        sql.append(" (");
        sql.append(TextoUtil.agrupar(campos, ","));
        sql.append(") ");
        sql.append("VALUES ");
        sql.append("(");
        sql.append(TextoUtil.agrupar(parametros, ","));
        sql.append(")");

        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            this.conexao.setAutoCommit(false);
            setParameters(pst, obj);
            pst.executeUpdate();
            this.conexao.commit();
        } catch (Exception e) {
            this.conexao.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public void altera(T obj) throws Exception {
        this.operacao = Operacao.UPDATE;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(obj.getClass(), operacao);
        String[] parametros = new String[campos.length];
        Arrays.fill(parametros, "?");

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tabela);
        sql.append(" SET ");
        for (int i = 0; i < campos.length; i++) {
            sql.append(campos[i]).append(" = ? ");
            if (i < campos.length - 1) {
                sql.append(",");
            }
        }
        sql.append(" WHERE ").append(getNomeCampoId(classeDaEntidade)).append(" = ? ");

        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            this.conexao.setAutoCommit(false);
            setParameters(pst, obj);
            pst.executeUpdate();
            this.conexao.commit();
        } catch (Exception e) {
            this.conexao.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    public Dao buscaLista() {
        this.operacao = Operacao.SELECT;
        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        this.query = new StringBuilder();
        this.query.append("SELECT ");
        this.query.append(TextoUtil.agrupar(campos, ", "));
        this.query.append(" FROM ").append(tabela).append(" ");

        queryIniciada = true;
        return this;
    }

    public Dao where(Class<?> classe, String atributo, Operador operador, Object... valor) {
        return and(classe, atributo, operador, valor);
    }

    public Dao and(Class<?> classe, String atributo, Operador operador, Object... valor) {
        add("AND", classe, atributo, operador, valor);
        return this;
    }

    public Dao or(Class<?> classe, String atributo, Operador operador, Object... valor) {
        add("OR", classe, atributo, operador, valor);
        return this;
    }

    private void add(String op, Class<?> classe, String atributo, Operador operador, Object... valor) {
        if (orderByInserido) {
            throw new IllegalStateException("Após order by não deve ser adicionado cláusulas where!");
        }
        if (classe != classeDaEntidade && !classes.contains(classe)) {
            classes.add(classe);
        }
        
        String nomeTabela = getNomeTabela(classe);
        String nomeColuna = getNomeColuna(classe, atributo);
        
        where.append("\r\n ").append(op).append(" ");
        where.append(nomeTabela).append(".").append(nomeColuna).append(" ");
        if (null != operador) {
            switch (operador) {
                case ENTRE:
                    if (valor.length != 2) {
                        throw new IllegalArgumentException("A quantidade de valores esperada era: 2!"
                                + "\nQuantidade encontrada: " + valor.length);
                    }
                    where.append(operador).append(" ")
                            .append("'").append(valor[0]).append("'")
                            .append(" AND ")
                            .append("'").append(valor[1]).append("' ");
                    break;
                case CONTEM:
                case NAO_CONTEM:
                    String[] str = new String[valor.length];
                    for (int i = 0; i < valor.length; i++) {
                        str[i] = "'" + valor[i] + "'";
                    }
                    where.append(operador).append(" ")
                            .append("(")
                            .append(TextoUtil.agrupar(str, ","))
                            .append(") ");
                    break;
                case SIMILAR:
                    if (valor.length != 1) {
                        throw new IllegalArgumentException("A quantidade de valores esperada era: 1!"
                                + "\nQuantidade encontrada: " + valor.length);
                    }
                    where.append(operador).append(" ")
                            .append("'%")
                            .append(valor[0])
                            .append("%' ");
                    break;
                default:
                    if (valor.length != 1) {
                        throw new IllegalArgumentException("A quantidade de valores esperada era: 1!"
                                + "\nQuantidade encontrada: " + valor.length);
                    }
                    where.append(operador)
                            .append(" '")
                            .append(valor[0])
                            .append("' ");
                    break;
            }
        }
    }
    
    public Dao orderBy(Class<?> classe, String atributo) {
        checaAtributo(atributo, classe);
        String nomeColuna = "";
        try {
            Field field = classe.getDeclaredField(atributo);
            nomeColuna = getNomeColuna(field);
        } catch (NoSuchFieldException | SecurityException ex) {
            // já verificado -> checaAtributo
        }
        String nomeTabela = getNomeTabela(classe);
        
        if (orderBy.toString().isEmpty()) {
            orderBy.append("\r\n ").append("ORDER BY ").append("\r\n ")
                    .append(nomeTabela).append(".").append(nomeColuna);
        } else {
            orderBy.append("\r\n , ")
                    .append(nomeTabela).append(".").append(nomeColuna);
        }
        
        orderByInserido = true;
        return this;
    }

    public List<T> toList() throws Exception {
        if (!queryIniciada) {
            throw new IllegalStateException("Busca incompleta!");
        }
        
        checkJoins();
        this.query.append(join);
        this.query.append(where);
        this.query.append(orderBy);
        
        return buscaLista(this.query.toString());
    }

    private void checkJoins() {
        if (classes.size() > 0) {
            String nomeTabela = getNomeTabela(classeDaEntidade);
            for (Class<?> classe : classes) {
                String campoFk = getCampoIdFk(classeDaEntidade, classe);

                String nomeTabelaFk = getNomeTabela(classe);
                String campoIdFk = getNomeCampoId(classe);

                join.append("\r\n ").append("LEFT OUTER JOIN ").append(nomeTabelaFk);
                join.append("\r\n ").append("   ON ")
                        .append(nomeTabelaFk).append(".").append(campoIdFk)
                        .append(" = ")
                        .append(nomeTabela).append(".").append(campoFk);

            }
        }
    }

    /**
     * 
     * @param criterios
     * @return
     * @throws Exception
     * @deprecated Use o método <code>buscaLista().where(...).toList()</code> ao invés deste.
     */
    @Deprecated
    public List<T> buscaListaPorCriterios(Criterios criterios) throws Exception {
        if (criterios == null) {
            throw new Exception("Critérios nulo!");
        }

        this.operacao = Operacao.SELECT;
        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(TextoUtil.agrupar(campos, ","));
        sql.append(" FROM ").append(tabela);
        if (criterios.getJoin() != null) {
            sql.append(criterios.getJoin().getJoins());
        }
        if (criterios.getWhere() != null) {
            sql.append(criterios.getWhere().getClausulasWhere());
        }

        return buscaLista(sql.toString());
    }

    /**
     * 
     * @param where
     * @return
     * @throws Exception
     * @deprecated Use o método <code>buscaLista().where(...).toList()</code> ao invés deste.
     */
    @Deprecated
    public List<T> buscaLista(Where where) throws Exception {
        this.operacao = Operacao.SELECT;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(TextoUtil.agrupar(campos, ","));
        sql.append(" FROM ").append(tabela);
        sql.append(where.getClausulasWhere());

        return buscaLista(sql.toString());
    }

    private List<T> buscaLista(String sql) throws Exception {
        List<T> lista = new ArrayList<>();
        ResultSet rs = null;
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            rs = pst.executeQuery();
            while (rs.next()) {
                lista.add(populaObjeto(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        return lista;
    }

    public T buscaPorId(int id) throws Exception {
        this.operacao = Operacao.SELECT;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(TextoUtil.agrupar(campos, ","));
        sql.append(" FROM ").append(tabela);
        sql.append(" WHERE ").append(getNomeCampoId(classeDaEntidade)).append(" = ? ");

        ResultSet rs = null;
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            pst.setInt(1, id);
            rs = pst.executeQuery();
            if (rs.next()) {
                return populaObjeto(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        return null;
    }

    /**
     * 
     * @return
     * @throws Exception
     * @deprecated Use o método <code>buscaLista().toList()</code> ao invés deste.
     */
    @Deprecated
    public List<T> buscaTodos() throws Exception {
        this.operacao = Operacao.SELECT;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(TextoUtil.agrupar(campos, ","));
        sql.append(" FROM ").append(tabela);

        return buscaLista(sql.toString());
    }

    public void remove(T obj) throws Exception {
        this.operacao = Operacao.DELETE;

        String tabela = getNomeTabela(classeDaEntidade);

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tabela);
        sql.append(" WHERE ").append(getNomeCampoId(classeDaEntidade)).append(" = ? ");

        try {
            this.conexao.setAutoCommit(false);
            PreparedStatement pst = this.conexao.prepareStatement(sql.toString());

            Field campoId = getCampoId(obj.getClass());
            Class<?> classeId = ajustaTipoClasse(campoId.getType());
            Method getter = classeDaEntidade.getDeclaredMethod(getNomeGetter(campoId));
            Class[] parametrosSetter = new Class[]{Integer.TYPE, classeId};
            Method method = PreparedStatement.class.getDeclaredMethod(getNomeSetter(classeId), parametrosSetter);
            method.invoke(pst, 1, getter.invoke(obj));

            pst.executeUpdate();
            this.conexao.commit();
        } catch (Exception e) {
            this.conexao.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    private void setParameters(PreparedStatement pst, T obj) throws Exception {
        Class<?> objClass = obj.getClass();
        int cont = 0;
        for (Field field : objClass.getDeclaredFields()) {
            if (isColuna(field)) {
                if (isAutoIncrement(field) && (operacao == Operacao.INSERT || operacao == Operacao.UPDATE)) {
                    continue;
                }
                if (isCampoNulo(field, obj)) {
                    if (isNotNull(field)) {
                        throw new Exception("Campo nulo: [" + field.getName() + "]");
                    } else {
                        continue;
                    }
                }

                Object fkObj = null;
                Class<?> fieldClass = field.getType();
                Method fieldGetter = objClass.getDeclaredMethod(getNomeGetter(field));

                if (isForeignKey(field)) {
                    fkObj = fieldGetter.invoke(obj);
                    Method getFkId = fkObj.getClass().getDeclaredMethod(getNomeGetterId(fkObj));
                    fieldClass = getFkId.invoke(fkObj).getClass();
                    fieldGetter = getFkId;
                }

                fieldClass = ajustaTipoClasse(fieldClass);
                Class[] parametrosSetter = new Class[]{Integer.TYPE, fieldClass};

                String nomeClasse = ajustaCamelCase(fieldClass.getName());
                if (nomeClasse.contains(".")) {
                    nomeClasse = nomeClasse.substring(nomeClasse.lastIndexOf(".") + 1);
                }
                if (nomeClasse.equals("Integer")) {
                    nomeClasse = "Int";
                }
                String setterName = "set" + nomeClasse;
                Method method = PreparedStatement.class.getDeclaredMethod(setterName, parametrosSetter);

                if (isForeignKey(field)) {
                    method.invoke(pst, ++cont, fieldGetter.invoke(fkObj));
                } else {
                    if (fieldClass == java.sql.Date.class) {
                        Method getTime = java.util.Date.class.getDeclaredMethod("getTime");
                        method.invoke(pst, ++cont, new java.sql.Date((long) getTime.invoke(fieldGetter.invoke(obj))));
                    } else {
                        method.invoke(pst, ++cont, fieldGetter.invoke(obj));
                    }
                }
            }
        }

        if (operacao == Operacao.UPDATE) {
            Field campoId = getCampoId(obj.getClass());
            Class<?> classeId = campoId.getType();

            classeId = ajustaTipoClasse(classeId);

            Method getter = objClass.getDeclaredMethod(getNomeGetter(campoId));
            Class[] parametrosSetter = new Class[]{Integer.TYPE, classeId};
            Method pstSetter = PreparedStatement.class.getDeclaredMethod(getNomeSetter(classeId), parametrosSetter);
            pstSetter.invoke(pst, ++cont, getter.invoke(obj));
        }
    }

    private T populaObjeto(ResultSet rs) throws Exception {
        T obj = (T) classeDaEntidade.newInstance();

        for (Field campo : classeDaEntidade.getDeclaredFields()) {
            if (isColuna(campo)) {
                Object objFk = null;

                String nomeColuna = getNomeColuna(campo);
                Class<?> classeCampo = campo.getType();
                Class[] parametrosGetter = new Class[]{String.class};

                if (isForeignKey(campo)) {
                    objFk = classeCampo.newInstance();
                    classeCampo = getCampoId(classeCampo).getType();
                }

                String nomeClasse = ajustaCamelCase(classeCampo.getName());
                if (nomeClasse.contains(".")) {
                    nomeClasse = nomeClasse.substring(nomeClasse.lastIndexOf(".") + 1);
                }
                if (nomeClasse.equals("Integer")) {
                    nomeClasse = "Int";
                }

                String getterName = "get" + nomeClasse;
                Method getter = ResultSet.class.getDeclaredMethod(getterName, parametrosGetter);

                if (isForeignKey(campo)) {
                    Class<?> classeDao = DaoFactory.getClasseDao(objFk.getClass());
                    Class[] parametrosConstrutor = new Class[]{Connection.class};
                    Constructor construtorDao = classeDao.getDeclaredConstructor(parametrosConstrutor);
                    Dao dao = (Dao) construtorDao.newInstance(this.conexao);

                    objFk = dao.buscaPorId((int) getter.invoke(rs, nomeColuna));
                    classeCampo = objFk.getClass();
                }

                Class[] parametrosSetter = new Class[]{classeCampo};
                Method setter = classeDaEntidade.getDeclaredMethod(getNomeSetter(campo), parametrosSetter);

                if (isForeignKey(campo)) {
                    setter.invoke(obj, objFk);
                } else {
                    setter.invoke(obj, getter.invoke(rs, nomeColuna));
                }
            }
        }

        return obj;
    }

    private Class<?> ajustaTipoClasse(Class<?> fieldClass) {
        if (fieldClass == java.lang.Integer.class) {
            fieldClass = Integer.TYPE;
        } else if (fieldClass == java.lang.Long.class) {
            fieldClass = Long.TYPE;
        } else if (fieldClass == java.lang.Double.class) {
            fieldClass = Double.TYPE;
        } else if (fieldClass == java.lang.Boolean.class) {
            fieldClass = Boolean.TYPE;
        } else if (fieldClass == java.util.Date.class) {
            fieldClass = java.sql.Date.class;
        }
        return fieldClass;
    }

}
