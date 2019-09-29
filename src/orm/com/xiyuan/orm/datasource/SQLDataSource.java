package com.xiyuan.orm.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.xiyuan.core.constants.SignConstants;
import org.xiyuan.core.control.ThreadLock;
import org.xiyuan.core.control.Threadx;
import org.xiyuan.core.logging.Log;
import org.xiyuan.core.logging.LogFactory;
import org.xiyuan.core.util.Classes;
import org.xiyuan.core.util.Lists;
import org.xiyuan.core.util.Randoms;
import org.xiyuan.core.util.Threads;
import org.xiyuan.core.util.seqs.Sequence;

/**
 * 数据库配置与管理类 <br>
 *
 * @version v1.0.0 @author lgz 2016-3-21 新建与整理
 */
public class SQLDataSource extends Threadx implements DataSource, Runnable, SignConstants
{
    private static final Log log = LogFactory.getLog("database.log");
    
    private static final String JNDI = "jndi"; // JNDI
    private static final int CHK_TIME = 60 * 1000; // 每60秒检查一次线程池
    
    // 数据库基础属性
    private final String id; // 数据库服务编号
    private final Sequence sequence = new Sequence(6); // 数据库连接序号
    private final ThreadLock lock = new ThreadLock(); // 数据库连接有效时通知锁
    private final List<SQLConnection> connList; // 数据库连接池
    private final ConcurrentHashMap<SQLConnection, String> closedMap; // 数据库连接池等待关闭列表
    private final AtomicInteger activeSize; // 数据库连接活跃数
    
    // 数据库驱动配置
    private String driver; // 数据库驱动
    private String url; // 数据库URL
    private String user; // 数据库用户名
    private String pass; // 数据库密码
    private int level; // 事务隔离级别0/1/2/4/8分别表示不支持事务/脏读/不支持读未提交/再次读取相同的数据而不会失败，但虚读仍然会出现/防止脏读、不可重复的读和虚读
    
    // 数据库连接池配置
    private int minPoolSize; // 连接池最小数目
    private int maxPoolSize; // 连接池最大数目
    private long maxKeepTimeMs; // 数据库连接最大保持毫秒时间，超出则重建连接
    private long maxIdleTimeMs; // 数据库连接最大空闲毫秒时间，超出则重建连接
    
    // 数据库连接检查配置
    private boolean isChkConnOnTimer; // 是否定时检查连接有效性
    private boolean isGetEffective; // 是否在获取连接时检查连接有效性
    
    // 数据库连接耗尽策略配置
    private int outOfConnWaitTimeMs; // 数据库连接耗尽时，最大等待时长，单位毫秒，建议5000毫秒
    private int outOfConnRetryCount; // 数据库连接耗尽时，重试次数，建议重试1次
    
    // 数据库状态属性
    private boolean isDbBreak; // 数据库是否已断开
    private ConnectionTester tester; // 数据库连接测试类
    
    /**
     * 默认参数构造函数，默认空闲时间=保持时间，最大100001次调用，定时检查连接有效性，获取和释放时检查有效性
     * 
     * @param id                            数据库服务编号
     * @param driver                        数据库驱动
     * @param url                           数据库URL
     * @param user                          数据库用户名
     * @param pass                          数据库密码
     * @param minPoolSize                   连接池最小数目
     * @param maxPoolSize                   连接池最大数目
     * @param maxKeepTime                   数据库连接最大保持时长，超出则重建连接，单位:秒
     */
    public SQLDataSource(String id, String driver, String url, String user, String pass, int minPoolSize, int maxPoolSize, int maxKeepTime)
    {
        this(id, driver, url, user, pass, -1, minPoolSize, maxPoolSize, maxKeepTime, maxKeepTime, true, false, false, 5, 1);
    }
    
    /**
     * 全参数构造函数
     * 
     * @param id                            数据库服务编号
     * @param driver                        数据库驱动
     * @param url                           数据库URL
     * @param user                          数据库用户名
     * @param pass                          数据库密码
     * @param level                         数据库连接事务隔离级别
     * @param minPoolSize                   连接池最小数目
     * @param maxPoolSize                   连接池最大数目
     * @param maxKeepTime                   数据库连接最大保持时长，超出则重建连接，单位:秒
     * @param maxIdleTime                   数据库连接最大空闲时长，超出则重建连接，单位:秒
     * @param isChkConnOnTimer              是否定时检查连接有效性
     * @param isGetEffective                是否在获取连接时检查连接有效性
     * @param outOfConnWaitTime             数据库连接耗尽时，最大等待时长，单位秒
     * @param outOfConnRetryCount           数据库连接耗尽时，重试次数，建议重试1次
     */
    public SQLDataSource(String id, String driver, String url, String user, String pass, int level, int minPoolSize, int maxPoolSize, int maxKeepTime, int maxIdleTime,
            boolean isChkConnOnTimer, boolean isGetEffective, boolean isChkConnOnRelease, int outOfConnWaitTime, int outOfConnRetryCount)
    {
        // 配置
        this.id = id != null ? id : Randoms.lettersDigits(2);
        this.connList = new LinkedList<>();
        this.closedMap = new ConcurrentHashMap<>();
        this.activeSize = new AtomicInteger(0);
        
        // 基础参数
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.level = level;
        
        // 连接池/处理参数
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.maxKeepTimeMs = maxKeepTime * 1000;
        this.maxIdleTimeMs = maxIdleTime * 1000;
        this.isChkConnOnTimer = isChkConnOnTimer;
        this.isGetEffective = isGetEffective;
        this.outOfConnWaitTimeMs = outOfConnWaitTime * 1000;
        this.outOfConnRetryCount = outOfConnRetryCount;
    }
    
