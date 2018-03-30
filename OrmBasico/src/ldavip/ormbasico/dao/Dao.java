package ldavip.ormbasico.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ldavip.ormbasico.query.Operador;
import ldavip.ormbasico.util.ClasseUtil;
import ldavip.ormbasico.util.TabelaUtil;
import static ldavip.ormbasico.util.TabelaUtil.getCampoId;
import static ldavip.ormbasico.util.TabelaUtil.getCampoIdFk;
import static ldavip.ormbasico.util.TabelaUtil.getNomeCampoId;
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
import static ldavip.ormbasico.util.TabelaUtil.getNomeCamposInsert;
import static ldavip.ormbasico.util.TabelaUtil.getNomeCampos;

/**
 *
 * @author Luis Davi
 */
public abstract class Dao<T> {

    public enum Operacao {
        INSERT, SELECT, UPDATE, DELETE
    }
    
    public enum Ordem {
        CRESCENTE, DECRESCENTE;

        @Override
        public String toString() {
            switch (ordinal()) {
                case 0:
                    return "ASC";
                case 1:
                    return "DESC";
                default:
                    return null;
            }
        }
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
    private boolean direcaoOrderByInserido = false;

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
        String[] campos = getNomeCamposInsert(obj, operacao);
        String[] parametros = new String[campos.length];
        Arrays.fill(parametros, "?");

        String sql = new StringBuilder()
                .append("INSERT INTO ").append(tabela)
                .append(" (")
                .append(TextoUtil.agrupar(campos, ", "))
                .append(") ")
                .append("\r\n ")
                .append("VALUES ")
                .append("(")
                .append(TextoUtil.agrupar(parametros, ", "))
                .append(")")
                .toString();

        try (PreparedStatement pst = this.conexao.prepareStatement(sql)) {
            this.conexao.setAutoCommit(false);
            setParameters(pst, obj);
            pst.executeUpdate();
            
            setIdObjeto(obj);
            
            this.conexao.commit();
        } catch (Exception e) {
            this.conexao.rollback();
            e.printStackTrace();
            throw e;
        }
    }
    
