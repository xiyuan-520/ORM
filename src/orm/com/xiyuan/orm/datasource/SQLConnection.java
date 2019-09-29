package com.xiyuan.orm.datasource;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 数据库连接类，对Connection进行封装，增加对是否空闲，空闲时长和处理数等字段，以及close释放到连接池中 <br><br>
 * 1.connection属性为通过DataSource创建的连接信息<br>
 * 2.isIdle,idleTime,completedCount表示空闲时数据以及处理数<br>
 * 3.close()为提供业务关闭时释放连接<br>
 * 4.shutdown()为连接池关闭连接<br>
 * 5.isConnectionAvailable()检查数据库连接有效性<br>
 * 6.isExcessTimeOrNum()检查是否超出空闲时长和处理数<br>
 * 采用LRU(Least Recently Used)算法对连接进行管理，即每次提供的缓存连接都是最近的，那么最远的就会超出最大空闲了。
 *
 * @version v1.0.0 @author lgz 2016-3-21 新建与整理
 */
public class SQLConnection implements Connection
{
    private SQLDataSource source;
    private Connection conn;
    private String connId;
    private long keepTimeMs;
    
    private volatile boolean autoCommit;
    private volatile boolean closed;
    
    private volatile boolean idle;
    private volatile long idleTimeMs;
    
    public SQLConnection(SQLDataSource source, Connection conn)
    {
        this.source = source;
        this.conn = conn;
        this.connId = source.nextSequence();
        this.keepTimeMs = System.currentTimeMillis();
        
        try
        {
            this.autoCommit = conn.getAutoCommit();
        }
        catch (SQLException e)
        {
            this.autoCommit = true;
        }
        
        this.idle = true;
        this.idleTimeMs = keepTimeMs;
    }
    
    /** 当前连接编号 */
    public String getId()
    {
        return connId;
    }
    
    /** 当前连接是否空闲 */
    public boolean isIdle()
    {
        return !closed && idle;
    }
    
    /** 当前线程是否运行 */
    public boolean isActive()
    {
        return !idle;
    }
    
    /** 设置连接为运行中 */
    public SQLConnection active()
    {
        idle = false;
        source.active();
        return this;
    }
    
    /** 设置连接为空闲中 */
    public void idle()
    {
        idle = true;
        idleTimeMs = System.currentTimeMillis();
        source.idle();
    }
    
    /**
     * 检查数据库连接是否超过指定的最大保持时长、最大空闲时长和最大调用量
     * 
     * @return =true表示超过,=false可用
     */
    public boolean isKeepOrIdleTimeout()
    {
        long cutTime = System.currentTimeMillis();
        if ((cutTime - keepTimeMs) > source.getMaxKeepTimeMs())
            return true;
        
        if ((cutTime - idleTimeMs) > source.getMaxIdleTimeMs())
            return true;
        
        return false;
    }
    
    /**
     * 检查数据库连接
     * 
     * @return =true,正常,=false不可用
     */
    public boolean isConnectionAvailable()
    {
        if (conn == null)
            return false;
        
        ResultSet rst = null;
        
        try
        {
            DatabaseMetaData metaData = conn.getMetaData();
            rst = metaData.getTables(null, null, "PROBABLYNOT", new String[] { "TABLE" });
            return true;
        }
        catch (SQLException e)
        {
            return false;
        }
        finally
        {
            DBClose.close(rst);
        }
    }
    
    public void setAutoCommit()
    {
        try
        {
            conn.setAutoCommit(autoCommit);
        }
        catch (SQLException e)
        {}
    }
    
    void shutdown()
    {
        closed = true;
        try
        {
            if (!conn.isClosed())
                conn.close();
        }
        catch (SQLException e)
        {}
    }
    
    public void close() throws SQLException
    {
        try
        {
            conn.clearWarnings();
        }
        catch (Exception e)
        {}
        source.release(this);
    }
    
    /** 获取连接已保持时长 */
    public long getKeepTimeMs()
    {
        return keepTimeMs;
    }
    
    /** 获取连接已空闲时长 */
    public long getIdleTimeMs()
    {
        return idleTimeMs;
    }
    
    public int getHoldability() throws SQLException
    {
        return conn.getHoldability();
    }
    
    public int getTransactionIsolation() throws SQLException
    {
        return conn.getTransactionIsolation();
    }
    
