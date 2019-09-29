package com.xiyuan.orm.datasource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.xiyuan.core.logging.Log;
import org.xiyuan.core.logging.LogFactory;

/**
 * 数据库连接测试类 <br>
 *
 * @version v1.0.0 @author lgz 2016-3-21 新建与整理
 */
public class ConnectionTester
{
    private static final Log log = LogFactory.getLog("database.log");
    
    public final static int CONNECTION_IS_OKAY       =  0;
    public final static int CONNECTION_IS_INVALID    = -1;
    public final static int DATABASE_IS_INVALID      = -8;
    
    private final static Set<String> INVALID_DB_STATES;
    static
    {
        Set<String> temp = new HashSet<String>();
        temp.add("08001"); //SQL State "Unable to connect to data source"
        temp.add("08007"); //SQL State "Connection failure during transaction"
        temp.add(null);    //SQL State "Io 异常: Connection reset by peer: socket write error"
        
        // MySql appently uses this state to indicate a stale, expired
        // connection when the database is fine, so we'll not presume
        // this SQL state signals an invalid database.
        temp.add("08S01"); //SQL State "Communication link failure"
    
        INVALID_DB_STATES = Collections.unmodifiableSet(temp);
    }

    private SQLDataSource dataSource = null;
    private SQLConnection testconnection = null;
    
    public ConnectionTester(SQLDataSource dataSource)
    {
        this.dataSource = dataSource;
    }
    
    /**
     * 判断数据库是否断开
     * 
     * @return boolean =true表示断开,=false表示正常
     */
    public boolean isDbBreak()
    {
        if (testconnection == null)
        {
            testconnection = dataSource.newConnection("测试");
            if (testconnection == null)
                return true;
        }
        
        if (testconnection.isClosed() || testconnection.isKeepOrIdleTimeout())
        {
            testconnection.shutdown();
            testconnection = null;
            testconnection = dataSource.newConnection("测试");
            if (testconnection == null)
                return true;
        }  
        
        int status = isConnectionAvailable(testconnection);
        if (status == CONNECTION_IS_OKAY)
            return false;
        
        testconnection.shutdown();
        testconnection = null;
        return true;
    }
    
    /**
     * 检查数据库连接
     * 
     * @param connection 数据库连接
     * @return =0,正常,=-1,该连接异常,=-8,数据库异常
     */
    public int isConnectionAvailable(Connection connection)
    {
        if (connection == null)
            return CONNECTION_IS_INVALID;
            
        ResultSet rst = null;
        
        try
        {
            DatabaseMetaData metaData = connection.getMetaData();

            rst = metaData.getTables(null, null, "PROBABLYNOT", new String[]{"TABLE"});
            return CONNECTION_IS_OKAY;
        }
        catch (SQLException e)
        {
            String state = e.getSQLState();
            if (INVALID_DB_STATES.contains(state))
            {
                log.error("检查数据库连接是否可用时:数据库不可用");
                return DATABASE_IS_INVALID;
            }
            else
            {
                log.error("检查数据库连接是否可用时:连接不可用");
                return CONNECTION_IS_INVALID; 
            }
        }
        finally
        {
            DBClose.close(rst);
        }
    }
    
    /** 关闭测试连接 */
    public void shutdown()
    {
        if (testconnection != null)
        {
            testconnection.shutdown();
            testconnection = null;
        }
    }
}
