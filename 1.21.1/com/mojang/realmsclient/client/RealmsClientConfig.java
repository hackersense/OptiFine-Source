package com.mojang.realmsclient.client;

import java.net.Proxy;
import javax.annotation.Nullable;

public class RealmsClientConfig
{
    @Nullable
    private static Proxy proxy;

    @Nullable
    public static Proxy getProxy()
    {
        return proxy;
    }

    public static void setProxy(Proxy p_87294_)
    {
        if (proxy == null)
        {
            proxy = p_87294_;
        }
    }
}