    public void clearWarnings() throws SQLException
    {
        conn.clearWarnings();
    }
    
    public void commit() throws SQLException
    {
        conn.commit();
    }
    
    public void rollback() throws SQLException
    {
        conn.rollback();
    }
    
    public boolean getAutoCommit() throws SQLException
    {
        return conn.getAutoCommit();
    }
    
    public boolean isClosed()
    {
        if (closed)
            return true;
        
        try
        {
            return conn.isClosed();
        }
        catch (SQLException e)
        {
            return true;
        }
    }
    
    public boolean isReadOnly() throws SQLException
    {
        return conn.isReadOnly();
    }
    
    public void setHoldability(int holdability) throws SQLException
    {
        conn.setHoldability(holdability);
    }
    
    public void setTransactionIsolation(int level) throws SQLException
    {
        conn.setTransactionIsolation(level);
    }
    
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        this.autoCommit = autoCommit;
        conn.setAutoCommit(autoCommit);
    }
    
    public void setReadOnly(boolean readOnly) throws SQLException
    {
        conn.setReadOnly(readOnly);
    }
    
    public String getCatalog() throws SQLException
    {
        return conn.getCatalog();
    }
    
    public void setCatalog(String catalog) throws SQLException
    {
        conn.setCatalog(catalog);
    }
    
    public DatabaseMetaData getMetaData() throws SQLException
    {
        return conn.getMetaData();
    }
    
    public SQLWarning getWarnings() throws SQLException
    {
        return conn.getWarnings();
    }
    
    public Savepoint setSavepoint() throws SQLException
    {
        return conn.setSavepoint();
    }
    
    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        conn.releaseSavepoint(savepoint);
    }
    
    public void rollback(Savepoint savepoint) throws SQLException
    {
        conn.rollback(savepoint);
    }
    
    public Statement createStatement() throws SQLException
    {
        return conn.createStatement();
    }
    
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return conn.createStatement(resultSetType, resultSetConcurrency);
    }
    
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return conn.getTypeMap();
    }
    
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException
    {
        conn.setTypeMap(map);
    }
    
    public String nativeSQL(String sql) throws SQLException
    {
        return conn.nativeSQL(sql);
    }
    
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        return conn.prepareCall(sql);
    }
    
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }
    
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        return conn.prepareStatement(sql);
    }
    
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        return conn.prepareStatement(sql, autoGeneratedKeys);
    }
    
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }
    
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        return conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        return conn.prepareStatement(sql, columnIndexes);
    }
    
    public Savepoint setSavepoint(String name) throws SQLException
    {
        return conn.setSavepoint(name);
    }
    
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        return conn.prepareStatement(sql, columnNames);
    }
    
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return conn.unwrap(iface);
    }
    
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return conn.isWrapperFor(iface);
    }
    
    public Clob createClob() throws SQLException
    {
        return conn.createClob();
    }
    
    public Blob createBlob() throws SQLException
    {
        return conn.createBlob();
    }
    
    public NClob createNClob() throws SQLException
    {
        return conn.createNClob();
    }
    
    public SQLXML createSQLXML() throws SQLException
    {
        return conn.createSQLXML();
    }
    
    public boolean isValid(int timeout) throws SQLException
    {
        return conn.isValid(timeout);
    }
    
    public void setClientInfo(String name, String value) throws SQLClientInfoException
    {
        conn.setClientInfo(name, value);
    }
    
    public void setClientInfo(Properties properties) throws SQLClientInfoException
    {
        conn.setClientInfo(properties);
    }
    
    public String getClientInfo(String name) throws SQLException
    {
        return conn.getClientInfo(name);
    }
    
    public Properties getClientInfo() throws SQLException
    {
        return conn.getClientInfo();
    }
    
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        return conn.createArrayOf(typeName, elements);
    }
    
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return conn.createStruct(typeName, attributes);
    }
    
    /**********************************************************/
    // JDK1.7增加
    /**********************************************************/
    
    public void setSchema(String schema) throws SQLException
    {
    }
    
    public String getSchema() throws SQLException
    {
        return null;
    }
    
    public void abort(Executor executor) throws SQLException
    {
    }
    
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
    {
    }
    
    public int getNetworkTimeout() throws SQLException
    {
        return 0;
    }
}
