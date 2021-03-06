package ldavip.ormbasico.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ldavip.ormbasico.annotation.CampoEnum;
import ldavip.ormbasico.exception.NotNullException;
import ldavip.ormbasico.query.Operador;
import ldavip.ormbasico.util.ClasseUtil;
import ldavip.ormbasico.util.ConnectionFactory;
import ldavip.ormbasico.util.TabelaUtil;
import static ldavip.ormbasico.util.TabelaUtil.getCampoAutoIncrement;
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
import static ldavip.ormbasico.util.TabelaUtil.isIgnore;
import ldavip.ormbasico.annotation.Data;
import static ldavip.ormbasico.util.TabelaUtil.isData;

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

    protected boolean isAutoClose = false;
    protected boolean isAutoCommit = true;
    protected Connection conexao;
    private Operacao operacao;

    private final Class<?> classeDaEntidade;
    private List<Class<?>> classes = new ArrayList<>();
    private List<Object> valoresFiltro = new ArrayList<>();

    private StringBuilder query;
    private StringBuilder join = new StringBuilder();
    private StringBuilder where = new StringBuilder("\r\n WHERE 1 = 1 ");
    private StringBuilder orderBy = new StringBuilder();
    
    private boolean queryIniciada = false;
    private boolean orderByInserido = false;
    private boolean direcaoOrderByInserido = false;

    public Dao() {
        this(ConnectionFactory.getConnection());
        isAutoClose = true;
        isAutoCommit = true;
    }

    public Dao(Connection conexao) {
        isAutoClose = false;
        isAutoCommit = false;
        this.conexao = conexao;
        try {
            this.conexao.setAutoCommit(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        this.classeDaEntidade = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    public void insere(T obj) throws Exception {
        this.operacao = Operacao.INSERT;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCamposInsert(obj, operacao);
        String[] parametros = new String[campos.length];
        Arrays.fill(parametros, "?");

        String sql = new StringBuilder()
                .append("\r\n")
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

        abreConexao();
        try (PreparedStatement pst = conexao.prepareStatement(sql)) {
            conexao.setAutoCommit(false);
            setParameters(pst, obj);
            System.out.println(pst);
            pst.executeUpdate();
            
            setIdObjeto(obj);
            if (isAutoCommit) {
                conexao.commit();
            }
        } catch (Exception e) {
            conexao.rollback();
            e.printStackTrace();
            throw e;
        } finally {
            fechaConexao();
        }
    }
    
    private void setIdObjeto(T obj) throws Exception {
        Field campoAutoIncrement = TabelaUtil.getCampoAutoIncrement(obj.getClass());
        
        if (campoAutoIncrement == null) {
            return;
        }
        
        String nomeCampoAutoIncrement = getNomeColuna(campoAutoIncrement);
        StringBuilder sql = new StringBuilder();
        sql.append("\r\n").append("SELECT MAX(").append(nomeCampoAutoIncrement)
                .append(") AS ").append(nomeCampoAutoIncrement)
                .append("\r\n").append(" FROM ").append(getNomeTabela(classeDaEntidade));
        
        ResultSet rs = null;
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            System.out.println(pst);
            rs = pst.executeQuery();
            Object id = null;
            Class tipoCampo = ajustaTipoClasse(campoAutoIncrement.getType());
            Method getter = ResultSet.class.getDeclaredMethod(getNomeGetter(tipoCampo), new Class[]{ String.class });
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
        sql.append("\r\n").append("UPDATE ").append(tabela).append(" SET ")
                .append("\r\n   ");
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

        abreConexao();
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            this.conexao.setAutoCommit(false);
            setParameters(pst, obj);
            System.out.println(pst);
            pst.executeUpdate();
            if (isAutoCommit) {
                this.conexao.commit();
            }
        } catch (Exception e) {
            this.conexao.rollback();
            e.printStackTrace();
            throw e;
        } finally {
            fechaConexao();
        }
    }
    
    public void remove(T obj) throws Exception {
        this.operacao = Operacao.DELETE;

        String tabela = getNomeTabela(classeDaEntidade);

        StringBuilder sql = new StringBuilder();
        sql.append("\r\n").append("DELETE FROM ").append(tabela);
        sql.append("\r\n").append(" WHERE ");
        String[] nomesCamposPk = TabelaUtil.getNomesCamposId(classeDaEntidade);
        for (int i = 0; i < nomesCamposPk.length; i++) {
            sql.append(nomesCamposPk[i]).append(" = ? ");
            if (i < nomesCamposPk.length - 1) {
                sql.append("\r\n ").append(" AND ");
            }
        }

        abreConexao();
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            this.conexao.setAutoCommit(false);

            setParameters(pst, obj);
            System.out.println(pst);
            pst.executeUpdate();
            if (isAutoCommit) {
                this.conexao.commit();
            }
        } catch (Exception e) {
            this.conexao.rollback();
            e.printStackTrace();
            throw e;
        } finally {
            fechaConexao();
        }
    }
    
    public T buscaPorId(Object id) throws Exception {
        this.operacao = Operacao.SELECT;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        StringBuilder sql = new StringBuilder();
        sql.append("\r\n").append("SELECT ");
        sql.append("\r\n").append(TextoUtil.agrupar(campos, ", "));
        sql.append("\r\n").append(" FROM ").append(tabela);
        sql.append("\r\n").append(" WHERE ");
        Field field = getCampoAutoIncrement(classeDaEntidade);
        if (field == null) {
            field = getCampoId(classeDaEntidade);
        }
        sql.append("\r\n").append(getNomeColuna(field));
        sql.append(" = ? ");

        abreConexao();
        ResultSet rs = null;
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            Field campoId = getCampoAutoIncrement(classeDaEntidade);
            if (campoId == null) {
                campoId = getCampoId(classeDaEntidade);
            }
            Class<?> fieldClass = campoId.getType();
            fieldClass = ajustaTipoClasse(fieldClass);
            Class[] parametrosSetter = new Class[]{Integer.TYPE, fieldClass};
            String nomeClasse = ClasseUtil.getNomeClasse(fieldClass);
            if (nomeClasse.equals("Integer")) {
                nomeClasse = "Int";
            }
            String setterName = "set" + nomeClasse;
            Method setter = PreparedStatement.class.getDeclaredMethod(setterName, parametrosSetter);
            setter.invoke(pst, 1, id);
            System.out.println(pst);
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
            fechaConexao();
        }

        return null;
    }
    
    public T buscarUltimo() throws Exception {
        this.operacao = Operacao.SELECT;

        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        String nomeCampoId = getNomeCampoId(classeDaEntidade);
        StringBuilder sql = new StringBuilder();
        sql.append("\r\n ").append("SELECT ");
        sql.append("\r\n ").append(TextoUtil.agrupar(campos, ", "));
        sql.append("\r\n ").append("FROM ").append(tabela);
        sql.append("\r\n ").append("WHERE ").append(nomeCampoId).append(" = ");
        sql.append("\r\n ").append("(");
        sql.append("\r\n ").append("   SELECT MAX(").append(nomeCampoId).append(") AS ").append(nomeCampoId);
        sql.append("\r\n ").append("   FROM ").append(tabela);
        sql.append("\r\n ").append(")");

        abreConexao();
        ResultSet rs = null;
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            System.out.println(pst);
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
            fechaConexao();
        }

        return null;
    }

    /**
     * Use apenas o método <code>toList()</code>
     * @see toList()
     * @return
     * @deprecated
     */
    @Deprecated
    public Dao buscaLista() {
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
                    valoresFiltro.add(valor[0]);
                    valoresFiltro.add(valor[1]);
                    
                    where.append(operador).append(" ? AND ? ");
                    break;
                case CONTEM:
                case NAO_CONTEM:
                    String[] str = new String[valor.length];
                    Arrays.fill(str, "?");
                    for (Object val : valor) {
                        valoresFiltro.add(val);
                    }
                    
                    where.append(operador).append(" (").append(TextoUtil.agrupar(str, ", ")).append(") ");
                    break;
                case SIMILAR:
                    if (valor.length != 1) {
                        throw new IllegalArgumentException("A quantidade de valores esperada era: 1!"
                                + "\nQuantidade encontrada: " + valor.length);
                    }
                    valoresFiltro.add("%" + valor[0] + "%");
                    where.append(operador).append(" ? ");
                    break;
                default:
                    if (valor.length != 1) {
                        throw new IllegalArgumentException("A quantidade de valores esperada era: 1!"
                                + "\nQuantidade encontrada: " + valor.length);
                    }
                    valoresFiltro.add(valor[0]);
                    where.append(operador).append(" ? ");
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

    /**
     * Retorna uma lista com os objetos encontrados.
     * @return
     * @throws Exception 
     */
    public List<T> toList() throws Exception {
        this.operacao = Operacao.SELECT;
        String tabela = getNomeTabela(classeDaEntidade);
        String[] campos = getNomeCampos(classeDaEntidade, operacao);

        this.query = new StringBuilder();
        this.query.append("\r\n ").append("SELECT ");
        this.query.append("\r\n ").append(TextoUtil.agrupar(campos, ", "));
        this.query.append("\r\n ").append("FROM ").append(tabela).append(" ");

        queryIniciada = true;
        
        checkJoins();
        this.query.append(join);
        this.query.append(where);
        this.query.append(orderBy);
        
        return buscaLista(this.query.toString());
    }
    
    public T retornaPrimeiroObjeto() throws Exception {
        List<T> lista = toList();
        if (lista.isEmpty()) {
            return null;
        }
        return lista.get(0);
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
        abreConexao();
        ResultSet rs = null;
        try (PreparedStatement pst = this.conexao.prepareStatement(sql.toString())) {
            setParametrosFiltro(pst, valoresFiltro);
            System.out.println(pst);
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
            fechaConexao();
        }
        finalizar();
        return lista;
    }
    
    private void finalizar() {
        operacao = null;
        valoresFiltro.clear();
        classes.clear();
        query = new StringBuilder();
        join = new StringBuilder();
        where = new StringBuilder("\r\n WHERE 1 = 1 ");
        orderBy = new StringBuilder();
        queryIniciada = false;
        orderByInserido = false;
        direcaoOrderByInserido = false;
    }
    
    private void setParametrosFiltro(PreparedStatement pst, List<Object> parametros) throws Exception {
        int cont = 0;
        for (Object parametro : parametros) {
            Class classeParametro = ajustaTipoClasse(parametro.getClass());
            Method setter = PreparedStatement.class.getDeclaredMethod(getNomeSetter(classeParametro), new Class[]{Integer.TYPE, classeParametro});
            setter.invoke(pst, ++cont, parametro);
        }
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
                    if (isIgnore(field, operacao)) {
                        continue;
                    }
                    if (isAutoIncrement(field) && (operacao == Operacao.INSERT || operacao == Operacao.UPDATE)) {
                        continue;
                    }
                    if (camposPk.contains(field) && (operacao == Operacao.UPDATE)) {
                        continue;
                    }
                    if (isCampoNulo(field, obj) && operacao == Operacao.INSERT) {
                        if (isNotNull(field)) {
                            throw new NotNullException(field);
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

    private void setParameter(Field field, T obj, PreparedStatement pst, int pos) throws Exception {
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
        if (ClasseUtil.isEnum(fieldClass)) {
            CampoEnum campoEnum = TabelaUtil.getCampoEnum(field);
            if (campoEnum == null) {
                throw new Exception("Não definido o tipo de dado do campo Enum: [" + field.getName() + "]");
            }
            fieldClass = campoEnum.tipoDaColuna();
        }
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
                try {
                    Method getTime = java.util.Date.class.getDeclaredMethod("getTime");
                    method.invoke(pst, pos, new java.sql.Date((long) getTime.invoke(fieldGetter.invoke(obj))));
                } catch (NullPointerException e) {
                    method.invoke(pst, pos, null);
                }
            } else {
                Object valor = fieldGetter.invoke(obj);
                if (ClasseUtil.isEnum(field.getType())) {
                    CampoEnum campoEnum = TabelaUtil.getCampoEnum(field);
                    if (campoEnum.tipoDaColuna() == int.class) {
                        valor = ((Enum) valor).ordinal();
                    } else {
                        valor = valor.toString();
                    }
                }
                method.invoke(pst, pos, valor);
            }
        }
    }

    protected T populaObjeto(ResultSet rs) throws Exception {
        T obj = (T) classeDaEntidade.newInstance();

        for (Field campo : classeDaEntidade.getDeclaredFields()) {
            if (isIgnore(campo, operacao)) {
                continue;
            }
            if (isColuna(campo)) {
                Object objFk = null;
                CampoEnum campoEnum = null;
                boolean isEnum = false;
                String nomeColuna = getNomeColuna(campo);
                Class<?> classeCampo = campo.getType();
                if (ClasseUtil.isEnum(campo.getType())) {
                    isEnum = true;
                    campoEnum = TabelaUtil.getCampoEnum(campo);
                    if (campoEnum == null) {
                        throw new Exception("Não definido o tipo de dado do campo Enum: [" + campo.getName() + "]");
                    }
                    classeCampo = campoEnum.tipoDaColuna();
                }
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

                    objFk = dao.buscaPorId(getter.invoke(rs, nomeColuna));
                    classeCampo = objFk.getClass();
                }

                Class[] parametrosSetter = new Class[]{campo.getType()};
                Method setter = classeDaEntidade.getDeclaredMethod(getNomeSetter(campo), parametrosSetter);

                if (isForeignKey(campo)) {
                    setter.invoke(obj, objFk);
                } else {
                    Object valor = null;
                    try {
                        if (isData(campo)) {
                            Data data = TabelaUtil.getData(classeCampo);
                            String strDate = rs.getString(nomeColuna);
                            valor = new java.text.SimpleDateFormat(data.formato()).parse(strDate);
                        } else {
                            valor = getter.invoke(rs, nomeColuna);
                        }
                    } catch (Exception e) {
                    }
                    if (isEnum) {
                        if (campoEnum.tipoDaColuna() == int.class) {
                            Object[] values = campo.getType().getEnumConstants();
                            valor = values[Integer.parseInt(String.valueOf(valor))];
                        } else {
                            boolean encontrado = false;
                            Object[] enums = campo.getType().getEnumConstants();
                            for (Object aEnum : enums) {
                                if (((Enum) aEnum).name().toUpperCase().equals(String.valueOf(valor).toUpperCase())) {
                                    encontrado = true;
                                    valor = aEnum;
                                    break;
                                }
                            }
                            if (!encontrado) {
                                throw new Exception("Valor para o Enum: [" + campo.getName() + "] não encontrado!");
                            }
                        }
                    }
                    setter.invoke(obj, valor);
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
    
    public void setAutoCommit(boolean autoCommit) {
        this.isAutoCommit = autoCommit;
        if (conexao != null) {
            try {
                conexao.setAutoCommit(autoCommit);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void commit() throws SQLException {
        conexao.commit();
    }
    
    protected void fechaConexao() throws SQLException {
        if (isAutoClose) {
            conexao.close();
        }
    }

    protected void abreConexao() {
        if (isAutoClose) {
            conexao = ConnectionFactory.getConnection();
        }
    }
}
