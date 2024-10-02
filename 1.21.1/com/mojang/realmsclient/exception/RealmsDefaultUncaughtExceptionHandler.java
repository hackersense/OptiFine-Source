package com.mojang.realmsclient.exception;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;

public class RealmsDefaultUncaughtExceptionHandler implements UncaughtExceptionHandler
{
    private final Logger logger;

    public RealmsDefaultUncaughtExceptionHandler(Logger p_202332_)
    {
        this.logger = p_202332_;
    }

    @Override
    public void uncaughtException(Thread p_87768_, Throwable p_87769_)
    {
        this.logger.error("Caught previously unhandled exception", p_87769_);
    }
}
