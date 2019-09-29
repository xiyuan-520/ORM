package com.xiyuan.orm.datasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.xiyuan.core.logging.Log;
import org.xiyuan.core.logging.LogFactory;

/***
 * 数据库关闭工具类
 *
 * @version v1.0.0 @author lgz 2019-9-29 新建与整理
 */
public final class DBClose
{
    private static final Log log = LogFactory.getLog(DBClose.class);

    /** 关闭连接结果集 */
    public static void close(ResultSet rst)
    {
        try
        {
            if (rst != null)
                rst.close();
        }
        catch (Exception e)
        {
            log.error(e.getMessage());
        }
    }
    
    /** 关闭连接属性 */
    public static void close(Statement stmt)
    {
        try
        {
            if (stmt != null)
                stmt.close();
        }
        catch (Exception e)
        {
            log.error(e.getMessage());
        }
    }
    
    /** 关闭连接属性 */
    public static void close(ResultSet rst, Statement stmt)
    {
        close(rst);
        close(stmt);
    }
    
    /** 关闭/归还连接 */
    public static void close(Connection conn)
    {
        try
        {
            if (conn != null)
                conn.close();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage());
        }
    }
    
    /** 关闭和归还连接属性 */
    public static void close(Statement stmt, Connection conn)
    {
        close(stmt);
        close(conn);
    }
    
    /** 关闭和归还连接属性 */
    public static void close(ResultSet rst, Statement stmt, Connection conn)
    {
        close(rst);
        close(stmt);
        close(conn);
    }
}
