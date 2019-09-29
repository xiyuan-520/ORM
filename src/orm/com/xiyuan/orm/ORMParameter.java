package com.xiyuan.orm;

import java.io.File;

import org.xiyuan.core.Global;
import org.xiyuan.core.config.Group;
import org.xiyuan.core.config.Item;
import org.xiyuan.core.extend.HashMapSS;
import org.xiyuan.core.extend.HashMapSV;
import org.xiyuan.core.util.Asserts;
import org.xiyuan.core.util.Resources;
import org.xiyuan.core.util.Systems;
import org.xiyuan.core.util.codes.Base64;
import org.xiyuan.core.util.codes.HEX;

/**
 * ORM服务需要配置的参数
 *
 * @version v1.0.0 @author lgz 2016-3-21 新建与整理
 */
public class ORMParameter implements ORMConstants
{
    private String id;
    
    //必填五项
    private String dbType;
    private String driver;
    private String url;
    private String user;
    private String pass;
    private int level;
    
    //常用配置三项
    private int minPoolSize = 2;
    private int maxPoolSize = 10;
    private int maxKeepTime = 7001;
    
    //连接时效两项
    private int maxIdleTime = 7001;
    
    //密码类型
    private String passType = null;

    //检查连接三项
    private boolean isChkConnOnTimer = false;
    private boolean isChkConnOnGet = false;
    private boolean isChkConnOnRelease = false;
    
    //耗尽重试两项
    private int outOfConnWaitTime = 5;
    private int outOfConnRetryCount = 1;
    
    //SQL输出日志两项
    private boolean isUpdateSqlLog = false;
    private boolean isQuerySqlLog = false;
    
    //sql文件表
    private HashMapSV<Integer> sqlMap = new HashMapSV<>();
    private HashMapSS cacheMap = new HashMapSS();
    
    /***********************************************************************************/
    //构造函数
    /***********************************************************************************/
    
    /** 空构造，然后手动设置参数 */
    public ORMParameter()
    {
    }
    
    /** 通过配置生成参数对象 */
    public ORMParameter(Group group)
    {
        this.id = group.getId();
        
        //5项必须
        this.dbType = group.getString("dbType");
        this.driver = group.getString("driver");
        this.url = group.getString("url");
        this.user = group.getString("user");
        this.pass = group.getString("pass");
        this.level = group.getInt("level", -1);
        
        //其他有默认值
        this.passType = group.getString("passType");
        this.minPoolSize = group.getInt("minPoolSize", 2);
        this.maxPoolSize = group.getInt("maxPoolSize", 10);
        
        this.maxKeepTime = group.getInt("maxKeepTime", 7001);
        this.maxIdleTime = group.getInt("maxIdleTime", this.maxKeepTime);
        this.isChkConnOnTimer = group.isTrue("isChkConnOnTimer", false);
        this.isChkConnOnGet = group.isTrue("isChkConnOnGet", false);
        this.isChkConnOnRelease = group.isTrue("isChkConnOnRelease", false);
        
        this.outOfConnWaitTime = group.getInt("outOfConnWaitTime", 5);
        this.outOfConnRetryCount = group.getInt("outOfConnRetryCount", 1);
        
        this.isUpdateSqlLog = group.isTrue("isUpdateSqlLog", false);
        this.isQuerySqlLog = group.isTrue("isQuerySqlLog", false);
        
        //SQL文件
        Group grp = Global.getGroup(group.getId()+".sql");
        if (grp != null)
        {
            for (Item item : grp.list())
            {
                addSqlConfig(item.getKey(), item.getString());
            }
        }
        
        //表缓存
        grp = Global.getGroup(group.getId()+".cache");
        if (grp != null)
        {
            for (Item item : grp.list())
            {
                addCacheConfig(item.getKey(), item.getString());
            }
        }
    }
    
