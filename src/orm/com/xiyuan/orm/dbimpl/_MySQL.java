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
 * 
 * create table if not exists TEST_STRING (
 *  T_ID bigint(20) NOT NULL,
 *  T_STRING varchar(50) NULL,
 *  T_DATE date NULL,
 *  T_TIME time NULL,
 *  T_DATETIME datetime NULL,
 *  primary key (T_ID)
 * )engine=InnoDB default charset=utf8 collate=utf8_general_ci;
 *
 * MySQL数据库策略
 * 
 * 1.datetime类型对应java.sql.Timestamp，但没有毫微秒，格式如：'1000-01-01 00:00:00' 到 '9999-12-31 23:59:59'
 *
 * @version v1.0.0 @author lgz 2016-9-18 新建与整理
 */
public class _MySQL implements ORMPolicy
{
    private ORMServer server;
    
    public _MySQL(ORMServer server)
    {
        this.server = server;
    }
    
    @Override
    public boolean chkKeepTime(int maxKeepTime)
    {
        try
        {
            List<HashMapSO> list = server.sql().executeQuery("show session variables like 'wait_timeout';", HashMapSO.class);
            for (HashMapSO item : list)
            {
                if (item.containsKey("Value"))
                {
                    int timeout = Ints.toInt((String)item.get("Value"));
                    return timeout > maxKeepTime + 180;//大于3分钟
                }
            }
        }
        catch (ORMException | SQLException e)
        {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toExistsSQL(String databaseName, String tableName)
    {
        return Strings.format(T_EXISTS_MYSQL, databaseName, tableName);
    }
    
    @Override
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
        case Z_ORM_BOOLEAN_INT:     return "tinyint(1)";
        case Z_ORM_BYTE_INT:        return "tinyint(4)";
        case Z_ORM_SHORT_INT:       return "smallint(6)";
        case Z_ORM_INT_INT:         return "int";
        case Z_ORM_LONG_INT:        return "bigint";
        //小数和时间
        case Z_ORM_DATETIME_INT:    return "datetime";
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
                if (length <= 2000)
                    return "varchar("+values[1]+")";
                else if (length <= 20000)
                    return "text";
                else if (length <= 5000000)
                    return "mediumtext";
                else
                    return "longtext";
            }
            
            
        }
        //二进制字节数组
        case Z_ORM_BINARY_INT:  return "mediumblob";
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
        String type = _table.getType();
        String key = _table.getKey();
        
        String tableReplace = Sqls.formatReplaceMap(_table.getTable(), replaceMap);
        List<String> sqlList = new ArrayList<>();
        
        //1.表头
        StringBuilder strb = new StringBuilder();
        strb.append("create table if not exists ").append(tableReplace.toUpperCase()).append(_BR_)
            .append("(");
        
        //2.字段 PARENT_CODE varchar(32) NOT NULL
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
        strb.append(_BR_).append(")engine=").append(Validates.isEmptyBlank(type)?"InnoDB":type).append(" default charset=utf8 collate=utf8_general_ci;").append(_BR_);
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
            .append(" limit #minSize#, #pageSize#")
            .toString();
    }

    @Override
    public String toPageViewSQL(StringBuilder innerTableSQL, int maxNum, int pageNo)
    {
        return new StringBuilder()
            .append("select * from ").append(innerTableSQL).append(" p_ limit #minSize#, #pageSize#")
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
    {//采用系统存储过程sp_rename
        List<LinkedMapSO> result = server.sql().executeQuery(T_COLUMN_MYSQL, LinkedMapSO.class, new String[]{server.getDatabaseName(), table, column});
        if (result.isEmpty())
            throw new ORMException("ZSQL[alterColumnRename][未找到表%s列%s]", table, column);
        
        LinkedMapSO item = result.get(0);
        String columnType = (String)item.get("COLUMN_TYPE");
        boolean notNull = "NO".equals(item.get("IS_NULLABLE"));
        
        //执行change
        server.sql().execute(new StringBuilder()
            .append("alter table ").append(table.toUpperCase())
            .append(" change ").append(column.toUpperCase()).append(" ").append(newColumn.toUpperCase()).append(" ").append(columnType)
            .append(notNull?" not null":" default null")
            .toString());
    }
    
    @Override
    public void executeAlertColumnType(String table, String column, String newColumnType, boolean newNotNull) throws ORMException, SQLException
    {
        String sql = new StringBuilder()
            .append("alter table ").append(table.toUpperCase())
            .append(" modify column ").append(column.toUpperCase()).append(" ").append(newColumnType)
            .append(newNotNull?" not null":" default null")
            .toString();
        
        server.sql().execute(sql);
    }
    
    @Override
    public void executeAlertColumnInfo(String table, String column, String newColumn, String newColumnType, boolean newNotNull) throws ORMException, SQLException
    {//MySQL有change支持同时修改列名和类型
        String sql = new StringBuilder()
            .append("alter table ").append(table.toUpperCase())
            .append(" change ").append(column.toUpperCase()).append(" ").append(newColumn.toUpperCase()).append(" ").append(newColumnType)
            .append(newNotNull?" not null":" default null")
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