    /** 数据库信息 */
    public String toString()
    {
        return new StringBuilder("数据库[")
                .append("id:").append(id).append(",")
                .append("min:").append(minPoolSize).append(",")
                .append("max:").append(maxPoolSize).append(",")
                .append("cur:").append(getConnSize()).append(",")
                .append("active:").append(activeSize.get()).append(",")
                .append("mKeep:").append(maxKeepTimeMs / 1000).append(",")
                .append("mIdle:").append(maxIdleTimeMs / 1000).append(",")
                .append("isTimer:").append(isChkConnOnTimer).append(",")
                .append("isGetEffective:").append(isGetEffective).append(",")
                .append("outWait:").append(outOfConnWaitTimeMs / 1000).append(",")
                .append("outRetry:").append(outOfConnRetryCount).append("]")
                .toString();
    }
    
    /**********************************************************************************/
    // 数据库服务线程开启&关闭&运行
    /**********************************************************************************/
    
    @Override
    /** 线程名 */
    protected String getThreadName()
    {
        return "ZDataSource-" + id;
    }
    
    @Override
    /** 线程开启前 */
    protected boolean startBefore()
    {
        if (!createConnectionTester())
        {// 创建连接测试者
            return false;
        }
        
        // 2.先删除已有的连接
        deleteConnections();
        
        // 3.创建新的连接
        return createConnections("初始化");
    }
    
    @Override
    /** 线程关闭后 */
    protected void closeAfter()
    {
        deleteConnections();
        tester.shutdown();
        
        log.info("数据库[%s]监视线程退出", id);
    }
    
    @Override
    /** 线程首次运行 */
    protected void first()
    {
        Threads.sleepIgnoreException(CHK_TIME);
    }
    
    @Override
    /** 线程持续运行 */
    protected void loop()
    {
        // 1.打印数据库日记信息
        log.info(toString());
        
        // 2.关闭等待关闭的连接
        for (Iterator<SQLConnection> it = closedMap.keySet().iterator(); it.hasNext();)
        {
            SQLConnection conn = it.next();
            conn.shutdown();
            it.remove();
            log.info("监视关闭数据库等待关闭的连接[%s][%s]成功", id, conn.getId());
        }
        
        // 3.检查数据库是否断开
        isDbBreak = tester.isDbBreak();
        if (isDbBreak)
        {// 如果数据库断开,则关闭所有连接
            deleteConnections();
            return;
        }
        
        // 4.检查所有连接，关闭需要关闭的连接（已关闭、超出时长和连接不可用）
        List<SQLConnection> list = Lists.copy(connList, true);
        for (SQLConnection conn : list)
        {
            // 活动中的连接不检查
            if (conn.isActive())
                continue;
            
            if (conn.isClosed() || conn.isKeepOrIdleTimeout() || (isChkConnOnTimer && !conn.isConnectionAvailable()))
            {// 已关闭、已超出时长或数目、和要求定时检查连接连接不可用时，三种情况下销毁连接并从队列移除
                synchronized (connList)
                {
                    connList.remove(conn);
                }
                
                String connId = conn.getId();
                conn.shutdown();
                conn = null;
                
                log.info("监视关闭数据库连接[%s][%s]成功", id, connId);
            }
        }
        // 5.补足最小连接数
        createConnections("补足");
        // 6.等待60秒进入下次循环
        Threads.sleepIgnoreException(CHK_TIME);
    }
    
