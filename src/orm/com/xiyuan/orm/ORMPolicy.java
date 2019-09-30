package com.xiyuan.orm;

import java.sql.SQLException;

/**
 * ORM数据库策略
 *
 * @version v1.0.0 @author lgz 2016-9-18 新建与整理
 */
public interface ORMPolicy extends ORMConstants
{
    /**
     * 检查保持时长是否正确
     * 
     * @param maxKeepTime   保持时长
     * @return              =true正确，数据库定义的比该值大
     */
    public boolean chkKeepTime(int maxKeepTime);
    
    /**
     * 生成表是否存在的SQL
     * 
     * @param databaseName  数据库名
     * @param tableName     表名
     * @return              SQL语句
     */
    public String toExistsSQL(String databaseName, String tableName);
    
    /**
     * 增加表的一列，支持类型和是否为null
     * 
     * @param table         表名
     * @param column        列名
     * @param columnType    列类型
     * @param notNull       是否不允许为null
     * @return              SQL语句
     */
    public String toAlertColumnAdd(String table, String column, String columnType, boolean notNull);
    
    /**
     * ORM定义的类型，转化为SQL类型，如string,15转化为varchar(15)
     * 
     * @param type          ORM定义的类型如string,15
     * @return              SQL类型，如varchar(15)
     */
    public String toColumnType(String type);
    
    // /**
    // * 生成字段字符串
    // *
    // * @param field 表字段对象
    // * @return 生成字符串
    // */
    // public String toColumnString(_TableField field);
    //
    // /**
    // * 生成创建表字符串
    // *
    // * @param _table 表对象
    // * @param replaceMap 可替换表
    // * @return 得到创建表的SQL列表
    // */
    // public List<String> toTableString(_Table _table, MapSS replaceMap);
    
//    /**
//     * 生成查询一条数据的SQL语句
//     * 
//     * @param fieldSQL      字段SQL
//     * @param tableName     表名
//     * @param whereSQL      条件SQL
//     * @param orderbySQL    排序SQL
//     * @param groupbySQL    分组SQL
//     * @return              完整ZSQL
//     */
//    public String toItemSQL(String fieldSQL, String tableName, String whereSQL, String orderbySQL, String groupbySQL);
    
    /**
     * 生成分页显示的SQL语句
     * 
     * @param fieldSQL      字段SQL
     * @param tableName     表名
     * @param whereSQL      条件SQL
     * @param orderbySQL    排序SQL
     * @param groupbySQL    分组SQL
     * @param maxNum        最大数目
     * @param pageNo        页码
     * @return              完整SQL
     */
    public String toPageSQL(String fieldSQL, String tableName, String whereSQL, String orderbySQL, String groupbySQL, int maxNum, int pageNo);

//    /**
//     * 生成视图关联分页显示的SQL语句
//     * 
//     * @param innerTableSQL 内部表SQL
//     * @param maxNum        最大数目
//     * @param pageNo        页码
//     * @return              完整ZSQL
//     */
//    public String toPageViewSQL(StringBuilder innerTableSQL, int maxNum, int pageNo);
    
    /**************************************************************************************************/
    //不同的数据库处理
    /**************************************************************************************************/
    
    /**
     * 删除表的一列
     * 
     * @param table             表名
     * @param column            列名
     * @throws SQLException     数据库异常
     */
    public void executeAlertColumnDrop(String table, String column) throws ORMException, SQLException;
    
    /**
     * 修改表的一列值
     * 
     * @param table             表名
     * @param newColumnType     新类型
     * @param newNotNull        新列是否不为null
     * @param ORMException      ORM异常
     * @throws SQLException     数据库异常
     */
    public void executeAlertColumnType(String table, String column, String newColumnType, boolean newNotNull) throws ORMException, SQLException;
    
    /**
     * 修改表的一列名
     * 
     * @param table             表名
     * @param column            列名
     * @param newColumn         新列名
     * @throws ORMException     ORM异常
     * @throws SQLException     数据库异常
     */
    public void executeAlertColumnName(String table, String column, String newColumn) throws ORMException, SQLException;
    
    /**
     * 修改表的一列
     * 
     * @param table             表名
     * @param column            列名
     * @param newColumn         新列名
     * @param newColumnType     新类型
     * @param newNotNull        新列是否不为null
     * @throws ORMException     ORM异常
     * @throws SQLException     数据库异常
     */
    public void executeAlertColumnInfo(String table, String column, String newColumn, String newColumnType, boolean newNotNull) throws ORMException, SQLException;
    
    /**
     * 增加表的主键
     * 
     * @param table             表名
     * @param columns           列名，多个用逗号隔开如AAA_AAA,BBB_BBB
     * @throws SQLException     数据库异常
     */
    public void executeAlertPrimaryKeyAdd(String table, String columns) throws ORMException, SQLException;
    
    /**
     * 删除表的主键
     * 
     * @param table             表名
     * @throws SQLException     数据库异常
     */
    public void executeAlertPrimaryKeyDrop(String table) throws ORMException, SQLException;
}
