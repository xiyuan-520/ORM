package com.xiyuan.orm;

import org.xiyuan.core.constants.CodeConstants;
import org.xiyuan.core.constants.SignConstants;
import org.xiyuan.core.constants.TypeConstants;
import org.xiyuan.core.extend.KV;
import org.xiyuan.core.util.consts.Int;

/**
 * ORM常量定义
 *
 * @version v1.0.0 @author lgz 2016-3-21 新建与整理
 */
public interface ORMConstants extends SignConstants, TypeConstants, CodeConstants
{
    public enum CALL_TYPE {IN, INOUT, OUT};
    
    /*****************************************************************************/
    //目前支持的6种数据库类型
    /*****************************************************************************/
    
    public String ORM_ID                     = "orm.id";
    
    
    public int ORM_MYSQL_INT                 = 1;
    public int ORM_ORACLE_INT                = 2;
    public int ORM_MSSQL_INT                 = 3;
    public int ORM_PSQL_INT                  = 4;
    public int ORM_SQLITE_INT                = 5;
    public int ORM_HSQL_INT                  = 6;
    
    public KV<Integer, String> ORM_MYSQL     = new KV<Integer, String>(1, "mysql");
    public KV<Integer, String> ORM_ORACLE    = new KV<Integer, String>(2, "oracle");
    public KV<Integer, String> ORM_MSSQL     = new KV<Integer, String>(3, "mssql");
    public KV<Integer, String> ORM_PSQL      = new KV<Integer, String>(4, "postgresql");
    public KV<Integer, String> ORM_SQLITE    = new KV<Integer, String>(5, "sqlite");
    public KV<Integer, String> ORM_HSQL      = new KV<Integer, String>(6, "hsql");
    
    /*****************************************************************************/
    //目前支持的9种列类型
    /*****************************************************************************/
    
    public Int ORM_STRING                    = new Int(1, "string");
    public Int ORM_INT                       = new Int(2, "int");
    public Int ORM_LONG                      = new Int(3, "long");
    public Int ORM_BOOLEAN                   = new Int(4, "boolean");
    public Int ORM_BYTE                      = new Int(5, "byte");
    public Int ORM_SHORT                     = new Int(6, "short");
    
    public Int ORM_DATETIME                  = new Int(7, "datetime");
    public Int ORM_DECIMAL                   = new Int(8, "decimal");
    public Int ORM_BINARY                    = new Int(9, "binary");
    
    public int ORM_STRING_INT                = 1;
    public int ORM_INT_INT                   = 2;
    public int ORM_LONG_INT                  = 3;
    public int ORM_BOOLEAN_INT               = 4;
    public int ORM_BYTE_INT                  = 5;
    public int ORM_SHORT_INT                 = 6;
    public int ORM_DATETIME_INT              = 7;
    public int ORM_DECIMAL_INT               = 8;
    public int ORM_BINARY_INT                = 9;
    
    /*****************************************************************************/
    //SQL条件
    /*****************************************************************************/
    
    public int EQUAL                           = 0;//=             等于
    public int NOT_EQUAL                       = 1;//<>            不等
    public int OR                              = 2;//or            否则
    public int LIKE                            = 10;//'%value%'    两边都LIKE
    public int LIKE_LEFT                       = 11;//'%value'     左LIKE
    public int LIKE_RIGHT                      = 12;//'value%'     右LIKE
    public int THEN_G                          = 21;//>            大于
    public int THEN_GE                         = 22;//>=           大于等于
    public int THEN_L                          = 23;//<            小于
    public int THEN_LE                         = 24;//<=           小于等于
    public int IS_NULL                         = 31;//is null      为空
    public int IS_NOT_NULL                     = 32;//is not null  不为空
    public int IN                              = 33;//in           包含
    public int NOT_IN                          = 34;//not in       不包含
    
    public String ASC                          = "asc";//顺序
    public String DESC                         = "desc";//倒序
    
    public String JOIN_EQUAL                   = "EQUAL";//内连接inner join
    public String JOIN_LEFT                    = "LEFT";//左连接left join
    public String JOIN_RIGHT                   = "RIGHT";//右连接right join
    
    /*****************************************************************************/
    //表是否存在的SQL语句
    /*****************************************************************************/
    
    public String T_EXISTS_MYSQL               = "select count(*) from information_schema.tables where table_schema='%s' and table_name='%s'";
    public String T_EXISTS_ORACLE              = "select count(*) from tab where tabtype='TABLE' and tname='%s'";
    public String T_EXISTS_MSSQL               = "select count(*) from sysobjects where type='U' and name='%s'";
    public String T_EXISTS_PSQL                = "select count(*) from pg_tables where tablename='%s'";
    public String T_EXISTS_SQLITE              = "select count(*) from sqlite_master where type='table' and name='%s'";
    public String T_EXISTS_HSQL                = "select count(*) from information_schema.tables where table_name='%s';";
    
    public String T_COLUMN_MYSQL               = "select COLUMN_TYPE,IS_NULLABLE from information_schema.columns where table_schema=? and table_name=? and column_name=?";
    public String T_SELECT_IDENTITY            = "select @@IDENTITY";
}
