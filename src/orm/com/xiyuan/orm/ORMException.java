package com.xiyuan.orm;

import org.xiyuan.core.util.Strings;

/**
 * ORM异常定义
 *
 * @version v1.0.0 @author lgz 2019-9-29 新建与整理
 */
public class ORMException extends Exception
{
    private static final long serialVersionUID = 1L;

    public ORMException()
    {
        super();
    }
    
    public ORMException(String message)
    {
        super(message);
    }
    
    public ORMException(String message, Object...arguments)
    {
        super(Strings.format(message, arguments));
    }
    
    public ORMException(String message, Throwable e)
    {
        super(message, e);
    }
    
    public ORMException(Throwable e)
    {
        super(e);
    }
    
    public ORMException(String message, Throwable e, Object...arguments)
    {
        super(Strings.format(message, arguments), e);
    }
}
