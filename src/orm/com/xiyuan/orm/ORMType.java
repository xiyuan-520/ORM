package com.xiyuan.orm;

import java.util.ArrayList;
import java.util.List;

import org.xiyuan.core.extend.LinkedMapSV;
import org.xiyuan.core.extend.MapSV;
import org.xiyuan.core.util.consts.Int;
import org.xiyuan.orm.policy._HSQL;
import org.xiyuan.orm.policy._MSSQL;
import org.xiyuan.orm.policy._MySQL;
import org.xiyuan.orm.policy._Oracle;
import org.xiyuan.orm.policy._PostgetSQL;
import org.xiyuan.orm.policy._SQLite;

/**
 * ORM支持的数据库类型，当前支持6种，分别为ORACLE|MYSQL|MSSQL|HSQL|SQLite|PostgreSQL
 *
 * @version v1.0.0 @author lgz 2016-3-21 新建与整理
 */
public class ORMType implements ORMConstants
{
    private static final MapSV<Int> DB_TYPE_MAP = new LinkedMapSV<>();
    private static final MapSV<Int> DC_TYPE_MAP = new LinkedMapSV<>();
    
    static
    {
        //支持的6六种数据库类型
        DB_TYPE_MAP.put(Z_ORM_MYSQL.desc(),     Z_ORM_MYSQL);
        DB_TYPE_MAP.put(Z_ORM_ORACLE.desc(),    Z_ORM_ORACLE);
        DB_TYPE_MAP.put(Z_ORM_MSSQL.desc(),     Z_ORM_MSSQL);
        DB_TYPE_MAP.put(Z_ORM_PSQL.desc(),      Z_ORM_PSQL);
        DB_TYPE_MAP.put(Z_ORM_SQLITE.desc(),    Z_ORM_SQLITE);
        DB_TYPE_MAP.put(Z_ORM_HSQL.desc(),      Z_ORM_HSQL);
        
        //支持的9种数据字段类型
        DC_TYPE_MAP.put(Z_ORM_STRING.desc(),    Z_ORM_STRING);
        DC_TYPE_MAP.put(Z_ORM_INT.desc(),       Z_ORM_INT);
        DC_TYPE_MAP.put(Z_ORM_LONG.desc(),      Z_ORM_LONG);
        DC_TYPE_MAP.put(Z_ORM_BOOLEAN.desc(),   Z_ORM_BOOLEAN);
        DC_TYPE_MAP.put(Z_ORM_BYTE.desc(),      Z_ORM_BYTE);
        DC_TYPE_MAP.put(Z_ORM_SHORT.desc(),     Z_ORM_SHORT);
        DC_TYPE_MAP.put(Z_ORM_DATETIME.desc(),  Z_ORM_DATETIME);
        DC_TYPE_MAP.put(Z_ORM_DECIMAL.desc(),   Z_ORM_DECIMAL);
        DC_TYPE_MAP.put(Z_ORM_BINARY.desc(),    Z_ORM_BINARY);
    }
    
    /**********************************************************************/
    //数据库类型
    /**********************************************************************/
    
    public static ORMPolicy getDatabasePolicy(ORMServer server, String dbType)
    {
        Int value = DB_TYPE_MAP.get(dbType);
        if (value == null)
            return null;
        
        switch (value.value())
        {
        case Z_ORM_MYSQL_INT:return new _MySQL(server);
        case Z_ORM_ORACLE_INT:return new _Oracle(server);
        case Z_ORM_MSSQL_INT:return new _MSSQL(server);
        case Z_ORM_PSQL_INT:return new _PostgetSQL(server);
        case Z_ORM_SQLITE_INT:return new _SQLite(server);
        case Z_ORM_HSQL_INT:return new _HSQL(server);

        default:return null;
        }
    }
    
    public static Int getDatabaseType(String dbType)
    {
        return DB_TYPE_MAP.get(dbType);
    }
    
    public static boolean isDatabaseTypeSql(String name, String dbType)
    {
        boolean isSpecified = false;
        for (String s : DB_TYPE_MAP.keySet())
        {
            if (name.endsWith("." + s + Z_SQL_ENDSWITH))
            {
                isSpecified = true;
                break;
            }
        }
        
        //未特别指定的通用支持
        if (!isSpecified)
            return true;
        
        //指定的则和当前进行匹配
        return name.endsWith("." + dbType + Z_SQL_ENDSWITH);
    }
    
    public static List<String> getDatabaseTypeList()
    {
        List<String> list = new ArrayList<>(DB_TYPE_MAP.size());
        for (String key : DB_TYPE_MAP.keySet())
        {
            list.add(key);
        }
        return list;
    }
    
    public static String[] getDatabaseTypes()
    {
        String[] types = new String[DB_TYPE_MAP.size()];int i=0;
        for (String key : DB_TYPE_MAP.keySet())
        {
            types[i++] = key;
        }
        return types;
    }
    
    /**********************************************************************/
    //字段类型
    /**********************************************************************/
    
    public static boolean isSupportColumn(String columnType)
    {
        return DC_TYPE_MAP.containsKey(columnType);
    }
    
    public static boolean hasColumnLength(String columnType)
    {
        Int value = DC_TYPE_MAP.get(columnType);
        return value==null?false:hasColumnLength(value.value());
    }
    
    public static boolean hasColumnLength(int columnType)
    {
        return (columnType == Z_ORM_STRING_INT || columnType == Z_ORM_DECIMAL_INT);
    }
    
    public static int getColumnTypeMaybeLength(String columnTypeMaybeLength)
    {
        int ind = columnTypeMaybeLength.indexOf(",");
        if (ind == -1)
            return getColumnType(columnTypeMaybeLength).value();
        
        String ctype = columnTypeMaybeLength.substring(0, ind);
        return getColumnType(ctype).value();
    }
    
    public static Int getColumnType(String columnType)
    {
        return DC_TYPE_MAP.get(columnType);
    }
    
    public static List<String> getColumnTypeList()
    {
        List<String> list = new ArrayList<>(DC_TYPE_MAP.size());
        for (String key : DC_TYPE_MAP.keySet())
        {
            list.add(key);
        }
        return list;
    }
    
    public static String[] getColumnTypes()
    {
        String[] types = new String[DC_TYPE_MAP.size()];int i=0;
        for (String key : DC_TYPE_MAP.keySet())
        {
            types[i++] = key;
        }
        return types;
    }
    
    /**
     * 把带下划线的名称转为表名，如FMR_PARAM转换为FmrParam
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
        
        //3.遇到下划线则下一字母大写，遇到$$中间的字母大写，并把$替换成_
        boolean nextWord = false;
        boolean nextReplace = false;
        
        for (int i=0;i<name.length();i++)
        {
            char c = name.charAt(i);
            if (c == '_')
            {
                nextWord = true;
                continue;
            }
            
            if (c == '$')
            {//把$转化为_
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