    /** 初始化连接池 */
    public ZDataSource newDatabase()
    {
        //对密码支持简单加密，HEX/Base64两种
        if (_HEX_.equalsIgnoreCase(passType))
            pass = HEX.decrypt(pass);
        else if (_BASE64_.equalsIgnoreCase(passType))
            pass = Base64.decodeUTF8(pass);
            
        return new ZDataSource(id, driver, url, user, pass, level,
            minPoolSize, maxPoolSize, maxKeepTime, maxIdleTime, 
            isChkConnOnTimer, isChkConnOnGet, isChkConnOnRelease, 
            outOfConnWaitTime, outOfConnRetryCount);
    }
    
    /***********************************************************************************/
    //判断数据库类型
    /***********************************************************************************/
    
    /** 获取数据库类型 */
    public int getDatabaseType()
    {
        return ORMType.getDatabaseType(dbType).value();
    }
    
    /** 判断是否是Oracle数据库 */
    public boolean isOracle()
    {
        return getDatabaseType() == ORM_ORACLE.key();
    }
    
    /** 判断是否是Microsoft SQLServer数据库 */
    public boolean isMssql()
    {
        return getDatabaseType() == ORM_MSSQL.key();
    }
    
    /** 判断是否是SQLite数据库 */
    public boolean isSqlite()
    {
        return getDatabaseType() == ORM_SQLITE.key();
    }
    
    /** 判断是否是Hsql数据库 */
    public boolean isHsql()
    {
        return getDatabaseType() == ORM_HSQL.key();
    }
    
    /** 判断是否是MySql数据库 */
    public boolean isMysql()
    {
        return getDatabaseType() == ORM_MYSQL.key();
    }
    
    /** 获取数据库名 */
    public String getDbName()
    {
        if (isMysql())
        {
            if ("jndi".equalsIgnoreCase(driver))
                return user;//JNDI 时配置成用户
            
            String dbUrl = url;
            int ind = dbUrl.indexOf("?");
            if (ind != -1)
                dbUrl = dbUrl.substring(0, ind);
            
            int ind2 = dbUrl.lastIndexOf("/");
            return dbUrl.substring(ind2+1);
        }
        
        return null;
    }
    
    /***********************************************************************************/
    //SQL文件表
    /***********************************************************************************/
    
    public HashMapSV<Integer> getSqlConfig()
    {
        return sqlMap;
    }
    
    public void addSqlConfig(String key, String value)
    {
        int type = Z_SQL_FILE.equals(value)?Z_SQL_FILE_INT:
                   Z_SQL_FOLDER.equals(value)?Z_SQL_FOLDER_INT:
                   Z_SQL_CLASS.equals(value)?Z_SQL_CLASS_INT:
                   Z_SQL_PACKAGE.equals(value)?Z_SQL_PACKAGE_INT:-1;
        
        addSqlConfig(key, type);
    }
    
    public void addSqlConfig(String key, int type)
    {
        Asserts.notNull(key, _KEY_);
        Asserts.as((type >= 0 && type <= 3)?null:"增加ORM的SQL配置["+key+"]的类型不正确");
        
        switch (type)
        {
        case Z_SQL_FILE_INT:
        {
            Asserts.as(key.endsWith(Z_SQL_ENDSWITH)?null:"增加ORM的SQL配置["+key+"]必须以[.sql.xml]结尾的SQL配置文件名");
            
            File file = new File(key);
            Asserts.as((file.isFile() && file.canRead())?null:"增加ORM的SQL配置["+key+"]必须是文件并可读");
            break;
        }
        case Z_SQL_FOLDER_INT:
        {
            File folder = new File(key);
            Asserts.as((folder.isDirectory() && folder.canRead())?null:"增加ORM的SQL配置["+key+"]必须是文件夹并可读");
            break;
        }
        case Z_SQL_CLASS_INT:
        {
            Asserts.as(key.endsWith(Z_SQL_ENDSWITH)?null:"增加ORM的SQL配置["+key+"]必须以[.sql.xml]结尾的SQL配置文件名");
            Asserts.as(Resources.exists(ORMServer.class, key)?null:"增加ORM的SQL配置["+key+"]必须是文件并可读");
            break;
        }
        case Z_SQL_PACKAGE_INT:
        {
            Asserts.as(Resources.exists(ORMServer.class, key)?null:"增加ORM的SQL配置["+key+"]必须是存在");
            break;
        }
        }
        
        key = Systems.replacePropertyPath(key);
        sqlMap.put(key, type);
    }
    
