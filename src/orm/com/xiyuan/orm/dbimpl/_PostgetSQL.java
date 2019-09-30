package com.xiyuan.orm.dbimpl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.xiyuan.core.util.Arrays;
import org.xiyuan.core.util.Asserts;
import org.xiyuan.core.util.Ints;
import org.xiyuan.core.util.Lists;
import org.xiyuan.core.util.Sqls;
import org.xiyuan.core.util.Strings;
import org.xiyuan.core.util.Validates;

import com.xiyuan.orm.ORMException;
import com.xiyuan.orm.ORMPolicy;
import com.xiyuan.orm.ORMServer;
import com.xiyuan.orm.ORMType;

/**
 * PostgetSQL数据库策略
 *
 * @version v1.0.0 @author lgz 2016-9-18 新建与整理
 */
public class _PostgetSQL implements ORMPolicy
{
    private ORMServer server;
    
    public _PostgetSQL(ORMServer server)
    {
        this.server = server;
    }
    
    @Override
    public boolean chkKeepTime(int maxKeepTime)
    {
        return true;
    }
    
    @Override
    public String toExistsSQL(String databaseName, String tableName)
    {
        return Strings.format(T_EXISTS_PSQL, tableName);
    }
    
    /**
     * 增加表的一列，支持类型和是否为null
     * 
     * @param table         表名
     * @param column        列名
     * @param columnType    列类型
     * @param notNull       是否不允许为null
     * @return              SQL语句
     */
    public String toAlertColumnAdd(String table, String column, String columnType, boolean notNull)
    {
        return new StringBuilder()
            .append("alter table ").append(table.toUpperCase())
            .append(" add column ").append(column.toUpperCase()).append(" ").append(columnType)
            .append(notNull?" not null":" default null")
            .toString();
    }
    
    @Override
    public String toColumnType(String type)
    {
        int ctype = ORMType.getColumnTypeMaybeLength(type);
        switch (ctype)
        {
        //基本类型
        case Z_ORM_BOOLEAN_INT:     return "bool";
        case Z_ORM_BYTE_INT:        return "int2";
        case Z_ORM_SHORT_INT:       return "int2";
        case Z_ORM_INT_INT:         return "int4";
        case Z_ORM_LONG_INT:        return "int8";
        //小数和时间
        case Z_ORM_DATETIME_INT:    return "timestamp";
        case Z_ORM_DECIMAL_INT:
        {
            String[] values = Arrays.toStringArray(type);
            Asserts.asserts(values.length == 3, ORMI18n.ormFieldTypeIncorrect1, type);
            Asserts.asserts(Validates.isIntegerPositive(values[1]), ORMI18n.ormFieldTypeIncorrect1, type);
            Asserts.asserts(Validates.isIntegerPositive(values[2]), ORMI18n.ormFieldTypeIncorrect1, type);
            //decimal(15,3)前面11位整数相当于int，中间点号，后面3位小数的表示方式
            return "decimal("+values[1]+","+values[2]+")";
            
        }
        //字符串
        case Z_ORM_STRING_INT:
        {
            String[] values = Arrays.toStringArray(type);
            Asserts.asserts(values.length > 1, ORMI18n.ormFieldTypeIncorrect1, type);
            Asserts.asserts(Validates.isIntegerPositive(values[1]), ORMI18n.ormFieldTypeIncorrect1, type);
            
            if (values.length == 3 && "char".equals(values[2]))
            {//char(50)
                Asserts.asserts(Validates.isIntegerValue(values[1], 1, 255), ORMI18n.ormFieldTypeIncorrect1, type);
                return "char("+values[1]+")";
            }
            else
            {//varchar,text
                int length = Ints.toInt(values[1]);
                if (length <= 4000)
                    return "varchar("+values[1]+")";
                else
                    return "text";
            }
            
            
        }
        //二进制字节数组
        case Z_ORM_BINARY_INT:  return "bytea";
        default:throw Asserts.exception("传入的类型[%s]不支持", type);
        }
    }
    
    @Override
    public String toColumnString(_TableField field)
    {
        String column = field.getColumn();
        String columnType = toColumnType(field.getTypeAndLength());
        boolean notNull = field.isNotNull();
        
        //PARENT_CODE varchar(32) NOT NULL,(逗号在ZTable中处理)
        return new StringBuilder(column.toUpperCase()).append(" ").append(columnType).append(" ").append(notNull?"NOT NULL":"NULL").toString();
    }
    