    private void setIdObjeto(T obj) throws Exception {
        Field campoAutoIncrement = TabelaUtil.getCampoAutoIncrement(obj.getClass());
        
        if (campoAutoIncrement == null) {
            return;
        }
        
        String nomeCampoAutoIncrement = getNomeColuna(campoAutoIncrement);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(nomeCampoAutoIncrement)
                .append(" FROM ").append(getNomeTabela(classeDaEntidade))
                .append(" ORDER BY ").append(nomeCampoAutoIncrement).append(" DESC ")
                .append(" LIMIT 1 ");
        
        ResultSet rs = null;
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            
            rs = pst.executeQuery();
            Object id = null;
            Class tipoCampo = ajustaTipoClasse(campoAutoIncrement.getType());
            Method getter = ResultSet.class.getDeclaredMethod(getNomeGetter(tipoCampo), new Class[]{tipoCampo});
            if (rs.next()) {
                id = getter.invoke(rs, nomeCampoAutoIncrement);
            }
            if (id == null) {
                throw new Exception("Não foi encontrado o ID do objeto inserido!");
            }
            
            String nomeSetter = getNomeSetter(campoAutoIncrement);
            Class[] parametros = new Class[] {campoAutoIncrement.getType()};
            Method setter = classeDaEntidade.getDeclaredMethod(nomeSetter, parametros);
            
            setter.invoke(obj, id);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (rs != null) {
                rs.close();
            }
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
            sql.append(campos[i]).append(" = ? ").append("\r\n ");
            if (i < campos.length - 1) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE ");
        String[] nomesCamposPk = TabelaUtil.getNomesCamposId(classeDaEntidade);
        for (int i = 0; i < nomesCamposPk.length; i++) {
            sql.append(nomesCamposPk[i]).append(" = ? ");
            if (i < nomesCamposPk.length - 1) {
                sql.append("\r\n ").append(" AND ");
            }
        }

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
    
    public void remove(T obj) throws Exception {
        this.operacao = Operacao.DELETE;

        String tabela = getNomeTabela(classeDaEntidade);

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(tabela);
        sql.append(" WHERE ");
        String[] nomesCamposPk = TabelaUtil.getNomesCamposId(classeDaEntidade);
        for (int i = 0; i < nomesCamposPk.length; i++) {
            sql.append(nomesCamposPk[i]).append(" = ? ");
            if (i < nomesCamposPk.length - 1) {
                sql.append("\r\n ").append(" AND ");
            }
        }

        try {
            this.conexao.setAutoCommit(false);
            PreparedStatement pst = this.conexao.prepareStatement(sql.toString());

            setParameters(pst, obj);

            pst.executeUpdate();
            this.conexao.commit();
        } catch (Exception e) {
            this.conexao.rollback();
            e.printStackTrace();
            throw e;
        }
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
    
    public T buscarUltimo() throws Exception {
        this.operacao = Operacao.SELECT;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(TextoUtil.agrupar(campos, ", "));
        sql.append(" FROM ").append(tabela);
        sql.append(" ORDER BY ").append(getNomeCampoId(classeDaEntidade)).append(" DESC ");
        sql.append(" LIMIT 1 ");

        ResultSet rs = null;
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
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

    public Dao buscaLista() {
        this.operacao = Operacao.SELECT;
        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        this.query = new StringBuilder();
        this.query.append("SELECT ");
        this.query.append(TextoUtil.agrupar(campos, ", "));
        this.query.append("\r\n").append(" FROM ").append(tabela).append(" ");

        queryIniciada = true;
        return this;
    }
    
    public Dao where(String atributo, Operador operador, Object... valor) {
        return and(atributo, operador, valor);
    }
    
    public Dao and(String atributo, Operador operador, Object... valor) {
        return and(getClasseDoAtributo(atributo), buscarNomeDoAtributo(atributo), operador, valor);
    }
    
    public Dao or(String atributo, Operador operador, Object... valor) {
        return or(getClasseDoAtributo(atributo), buscarNomeDoAtributo(atributo), operador, valor);
    }
    
    private Class getClasseDoAtributo(String atributo) {
        Class[] classesFk = ClasseUtil.buscaClassesFk(atributo);
        adicionarClassesFk(classesFk);

        Class classe;
        if (classesFk.length > 0) {
            classe = classesFk[classesFk.length - 1];
        } else {
            classe = classeDaEntidade;
        }
        return classe;
    }

    private String buscarNomeDoAtributo(String atributo) {
        int indexDoUltimoPonto = atributo.lastIndexOf(".");
        if (indexDoUltimoPonto != -1) {
            atributo = atributo.substring(indexDoUltimoPonto + 1);
        }
        return atributo;
    }

    private void adicionarClassesFk(Class[] classesFk) {
        for (Class clazz : classesFk) {
            if (!classes.contains(clazz)) {
                classes.add(clazz);
            }
        }
    }

    private Dao and(Class<?> classe, String atributo, Operador operador, Object... valor) {
        add("AND", classe, atributo, operador, valor);
        return this;
    }

    private Dao or(Class<?> classe, String atributo, Operador operador, Object... valor) {
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
    
    public Dao ordenarPor(String... atributos) {
        String campos = TextoUtil.agrupar(ClasseUtil.buscaCampos(atributos), ", ");
        
        if (orderBy.toString().isEmpty()) {
            orderBy.append("\r\n ").append("ORDER BY ").append(" ").append(campos);
        } else {
            orderBy.append(", ").append(campos);
        }
        
        Class[] classesFk = ClasseUtil.buscaClassesFk(atributos);
        adicionarClassesFk(classesFk);
        
        orderByInserido = true;
        return this;
    }
    
    public Dao crescente() {
        validarDirecao();
        adicionarDirecaoOrderBy("ASC");
        direcaoOrderByInserido = true;
        return this;
    }
    
    public Dao decrescente() {
        validarDirecao();
        adicionarDirecaoOrderBy("DESC");
        direcaoOrderByInserido = true;
        return this;
    }
    
    private void validarDirecao() {
        if (orderByInserido) {
            if (direcaoOrderByInserido) {
                throw new IllegalStateException("A direção da ordenação já foi especificada!");
            }
        } else {
            throw new IllegalStateException("Não foram especifidados atributos para a ordenação!");
        }
    }
    
    private void adicionarDirecaoOrderBy(String direcao) {
        orderBy.append(" ").append(direcao).append(" ");
    }

    public List<T> toList() throws Exception {
        if (!queryIniciada) {
            throw new Exception("Busca incompleta! [O método: buscaLista() não foi utilizado!]");
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
            String nomeTabelaAlternativa = "";
            String campoFkAlternativo = "";
            boolean campoAlternativo = false;
            
            for (Class<?> classe : classes) {
                String campoFk = getCampoIdFk(classeDaEntidade, classe);
                if (campoFk == null) {
                    for (Class<?> clazz : classes) {
                        if (getCampoIdFk(clazz, classe) != null) {
                            campoAlternativo = true;
                            nomeTabelaAlternativa = getNomeTabela(clazz);
                            campoFkAlternativo = getCampoIdFk(clazz, classe);
                            break;
                        }
                    }
                    if (!campoAlternativo) {
                        throw new IllegalArgumentException("Não foi encontrado o caminho do atributo informado!");
                    }
                }
                    
                String nomeTabelaFk = getNomeTabela(classe);
                String campoIdFk = getNomeCampoId(classe);

                join.append("\r\n ").append("LEFT OUTER JOIN ").append(nomeTabelaFk);
                join.append("\r\n ").append("   ON ")
                        .append(nomeTabelaFk).append(".").append(campoIdFk)
                        .append(" = ");
                if (campoAlternativo) {
                    join.append(nomeTabelaAlternativa).append(".").append(campoFkAlternativo);
                } else {
                    join.append(nomeTabela).append(".").append(campoFk);
                }

            }
        }
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

    private void setParameters(PreparedStatement pst, T obj) throws Exception {
        List<Field> camposPk = Arrays.asList(TabelaUtil.getCamposId(obj.getClass()));
        if (operacao == Operacao.DELETE) {
            int cont = 0;
            for (Field field : camposPk) {
                setParameter(field, obj, pst, ++cont);
            }
        }
        else {
            int cont = 0;
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (isColuna(field)) {
                    if (isAutoIncrement(field) && (operacao == Operacao.INSERT || operacao == Operacao.UPDATE)) {
                        continue;
                    }
                    if (camposPk.contains(field) && (operacao == Operacao.UPDATE)) {
                        continue;
                    }
                    if (isCampoNulo(field, obj)) {
                        if (isNotNull(field)) {
                            throw new Exception("Campo nulo: [" + field.getName() + "]");
                        } else {
                            continue;
                        }
                    }

                    setParameter(field, obj, pst, ++cont);
                }
            }
            if (operacao == Operacao.UPDATE) {
                for (Field field : camposPk) {
                    setParameter(field, obj, pst, ++cont);
                }
            }
        }
    }

    private void setParameter(Field field, T obj, PreparedStatement pst, int pos) throws SecurityException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException {
        Object fkObj = null;
        Class<?> fieldClass = field.getType();
        Method fieldGetter = obj.getClass().getDeclaredMethod(getNomeGetter(field));
        if (isForeignKey(field)) {
            fkObj = fieldGetter.invoke(obj);
            Method getFkId = fkObj.getClass().getDeclaredMethod(getNomeGetterId(fkObj));
            fieldClass = getFkId.invoke(fkObj).getClass();
            fieldGetter = getFkId;
        }
        fieldClass = ajustaTipoClasse(fieldClass);
        Class[] parametrosSetter = new Class[]{Integer.TYPE, fieldClass};
        String nomeClasse = ClasseUtil.getNomeClasse(fieldClass);
        if (nomeClasse.equals("Integer")) {
            nomeClasse = "Int";
        }
        String setterName = "set" + nomeClasse;
        Method method = PreparedStatement.class.getDeclaredMethod(setterName, parametrosSetter);
        if (isForeignKey(field)) {
            method.invoke(pst, pos, fieldGetter.invoke(fkObj));
        } else {
            if (fieldClass == java.sql.Date.class) {
                Method getTime = java.util.Date.class.getDeclaredMethod("getTime");
                method.invoke(pst, pos, new java.sql.Date((long) getTime.invoke(fieldGetter.invoke(obj))));
            } else {
                method.invoke(pst, pos, fieldGetter.invoke(obj));
            }
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