    /***********************************************************************************/
    //Cache配置表
    /***********************************************************************************/
    
    public void addCacheConfig(String key, String value)
    {
        cacheMap.put(key, value);
    }
    
    public HashMapSS getCacheConfig()
    {
        return cacheMap;
    }
    
    /***********************************************************************************/
    //参数的设置和获取
    /***********************************************************************************/
    
    public String getDbType()
    {
        return dbType;
    }
    
    public void setDbType(String dbType)
    {
        this.dbType = dbType;
    }
    
    public boolean isUpdateSqlLog()
    {
        return isUpdateSqlLog;
    }
    
    public void setUpdateSqlLog(boolean isUpdateSqlLog)
    {
        this.isUpdateSqlLog = isUpdateSqlLog;
    }
    
    public boolean isQuerySqlLog()
    {
        return isQuerySqlLog;
    }
    
    public void setQuerySqlLog(boolean isQuerySqlLog)
    {
        this.isQuerySqlLog = isQuerySqlLog;
    }
    
    public String getDriver()
    {
        return driver;
    }
    
    public void setDriver(String driver)
    {
        this.driver = driver;
    }
    
    public String getUrl()
    {
        return url;
    }
    
    public void setUrl(String url)
    {
        this.url = url;
    }
    
    public String getUser()
    {
        return user;
    }
    
    public void setUser(String user)
    {
        this.user = user;
    }
    
    public String getPass()
    {
        return pass;
    }
    
    public void setPass(String pass)
    {
        this.pass = pass;
    }
    
    public String getPassType()
    {
        return passType;
    }
    
    public void setPassType(String passType)
    {
        this.passType = passType;
    }
    
    public int getLevel()
    {
        return level;
    }
    
    public void setLevel(int level)
    {
        this.level = level;
    }
    
    public int getMinPoolSize()
    {
        return minPoolSize;
    }
    
    public void setMinPoolSize(int minPoolSize)
    {
        this.minPoolSize = minPoolSize;
    }
    
    public int getMaxPoolSize()
    {
        return maxPoolSize;
    }
    
    public void setMaxPoolSize(int maxPoolSize)
    {
        this.maxPoolSize = maxPoolSize;
    }
    
    public int getMaxKeepTime()
    {
        return maxKeepTime;
    }
    
    public void setMaxKeepTime(int maxKeepTime)
    {
        this.maxKeepTime = maxKeepTime;
    }
    
    public int getMaxIdleTime()
    {
        return maxIdleTime;
    }
    
    public void setMaxIdleTime(int maxIdleTime)
    {
        this.maxIdleTime = maxIdleTime;
    }
    
    public boolean isChkConnOnTimer()
    {
        return isChkConnOnTimer;
    }
    
    public void setChkConnOnTimer(boolean isChkConnOnTimer)
    {
        this.isChkConnOnTimer = isChkConnOnTimer;
    }
    
    public boolean isChkConnOnGet()
    {
        return isChkConnOnGet;
    }
    
    public void setChkConnOnGet(boolean isChkConnOnGet)
    {
        this.isChkConnOnGet = isChkConnOnGet;
    }
    
    public boolean isChkConnOnRelease()
    {
        return isChkConnOnRelease;
    }
    
    public void setChkConnOnRelease(boolean isChkConnOnRelease)
    {
        this.isChkConnOnRelease = isChkConnOnRelease;
    }
    
    public int getOutOfConnWaitTime()
    {
        return outOfConnWaitTime;
    }
    
    public void setOutOfConnWaitTime(int outOfConnWaitTime)
    {
        this.outOfConnWaitTime = outOfConnWaitTime;
    }
    
    public int getOutOfConnRetryCount()
    {
        return outOfConnRetryCount;
    }
    
    public void setOutOfConnRetryCount(int outOfConnRetryCount)
    {
        this.outOfConnRetryCount = outOfConnRetryCount;
    }
}