    /**创建测试连接*/
    private boolean createConnectionTester()
    {
        if (tester != null)
        {// 如果已有测试器，先关闭
            tester.shutdown();
            tester = null;
        }
        
        try
        {
            if (!JNDI.equalsIgnoreCase(driver))
            {// 判断驱动类是否能加载到
                Class<?> cls = Classes.forName(driver);
                if (cls == null)
                {
                    log.error("数据库[%s]初始化失败，未找到驱动类[%s]", id, driver);
                    return false;
                }
            }
            
            tester = new ConnectionTester(this);
            if (tester.isDbBreak())
            {
                log.error("数据库[%s]初始化失败，连接断开", id);
                return false;
            }
        }
        catch (Exception e)
        {
            log.error("数据库[%s]初始化失败，%s", id, e.getMessage());
            return false;
        }
        
        return true;
    }
    
    /** 从池里获取一个连接，当连接耗尽时重复等待多少次 */
    public Connection getConnection() throws SQLException
    {
        return getConnection(0);
    }
    
    /** 从池里获取一个连接 */
    private Connection getConnection(int times) throws SQLException
    {
        // 第一步，先看是否能从连接池找到或新建
        synchronized (connList)
        {
            // 1.1循环寻找一个有效的空闲连接，并对无效连接进行处理
            for (Iterator<SQLConnection> it = connList.iterator(); it.hasNext();)
            {
                SQLConnection conn = it.next();
                // 1.1.1 先判断是否活动中，活动中继续找下一个
                if (conn.isActive())
                    continue;
                
                if (conn.isClosed() || conn.isKeepOrIdleTimeout() || (isGetEffective && !conn.isConnectionAvailable()))
                {// 1.1.2 已关闭、已超出时长或数目、和要求获取时检查连接连接不可用时，三种情况下销毁连接并从队列移除
                    closedMap.put(conn, _EMPTY_);
                    it.remove();
                    continue;
                }
                
                // 1.1.3 找到一个空闲并可用的连接即返回
                return conn.active();
            }
            
            // 1.2如果没有空闲连接则检查是否达到最达连接数，没到最大值新建一个并返回
            if (connList.size() < maxPoolSize)
            {
                SQLConnection conn = newConnection("获取");
                if (conn != null)
                {
                    connList.add(conn);
                    return conn.active();
                }
            }
        }
        
        // 第二步，如果没有空闲且连接达到最大数，则线程进入等待状态5秒，等待别的线程处理完释放连接
        lock.lock(outOfConnWaitTimeMs);
        
        // 第三步，别的线程有释放或时间到达，检查是否有效线程，有则返回
        synchronized (connList)
        {
            for (SQLConnection conn : connList)
            {
                if (conn.isActive())
                    continue;
                
                return conn.active();
            }
        }
        
        // 第四步，最后还是没有获取到连接则抛出异常，防止线程锁死在等待数据库连接上，导致发现不了问题
        String fatal = "数据库连接[" + id + "]连接耗尽，[max:" + getMaxPoolSize() + ",cur:" + getConnSize() + ",active:" + activeSize.get() + "]";
        if (times < outOfConnRetryCount)
        {
            log.fatal("%s[第%s]", fatal, times + 1);
            return getConnection(times + 1);
        }
        else
        {
            log.fatal("%s[第%s][全部失败抛出异常到业务层]", fatal, times + 1);
            throw new SQLException("数据库连接耗尽，请与管理员联系检查数据库是否正常工作和连接池配置是否足够!");
        }
    }
    
    /** 释放一个连接到池内 */
    public void release(SQLConnection conn)
    {
        if (conn == null)
            return;
        
        if (conn.isClosed() || conn.isKeepOrIdleTimeout() || !conn.isConnectionAvailable())
        {// 已关闭、已超出时长或数目、和要求释放时检查连接连接不可用时，三种情况下销毁连接并从队列移除
            String connId = conn.getId();
            conn.shutdown();
            synchronized (connList)
            {
                connList.remove(conn);
            }
            conn = null;
            log.info("释放关闭数据库连接[%s][%s]成功", id, connId);
            return;
        }
        // 连接没问题即设置为空闲并通知
        conn.idle();
        lock.unlock();
    }
    
    /** 创建所有连接 */
    private boolean createConnections(String type)
    {
        int num = minPoolSize - connList.size();
        for (int i = 0; i < num; i++)
        {
            SQLConnection conn = newConnection(type);
            if (conn == null)
                return false;
            
            synchronized (connList)
            {
                connList.add(conn);
            }
        }
        
        if (num > 0)
        {// 有创建连接的解锁通知
            lock.unlock();
        }
        
        return true;
    }
    
    /** 删除所有连接 */
    private void deleteConnections()
    {
        synchronized (connList)
        {
            for (Iterator<SQLConnection> it = connList.iterator(); it.hasNext();)
            {
                SQLConnection conn = it.next();
                conn.shutdown();
                it.remove();
                log.info("销毁关闭数据库连接[%s][%s]成功", id, conn.getId());
                conn = null;
            }
        }
    }
    
