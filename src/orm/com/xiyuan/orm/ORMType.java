package com.xiyuan.orm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xiyuan.core.util.consts.KV;

import com.xiyuan.orm.dbimpl._HSQL;
import com.xiyuan.orm.dbimpl._MSSQL;
import com.xiyuan.orm.dbimpl._MySQL;
import com.xiyuan.orm.dbimpl._Oracle;
import com.xiyuan.orm.dbimpl._PostgetSQL;
import com.xiyuan.orm.dbimpl._SQLite;

/**
 * ORM支持的数据库类型，当前支持6种，分别为ORACLE|MYSQL|MSSQL|HSQL|SQLite|PostgreSQL
 *
 * @version v1.0.0 @author lgz 2016-3-21 新建与整理
 */
public class ORMType implements ORMConstants
{
    private static final Map<String, KV<Integer, String>> DB_TYPE_MAP = new HashMap<String, KV<Integer, String>>();
    private static final Map<String, KV<Integer, String>> DATA_TYPE_MAP = new HashMap<String, KV<Integer, String>>();
    
    static
    {
        // 支持的6六种数据库类型
        DB_TYPE_MAP.put(ORM_MYSQL.value(), ORM_MYSQL);
        DB_TYPE_MAP.put(ORM_ORACLE.value(), ORM_ORACLE);
        DB_TYPE_MAP.put(ORM_MSSQL.value(), ORM_MSSQL);
        DB_TYPE_MAP.put(ORM_PSQL.value(), ORM_PSQL);
        DB_TYPE_MAP.put(ORM_SQLITE.value(), ORM_SQLITE);
        DB_TYPE_MAP.put(ORM_HSQL.value(), ORM_HSQL);
        
        // 支持的9种数据字段类型
        DATA_TYPE_MAP.put(ORM_STRING.value(), ORM_STRING);
        DATA_TYPE_MAP.put(ORM_INT.value(), ORM_INT);
        DATA_TYPE_MAP.put(ORM_LONG.value(), ORM_LONG);
        DATA_TYPE_MAP.put(ORM_BOOLEAN.value(), ORM_BOOLEAN);
        DATA_TYPE_MAP.put(ORM_BYTE.value(), ORM_BYTE);
        DATA_TYPE_MAP.put(ORM_SHORT.value(), ORM_SHORT);
        DATA_TYPE_MAP.put(ORM_DATETIME.value(), ORM_DATETIME);
        DATA_TYPE_MAP.put(ORM_DECIMAL.value(), ORM_DECIMAL);
        DATA_TYPE_MAP.put(ORM_BINARY.value(), ORM_BINARY);
    }
    
    /**********************************************************************/
    // 数据库类型
    /**********************************************************************/
    
    public static ORMPolicy getDatabasePolicy(ORMServer server, String dbType)
    {
        KV<Integer, String> value = DB_TYPE_MAP.get(dbType);
        if (value == null)
            return null;
        
        switch (value.key())
        {
            case ORM_MYSQL_INT:
                return new _MySQL(server);
            case ORM_ORACLE_INT:
                return new _Oracle(server);
            case ORM_MSSQL_INT:
                return new _MSSQL(server);
            case ORM_PSQL_INT:
                return new _PostgetSQL(server);
            case ORM_SQLITE_INT:
                return new _SQLite(server);
            case ORM_HSQL_INT:
                return new _HSQL(server);
                
            default:
                return null;
        }
    }
    
    /**
     * 获取数据库类型
     * @param dbType
     * @return
     */
    public static int getDatabaseType(String dbType)
    {
        return DB_TYPE_MAP.get(dbType).key();
    }
    
    // public static boolean isDatabaseTypeSql(String name, String dbType)
    // {
    // boolean isSpecified = false;
    // for (String s : DB_TYPE_MAP.keySet())
    // {
    // if (name.endsWith("." + s + SQL_ENDSWITH))
    // {
    // isSpecified = true;
    // break;
    // }
    // }
    //
    // //未特别指定的通用支持
    // if (!isSpecified)
    // return true;
    //
    // //指定的则和当前进行匹配
    // return name.endsWith("." + dbType + SQL_ENDSWITH);
    // }
    
    /**获取数据库类型*/
    public static List<String> getDatabaseTypeList()
    {
        List<String> list = new ArrayList<>(DB_TYPE_MAP.size());
        for (String key : DB_TYPE_MAP.keySet())
            list.add(key);
        
        return list;
    }
    
    /**获取数据库类型*/
    public static String[] getDatabaseTypes()
    {
        String[] types = new String[DB_TYPE_MAP.size()];
        int i = 0;
        for (String key : DB_TYPE_MAP.keySet())
        {
            types[i++] = key;
        }
        return types;
    }
    
    /**********************************************************************/
    // 字段类型
    /**********************************************************************/
    
    public static boolean isSupportColumn(String columnType)
    {
        return DATA_TYPE_MAP.containsKey(columnType);
    }
    
    public static boolean hasColumnLength(String columnType)
    {
        KV<Integer, String> value = DATA_TYPE_MAP.get(columnType);
        return value == null ? false : hasColumnLength(value.key());
    }
    
    public static boolean hasColumnLength(int columnType)
    {
        return (columnType == ORM_STRING_INT || columnType == ORM_DECIMAL_INT);
    }
    
    public static List<String> getColumnTypeList()
    {
        List<String> list = new ArrayList<>(DATA_TYPE_MAP.size());
        for (String key : DATA_TYPE_MAP.keySet())
        {
            list.add(key);
        }
        return list;
    }
    
    public static String[] getColumnTypes()
    {
        String[] types = new String[DATA_TYPE_MAP.size()];
        int i = 0;
        for (String key : DATA_TYPE_MAP.keySet())
            types[i++] = key;
        return types;
    }
    
    /**
     * 把带下划线的名称转为表名，如Param转换为PARAM
     * 
     * @param name      名称
     * @return          表名
     */
    public static String toUpperName(String name)
    {
        return name.substring(0, 1).toUpperCase() + toTableFieldName(name.substring(1));
    }
    
    /**
     * 把带下划线的列名转为字段名，如PARAM_NAME转换为paramName
     * 
     * @param name      名称
     * @return          字段名
     */
    public static String toLowerName(String name)
    {
        return name.substring(0, 1).toLowerCase() + toTableFieldName(name.substring(1));
    }
    
    /** 去除表名字段名第一个字母的数据处理 */
    private static String toTableFieldName(String name)
    {
        StringBuilder strb = new StringBuilder();
        
        // 3.遇到下划线则下一字母大写，遇到$$中间的字母大写，并把$替换成_
        boolean nextWord = false;
        boolean nextReplace = false;
        
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (c == '_')
            {
                nextWord = true;
                continue;
            }
            
            if (c == '$')
            {// 把$转化为_
                nextReplace = !nextReplace;
                strb.append("_");
                continue;
            }
            
            if (nextWord)
            {
                strb.append(String.valueOf(c).toUpperCase());
                nextWord = false;
            }
            else if (nextReplace)
            {
                strb.append(String.valueOf(c).toUpperCase());
            }
            else
            {
                strb.append(String.valueOf(c).toLowerCase());
            }
        }
        
        return strb.toString();
    }
}
