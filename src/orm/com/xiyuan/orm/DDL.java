package com.xiyuan.orm;

import java.sql.SQLException;

import org.xiyuan.core.MultiInstance;

/**
 * DDL调用，支持(alter)<br><br>
 *                      
 * @version v1.0.0 @author lgz 2016-3-21 新建与整理
 */
public interface DDL extends MultiInstance
{
    /*****************************************************/
    //字段和列类型之间的转换
    /*****************************************************/
    
    /**
     * ORM定义的类型，转化为SQL类型，如string,15转化为varchar(15)
     * 
     * @param type          ORM定义的类型如string,15
     * @return              SQL类型，如varchar(15)
     */
    public String toColumnType(String type);
    
    /**
     * ORM定义的类型，转化为SQL类型，如string,15转化为varchar(15)
     * 
     * @param fieldType     ORM定义的类型如string,int
     * @param fieldLength   ORM定义的类型长度，string和decimal时有效，如19,char | 10,2
     * @return              SQL类型，如varchar(15)
     */
    public String toColumnType(String fieldType, String fieldLength);
    
    /*****************************************************/
    //alter DDL语句
    /*****************************************************/
    
    /**
     * 修改一个表的名称
     * 
     * @param table             表名
     * @param newTable          新表名
     * @throws SQLException     数据库异常
     */
    public void alterTableName(String table, String newTable) throws SQLException;
    
    /**
     * 增加一个表的一列
     * 
     * @param table             表名
     * @param column            列名
     * @param columnType        列类型
     * @param notNull           是否允许为null
     * @throws SQLException     数据库异常
     */
    public void alterColumnAdd(String table, String column, String columnType, boolean notNull) throws SQLException;
    
    /**
     * 删除一个表的一列
     * 
     * @param table             表名
     * @param column            列名
     * @throws SQLException     数据库异常
     */
    public void alterColumnDrop(String table, String column) throws ORMException, SQLException;
    
    /**
     * 修改一个表的一列信息
     * 
     * @param table             表名
     * @param column            列名
     * @param newColumn         新列名
     * @param newColumnType     新列类型
     * @param newNotNull        新列是否不为null
     * @throws SQLException     数据库异常
     */
    public void alterColumnInfo(String table, String column, String newColumn, String newColumnType, boolean newNotNull) throws ORMException, SQLException;
    
    /**
     * 修改一个表的列名
     * 
     * @param table             表名
     * @param column            列名
     * @param newColumn         新列名
     * @throws SQLException     数据库异常
     */
    public void alterColumnName(String table, String column, String newColumn) throws ORMException, SQLException;
    
    /**
     * 修改一个表的一列类型
     * 
     * @param table             表名
     * @param column            列名
     * @param newType           新类型
     * @throws SQLException     数据库异常
     */
    public void alterColumnType(String table, String column, String newColumnType, boolean newNotNull) throws ORMException, SQLException;

    /**
     * 增加表的主键
     * 
     * @param table             表名
     * @param columns           列名，多个用逗号隔开如AAA_AAA,BBB_BBB
     * @throws SQLException     数据库异常
     */
    public void alertPrimaryKeyAdd(String table, String columns) throws ORMException, SQLException;
    
    /**
     * 删除表的主键
     * 
     * @param table             表名
     * @throws SQLException     数据库异常
     */
    public void alertPrimaryKeyDrop(String table) throws ORMException, SQLException;
}