    /** 获取一个新的连接 */
    protected SQLConnection newConnection(String desc)
    {
        try
        {
            Connection conn = null;
            if (JNDI.equalsIgnoreCase(driver))
            {// JNDI
                DataSource dataSource = (DataSource) new InitialContext().lookup(url);
                if (dataSource == null)
                    return null;
                
                dataSource.setLoginTimeout(10);
                conn = dataSource.getConnection();
            }
            else
            {// JDBC
                DriverManager.setLoginTimeout(10);
                conn = DriverManager.getConnection(url, user, pass);
            }
            
            if (conn == null)
            {
                log.error("创建数据库连接[%s]失败，[%s]", id, desc);
                return null;
            }
            
            SQLConnection connection = new SQLConnection(this, conn);
            if (level == 0 || level == 1 || level == 2 || level == 4 || level == 8)
            {// 事务隔离等级，分5级，分别是0/1/2/4/8，越大越严格，其中0表示不支持事务，默认取决于数据库
                connection.setTransactionIsolation(level);
            }
            
            log.info("创建数据库连接[%s][%s]成功，[%s]", id, connection.getId(), desc);
            return connection;
        }
        catch (SQLException e)
        {
            log.error("创建数据库连接[%s]时，发生异常：[%s]", id, e.getMessage());
            return null;
        }
        catch (Exception e)
        {
            log.error("创建数据库连接[%s]时，发生异常：[%s]", id, e.getMessage());
            return null;
        }
    }
    
    /**设置活跃数+1*/
    void active()
    {
        activeSize.getAndIncrement();
    }
    
    /**设置活跃数-1*/
    void idle()
    {
        activeSize.getAndDecrement();
    }
    
    /***********************************/
    // DataSource 参数信息，密码不支持获取
    /***********************************/
    
    /** 获取当前连接数 */
    public int getConnSize()
    {
        return connList.size();
    }
    
    /** 获取当前连接活跃数 */
    public int getConnActiveSize()
    {
        return activeSize.get();
    }
    
    /** 判断数据库是否断开 */
    public boolean isDbBreak()
    {
        return tester.isDbBreak();
    }
    
    /** 下一个连接序号 */
    public String nextSequence()
    {
        return sequence.nextString();
    }
    
    public String getId()
    {
        return id;
    }
    
    public String getDriver()
    {
        return driver;
    }
    
    public String getUrl()
    {
        return url;
    }
    
    public String getUser()
    {
        return user;
    }
    
    public int getMinPoolSize()
    {
        return minPoolSize;
    }
    
    public int getMaxPoolSize()
    {
        return maxPoolSize;
    }
    
    public long getMaxKeepTimeMs()
    {
        return maxKeepTimeMs;
    }
    
    public long getMaxIdleTimeMs()
    {
        return maxIdleTimeMs;
    }
    
    public boolean isChkConnOnTimer()
    {
        return isChkConnOnTimer;
    }
    
    public boolean isGetEffective()
    {
        return isGetEffective;
    }
    
    
    /***********************************/
    // DataSource 支持动态修改的参数
    /***********************************/
    
    public void setMinPoolSize(int minPoolSize)
    {
        this.minPoolSize = minPoolSize;
    }
    
    public void setMaxPoolSize(int maxPoolSize)
    {
        this.maxPoolSize = maxPoolSize;
    }
    
    public void setMaxKeepTime(long maxKeepTime)
    {
        this.maxKeepTimeMs = maxKeepTime * 1000;
    }
    
    public void setMaxIdleTime(long maxIdleTime)
    {
        this.maxIdleTimeMs = maxIdleTime * 1000;
    }
    
    public void setChkConnOnTimer(boolean isChkConnOnTimer)
    {
        this.isChkConnOnTimer = isChkConnOnTimer;
    }
    
    public void setGetEffective(boolean isGetEffective)
    {
        this.isGetEffective = isGetEffective;
    }
    
    /***********************************/
    // DataSource要求实现的方法，但没用上
    /***********************************/
    
    public Connection getConnection(String username, String password) throws SQLException
    {
        throw new SQLException("不支持传入用户名和密码获取连接");
    }
    
    public PrintWriter getLogWriter() throws SQLException
    {
        return DriverManager.getLogWriter();
    }
    
    public int getLoginTimeout() throws SQLException
    {
        return DriverManager.getLoginTimeout();
    }
    
    public void setLogWriter(PrintWriter out) throws SQLException
    {
        DriverManager.setLogWriter(out);
    }
    
    public void setLoginTimeout(int seconds) throws SQLException
    {
        DriverManager.setLoginTimeout(seconds);
    }
    
    /***********************************/
    // JDK1.6增加
    /***********************************/
    
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }
    
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return null;
    }
    
    /***********************************/
    // JDK1.7增加
    /***********************************/
    
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return null;
    }
}
