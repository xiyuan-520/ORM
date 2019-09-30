package com.xiyuan.orm.dbimpl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xiyuan.core.util.Arrays;
import org.xiyuan.core.util.Asserts;
import org.xiyuan.core.util.Ints;
import org.xiyuan.core.util.Lists;
import org.xiyuan.core.util.Randoms;
import org.xiyuan.core.util.Sqls;
import org.xiyuan.core.util.Strings;
import org.xiyuan.core.util.Validates;

import com.xiyuan.orm.ORMException;
import com.xiyuan.orm.ORMPolicy;
import com.xiyuan.orm.ORMServer;
import com.xiyuan.orm.ORMType;

/**
 * SQLite数据库策略
 *
 * @version v1.0.0 @author lgz 2016-9-18 新建与整理
 */
public class _SQLite implements ORMPolicy
{
    private ORMServer server;
    
    public _SQLite(ORMServer server)
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
        return Strings.format(T_EXISTS_SQLITE, tableName);
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
            .append(notNull?" not null default 0":" default null")
            .toString();
    }
    
    @Override
    public String toColumnType(String type)
    {
        int ctype = ORMType.getColumnTypeMaybeLength(type);
        switch (ctype)
        {
        //基本类型
        case Z_ORM_BOOLEAN_INT:
        case Z_ORM_BYTE_INT:
        case Z_ORM_SHORT_INT:
        case Z_ORM_INT_INT:
        case Z_ORM_LONG_INT:        return "integer";
        //小数和时间
        case Z_ORM_DATETIME_INT:    return "datetime";
        case Z_ORM_DECIMAL_INT:
        {
            String[] values = Arrays.toStringArray(type);
            Asserts.asserts(values.length == 3, ORMI18n.ormFieldTypeIncorrect1, type);
            Asserts.asserts(Validates.isIntegerPositive(values[1]), ORMI18n.ormFieldTypeIncorrect1, type);
            Asserts.asserts(Validates.isIntegerPositive(values[2]), ORMI18n.ormFieldTypeIncorrect1, type);
            //decimal(15,3)前面11位整数相当于int，中间点号，后面3位小数的表示方式
            return "numeric("+values[1]+","+values[2]+")";
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
        case Z_ORM_BINARY_INT:  return "blob";
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
        strb.append("create table if not exists ").append(tableReplace).append(_BR_)
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
    {//1.先新建临时表，2.拷贝数据到临时表，3.删除原表并修改临时表名为原表名
        table = table.toUpperCase();
        List<LinkedMapSO> list = getTableColumnList(table);
        
        column = column.toUpperCase();
        boolean exists = false;
        StringBuilder oldColumns = new StringBuilder();
        StringBuilder tempTableKeys = new StringBuilder();
        
        for (Iterator<LinkedMapSO> it=list.iterator();it.hasNext();)
        {
            LinkedMapSO map = it.next();
            String columnName = (String)map.get("name");
            if (columnName.equalsIgnoreCase(column))
            {//找到要删除的列名
                it.remove();
                exists = true;
            }
            else
            {
                oldColumns.append(columnName).append(",");
                
                if ((Integer)map.get("pk") == 1)
                    tempTableKeys.append(columnName).append(",");
            }
        }
        
        if (!exists)
            throw new ORMException("表[%s]没有列[%s]", table, column);
        
        if (list.isEmpty())
            throw new ORMException("表[%s]只有该[%s]，不允许删除，如果要删除请删除表", table, column);
        
        //去掉最后一个逗号
        oldColumns.setLength(oldColumns.length()-1);
        if (tempTableKeys.length() > 0)
            tempTableKeys.setLength(tempTableKeys.length()-1);

        //重建表
        recreateTable("删除列", table, list, oldColumns, tempTableKeys);
    }
    
    @Override
    public void executeAlertColumnType(String table, String column, String newColumnType, boolean newNotNull)throws ORMException, SQLException
    {//1.先新建临时表，2.拷贝数据到临时表，3.删除原表并修改临时表名为原表名
        table = table.toUpperCase();
        List<LinkedMapSO> list = getTableColumnList(table);
        
        column = column.toUpperCase();
        boolean exists = false;
        StringBuilder oldColumns = new StringBuilder();
        StringBuilder tempTableKeys = new StringBuilder();
        
        for (LinkedMapSO map : list)
        {
            String columnName = (String)map.get("name");
            oldColumns.append(columnName).append(",");
            
            if (columnName.equalsIgnoreCase(column))
            {//找到要修改的列
                map.put("type", newColumnType);
                map.put("notnull", newNotNull?1:0);
                exists = true;
            }
            
            if ((Integer)map.get("pk") == 1)
                tempTableKeys.append(columnName).append(",");
        }
        
        if (!exists)
            throw Asserts.exception("表[%s]没有列[%s]", table, column);
        
        //去掉最后一个逗号
        oldColumns.setLength(oldColumns.length()-1);
        if (tempTableKeys.length() > 0)
            tempTableKeys.setLength(tempTableKeys.length()-1);
        
        //重建表
        recreateTable("修改列类型", table, list, oldColumns, tempTableKeys);
    }
    
    @Override
    public void executeAlertColumnName(String table, String column, String newColumn) throws ORMException, SQLException
    {//1.先新建临时表，2.拷贝数据到临时表，3.删除原表并修改临时表名为原表名
        table = table.toUpperCase();
        List<LinkedMapSO> list = getTableColumnList(table);
        
        column = column.toUpperCase();
        boolean exists = false;
        StringBuilder oldColumns = new StringBuilder();
        StringBuilder tempTableKeys = new StringBuilder();
        
        for (LinkedMapSO map : list)
        {
            String columnName = (String)map.get("name");
            oldColumns.append(columnName).append(",");
            
            if (columnName.equalsIgnoreCase(column))
            {//找到要修改的列名
                columnName = newColumn;
                map.put("name", newColumn);
                exists = true;
            }
            
            if ((Integer)map.get("pk") == 1)
                tempTableKeys.append(columnName).append(",");
        }
        
        if (!exists)
            throw new ORMException("表[%s]没有列[%s]", table, column);
        
        //去掉最后一个逗号
        oldColumns.setLength(oldColumns.length()-1);
        if (tempTableKeys.length() > 0)
            tempTableKeys.setLength(tempTableKeys.length()-1);

        //重建表
        recreateTable("修改列名", table, list, oldColumns, tempTableKeys);
    }
    
    @Override
    public void executeAlertColumnInfo(String table, String column, String newColumn, String newColumnType, boolean newNotNull) throws ORMException, SQLException
    {//1.先新建临时表，2.拷贝数据到临时表，3.删除原表并修改临时表名为原表名
        table = table.toUpperCase();
        List<LinkedMapSO> list = getTableColumnList(table);
        
        column = column.toUpperCase();
        boolean exists = false;
        StringBuilder oldColumns = new StringBuilder();
        StringBuilder tempTableKeys = new StringBuilder();
        
        for (LinkedMapSO map : list)
        {
            String columnName = (String)map.get("name");
            oldColumns.append(columnName).append(",");
            
            if (columnName.equalsIgnoreCase(column))
            {//找到要修改的列名
                columnName = newColumn;
                map.put("name", newColumn);
                map.put("type", newColumnType);
                map.put("notnull", newNotNull?1:0);
                exists = true;
            }
            
            if ((Integer)map.get("pk") == 1)
                tempTableKeys.append(columnName).append(",");
        }
        
        if (!exists)
            throw new ORMException("表[%s]没有列[%s]", table, column);
        
        //去掉最后一个逗号
        oldColumns.setLength(oldColumns.length()-1);
        tempTableKeys.setLength(tempTableKeys.length()-1);
        if (tempTableKeys.length() > 0)
            tempTableKeys.setLength(tempTableKeys.length()-1);
        
        //重建表
        recreateTable("修改列信息", table, list, oldColumns, tempTableKeys);
    }
    
    @Override
    public void executeAlertPrimaryKeyAdd(String table, String columns) throws ORMException, SQLException
    {
        table = table.toUpperCase();
        List<LinkedMapSO> list = getTableColumnList(table);
        
        StringBuilder oldColumns = new StringBuilder();
        for (LinkedMapSO map : list)
        {
            String columnName = (String)map.get("name");
            oldColumns.append(columnName).append(",");
        }
        oldColumns.setLength(oldColumns.length()-1);
        
        StringBuilder tempTableKeys = new StringBuilder(columns);
        
        //重建表
        recreateTable("修改主键", table, list, oldColumns, tempTableKeys);
    }
    
    @Override
    public void executeAlertPrimaryKeyDrop(String table) throws ORMException, SQLException
    {
        table = table.toUpperCase();
        List<LinkedMapSO> list = getTableColumnList(table);
        
        StringBuilder oldColumns = new StringBuilder();
        for (LinkedMapSO map : list)
        {
            String columnName = (String)map.get("name");
            oldColumns.append(columnName).append(",");
        }
        oldColumns.setLength(oldColumns.length()-1);
        
        StringBuilder tempTableKeys = new StringBuilder();
        
        //重建表
        recreateTable("修改主键", table, list, oldColumns, tempTableKeys);
    }
    
    /**************************************************************************************************/
    //私有方法
    /**************************************************************************************************/
    
    /** 获取列表 */
    private List<LinkedMapSO> getTableColumnList(String table) throws ORMException, SQLException
    {
        List<LinkedMapSO> list = server.sql().executeQuery("pragma table_info ('"+table+"')");
        if (list.isEmpty())
            throw new ORMException("表[%s]不存在或没有任何列", table);
        
        return list;
    }
    
    /** 重建表 */
    private void recreateTable(String intro, String table, List<LinkedMapSO> list, StringBuilder oldColumns, StringBuilder tempTableKeys) throws ORMException, SQLException
    {
        String tempTable = table + "_TEMP_" + Randoms.upperLetters(4);
        String createSQL = toTableSQL(tempTable, tempTableKeys.toString(), list);
        String insertSQL = "insert into " + tempTable + " select " + oldColumns.toString() + " from " + table + ";";
        String dropSQL = "drop table " + table + ";";
        String renameSQL = "alter table " + tempTable + " rename to " + table + ";";
            
        try
        {//四条语句一起执行
            List<String> sqlList = new ArrayList<>(4);
            sqlList.add(createSQL);
            sqlList.add(insertSQL);
            sqlList.add(dropSQL);
            sqlList.add(renameSQL);
            
            server.sql().execute(sqlList);
        }
        catch(Exception e)
        {//失败尝试删除临时表
            server.sql().execute("drop table if exists " + tempTable);
            throw new ORMException("表[%s][%s]时异常", e, table, intro);
        }
    }
    
    /** 内部创建表 */
    private String toTableSQL(String table, String key, List<LinkedMapSO> list)
    {
        //1.表头
        StringBuilder strb = new StringBuilder();
        strb.append("create table ").append(table.toUpperCase()).append(_BR_)
            .append("(");
        
        //2.字段 PARENT_CODE varchar2(32) NOT NULL
        for (LinkedMapSO map : list)
        {
            String column = (String)map.get("name");
            String columnType = (String)map.get("type");
            boolean notNull = (Integer)map.get("notnull") == 1;
            strb.append(_BR_).append(_FOUR_).append(column.toUpperCase()).append(" ").append(columnType).append(" ").append(notNull?"NOT NULL":"NULL").append(",");
        }
        
        //3.主键primary key (ITEM_CODE)
        if (Validates.isEmpty(key))
            strb.setLength(strb.length()-1);
        else
            strb.append(_BR_).append(_FOUR_).append("primary key (").append(key.toUpperCase()).append(")");
        
        //4.表尾
        strb.append(_BR_).append(");").append(_BR_);
        
        return strb.toString();
    }

}