    @Override
    public List<String> toTableString(_Table _table, MapSS replaceMap)
    {
        String key = _table.getKey();
        
        String tableReplace = Sqls.formatReplaceMap(_table.getTable(), replaceMap);
        List<String> sqlList = new ArrayList<>();
        
        //1.表头
        StringBuilder strb = new StringBuilder();
        strb.append("create table if not exists ").append(tableReplace.toUpperCase()).append(_BR_)
            .append("(");
        
        //2.字段 PARENT_CODE varchar2(32) NOT NULL
        for (_TableField field : _table.getFieldList())
        {
            strb.append(_BR_).append(_FOUR_).append(toColumnString(field)).append(",");
        }
        
        //3.主键primary key (ITEM_CODE)
        if (_table.getKeyArr().length == 0)
            strb.setLength(strb.length()-1);
        else
            strb.append(_BR_).append(_FOUR_).append("primary key (").append(key).append(")");
        
        //4.表尾
        strb.append(_BR_).append(");").append(_BR_);
        sqlList.add(Sqls.formatSpace(strb.toString()));
        
        
        //5.索引 create unique index IX_USER_INFO_NAME_NICK on USER_INFO(USER_NAME,USER_NICK);
        for (_TableIndex index : _table.getIndexList())
        {
            String indexNameReplace = Sqls.formatReplaceMap(index.getName(), replaceMap);
            StringBuilder strbIndex = new StringBuilder("create ").append(index.getType()).append(" index ").append(indexNameReplace)
                .append(" on ").append(tableReplace).append("(").append(index.getColumn()).append(");")
                .append(_BR_);
            
            sqlList.add(Sqls.formatSpace(strbIndex.toString()));
        }
        
        return Lists.trim(sqlList);
    }
    
    @Override
    public String toItemSQL(String fieldSQL, String tableName, String whereSQL, String orderbySQL, String groupbySQL)
    {
        return new StringBuilder()
            .append("select ").append(fieldSQL)
            .append(" from ").append(tableName).append(whereSQL).append(orderbySQL).append(groupbySQL)
            .append(" limit 1")
            .toString();
    }
    
    @Override
    public String toPageSQL(String fieldSQL, String tableName, String whereSQL, String orderbySQL, String groupbySQL, int maxNum, int pageNo)
    {
        return new StringBuilder()
            .append("select ").append(fieldSQL)
            .append(" from ").append(tableName).append(whereSQL).append(orderbySQL).append(groupbySQL)
            .append(" limit #pageSize# offset #minSize#")
            .toString();
    }
    
    @Override
    public String toPageViewSQL(StringBuilder innerTableSQL, int maxNum, int pageNo)
    {
        return new StringBuilder()
            .append("select * from ").append(innerTableSQL).append(" p_ limit #pageSize# offset #minSize#")
            .toString();
    }
    
    /**************************************************************************************************/
    //不同的数据库处理
    /**************************************************************************************************/
    
    @Override
    public void executeAlertColumnDrop(String table, String column) throws ORMException, SQLException
    {//标准SQL92 drop column
        server.sql().execute(new StringBuilder("alter table ").append(table.toUpperCase()).append(" drop column ").append(column.toUpperCase()).toString());
    }
    
    @Override
    public void executeAlertColumnName(String table, String column, String newColumn) throws ORMException, SQLException
    {//标准SQL92 rename column
        server.sql().execute(new StringBuilder("alter table ").append(table.toUpperCase()).append(" rename column ").append(column.toUpperCase()).append(" to ").append(newColumn.toUpperCase()).toString());
    }
    
    @Override
    public void executeAlertColumnType(String table, String column, String newColumnType, boolean newNotNull) throws ORMException, SQLException
    {//分成两句处理，和HSQL不同，这里是set not null和drop not null
        String sql = new StringBuilder()
            .append("alter table ").append(table).append(" alter column ").append(column).append(" type ").append(newColumnType).append(";")
            .append("alter table ").append(table).append(" alter column ").append(column).append(newNotNull?" set not null":" drop not null;")
            .toString();
        
        server.sql().execute(sql);
    }
    
    @Override
    public void executeAlertColumnInfo(String table, String column, String newColumn, String newColumnType, boolean newNotNull) throws ORMException, SQLException
    {//分成三句处理
        String sql = new StringBuilder()
            .append("alter table ").append(table.toUpperCase()).append(" rename column ").append(column.toUpperCase()).append(" to ").append(newColumn.toUpperCase()).append(";")
            .append("alter table ").append(table.toUpperCase()).append(" alter column ").append(column.toUpperCase()).append(" type ").append(newColumnType).append(";")
            .append("alter table ").append(table.toUpperCase()).append(" alter column ").append(column.toUpperCase()).append(newNotNull?" set not null":" drop not null").append(";")
            .toString();
    
        server.sql().execute(sql);
    }
    
    @Override
    public void executeAlertPrimaryKeyAdd(String table, String columns) throws SQLException
    {
        server.sql().execute("alter table "+table+" add primary key("+columns+")");
    }
    
    @Override
    public void executeAlertPrimaryKeyDrop(String table) throws SQLException
    {
        server.sql().execute("alter table "+table+" drop primary key");
    }
}
