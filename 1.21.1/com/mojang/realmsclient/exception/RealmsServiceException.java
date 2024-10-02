package com.mojang.realmsclient.exception;

import com.mojang.realmsclient.client.RealmsError;

public class RealmsServiceException extends Exception
{
    public final RealmsError realmsError;

    public RealmsServiceException(RealmsError p_299387_)
    {
        this.realmsError = p_299387_;
    }

    @Override
    public String getMessage()
    {
        return this.realmsError.logMessage();
    }
}
